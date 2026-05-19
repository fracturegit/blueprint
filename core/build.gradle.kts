dependencies {
    api("com.mojang:datafixerupper:7.0.14")
    api("net.mcbrawls:codex:2.0.1")
    api("org.joml:joml:1.10.8")
    api("org.slf4j:slf4j-api:2.0.17")
    api("net.kyori:adventure-nbt:5.1.0")
    api("net.kyori:adventure-key:5.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
