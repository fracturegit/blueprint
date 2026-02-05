@file:Suppress("PropertyName", "LocalVariableName")

pluginManagement {
    plugins {
        kotlin("jvm") version("2.3.0") apply(false)
    }
}

rootProject.name = "blueprint"

include(
    "core",
    "editors",
    "minestom",
)
