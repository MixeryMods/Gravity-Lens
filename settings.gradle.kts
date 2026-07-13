pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }

    val loomVersion: String by settings
    val kotlinVersion: String by settings
    plugins {
        id("net.fabricmc.fabric-loom") version loomVersion
        kotlin("jvm") version kotlinVersion
    }
}

rootProject.name = "gravitylens"
