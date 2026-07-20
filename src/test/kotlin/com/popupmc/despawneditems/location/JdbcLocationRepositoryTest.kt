package com.popupmc.despawneditems.location

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [JdbcLocationRepository] against a real (temp-file) SQLite database. The same
 * dialect-agnostic SQL runs on MySQL/MariaDB, so this exercises the production path.
 */
class JdbcLocationRepositoryTest {

    private val logger: Logger = Logger.getLogger("JdbcLocationRepositoryTest")
    private val alice: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val bob: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000b2")

    private fun open(dir: File): JdbcLocationRepository {
        val cfg = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${File(dir, "test.db").absolutePath}"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 1 // SQLite is single-writer
        }
        val ds = HikariDataSource(cfg)
        return JdbcLocationRepository(ds, logger, ds)
    }

    @Test
    fun `save then load roundtrips`(@TempDir dir: File) {
        val repo = open(dir)
        try {
            val locs = listOf(
                DespawnLocation("world", 1, 2, 3, alice),
                DespawnLocation("world", 4, 5, 6, alice),
                DespawnLocation("world_nether", -7, 8, -9, bob),
            )
            val byOwner = locs.groupBy { it.owner }
            repo.saveOwners(listOf(alice, bob)) { byOwner[it].orEmpty() }
            assertEquals(locs.toSet(), repo.loadAll().toSet())
        } finally {
            repo.close()
        }
    }

    @Test
    fun `saveOwners replaces only the given owner's rows`(@TempDir dir: File) {
        val repo = open(dir)
        try {
            repo.saveOwners(listOf(alice, bob)) { listOf(DespawnLocation("world", 1, 1, 1, it)) }
            // Re-save alice with a different set; bob must be untouched.
            repo.saveOwners(listOf(alice)) {
                listOf(DespawnLocation("world", 2, 2, 2, it), DespawnLocation("world", 3, 3, 3, it))
            }
            val all = repo.loadAll().toSet()
            assertEquals(
                setOf(
                    DespawnLocation("world", 2, 2, 2, alice),
                    DespawnLocation("world", 3, 3, 3, alice),
                    DespawnLocation("world", 1, 1, 1, bob),
                ),
                all,
            )
        } finally {
            repo.close()
        }
    }

    @Test
    fun `emptying an owner removes their rows`(@TempDir dir: File) {
        val repo = open(dir)
        try {
            repo.saveOwners(listOf(alice)) { listOf(DespawnLocation("world", 1, 1, 1, alice)) }
            repo.saveOwners(listOf(alice)) { emptyList() }
            assertTrue(repo.loadAll().isEmpty())
        } finally {
            repo.close()
        }
    }

    @Test
    fun `data survives reopening the database`(@TempDir dir: File) {
        open(dir).use2 { it.saveOwners(listOf(alice)) { _ -> listOf(DespawnLocation("world", 5, 6, 7, alice)) } }
        val reopened = open(dir)
        try {
            assertEquals(listOf(DespawnLocation("world", 5, 6, 7, alice)), reopened.loadAll())
        } finally {
            reopened.close()
        }
    }

    // Small helper so the first repo is closed before reopening.
    private inline fun JdbcLocationRepository.use2(block: (JdbcLocationRepository) -> Unit) {
        try {
            block(this)
        } finally {
            close()
        }
    }
}
