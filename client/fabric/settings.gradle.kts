// A SEPARATE Gradle build from the plugin, on purpose.
//
// Fabric Loom rewrites the whole toolchain around a deobfuscated Minecraft client: its own
// repositories, its own remapping tasks, its own run configurations. Folding that into the
// plugin's build would drag every one of those concerns into a project that just wants to
// compile Kotlin against the Paper API — and would mean a Loom or mappings problem could
// break the plugin's release. Two builds, one repository, no shared risk.

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
    }
}

rootProject.name = "papermc-despawned-items-client"
