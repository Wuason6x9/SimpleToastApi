/*
 *     Copyright (C) 2026 Wuason6x9 and RubenArtz
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21" apply false
    id("com.gradleup.shadow") version "9.1.0"
    id("maven-publish")
}

allprojects {

    group = "dev.wuason"
    version = "0.11.1"

    apply(plugin = "java")
    apply(plugin = "org.gradle.maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
        //maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // For Spigot
        maven("https://repo.papermc.io/repository/maven-public/") // For Paper
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    if ("nms" in project.path) {
        dependencies {
            compileOnly(project(":bukkit"))
        }
    }
}

dependencies {
    implementation(project(":bukkit"))

    allprojects.filter { ":nms:" in it.path }.forEach {
        val noReobfVersions = listOf("v1_16", "v26_1", "v26_1_1", "v26_1_2")

        val config = if (noReobfVersions.any { v -> it.path.contains(v, true) }) {
            "default"
        } else {
            io.papermc.paperweight.util.constants.REOBF_CONFIG
        }

        implementation(project(it.path, config))
    }
}


tasks {

    shadowJar {
        //simple toast
        archiveFileName.set("SimpleToast-${rootProject.version}.jar")
        archiveClassifier.set("")
    }

    build {
        dependsOn(":bukkit:test")
        dependsOn(shadowJar)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://repo.techmc.es/releases")
            credentials(PasswordCredentials::class) {

                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = rootProject.group.toString()
            artifactId = "simple-toast"
            version = rootProject.version.toString()
            artifact(tasks.shadowJar)
            pom {
                name = "SimpleToast API"
                url = "https://github.com/Wuason6x9/SimpleToastApi/"
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                        distribution = "repo"
                    }
                }
            }
        }
    }
}