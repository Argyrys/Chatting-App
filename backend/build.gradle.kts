plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-websockets:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation("io.ktor:ktor-client-core:2.3.0")
    implementation("io.ktor:ktor-client-cio:2.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.0")
    implementation("ch.qos.logback:logback-classic:1.4.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.xerial:sqlite-jdbc:3.43.0.0")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("MainKt")
}
