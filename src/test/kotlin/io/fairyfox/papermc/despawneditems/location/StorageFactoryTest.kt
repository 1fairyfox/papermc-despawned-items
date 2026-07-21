package io.fairyfox.papermc.despawneditems.location

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.editConfig
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.io.File
import java.sql.SQLException
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Backend selection, YAML→database migration, and pool-exhaustion behaviour.
 * MySQL connection paths need a live server and stay real-server territory
 * (`notes/plans/testing.md` §14).
 */
class StorageFactoryTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `yaml and all its aliases build the flat-file repository`() {
        for (alias in listOf("yaml", "yml", "flat", "flatfile", "file")) {
            editConfig(plugin, "storage.type" to alias)
            val repo = StorageFactory.create(plugin)
            assertIs<YamlLocationRepository>(repo, "alias '$alias'")
        }
    }

    @Test
    fun `an unknown type warns and falls back to yaml`() {
        editConfig(plugin, "storage.type" to "oracle")
        assertIs<YamlLocationRepository>(StorageFactory.create(plugin))
    }

    @Test
    fun `sqlite builds a database repository and creates the db file`() {
        editConfig(plugin, "storage.type" to "sqlite")
        val repo = StorageFactory.create(plugin)
        try {
            assertIs<JdbcLocationRepository>(repo)
            assertTrue(File(plugin.dataFolder, "locations.db").exists())
        } finally {
            repo.close()
        }
    }

    @Test
    fun `switching to sqlite migrates existing yaml data once`() {
        val owner = UUID.randomUUID()
        val locations = listOf(DespawnLocation("world", 1, 64, 1, owner), DespawnLocation("world", 2, 64, 2, owner))
        YamlLocationRepository(plugin.dataFolder, plugin.logger).saveOwners(listOf(owner)) { locations }

        editConfig(plugin, "storage.type" to "sqlite")
        val repo = StorageFactory.create(plugin)
        try {
            assertEquals(locations.toSet(), repo.loadAll().toSet(), "yaml rows must migrate into the db")
        } finally {
            repo.close()
        }

        // Second start: the DB is non-empty, so migration must not duplicate.
        val again = StorageFactory.create(plugin)
        try {
            assertEquals(2, again.loadAll().size, "migration is one-time")
        } finally {
            again.close()
        }
    }

    @Test
    fun `the full manager reload persists and reloads through sqlite`() {
        editConfig(plugin, "storage.type" to "sqlite")
        val world = server.addSimpleWorld("world")
        plugin.locations.reload()

        plugin.locations.add(org.bukkit.Location(world, 3.0, 64.0, 3.0), UUID.randomUUID())
        plugin.locations.saveNow()
        plugin.locations.reload()

        assertEquals(1, plugin.locations.count, "the row must survive a full reload cycle")
    }

    @Test
    fun `an exhausted pool surfaces as an SQLException for the flush to log`() {
        val dbFile = File(plugin.dataFolder.also { it.mkdirs() }, "exhaust.db")
        val ds =
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                    driverClassName = "org.sqlite.JDBC"
                    maximumPoolSize = 1
                    connectionTimeout = 300 // ms; Hikari's minimum is 250
                    validationTimeout = 250
                    poolName = "exhaust-test"
                },
            )
        try {
            val repo = JdbcLocationRepository(ds, plugin.logger)
            ds.connection.use {
                // The pool's only connection is held: acquisition must time out.
                assertFailsWith<SQLException> { repo.loadAll() }
            }
            // With the connection released the repository works again.
            assertEquals(0, repo.loadAll().size)
        } finally {
            ds.close()
        }
    }
}
