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
    id("maven-publish")
    alias(libs.plugins.paperweight) apply false
    alias(libs.plugins.shadow)
}

allprojects {
    group = "dev.wuason"
    version = "0.11.2"

    apply(plugin = "java")
    apply(plugin = "org.gradle.maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.viaversion.com")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
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

val embeddedNmsModules by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    implementation(project(":bukkit"))

    allprojects.filter { ":nms:" in it.path && !it.path.contains("v26_1") }.forEach {
        val noReobfVersions = listOf("v1_16")

        val config = if (noReobfVersions.any { v -> it.path.contains(v, true) }) {
            "default"
        } else {
            io.papermc.paperweight.util.constants.REOBF_CONFIG
        }

        implementation(project(it.path, config))
    }

    embeddedNmsModules(project(path = ":nms:v26_1", configuration = "runtimeElements"))
    embeddedNmsModules(project(path = ":nms:v26_1_1", configuration = "runtimeElements"))
    embeddedNmsModules(project(path = ":nms:v26_1_2", configuration = "runtimeElements"))
}

tasks {
    shadowJar {
        archiveFileName.set("SimpleToast-${rootProject.version}.jar")
        archiveClassifier.set("")

        dependencies {
            exclude(project(":nms:v26_1"))
            exclude(project(":nms:v26_1_1"))
            exclude(project(":nms:v26_1_2"))
        }

        from(project(":nms:v26_1").tasks.named("jar")) {
            into("nms_modules")
            rename { "v26_1.jar" }
        }

        from(project(":nms:v26_1_1").tasks.named("jar")) {
            into("nms_modules")
            rename { "v26_1_1.jar" }
        }

        from(project(":nms:v26_1_2").tasks.named("jar")) {
            into("nms_modules")
            rename { "v26_1_2.jar" }
        }
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