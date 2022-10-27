package com.anksystems.fenomy_mail.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.InputStream
import javax.mail.Address
import javax.mail.Message
import javax.mail.internet.InternetAddress

@Service
class EmailService(
    @Autowired private val javaMailSender: JavaMailSender,
    @Value("\${spring.mail.sender.email}") private val senderEmail: String,
    @Value("\${spring.mail.sender.text}") private val senderText: String
) {

    fun sendSimpleEmail(receiver: String, subject: String, content: String) {
        val message = javaMailSender.createMimeMessage(content.byteInputStream(charset = Charsets.UTF_8))
        message.setFrom(senderEmail)
        message.setRecipient(Message.RecipientType.TO, InternetAddress(receiver))
        message.subject = subject
        //message.setText(content)
        javaMailSender.send(message)
    }

    fun sendMimeEmail(addressTo: String, subject: String, content: String) {
        val helper: MimeMessageHelper? = null

    }
}