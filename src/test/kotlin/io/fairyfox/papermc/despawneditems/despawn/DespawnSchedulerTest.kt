package io.fairyfox.papermc.despawneditems.despawn

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MockBukkit tests for [DespawnScheduler]'s queue behaviour. The full relocation
 * mechanics (chunk loading, block states) are reserved for real-server tests; here we
 * verify enqueue and the "no locations → drain the backlog" tick path.
 */
class DespawnSchedulerTest {
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
    fun `enqueue increases the queued count`() {
        plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT))
        plugin.despawnScheduler.enqueue(ItemStack(Material.STONE))
        assertEquals(2, plugin.despawnScheduler.queued)
    }

    @Test
    fun `a tick with no despawn locations drains the queue`() {
        repeat(5) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }
        assertEquals(5, plugin.despawnScheduler.queued)

        // The repeating scheduler task (started in onEnable) runs on tick.
        server.scheduler.performTicks(2)

        assertEquals(0, plugin.despawnScheduler.queued, "with no locations the backlog is discarded")
    }
}
