package com.anksystems.fenomy_mail.model

import com.anksystems.lib.Searchable

data class MailMessage(
    val id: String,
    val addressFrom: String,
    val addressTo: String,
    val subject: String,
    val content: String,
): Searchable<MailMessage> {
    override fun compare(other: MailMessage): Boolean = other.id==this.id
}

