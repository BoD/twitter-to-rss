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
    implementation("org.twitter4j:twitter4j-core:_")
    implementation("org.redundent:kotlin-xml-builder:_")
    runtimeOnly("ch.qos.logback:logback-classic:_")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.jraf.twittertorss.MainKt")
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = "7.3.1"
    }

    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

// Run `./gradlew refreshVersions` to update dependencies
