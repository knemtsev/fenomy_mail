package com.anksystems.fenomy_mail.model

enum class MailStatus(val status: String) {
    NEW("new"),
    PROCESSED("processed"),
    SUBMITTED("submitted"),
    FAILED("failed")
}