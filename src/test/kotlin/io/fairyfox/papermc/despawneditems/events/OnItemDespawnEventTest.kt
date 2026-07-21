package io.fairyfox.papermc.despawneditems.events

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OnItemDespawnEventTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock

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

    private fun despawnEventFor(stack: ItemStack): ItemDespawnEvent {
        val location = Location(world, 0.0, 64.0, 0.0)
        val entity = world.dropItem(location, stack)
        return ItemDespawnEvent(entity, location)
    }

    @Test
    fun `a despawning item is queued for relocation`() {
        server.pluginManager.callEvent(despawnEventFor(ItemStack(Material.DIRT, 12)))
        assertEquals(1, plugin.despawnScheduler.queued)
    }

    @Test
    fun `a cancelled despawn event is ignored`() {
        val event = despawnEventFor(ItemStack(Material.DIRT))
        event.isCancelled = true
        server.pluginManager.callEvent(event)
        assertEquals(0, plugin.despawnScheduler.queued, "ignoreCancelled must skip the handler")
    }
}
