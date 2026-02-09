val minestom_version: String by properties

dependencies {
    api("net.minestom:minestom:$minestom_version")
    api("net.mcbrawls:minestom-commands:1.0.0")
    api(project(":core"))
}
