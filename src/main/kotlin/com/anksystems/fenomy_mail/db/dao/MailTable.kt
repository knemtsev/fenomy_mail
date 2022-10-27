package com.anksystems.fenomy_mail.db.dao

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object MailTable : Table("db.email") {
    val id = uuid("id")
    val status = text("status")
    val cdate = datetime("cdate")
    val udate = datetime("udate")
    val addressFrom = text("address_from")
    val addressTo = text("address_to")
    val subject = text("subject").nullable()
    val content = text("content").nullable()
}
