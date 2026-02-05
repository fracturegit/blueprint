val minestom_version: String by properties

dependencies {
    api("net.minestom:minestom:$minestom_version")
    api(project(":core"))
}
