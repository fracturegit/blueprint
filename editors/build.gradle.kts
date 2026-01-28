@file:Suppress("PropertyName")

val minestom_version: String by properties

dependencies {
    implementation("net.minestom:minestom:$minestom_version")
    api(project(":blueprint:minestom"))
    api(project(":commands"))
    api("io.github.openminigameserver.worldedit:MinestomWorldEdit:1.4")
}
