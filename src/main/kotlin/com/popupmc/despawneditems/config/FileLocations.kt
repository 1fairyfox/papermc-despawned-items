package com.popupmc.despawneditems.config

import com.popupmc.despawneditems.DespawnedItems
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Persists despawn locations, one YAML file per owner under `userdata/<uuid>.yml`.
 * Keeps a flat in-memory list ([locationEntries]) that the rest of the plugin
 * queries and mutates through [add], [remove], [exists], and friends.
 */
class FileLocations(private val plugin: DespawnedItems) {

    /** All known despawn locations across every owner. */
    val locationEntries: MutableList<LocationEntry> = mutableListOf()

    init {
        load()
    }

    private fun playerDataDir(): Path = plugin.dataFolder.toPath().resolve("userdata")

    private fun ensurePlayerDataDirExists(): Boolean = try {
        Files.createDirectories(playerDataDir())
        true
    } catch (e: IOException) {
        plugin.logger.warning("Unable to create userdata directory: ${e.message}")
        false
    }

    fun load() {
        if (!ensurePlayerDataDirExists()) return

        val files = playerDataDir().toFile().listFiles() ?: emptyArray()
        for (file in files) {
            if (!file.name.endsWith(".yml")) continue

            val uuidStr = file.name.dropLast(4)
            val owner = try {
                UUID.fromString(uuidStr)
            } catch (ex: IllegalArgumentException) {
                continue
            }

            loadFile(owner, file)
        }

        // Only rebuild once the index actually exists (it is created after the
        // first load during plugin enable).
        if (plugin.isDespawnIndexesReady) {
            plugin.despawnIndexes.rebuildIndexes()
        }
    }

    private fun clearFiles() {
        val files = playerDataDir().toFile().listFiles() ?: return
        for (file in files) {
            if (!file.name.endsWith(".yml")) continue
            val uuidStr = file.name.dropLast(4)
            try {
                UUID.fromString(uuidStr)
            } catch (ex: IllegalArgumentException) {
                continue
            }
            runCatching { file.delete() }
        }
    }

    private fun loadFile(owner: UUID, file: File) {
        val config = YamlConfiguration()

        if (!file.exists()) {
            try {
                Files.copy(
                    plugin.getResource("locations.yml")!!,
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (e: IOException) {
                plugin.logger.warning("Unable to create locations file ${file.name}: ${e.message}")
                return
            }
        }

        try {
            config.load(file)
        } catch (ex: Exception) {
            plugin.logger.warning("Unable to load file ${file.name}: ${ex.message}")
            return
        }

        for (flattened in config.getStringList("locations")) {
            val entry = LocationEntry.fromString(flattened, owner, plugin)
            if (entry == null) {
                plugin.logger.warning("WARNING: Unable to load location entry, skipping $flattened")
                continue
            }
            if (exists(entry) == null) {
                locationEntries.add(entry)
            } else {
                plugin.logger.warning("WARNING: Duplicate location, skipping $flattened")
            }
        }
    }

    fun save() {
        if (!ensurePlayerDataDirExists()) return

        clearFiles()

        val splitFiles = HashMap<UUID, MutableList<LocationEntry>>()
        for (entry in ArrayList(locationEntries)) {
            splitFiles.getOrPut(entry.owner) { mutableListOf() }.add(entry)
        }

        for ((owner, entries) in splitFiles) {
            saveFile(getFile(owner), entries)
        }
    }

    private fun getFilePath(uuid: UUID): Path = playerDataDir().resolve("$uuid.yml")

    private fun getFile(uuid: UUID): File = getFilePath(uuid).toFile()

    private fun saveFile(file: File, entries: List<LocationEntry>) {
        val flattened = entries.map { it.toString() }
        val config = YamlConfiguration()
        config.set("locations", flattened)
        try {
            config.save(file)
        } catch (ex: IOException) {
            plugin.logger.warning("Unable to save locations file ${file.name}: ${ex.message}")
        }
    }

    fun add(location: Location, owner: UUID): Boolean {
        val existing = exists(location, owner)
        if (existing == null) {
            locationEntries.add(LocationEntry(location, owner, plugin))
            save()
        }
        return existing == null
    }

    fun remove(location: Location, owner: UUID): Boolean {
        val entry = exists(location, owner)
        if (entry != null) {
            locationEntries.remove(entry)
            save()
        }
        return entry != null
    }

    fun remove(location: Location): Boolean {
        val entry = exists(location)
        if (entry != null) {
            locationEntries.remove(entry)
            save()
        }
        return entry != null
    }

    fun remove(owner: UUID): Boolean {
        val entry = exists(owner)
        if (entry != null) {
            locationEntries.remove(entry)
            save()
        }
        return entry != null
    }

    fun removeAll(location: Location): Int {
        var count = 0
        while (remove(location)) count++
        return count
    }

    fun removeAll(owner: UUID): Int {
        var count = 0
        while (remove(owner)) count++
        return count
    }

    fun removeAll(): Int {
        val count = locationEntries.size
        locationEntries.clear()
        save()
        return count
    }

    fun exists(entry: LocationEntry): LocationEntry? =
        ArrayList(locationEntries).firstOrNull { it.matches(entry) }

    fun exists(owner: UUID): LocationEntry? =
        ArrayList(locationEntries).firstOrNull { it.matches(owner) }

    fun exists(location: Location, owner: UUID): LocationEntry? =
        ArrayList(locationEntries).firstOrNull { it.matches(location, owner) }

    fun exists(location: Location): LocationEntry? =
        ArrayList(locationEntries).firstOrNull { it.matches(location) }

    fun existsAll(owner: UUID): MutableList<LocationEntry> =
        ArrayList(locationEntries).filter { it.matches(owner) }.toMutableList()

    fun existsAll(location: Location): MutableList<LocationEntry> =
        ArrayList(locationEntries).filter { it.matches(location) }.toMutableList()
}
