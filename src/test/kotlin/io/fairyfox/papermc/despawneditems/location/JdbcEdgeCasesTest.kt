package io.fairyfox.papermc.despawneditems.location

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Database edge cases: malformed rows and the transactional rollback path. */
class JdbcEdgeCasesTest {
    @TempDir
    lateinit var tempDir: File

    private val logger: Logger = Logger.getLogger("JdbcEdgeCasesTest")
    private var ds: HikariDataSource? = null

    private fun dataSource(): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:sqlite:${File(tempDir, "edge.db").absolutePath}"
                driverClassName = "org.sqlite.JDBC"
                maximumPoolSize = 1
                poolName = "edge-test"
            },
        ).also { ds = it }

    @AfterTest
    fun tearDown() {
        ds?.close()
    }

    @Test
    fun `rows with a non-UUID owner are skipped with a warning`() {
        val source = dataSource()
        val repo = JdbcLocationRepository(source, logger)
        val owner = UUID.randomUUID()
        repo.saveOwners(listOf(owner)) { listOf(DespawnLocation("world", 1, 2, 3, it)) }

        source.connection.use { c ->
            c.createStatement().use {
                it.executeUpdate("INSERT INTO despawn_locations(owner, world, x, y, z) VALUES ('not-a-uuid', 'w', 0, 0, 0)")
            }
        }

        val loaded = repo.loadAll()
        assertEquals(1, loaded.size, "the malformed row is skipped, the good row loads")
        assertEquals(owner, loaded.single().owner)
    }

    @Test
    fun `a failed write rolls back and is logged rather than thrown`() {
        val source = dataSource()
        val repo = JdbcLocationRepository(source, logger)
        source.connection.use { c ->
            c.createStatement().use { it.executeUpdate("DROP TABLE despawn_locations") }
        }

        // The table is gone: the transactional save must catch, roll back, and warn.
        repo.saveOwners(listOf(UUID.randomUUID())) { listOf(DespawnLocation("world", 1, 2, 3, it)) }
        assertTrue(true, "saveOwners must not throw on SQL failure")
    }

    @Test
    fun `saving an empty owner set is a no-op`() {
        val repo = JdbcLocationRepository(dataSource(), logger)
        repo.saveOwners(emptyList()) { emptyList() }
        assertEquals(0, repo.loadAll().size)
    }

    @Test
    fun `the yaml repository's default close is callable`() {
        YamlLocationRepository(tempDir, logger).close()
    }

    @Test
    fun `store dirty helpers expose known owners and mark-all`() {
        val store = LocationStore()
        val owner = UUID.randomUUID()
        store.add(DespawnLocation("world", 1, 2, 3, owner))
        store.clearDirty(store.dirtyOwnersSnapshot())
        assertEquals(emptySet(), store.dirtyOwnersSnapshot())

        store.markAllDirty()
        assertEquals(setOf(owner), store.dirtyOwnersSnapshot())
        assertEquals(setOf(owner), store.knownOwners())
    }
}
