rootProject.name = "SimpleToastApi"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include(
    "bukkit",
    "nms:v1_16_R3",
    "nms:v1_17_R1",
    "nms:v1_18_R1",
    "nms:v1_18_R2",
    "nms:v1_19_R1",
    "nms:v1_19_R2",
    "nms:v1_19_R3",
    "nms:v1_19_R3",
    "nms:v1_20_R1",
    "nms:v1_20_R2",
    "nms:v1_20_R3",
    "nms:v1_20_R4",
    "nms:v1_21_R1",
    "nms:v1_21_R2",
)