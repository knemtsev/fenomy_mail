import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.archivesName
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileOutputStream
import java.util.Properties

plugins {
    application
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.spring") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    kotlin("kapt") version "1.7.10"
}

application {
    mainClass.set("com.anksystems.fenomy_mail.FenomyMailApplicationKt")
}

group = "com.anksystems"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    val exposedVer = "0.40.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVer")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVer")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVer")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVer")

    implementation("com.zaxxer:HikariCP:5.0.1")

    val configurationProcessor ="org.springframework.boot:spring-boot-configuration-processor:2.7.5"
    kapt("org.springframework.boot:spring-boot-configuration-processor:2.7.5")
    annotationProcessor(configurationProcessor)

    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")


    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    launchScript()
}

tasks.create("jarPath") {
    println("$archivesName-$version.jar")
}

val generatedVersionDir = "$buildDir/generated-version"
val resourceDir = "$buildDir/resources/main"
val versionProperties = "version.properties"

sourceSets {
    main {
        kotlin {
            output.dir(generatedVersionDir)
        }
    }
}

tasks.register("generateVersionProperties") {
    doLast {

//            val propertiesFile = file("$generatedVersionDir/$versionProperties")
//            propertiesFile.parentFile.mkdirs()
        val properties = Properties()
        properties.setProperty("version", "$version")
//            properties.store(FileOutputStream(propertiesFile), null)

        val resPropertiesFile = file("${resourceDir}/$versionProperties")
        resPropertiesFile.parentFile.mkdirs()
        properties.store(FileOutputStream(resPropertiesFile), null)
    }
}

tasks.named("processResources") {
    dependsOn("generateVersionProperties")
}
