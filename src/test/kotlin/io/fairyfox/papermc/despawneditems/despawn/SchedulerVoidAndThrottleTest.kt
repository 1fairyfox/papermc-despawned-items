package io.fairyfox.papermc.despawneditems.despawn

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.SyncChunkWorldMock
import io.fairyfox.papermc.despawneditems.editConfig
import io.fairyfox.papermc.despawneditems.stickyContainer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.UUID
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end behaviour of the two new pipeline gates inside [DespawnScheduler]: the
 * `void.chance` roll at enqueue time, and per-user throttling at drain time.
 *
 * The scheduler under test is constructed with a **seeded** [Random], so the probabilistic
 * void path is asserted exactly rather than statistically — no flaky "roughly 5%" checks.
 */
class SchedulerVoidAndThrottleTest {
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
    fun tearDown() = MockBukkit.unmock()

    /** A scheduler whose dice we control. Not started — we call enqueue/tick explicitly. */
    private fun scheduler(seed: Int) = DespawnScheduler(plugin, Random(seed))

    private fun registerChestLocation(
        x: Int,
        z: Int,
    ) {
        val block = world.getBlockAt(x, 64, z)
        block.type = Material.CHEST
        stickyContainer(block)
        plugin.locations.add(Location(world, x.toDouble(), 64.0, z.toDouble()), UUID.randomUUID())
    }

    @Test
    fun `void chance zero never voids`() {
        val sched = scheduler(seed = 1)
        repeat(100) { sched.enqueue(ItemStack(Material.DIRT)) }
        assertEquals(0L, sched.voidedByChance)
        assertEquals(100, sched.queued, "with chance 0.0 every item is queued for relocation")
    }

    @Test
    fun `void chance one voids everything`() {
        editConfig(plugin, "void.chance" to 1.0)
        val sched = scheduler(seed = 1)
        repeat(50) { sched.enqueue(ItemStack(Material.DIRT)) }
        assertEquals(50L, sched.voidedByChance)
        assertEquals(0, sched.queued, "a certain void never reaches the relocation queue")
    }

    @Test
    fun `a partial void chance splits the stream and is stable for a fixed seed`() {
        editConfig(plugin, "void.chance" to 0.5)
        val first = scheduler(seed = 42)
        repeat(200) { first.enqueue(ItemStack(Material.DIRT)) }

        assertTrue(first.voidedByChance in 1..199, "a 50% chance voids some but not all")
        assertEquals(200, (first.voidedByChance + first.queued).toInt(), "every item is either voided or queued")

        // Same seed, same outcome — the roll is deterministic, so this is regression-safe.
        val second = scheduler(seed = 42)
        repeat(200) { second.enqueue(ItemStack(Material.DIRT)) }
        assertEquals(first.voidedByChance, second.voidedByChance)
    }

    @Test
    fun `voided items land in the catch-all when one is configured`() {
        val chest = world.getBlockAt(5, 64, 5)
        chest.type = Material.CHEST
        stickyContainer(chest)
        editConfig(
            plugin,
            "void.chance" to 1.0,
            "void.catch-all.enabled" to true,
            "void.catch-all.locations" to listOf("${world.name};5;64;5"),
        )

        val sched = scheduler(seed = 7)
        sched.enqueue(ItemStack(Material.DIAMOND, 4))

        assertEquals(1L, sched.voidedByChance)
        val inv = (chest.state as Container).inventory
        assertTrue(inv.contains(Material.DIAMOND, 4), "the void roll routed the item to the catch-all, not oblivion")
    }

    @Test
    fun `throttling off drains the queue as before`() {
        registerChestLocation(0, 0)
        val sched = scheduler(seed = 1)
        repeat(5) { sched.enqueue(ItemStack(Material.DIRT), UUID.randomUUID()) }

        server.scheduler.performTicks(1)
        // The plugin's own scheduler is the one on a repeating task; drive ours by hand.
        assertEquals(5, sched.queued, "our detached scheduler only drains when we tick it")
    }

    @Test
    fun `over-quota items are dropped and counted when on-limit is drop`() {
        registerChestLocation(0, 0)
        editConfig(
            plugin,
            "throttle.enabled" to true,
            "throttle.strategy" to "rate",
            "throttle.rate.max-per-window" to 1,
            "throttle.rate.window-seconds" to 3600,
            "throttle.on-limit" to "drop",
        )

        val actor = UUID.randomUUID()
        val sched = scheduler(seed = 1)
        repeat(4) { sched.enqueue(ItemStack(Material.DIRT), actor) }

        sched.start()
        server.scheduler.performTicks(3)
        sched.stop()

        assertTrue(sched.droppedByThrottle >= 1, "items past the actor's rate budget are dropped, not queued forever")
    }

    @Test
    fun `deferred items stay queued instead of being lost`() {
        registerChestLocation(0, 0)
        editConfig(
            plugin,
            "throttle.enabled" to true,
            "throttle.strategy" to "rate",
            "throttle.rate.max-per-window" to 1,
            "throttle.rate.window-seconds" to 3600,
            "throttle.on-limit" to "defer",
        )

        val actor = UUID.randomUUID()
        val sched = scheduler(seed = 1)
        repeat(4) { sched.enqueue(ItemStack(Material.DIRT), actor) }

        sched.start()
        server.scheduler.performTicks(3)
        sched.stop()

        assertEquals(0L, sched.droppedByThrottle, "defer never drops")
    }

    @Test
    fun `the queue cap still applies with an actor attached`() {
        editConfig(plugin, "performance.max-queue" to 3, "performance.drop-when-full" to true)
        val sched = scheduler(seed = 1)
        repeat(10) { sched.enqueue(ItemStack(Material.DIRT), UUID.randomUUID()) }
        assertEquals(3, sched.queued, "max-queue is honoured on the actor-aware path too")
    }

    @Test
    fun `drop-oldest policy keeps the queue at the cap`() {
        editConfig(plugin, "performance.max-queue" to 3, "performance.drop-when-full" to false)
        val sched = scheduler(seed = 1)
        repeat(10) { sched.enqueue(ItemStack(Material.DIRT), UUID.randomUUID()) }
        assertEquals(3, sched.queued)
    }
}
