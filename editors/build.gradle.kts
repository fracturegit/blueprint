@file:Suppress("PropertyName")

val minestom_version: String by properties

dependencies {
    implementation("net.minestom:minestom:$minestom_version")
    api(project(":blueprint:minestom"))
    api(project(":commands"))
}
