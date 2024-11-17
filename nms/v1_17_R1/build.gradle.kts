plugins {
    id("io.papermc.paperweight.userdev")
}
dependencies {
    paperweight.paperDevBundle("1.17.1-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}