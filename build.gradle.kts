val minecraftVersion: String by project
val loaderVersion: String by project
val fabricApiVersion: String by project
val fabricLanguageKotlinVersion: String by project
val modVersion: String by project
val mavenGroup: String by project
val modId: String by project

plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
    `maven-publish`
}

version = modVersion
group = mavenGroup

repositories {
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    mavenCentral()
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create(modId) {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricLanguageKotlinVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.named<Jar>("sourcesJar") {
    archiveFileName.set("Gravity-Lens.26.2-sources.jar")
}

kotlin {
    jvmToolchain(25)
}

tasks.processResources {
    val ver = project.version.toString()
    inputs.property("version", ver)
    filesMatching("fabric.mod.json") {
        expand("version" to ver)
    }
}

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)
    archiveFileName.set("Gravity-Lens.26.2.jar")
    from("LICENSE") {
        rename { "${it}_$projectName" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
