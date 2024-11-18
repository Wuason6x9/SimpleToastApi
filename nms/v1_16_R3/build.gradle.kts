dependencies {
    compileOnly(fileTree("libs") {
        include("*.jar")
    })
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

