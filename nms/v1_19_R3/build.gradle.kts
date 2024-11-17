plugins {
    id("io.papermc.paperweight.userdev")
}
dependencies {
    paperweight.paperDevBundle("1.19.4-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}