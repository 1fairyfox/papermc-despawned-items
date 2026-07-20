package com.popupmc.despawneditems.location

import java.util.UUID
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Database-backed [LocationRepository] that works for **SQLite** and
 * **MySQL/MariaDB** — the SQL is deliberately dialect-agnostic (`CREATE TABLE IF NOT
 * EXISTS`, `INT`/`VARCHAR`, a composite primary key). It talks to a JDBC
 * [DataSource] (a HikariCP pool in production) and touches only `java.sql` /
 * `javax.sql`, so nothing here needs to be shaded into the plugin jar.
 *
 * Writes are per-owner and transactional: [saveOwners] deletes and re-inserts each
 * given owner's rows inside one transaction, so a crash mid-write can't leave an owner
 * half-persisted.
 */
class JdbcLocationRepository(
    private val dataSource: DataSource,
    private val logger: Logger,
    /** Optional resource (e.g. the HikariCP pool) closed by [close]. */
    private val poolCloseable: AutoCloseable? = null,
) : LocationRepository {

    init {
        createSchema()
    }

    private fun createSchema() {
        dataSource.connection.use { c ->
            c.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS despawn_locations (
                        owner CHAR(36) NOT NULL,
                        world VARCHAR(255) NOT NULL,
                        x INT NOT NULL,
                        y INT NOT NULL,
                        z INT NOT NULL,
                        PRIMARY KEY (owner, world, x, y, z)
                    )
                    """.trimIndent(),
                )
            }
        }
    }

    override fun loadAll(): List<DespawnLocation> {
        val result = ArrayList<DespawnLocation>()
        dataSource.connection.use { c ->
            c.prepareStatement("SELECT owner, world, x, y, z FROM despawn_locations").use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val owner = runCatching { UUID.fromString(rs.getString(1)) }.getOrNull()
                        if (owner == null) {
                            logger.warning("Skipping row with non-UUID owner '${rs.getString(1)}'")
                            continue
                        }
                        result.add(
                            DespawnLocation(rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), owner),
                        )
                    }
                }
            }
        }
        return result
    }

    override fun saveOwners(owners: Collection<UUID>, locationsOf: (UUID) -> Collection<DespawnLocation>) {
        if (owners.isEmpty()) return
        dataSource.connection.use { c ->
            val previousAutoCommit = c.autoCommit
            c.autoCommit = false
            try {
                c.prepareStatement("DELETE FROM despawn_locations WHERE owner = ?").use { del ->
                    for (owner in owners) {
                        del.setString(1, owner.toString())
                        del.addBatch()
                    }
                    del.executeBatch()
                }
                c.prepareStatement(
                    "INSERT INTO despawn_locations(owner, world, x, y, z) VALUES (?, ?, ?, ?, ?)",
                ).use { ins ->
                    for (owner in owners) {
                        for (loc in locationsOf(owner)) {
                            ins.setString(1, owner.toString())
                            ins.setString(2, loc.world)
                            ins.setInt(3, loc.x)
                            ins.setInt(4, loc.y)
                            ins.setInt(5, loc.z)
                            ins.addBatch()
                        }
                    }
                    ins.executeBatch()
                }
                c.commit()
            } catch (e: Exception) {
                runCatching { c.rollback() }
                logger.warning("Failed to persist locations to the database: ${e.message}")
            } finally {
                runCatching { c.autoCommit = previousAutoCommit }
            }
        }
    }

    override fun close() {
        runCatching { poolCloseable?.close() }
            .onFailure { logger.warning("Error closing database pool: ${it.message}") }
    }
}
