package com.popupmc.despawneditems.location

import com.popupmc.despawneditems.PaperMcDespawnedItems
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

/**
 * Integration test for [LocationManager] through a mocked Paper server: exercises the
 * add/remove/query wiring and the persist-then-reload path (default YAML backend).
 */
class LocationManagerTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private val alice: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val bob: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000b2")

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

    private fun at(
        x: Int,
        y: Int,
        z: Int,
    ) = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

    @Test
    fun `add, has, count, duplicate, and remove`() {
        val loc = at(10, 64, 10)
        assertTrue(plugin.locations.add(loc, alice))
        assertTrue(plugin.locations.has(loc, alice))
        assertEquals(1, plugin.locations.count)
        assertEquals(1, plugin.locations.countOfOwner(alice))
        assertFalse(plugin.locations.add(loc, alice), "duplicate add is a no-op")

        assertTrue(plugin.locations.remove(loc, alice))
        assertEquals(0, plugin.locations.count)
    }

    @Test
    fun `same block two owners, removeAllAt clears both`() {
        val loc = at(5, 70, 5)
        plugin.locations.add(loc, alice)
        plugin.locations.add(loc, bob)
        assertEquals(setOf(alice, bob), plugin.locations.ownersAt(loc))
        assertEquals(2, plugin.locations.removeAllAt(loc))
        assertTrue(plugin.locations.isEmpty())
    }

    @Test
    fun `locations persist across a save and reload`() {
        val loc = at(1, 2, 3)
        plugin.locations.add(loc, alice)
        plugin.locations.saveNow()

        // Drop in-memory state and reload from disk (default yaml backend).
        plugin.locations.reload()

        assertTrue(plugin.locations.has(loc, alice), "location should survive save + reload")
        assertEquals(1, plugin.locations.count)
    }

    @Test
    fun `removeAllOfOwner leaves other owners intact`() {
        plugin.locations.add(at(1, 1, 1), alice)
        plugin.locations.add(at(2, 2, 2), alice)
        plugin.locations.add(at(3, 3, 3), bob)
        assertEquals(2, plugin.locations.removeAllOfOwner(alice))
        assertEquals(0, plugin.locations.countOfOwner(alice))
        assertEquals(1, plugin.locations.countOfOwner(bob))
    }
}
