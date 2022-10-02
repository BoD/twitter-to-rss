plugins {
    kotlin("jvm")
    application
}

group = "org.jraf"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(Ktor.server.core)
    implementation(Ktor.server.netty)
    implementation(Ktor.server.defaultHeaders)
    implementation(Ktor.server.statusPages)
    implementation("org.twitter4j:twitter4j-core:_")
    implementation("org.redundent:kotlin-xml-builder:_")
    runtimeOnly("ch.qos.logback:logback-classic:_")
}

application {
    mainClass.set("org.jraf.twittertorss.MainKt")
}

// `./gradlew refreshVersions` to update dependencies
// `./gradlew distZip` to create a zip distribution
