package io.fairyfox.papermc.despawneditems.location

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import java.io.File

/**
 * Builds the configured [LocationRepository] and, when switching to a database for the
 * first time, migrates any existing flat-file data into it. Drivers and HikariCP are
 * provided at runtime by Paper's `libraries:` loader (see `plugin.yml`).
 */
object StorageFactory {
    fun create(plugin: PaperMcDespawnedItems): LocationRepository {
        val logger = plugin.logger
        return when (plugin.settings.storage.type) {
            "sqlite" -> migrateIfEmpty(plugin, buildSqlite(plugin))
            "mysql", "mariadb" -> migrateIfEmpty(plugin, buildMysql(plugin))
            "yaml", "yml", "flat", "flatfile", "file" -> YamlLocationRepository(plugin.dataFolder, logger)
            else -> {
                logger.warning("Unknown storage.type '${plugin.settings.storage.type}'; using yaml.")
                YamlLocationRepository(plugin.dataFolder, logger)
            }
        }
    }

    private fun buildSqlite(plugin: PaperMcDespawnedItems): JdbcLocationRepository {
        plugin.dataFolder.mkdirs()
        val dbFile = File(plugin.dataFolder, "locations.db")
        val cfg =
            HikariConfig().apply {
                jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                driverClassName = "org.sqlite.JDBC"
                maximumPoolSize = 1 // SQLite is a single writer
                poolName = "PaperMcDespawnedItems-SQLite"
            }
        val ds = HikariDataSource(cfg)
        plugin.logger.info("Using SQLite storage at ${dbFile.name}")
        return JdbcLocationRepository(ds, plugin.logger, ds)
    }

    private fun buildMysql(plugin: PaperMcDespawnedItems): JdbcLocationRepository {
        val s = plugin.settings.storage
        val query = if (s.mysqlProperties.isBlank()) "" else "?${s.mysqlProperties}"
        val cfg =
            HikariConfig().apply {
                // The MariaDB driver speaks the MySQL protocol too.
                jdbcUrl = "jdbc:mariadb://${s.mysqlHost}:${s.mysqlPort}/${s.mysqlDatabase}$query"
                driverClassName = "org.mariadb.jdbc.Driver"
                username = s.mysqlUsername
                password = s.mysqlPassword
                maximumPoolSize = s.poolMaximumSize
                poolName = "PaperMcDespawnedItems-MySQL"
            }
        val ds = HikariDataSource(cfg)
        plugin.logger.info("Using MySQL/MariaDB storage at ${s.mysqlHost}:${s.mysqlPort}/${s.mysqlDatabase}")
        return JdbcLocationRepository(ds, plugin.logger, ds)
    }

    /** One-time YAML → database import when the DB is empty but flat files exist. */
    private fun migrateIfEmpty(
        plugin: PaperMcDespawnedItems,
        db: JdbcLocationRepository,
    ): LocationRepository {
        if (db.loadAll().isNotEmpty()) return db
        val existing = YamlLocationRepository(plugin.dataFolder, plugin.logger).loadAll()
        if (existing.isEmpty()) return db

        plugin.logger.info("Migrating ${existing.size} despawn location(s) from yaml into the database…")
        val byOwner = existing.groupBy { it.owner }
        db.saveOwners(byOwner.keys) { byOwner[it].orEmpty() }
        plugin.logger.info("Migration complete. The old userdata/ files were left in place as a backup.")
        return db
    }
}
