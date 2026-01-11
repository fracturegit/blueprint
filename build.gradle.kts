@file:Suppress("PropertyName")

val adventure_version: String by properties

dependencies {
    api("net.kyori:adventure-nbt:$adventure_version")
    api(project(":codex"))
}
