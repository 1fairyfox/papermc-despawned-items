package io.fairyfox.papermc.despawneditems.despawn

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.SyncChunkWorldMock
import io.fairyfox.papermc.despawneditems.TargetingPlayerMock
import io.fairyfox.papermc.despawneditems.location.YamlLocationRepository
import io.fairyfox.papermc.despawneditems.stickyContainer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.system.measureNanoTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Combined high-load scenarios (owner mandate): a despawn storm draining through the
 * throttled pipeline WHILE many players hammer commands — the "busy public server"
 * shape. On a real server both flows share the main thread, which is exactly how
 * MockBukkit executes them, so per-tick wall time here is a faithful lower-bound
 * model of main-thread cost. Prints measured numbers so CI logs double as a
 * performance record.
 */
class CombinedLoadTest {
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

    private fun registerChests(count: Int) {
        for (i in 0 until count) {
            val block = world.getBlockAt(i * 4, 64, 0)
            block.type = Material.CHEST
            stickyContainer(block)
            plugin.locations.add(Location(world, (i * 4).toDouble(), 64.0, 0.0), java.util.UUID.randomUUID())
        }
    }

    private fun addPlayers(count: Int): List<TargetingPlayerMock> =
        (1..count).map { i ->
            TargetingPlayerMock(server, "Load$i").also {
                server.addPlayer(it)
                it.target = world.getBlockAt(0, 64, 0)
            }
        }

    @Test
    fun `a despawn storm drains on budget while 50 players run commands every tick`() {
        registerChests(20)
        val players = addPlayers(50)
        repeat(5_000) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }

        val commands =
            listOf(
                "despi locations mine",
                "despi exists anywhere owned-by-me",
                "despi exists here owned-by-me",
                "recycle",
            )

        val tickNanos = LongArray(100)
        for (tick in 0 until 100) {
            tickNanos[tick] =
                measureNanoTime {
                    // Ten players act per tick (rotating), i.e. every player issues a
                    // command every 5 ticks — heavy interactive traffic.
                    for (p in 0 until 10) {
                        val player = players[(tick * 10 + p) % players.size]
                        server.dispatchCommand(player, commands[(tick + p) % commands.size])
                    }
                    server.scheduler.performTicks(1)
                }
        }

        val avgMs = tickNanos.average() / 1_000_000.0
        val maxMs = tickNanos.max() / 1_000_000.0
        val sorted = tickNanos.sorted()
        val p95Ms = sorted[(sorted.size * 95) / 100 - 1] / 1_000_000.0
        println("LOAD storm+commands: avg=%.2fms p95=%.2fms max=%.2fms per tick".format(avgMs, p95Ms, maxMs))

        // Budget: 20 relocations/tick × 100 ticks = 2000 items must have left the queue.
        assertTrue(plugin.despawnScheduler.queued <= 3_000, "queue must drain on budget; queued=${plugin.despawnScheduler.queued}")
        // A 50ms tick is the server-meltdown threshold; average must sit far below it.
        assertTrue(avgMs < 50.0, "average tick must stay under 50ms; was %.2fms".format(avgMs))
        // Every player must have received replies (commands actually executed).
        assertTrue(players.all { it.nextMessage() != null }, "every player got at least one reply")
    }

    @Test
    fun `a 1000-command burst from 100 players completes quickly and correctly`() {
        registerChests(5)
        val players = addPlayers(100)
        players.forEach { it.isOp = true }

        val commands =
            listOf(
                "despi locations count",
                "despi locations mine",
                "despi exists anywhere owned-by-me",
                "despi despawn count-ongoing",
                "despi effects count-ongoing",
                "despi exists here owned-by-anyone",
                "despi locations here",
                "recycle",
                "despi exists here owned-by-me",
                "despi despawn",
            )

        val elapsedNanos =
            measureNanoTime {
                for (round in 0 until 10) {
                    for (player in players) {
                        server.dispatchCommand(player, commands[round])
                    }
                }
            }
        val totalMs = elapsedNanos / 1_000_000.0
        println("LOAD command-burst: 1000 dispatches in %.1fms (%.3fms/command)".format(totalMs, totalMs / 1000))

        assertTrue(totalMs < 5_000, "1000 dispatches must complete in under 5s; took %.1fms".format(totalMs))
        assertTrue(players.all { it.nextMessage() != null }, "every player received replies")
    }

    /**
     * The job actually gets DONE under load: every enqueued item must land in a
     * container (conservation), and the drain must finish inside the budgeted window
     * — low footprint alone isn't enough, the pipeline has to deliver.
     */
    @Test
    fun `a 5000-item burst fully lands in chests - nothing lost, on schedule`() {
        registerChests(20)
        repeat(5_000) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }

        // Default budget 20/tick → theoretical minimum 250 ticks; allow 30% overhead
        // (each process takes one scheduling tick to run).
        var ticks = 0
        while ((plugin.despawnScheduler.queued > 0 || plugin.despawnProcesses.isNotEmpty()) && ticks < 325) {
            server.scheduler.performTicks(1)
            ticks++
        }
        println("LOAD throughput: 5000 items drained in $ticks ticks (theoretical minimum 250)")

        assertEquals(0, plugin.despawnScheduler.queued, "queue fully drained")
        assertEquals(0, plugin.despawnProcesses.size, "no stuck processes")
        val stored =
            (0 until 20).sumOf { i ->
                val chest = world.getBlockAt(i * 4, 64, 0).state as org.bukkit.block.Container
                chest.inventory.contents.filterNotNull().filter { it.type == Material.DIRT }.sumOf { it.amount }
            }
        assertEquals(5_000, stored, "every single item must be delivered — conservation under load")
        assertTrue(ticks <= 325, "drain must finish inside the budgeted window; took $ticks ticks")
    }

    /**
     * Operators can tune the budget up and throughput scales with it — the pipeline
     * is budget-bound, not implementation-bound.
     */
    @Test
    fun `raising max-per-tick scales throughput proportionally`() {
        io.fairyfox.papermc.despawneditems.editConfig(plugin, "performance.max-per-tick" to 100)
        registerChests(20)
        repeat(2_000) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }

        var ticks = 0
        while ((plugin.despawnScheduler.queued > 0 || plugin.despawnProcesses.isNotEmpty()) && ticks < 40) {
            server.scheduler.performTicks(1)
            ticks++
        }
        println("LOAD scaling: 2000 items at 100/tick drained in $ticks ticks (theoretical minimum 20)")
        assertEquals(0, plugin.despawnScheduler.queued, "queue drained at the raised budget")
        assertTrue(ticks <= 40, "100/tick must clear 2000 items in ≤40 ticks; took $ticks")
    }

    @Test
    fun `persistence stays consistent while flushing mid-storm`() {
        registerChests(10)
        val owner = java.util.UUID.randomUUID()
        repeat(2_000) { plugin.despawnScheduler.enqueue(ItemStack(Material.DIRT)) }

        for (tick in 0 until 50) {
            // Mutate ownership mid-drain and force synchronous flushes every 10 ticks.
            plugin.locations.add(Location(world, 200.0 + tick, 64.0, 0.0), owner)
            if (tick % 10 == 9) plugin.locations.saveNow()
            server.scheduler.performTicks(1)
        }
        plugin.locations.saveNow()

        val onDisk = YamlLocationRepository(plugin.dataFolder, plugin.logger).loadAll()
        assertEquals(
            plugin.locations.all().toSet(),
            onDisk.toSet(),
            "disk state must equal in-memory state after flushes under load",
        )
    }
}
