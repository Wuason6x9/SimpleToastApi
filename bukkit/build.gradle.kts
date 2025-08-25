dependencies {
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
    //compileOnly("org.spigotmc:spigot-api:1.21.8-R0.1-SNAPSHOT")

    // Adventure serializers (compileOnly, no shading)
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
