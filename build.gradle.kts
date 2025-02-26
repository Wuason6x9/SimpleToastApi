plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14" apply false
    id("com.gradleup.shadow") version "8.3.5"
    id("org.gradle.maven-publish")
}

allprojects {

    group = "dev.wuason"
    version = "0.3"

    apply(plugin = "java")
    apply(plugin = "org.gradle.maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
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

dependencies {
    implementation(project(":bukkit"))
    allprojects.filter { ":nms:" in it.path }.forEach {
        if (!it.path.contains("v1_16", true)) {
            implementation(project(it.path, "reobf"))
        }
        else {
            implementation(it)
        }
    }
}

tasks {

    shadowJar {
        //simple toast
        archiveFileName.set("SimpleToast-${rootProject.version}.jar")
        archiveClassifier.set("")
    }

    build {
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
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = rootProject.group.toString()
            artifactId = rootProject.name
            version = rootProject.version.toString()
            artifact(tasks.shadowJar)
        }
    }
}