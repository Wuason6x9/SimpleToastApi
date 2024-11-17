plugins {
    id("org.gradle.maven-publish")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

tasks {
    jar {
        archiveFileName.set("SimpleToast-Bukkit-${rootProject.version}.jar")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = rootProject.group.toString()
            artifactId = rootProject.name
            version = rootProject.version.toString()
            artifact(tasks.jar)
        }
    }
}