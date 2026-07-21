package io.fairyfox.papermc.despawneditems.despawn

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.SyncChunkWorldMock
import io.fairyfox.papermc.despawneditems.editConfig
import io.fairyfox.papermc.despawneditems.stickyContainer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.UUID
import kotlin.system.measureTimeMillis
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * High-load / DoS-guard tests (testing.md §62, §65–73): the throttled scheduler must
 * bound work per tick no matter how many items flood in.
 */
class DespawnLoadTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: SyncChunkWorldMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = SyncChunkWorldMock()
        server.addWorld(world)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun registerChest() {
        val block = world.getBlockAt(0, 64, 0)
        block.type = Material.CHEST
        stickyContainer(block)
        plugin.locations.add(Location(world, 0.0, 64.0, 0.0), UUID.randomUUID())
    }

    @Test
    fun `the queue cap ignores new items when drop-when-full is true`() {
        editConfig(plugin, "performance.max-queue" to 100, "performance.drop-when-full" to true)
        repeat(150) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }
        assertEquals(100, plugin.despawnScheduler.queued)
    }

    @Test
    fun `the queue cap drops the oldest when drop-when-full is false`() {
        editConfig(plugin, "performance.max-queue" to 2, "performance.drop-when-full" to false)
        plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT))
        plugin.despawnScheduler.enqueue(ItemStack(Material.STONE))
        plugin.despawnScheduler.enqueue(ItemStack(Material.OAK_LOG))
        assertEquals(2, plugin.despawnScheduler.queued, "the oldest item is evicted, cap holds")
    }

    @Test
    fun `each tick starts at most max-per-tick relocations`() {
        editConfig(plugin, "performance.max-per-tick" to 5, "performance.max-concurrent" to 200)
        registerChest()
        repeat(20) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }

        server.scheduler.performTicks(1)
        assertEquals(15, plugin.despawnScheduler.queued, "exactly 5 started on the first tick")
        assertTrue(plugin.despawnProcesses.size <= 5)
    }

    @Test
    fun `in-flight relocations never exceed max-concurrent`() {
        editConfig(plugin, "performance.max-per-tick" to 50, "performance.max-concurrent" to 10)
        registerChest()
        repeat(40) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }

        server.scheduler.performTicks(1)
        assertTrue(plugin.despawnProcesses.size <= 10, "concurrency bound respected")
        assertEquals(30, plugin.despawnScheduler.queued)
    }

    @Test
    fun `a 10k item flood is absorbed quickly and fully drained`() {
        editConfig(plugin, "performance.max-queue" to 10_000)
        registerChest()

        val enqueueMillis =
            measureTimeMillis {
                repeat(10_000) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }
            }
        assertTrue(enqueueMillis < 2_000, "flood intake must be cheap; took ${enqueueMillis}ms")
        assertEquals(10_000, plugin.despawnScheduler.queued)

        // Default budget: 20/tick → 20 seconds of game time clears 8k or more.
        server.scheduler.performTicks(600)
        assertTrue(
            plugin.despawnScheduler.queued < 10_000 - 5_000,
            "steady drain under budget; queued=${plugin.despawnScheduler.queued}",
        )
    }

    @Test
    fun `stop clears the queue and is idempotent with start`() {
        plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT))
        plugin.despawnScheduler.stop()
        assertEquals(0, plugin.despawnScheduler.queued)
        plugin.despawnScheduler.stop() // second stop: no crash
        plugin.despawnScheduler.start()
        plugin.despawnScheduler.start() // second start: no duplicate task
        server.scheduler.performTicks(2)
    }
}
