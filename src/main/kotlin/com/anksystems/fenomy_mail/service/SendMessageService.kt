package com.anksystems.fenomy_mail.service

import com.anksystems.fenomy_mail.MyProperties
import com.anksystems.fenomy_mail.model.MailMessage
import com.anksystems.fenomy_mail.model.MailMessageStatus
import com.anksystems.fenomy_mail.model.MailStatus
import com.anksystems.lib.ConcurrentQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.mail.MessagingException

@Service
class SendMessageService(
    @Autowired private val props: MyProperties,
    @Autowired private val log: LogService,
    @Autowired private val emailService: EmailService,
) : BaseService {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val queueToSend = ConcurrentQueue<MailMessage>(props.queueSendMailSize)
    private val queueToUpdateStatus = ConcurrentQueue<MailMessageStatus>(props.queueUpdateStatusSize)

    // statistic
    private var sentCount = AtomicInteger(0)
    private var sentCountSuccess = AtomicInteger(0)
    private var sentCountFailed = AtomicInteger(0)
    private var sentTotalTimeMs = AtomicLong(0L)

    init {
        log.i("${props}")
        initService()
    }

    fun getQueueToUpdateStatus() = queueToUpdateStatus

    final override fun initService() {
        serviceScope.launch {
            do {
                try {
                    sendMail(queueToSend.get())
                } catch (e: Exception) {
                    log.e(e)
                    delay(TimeUnit.SECONDS.toMillis(30))
                }
            } while (true)
        }

    }

    override fun resetService() {
        serviceScope.cancel()
    }

    suspend fun send(message: MailMessage) {
        queueToSend.put(message)
    }

    private val mailSendingPool = Semaphore(props.poolSendMailSize, 0)

    private suspend fun sendMail(message: MailMessage) {
        log.d("mailSendingPool.availablePermits=${mailSendingPool.availablePermits}")
        mailSendingPool.acquire()
        serviceScope.launch {
            val startTime = ZonedDateTime.now()
            try {
                emailService.sendSimpleEmail(message.addressTo, message.subject, message.content)
                val result = message.addressFrom + " " + message.addressTo + " " + message.subject
                val interval = ChronoUnit.MILLIS.between(startTime, ZonedDateTime.now())
                queueToUpdateStatus.put(MailMessageStatus(message.id, MailStatus.SUBMITTED))
                log.d("[$interval ms] OK: $result")
                sentCount.incrementAndGet()
                sentCountSuccess.incrementAndGet()
                sentTotalTimeMs.addAndGet(interval)
            } catch (e: MessagingException) {
                val interval = ChronoUnit.MILLIS.between(startTime, ZonedDateTime.now())
                log.e("[$interval ms] ERROR2: ${e.message}")
            } catch (e: Exception) {
                //queueToUpdateStatus.put(MailMessageStatus(message.id, MailStatus.FAILED))
                val interval = ChronoUnit.MILLIS.between(startTime, ZonedDateTime.now())
                log.e("[$interval ms] ERROR: ${e.message}")
                sentCount.incrementAndGet()
                sentCountFailed.incrementAndGet()
                sentTotalTimeMs.addAndGet(interval)
            }
            mailSendingPool.release()
        }
    }

    fun getStats(): String {
        return "Total: ${sentCount.get()} +${sentCountSuccess.get()} -${sentCountFailed.get()} avg: ${sentTotalTimeMs.get()/(sentCount.get().takeIf { it!=0 } ?: 1)}"
    }

}