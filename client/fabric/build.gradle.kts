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

    // Fabric API — used for exactly two things: the client networking registry and the
    // screen event that lets us add a button to an existing screen. Deliberately no other
    // Fabric API modules, so the mod stays small and its update surface stays narrow.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
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
