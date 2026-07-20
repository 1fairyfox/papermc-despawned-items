package com.popupmc.despawneditems.location

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.logging.Logger

/**
 * Flat-file [LocationRepository]: one YAML file per owner under `userdata/<uuid>.yml`
 * with a `locations:` list of `x;y;z;world` strings — the same on-disk format the
 * original plugin used, so existing servers' data loads unchanged.
 *
 * Unlike the old code (which rewrote *every* owner's file on *every* change), writes are
 * **per-owner and incremental**: [saveOwners] only touches the owners it is given, and
 * deletes the file of an owner left with no locations.
 */
class YamlLocationRepository(
    private val dataFolder: File,
    private val logger: Logger,
) : LocationRepository {
    private val userDataDir: File get() = File(dataFolder, "userdata")

    override fun loadAll(): List<DespawnLocation> {
        val dir = userDataDir
        if (!dir.isDirectory) return emptyList()
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".yml") } ?: return emptyList()

        val result = ArrayList<DespawnLocation>()
        for (file in files) {
            val owner = runCatching { UUID.fromString(file.name.removeSuffix(".yml")) }.getOrNull()
            if (owner == null) {
                logger.warning("Ignoring non-UUID userdata file ${file.name}")
                continue
            }
            val yaml =
                runCatching { YamlConfiguration.loadConfiguration(file) }.getOrElse {
                    logger.warning("Failed to load ${file.name}: ${it.message}")
                    continue
                }
            for (line in yaml.getStringList("locations")) {
                val loc = DespawnLocation.parse(line, owner)
                if (loc == null) {
                    logger.warning("Skipping malformed location '$line' in ${file.name}")
                    continue
                }
                result.add(loc)
            }
        }
        return result
    }

    override fun saveOwners(
        owners: Collection<UUID>,
        locationsOf: (UUID) -> Collection<DespawnLocation>,
    ) {
        if (owners.isEmpty()) return
        val dir = userDataDir
        if (!dir.exists() && !dir.mkdirs()) {
            logger.warning("Unable to create userdata directory ${dir.path}")
            return
        }

        for (owner in owners) {
            val file = File(dir, "$owner.yml")
            val locations = locationsOf(owner)

            if (locations.isEmpty()) {
                if (file.exists() && !file.delete()) {
                    logger.warning("Unable to delete empty userdata file ${file.name}")
                }
                continue
            }

            val yaml = YamlConfiguration()
            yaml.set("locations", locations.map { it.serialize() })
            runCatching { yaml.save(file) }
                .onFailure { logger.warning("Unable to save ${file.name}: ${it.message}") }
        }
    }
}
