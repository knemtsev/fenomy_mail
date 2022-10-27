package com.anksystems.fenomy_mail

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("mail")
data class MyProperties @ConstructorBinding constructor(
    var poolSendMailSize: Int = 1000,
    var queueUpdateStatusSize: Int = 100_000,
    var queueSendMailSize: Int = 100_000,
    var listenerTestRestartPeriod: Long = 60_000L,
    var statusUpdatePackageSize: Int = 100,
)