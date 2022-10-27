package com.anksystems.fenomy_mail.service

import com.anksystems.fenomy_mail.FenomyMailApplication
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LogService {
    companion object {
        val logger = LoggerFactory.getLogger(FenomyMailApplication::class.java);
    }

    fun i(msg: String) = logger.info(msg)
    fun d(msg: String) = logger.debug(msg)
    fun e(msg: String)  = logger.error(msg)
    fun e(e: Exception) = logger.error(e.message, e)
    fun t(msg: String) = logger.trace(msg)
    fun w(msg: String) = logger.warn(msg)
}