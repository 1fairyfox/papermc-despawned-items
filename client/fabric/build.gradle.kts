plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

version = "${property("mod_version")}"
group = "${property("maven_group")}"

base {
    archivesName.set("${property("archives_base_name")}")
}

repositories {
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    // Fabric API — only the two modules this mod actually uses: the client networking
    // registry and the screen event that lets us add a button to an existing screen.
    //
    // Pulling the WHOLE API also pulls modules we never touch, and one of them
    // (fabric-content-registries-v0) ships javadoc Loom 1.11 refuses to remap
    // ("must be have an intermediary source namespace") — so the narrow dependency is both
    // the fix and the design we wanted anyway: a smaller surface to break on updates.
    modImplementation(fabricApi.module("fabric-api-base", "${property("fabric_version")}"))
    modImplementation(fabricApi.module("fabric-networking-api-v1", "${property("fabric_version")}"))
    modImplementation(fabricApi.module("fabric-screen-api-v1", "${property("fabric_version")}"))
}

java {
    // Minecraft 1.21.x runs on Java 21, the same toolchain as the plugin.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "minecraft_version" to property("minecraft_version"),
        "loader_version" to property("loader_version"),
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}
