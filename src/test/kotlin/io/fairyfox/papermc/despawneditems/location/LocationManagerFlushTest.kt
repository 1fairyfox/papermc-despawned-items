package io.fairyfox.papermc.despawneditems.location

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import org.bukkit.Location
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Debounced/async persistence, reload/shutdown paths, and the remaining mutation edges. */
class LocationManagerFlushTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private val owner: UUID = UUID.randomUUID()

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun loc(x: Int) = Location(world, x.toDouble(), 64.0, 0.0)

    private fun onDisk(): List<DespawnLocation> = YamlLocationRepository(plugin.dataFolder, plugin.logger).loadAll()

    @Test
    fun `a mutation flushes asynchronously after the debounce window`() {
        plugin.locations.add(loc(0), owner)
        assertTrue(onDisk().isEmpty(), "nothing on disk before the debounce window")

        server.scheduler.performTicks(101)
        server.scheduler.waitAsyncTasksFinished()

        assertEquals(1, onDisk().size, "the debounced async flush must persist the location")
    }

    @Test
    fun `saveNow flushes synchronously`() {
        plugin.locations.add(loc(0), owner)
        plugin.locations.saveNow()
        assertEquals(1, onDisk().size)
    }

    @Test
    fun `reload keeps persisted data`() {
        plugin.locations.add(loc(0), owner)
        plugin.locations.reload()
        assertEquals(1, plugin.locations.count, "reload = flush + close + load")
    }

    @Test
    fun `disabling the plugin flushes on shutdown`() {
        plugin.locations.add(loc(0), owner)
        server.pluginManager.disablePlugin(plugin)
        assertEquals(1, onDisk().size, "onDisable must flush synchronously")
    }

    @Test
    fun `removing an owner's last location deletes their file`() {
        plugin.locations.add(loc(0), owner)
        plugin.locations.saveNow()
        assertEquals(1, onDisk().size)

        plugin.locations.removeAllOfOwner(owner)
        plugin.locations.saveNow()
        assertTrue(onDisk().isEmpty(), "empty owners' files are deleted")
    }

    @Test
    fun `remove variants report false or zero when nothing matches`() {
        assertFalse(plugin.locations.remove(loc(0), owner))
        assertFalse(plugin.locations.removeOneAt(loc(0)))
        assertFalse(plugin.locations.removeOneOfOwner(owner))
        assertEquals(0, plugin.locations.removeAllAt(loc(0)))
        assertEquals(0, plugin.locations.removeAllOfOwner(owner))
        assertEquals(0, plugin.locations.clearAll())
    }

    @Test
    fun `remove variants succeed when locations exist`() {
        plugin.locations.add(loc(0), owner)
        assertTrue(plugin.locations.removeOneAt(loc(0)))

        plugin.locations.add(loc(1), owner)
        assertTrue(plugin.locations.removeOneOfOwner(owner))

        plugin.locations.add(loc(2), owner)
        plugin.locations.add(loc(2), UUID.randomUUID())
        assertEquals(2, plugin.locations.removeAllAt(loc(2)))

        plugin.locations.add(loc(3), owner)
        assertEquals(1, plugin.locations.clearAll())
        assertTrue(plugin.locations.isEmpty())
    }

    @Test
    fun `queries expose owners and randomness`() {
        assertEquals(null, plugin.locations.random(), "empty store draws null")
        plugin.locations.add(loc(0), owner)
        assertTrue(plugin.locations.anyAt(loc(0)))
        assertEquals(setOf(owner), plugin.locations.ownersAt(loc(0)))
        assertEquals(1, plugin.locations.atLocation(loc(0)).size)
        assertEquals(plugin.locations.all().first(), plugin.locations.random())
        assertEquals(plugin.locations.all().first(), plugin.locations.firstOfOwner(owner))
    }

    @Test
    fun `replaceWith swaps the whole store and persists`() {
        plugin.locations.add(loc(0), owner)
        val replacement = DespawnLocation("world", 9, 64, 9, owner)
        plugin.locations.replaceWith(listOf(replacement))
        assertEquals(listOf(replacement), plugin.locations.all())
    }
}
