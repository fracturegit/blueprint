plugins {
    kotlin("jvm")
    id("java")
    id("maven-publish")
}

val adventure_version: String by properties

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    group = "net.mcbrawls.blueprint"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://maven.mcbrawls.net/releases/")
        maven("https://libraries.minecraft.net/")
    }

    dependencies {
    }

    kotlin {
        jvmToolchain(25)
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }
}

subprojects {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                groupId = project.group as String
                artifactId = project.name
                version = project.version as String
            }
        }

        repositories {
            val mavenUrl = System.getenv("MAVEN_URL")
            if (mavenUrl != null) {
                maven {
                    name = "envmaven"
                    url = uri(mavenUrl)
                    credentials {
                        username = System.getenv("MAVEN_USERNAME")
                        password = System.getenv("MAVEN_PASSWORD")
                    }
                }
            }
        }
    }
}
