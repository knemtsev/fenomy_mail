package com.anksystems.fenomy_mail

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.util.*

@EnableConfigurationProperties(MyProperties::class)
@SpringBootApplication()
@ConfigurationPropertiesScan("com.anksystems.fenomy_mail")
@Component
class FenomyMailApplication  {
    private val versionProperties = Properties()

    init {
        versionProperties.load(this.javaClass.getResourceAsStream("/version.properties"))
    }

    fun getVersion() : String = versionProperties.getProperty("version") ?: "no version"

}


fun main(args: Array<String>) {
    runApplication<FenomyMailApplication>(*args)
}
