package com.anksystems.fenomy_mail.model

import com.anksystems.lib.Searchable

data class MailMessageStatus(
    val id: String,
    val status: MailStatus = MailStatus.NEW,
): Searchable<MailMessageStatus> {
    override fun compare(other: MailMessageStatus): Boolean = other.id==this.id
}

