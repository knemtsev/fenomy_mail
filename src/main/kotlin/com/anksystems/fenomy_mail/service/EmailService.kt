package com.anksystems.fenomy_mail.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    @Autowired private val javaMailSender: JavaMailSender,
    @Value("\${spring.mail.sender.email}") private val senderEmail: String,
    @Value("\${spring.mail.sender.text}") private val senderText: String
) {

    fun sendMimeEmail(content: String) {
        val message = javaMailSender.createMimeMessage(content.byteInputStream(charset = Charsets.UTF_8))
        javaMailSender.send(message)
    }

}