package io.fairyfox.papermc.despawneditems.location

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.editConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Real MySQL/MariaDB backend integration (testing.md §14) via Testcontainers — covers
 * `StorageFactory.buildMysql`, the YAML→MySQL migration, and a full CRUD roundtrip
 * against a live server. Disabled automatically where Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class MariaDbStorageTest {
    companion object {
        @Container
        @JvmStatic
        private val mariadb: MariaDBContainer<*> = MariaDBContainer("mariadb:11")
    }

    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        editConfig(
            plugin,
            "storage.type" to "mysql",
            "storage.mysql.host" to mariadb.host,
            "storage.mysql.port" to mariadb.firstMappedPort,
            "storage.mysql.database" to mariadb.databaseName,
            "storage.mysql.username" to mariadb.username,
            "storage.mysql.password" to mariadb.password,
            "storage.mysql.properties" to "",
        )
    }

    @AfterEach
    fun tearDown() {
        // Drop the shared table so each test starts clean (the container is class-shared).
        val repo = StorageFactory.create(plugin)
        runCatching {
            com.zaxxer.hikari.HikariDataSource(
                com.zaxxer.hikari.HikariConfig().apply {
                    jdbcUrl = "jdbc:mariadb://${mariadb.host}:${mariadb.firstMappedPort}/${mariadb.databaseName}"
                    driverClassName = "org.mariadb.jdbc.Driver"
                    username = mariadb.username
                    password = mariadb.password
                    maximumPoolSize = 1
                },
            ).use { ds ->
                ds.connection.use { c ->
                    c.createStatement().use { it.executeUpdate("DROP TABLE IF EXISTS despawn_locations") }
                }
            }
        }
        repo.close()
        MockBukkit.unmock()
    }

    @Test
    fun `the mysql backend builds and round-trips locations`() {
        val repo = StorageFactory.create(plugin)
        try {
            assertIs<JdbcLocationRepository>(repo)

            val owner = UUID.randomUUID()
            val locations =
                listOf(
                    DespawnLocation("world", 1, 64, 1, owner),
                    DespawnLocation("world_nether", -20, 30, 400, owner),
                )
            repo.saveOwners(listOf(owner)) { locations }
            assertEquals(locations.toSet(), repo.loadAll().toSet())

            // Replace semantics: a save with fewer rows deletes the rest transactionally.
            repo.saveOwners(listOf(owner)) { listOf(locations.first()) }
            assertEquals(setOf(locations.first()), repo.loadAll().toSet())
        } finally {
            repo.close()
        }
    }

    @Test
    fun `existing yaml data migrates into mysql once`() {
        val owner = UUID.randomUUID()
        val legacy = listOf(DespawnLocation("world", 5, 70, 5, owner))
        YamlLocationRepository(plugin.dataFolder, plugin.logger).saveOwners(listOf(owner)) { legacy }

        val repo = StorageFactory.create(plugin)
        try {
            assertEquals(legacy, repo.loadAll(), "yaml rows must migrate into MySQL")
        } finally {
            repo.close()
        }

        val again = StorageFactory.create(plugin)
        try {
            assertEquals(1, again.loadAll().size, "migration must not duplicate on restart")
        } finally {
            again.close()
        }
    }

    @Test
    fun `the full manager lifecycle works on mysql`() {
        val world = server.addSimpleWorld("world")
        plugin.locations.reload()

        plugin.locations.add(org.bukkit.Location(world, 8.0, 64.0, 8.0), UUID.randomUUID())
        plugin.locations.saveNow()
        plugin.locations.reload()

        assertEquals(1, plugin.locations.count, "a row must survive a full reload cycle on MySQL")
    }
}
