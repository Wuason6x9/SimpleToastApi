rootProject.name = "SimpleToastApi"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include(
    "bukkit",
    "nms:v1_16_R3"
)