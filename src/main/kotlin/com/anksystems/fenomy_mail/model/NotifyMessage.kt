package com.anksystems.fenomy_mail.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement

@Serializable
data class NotifyMessage(
    @SerialName("id")               val id: String,
    @SerialName("cdate")            val cdate: String,
    @SerialName("address_from")     val addressFrom: String,
    @SerialName("address_to")       val addressTo: String,
    @SerialName("subject")          val subject: String,
) {
}
