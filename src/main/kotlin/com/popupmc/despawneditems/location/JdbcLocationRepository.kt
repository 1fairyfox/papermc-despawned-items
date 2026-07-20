package com.popupmc.despawneditems.location

import java.sql.SQLException
import java.util.UUID
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Database-backed [LocationRepository] that works for **SQLite** and
 * **MySQL/MariaDB** — the SQL is deliberately dialect-agnostic (`CREATE TABLE IF NOT
 * EXISTS`, `INT`/`VARCHAR`, a composite primary key). It talks to a JDBC [DataSource]
 * (a HikariCP pool in production) and touches only `java.sql` / `javax.sql`, so nothing
 * here needs to be shaded into the plugin jar.
 *
 * Writes are per-owner and transactional: [saveOwners] deletes and re-inserts each given
 * owner's rows inside one transaction.
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

    // JDBC try-with-resources (connection → statement → result set → row loop) is
    // inherently nested; extracting it would only obscure a standard idiom.
    @Suppress("NestedBlockDepth")
    override fun loadAll(): List<DespawnLocation> {
        val result = ArrayList<DespawnLocation>()
        dataSource.connection.use { c ->
            c.prepareStatement("SELECT owner, world, x, y, z FROM despawn_locations").use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val owner = runCatching { UUID.fromString(rs.getString(COL_OWNER)) }.getOrNull()
                        if (owner == null) {
                            logger.warning("Skipping row with non-UUID owner '${rs.getString(COL_OWNER)}'")
                            continue
                        }
                        result.add(
                            DespawnLocation(rs.getString(COL_WORLD), rs.getInt(COL_X), rs.getInt(COL_Y), rs.getInt(COL_Z), owner),
                        )
                    }
                }
            }
        }
        return result
    }

    @Suppress("NestedBlockDepth")
    override fun saveOwners(
        owners: Collection<UUID>,
        locationsOf: (UUID) -> Collection<DespawnLocation>,
    ) {
        if (owners.isEmpty()) return
        dataSource.connection.use { c ->
            val previousAutoCommit = c.autoCommit
            c.autoCommit = false
            try {
                c.prepareStatement("DELETE FROM despawn_locations WHERE owner = ?").use { del ->
                    for (owner in owners) {
                        del.setString(COL_OWNER, owner.toString())
                        del.addBatch()
                    }
                    del.executeBatch()
                }
                c.prepareStatement(
                    "INSERT INTO despawn_locations(owner, world, x, y, z) VALUES (?, ?, ?, ?, ?)",
                ).use { ins ->
                    for (owner in owners) {
                        for (loc in locationsOf(owner)) {
                            ins.setString(COL_OWNER, owner.toString())
                            ins.setString(COL_WORLD, loc.world)
                            ins.setInt(COL_X, loc.x)
                            ins.setInt(COL_Y, loc.y)
                            ins.setInt(COL_Z, loc.z)
                            ins.addBatch()
                        }
                    }
                    ins.executeBatch()
                }
                c.commit()
            } catch (e: SQLException) {
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

    private companion object {
        // Shared column ordinals for both the SELECT result set and the INSERT/DELETE params
        // (owner, world, x, y, z).
        const val COL_OWNER = 1
        const val COL_WORLD = 2
        const val COL_X = 3
        const val COL_Y = 4
        const val COL_Z = 5
    }
}
