package com.anksystems.fenomy_mail.service

import com.anksystems.fenomy_mail.FenomyMailApplication
import com.anksystems.fenomy_mail.MyProperties
import com.anksystems.fenomy_mail.db.dao.MailTable
import com.anksystems.lib.ext.decodeFromStringSafe
import com.anksystems.fenomy_mail.model.*
import com.anksystems.fenomy_mail.service.LogService
import com.anksystems.fenomy_mail.service.SendMessageService
import com.impossibl.postgres.api.jdbc.PGConnection
import com.impossibl.postgres.api.jdbc.PGNotificationListener
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.sql.SQLException
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*


@Service
class PGListenerService(
    @Autowired private val props: MyProperties,
    @Autowired private val env: Environment,
    @Autowired private val sendMessageService: SendMessageService,
    @Autowired private val log: LogService,
    @Autowired private val app: FenomyMailApplication
) {

    private val config by lazy {
        HikariConfig().apply {
            jdbcUrl = env.getProperty("spring.datasource.url") //props.dbUrl
            username = env.getProperty("spring.datasource.username")
            password = env.getProperty("spring.datasource.password")
            driverClassName = env.getProperty("spring.datasource.driver-class-name")
            //keepaliveTime = 60000

        }
    }

    private val ds by lazy {
        HikariDataSource(config)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val queueToUpdateStatus = sendMessageService.getQueueToUpdateStatus()

    private var lastNotificationTime: ZonedDateTime = ZonedDateTime.now()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        coerceInputValues = true
    }

    init {
        initService()
    }

    private fun initService() {
        log.i("${this.javaClass.name} start ${app.getVersion()}")

        if (areThereNewMail()) {
            serviceScope.launch {
                processNewMail()
                initListener()
            }
        } else {
            initListener()
        }
        // обновление статуса пушей
        serviceScope.launch {
            while (true) {
                val listToUpdate = mutableListOf(queueToUpdateStatus.get())
                delay(100) // Если статусы идут потоком, ждём, когда накопятся
                var count = 1
                do {
                    val next = queueToUpdateStatus.getOrNull()
                    next?.let { listToUpdate.add(next) }
                    count += 1
                } while (next != null && count < props.statusUpdatePackageSize) // обновляем пакетами
                updateStatus(listToUpdate)
            }
        }

        // переиодический перезапуск слушателя
        serviceScope.launch {
            while (true) {
                delay(props.listenerTestRestartPeriod)
                log.d("STATS: ${sendMessageService.getStats()}")
                val interval = ChronoUnit.MILLIS.between(lastNotificationTime, ZonedDateTime.now())
                if (interval > props.listenerTestRestartPeriod) {
                    if (areThereNewMail()) {
                        log.d("TEST period: $props.listenerTestRestartPeriod < $interval ms")
                        log.e("RESTART. Are there new emails, but listener did not process them. Restart listener.")
                        resetListener()
                        processNewMail()
                        initListener()
                    }
                }
            }
        }
    }

    private fun areThereNewMail() = countNewMail() > 0

    private fun countNewMail(): Long {
        var count = 0L
        try {
            Database.connect(ds)
            transaction {
                count = MailTable.select { MailTable.status.eq(MailStatus.NEW.status) }.count()
            }
        } catch (e: Exception) {
            log.e(e)
        }
        return count
    }

    private suspend fun processNewMail() {
        try {
            lateinit var listMail: List<MailMessage>
            Database.connect(ds)
            transaction {
                listMail = MailTable.select { MailTable.status.eq(MailStatus.NEW.status) }.map { row ->
                    MailMessage(
                        id = row[MailTable.id].toString(),
                        addressFrom = row[MailTable.addressFrom],
                        addressTo = row[MailTable.addressTo],
                        subject = row[MailTable.subject] ?: "",
                        content = row[MailTable.content] ?: "",
                    )
                }
            }

            listMail.forEach { mail ->
                sendMessageService.send(mail)
            }
        } catch (e: SQLException) {
            log.e(e.message.toString())
        }
    }

    var pgConn: PGConnection? = null
    var notificationListener: PGNotificationListener? = null

    private fun resetListener() {
        log.d("resetListener")
        pgConn?.let {
            try {
                log.d("resetListener $it $notificationListener")
                it.removeNotificationListener(notificationListener)
                it.close()
            } catch (e: Exception) {
                log.e(e.message.toString())
            }
        }
    }

    private fun initListener() {
        log.d("${this.javaClass.name} listener start")

        notificationListener = object : PGNotificationListener {
            override fun notification(processId: Int, channelName: String, payload: String) {
                lastNotificationTime = ZonedDateTime.now()
                //println("Received from PG: $processId, $channelName")
                log.d("Received from PG: $processId, $channelName")
                log.d("Payload: $payload")
                try {
                    val notifyMessage = json.decodeFromStringSafe<NotifyMessage>(payload)
                    if (notifyMessage != null) {
                        log.i("notifyMessage: $notifyMessage")
                        serviceScope.launch {
                            val mailMessage = getEmail(notifyMessage.id)
                            if(mailMessage!=null) {
                                log.d("Message ${mailMessage.addressTo} id=${mailMessage.id} ")
                                log.t("emailMessage: $mailMessage")
                                sendMessageService.send(mailMessage)
                            }
                            else
                            {
                                log.e("Message is NULL");
                            }
                        }
                    } else {
                        log.e("Decode json error: '$payload'")
                    }
                } catch (e: Exception) {
                    println(e.message)
                }
            }

            override fun closed() {
                log.e("Connection to Postgres lost! Try to reconnect...")
                serviceScope.launch {
                    delay(1000)
                    initListener()
                }
            }

        }

        try {
            val conn = ds.connection
            pgConn = conn.unwrap(PGConnection::class.java)

            pgConn?.addNotificationListener(notificationListener)
            pgConn?.createStatement().use { statement -> statement?.execute("LISTEN email;") }

        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
    }

    private fun getEmail(id: String): MailMessage? {
        var mail: MailMessage? = null
        try {
            Database.connect(ds)
            transaction {
                mail = MailTable.select {
                    MailTable.id.eq(UUID.fromString(id))
                }.map { row ->
                    MailMessage(
                        id = row[MailTable.id].toString(),
                        addressFrom = row[MailTable.addressFrom],
                        addressTo = row[MailTable.addressTo],
                        subject = row[MailTable.subject] ?: "",
                        content = row[MailTable.content] ?: "",
                    )
                }.firstOrNull()

            }
        } catch (e: SQLException) {
            log.e(e.message.toString())
        }
        return mail
    }

    fun updateStatus(messageStatusList: List<MailMessageStatus>) {
        messageStatusList.filter { it.status == MailStatus.SUBMITTED }.takeIf { it.isNotEmpty() }
            ?.let { setStatus(it, MailStatus.SUBMITTED.status) }
        messageStatusList.filter { it.status == MailStatus.FAILED }.takeIf { it.isNotEmpty() }
            ?.let { list ->
                setStatus(list, MailStatus.FAILED.status)
            }
    }

    fun setStatus(messageStatusList: List<MailMessageStatus>, status: String) {
        try {
            Database.connect(ds)
            transaction {
                MailTable.update({ MailTable.id.inList(messageStatusList.map { UUID.fromString(it.id) }) }) {
                    it[MailTable.status] = status
                }
            }
        } catch (e: SQLException) {
            log.e(e.message.toString())
        }
    }

}