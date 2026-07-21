package io.fairyfox.papermc.despawneditems.despawn

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.SyncChunkWorldMock
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.stickyContainer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end pipeline tests: a [DespawnProcess] walks real (mocked) world blocks through
 * the strategy chain tick by tick. [SyncChunkWorldMock] completes chunk loads
 * synchronously, so each location attempt costs exactly one tick.
 */
class DespawnProcessTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: SyncChunkWorldMock
    private val owner: UUID = UUID.randomUUID()

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

    private fun chestAt(
        x: Int,
        y: Int,
        z: Int,
    ): org.bukkit.block.Block {
        val block = world.getBlockAt(x, y, z)
        block.type = Material.CHEST
        stickyContainer(block)
        plugin.locations.add(Location(world, x.toDouble(), y.toDouble(), z.toDouble()), owner)
        return block
    }

    /** The sticky state's live inventory (mutations persist; see [stickyContainer]). */
    private fun inv(block: org.bukkit.block.Block) = (block.state as Container).inventory

    private fun fill(
        block: org.bukkit.block.Block,
        setup: (org.bukkit.inventory.Inventory) -> Unit,
    ) = setup(inv(block))

    @Test
    fun `an item is fully placed into a registered chest`() {
        val chest = chestAt(0, 64, 0)
        DespawnProcess(ItemStack(Material.DIRT, 64), plugin)

        server.scheduler.performTicks(3)

        assertTrue(inv(chest).contains(Material.DIRT, 64), "all 64 dirt should land in the chest")
        assertEquals(0, plugin.despawnProcesses.size, "process should self-destroy when done")
    }

    @Test
    fun `a successful placement plays the landing effect`() {
        chestAt(0, 64, 0)
        DespawnProcess(ItemStack(Material.DIRT, 1), plugin)
        server.scheduler.performTicks(3)
        assertTrue(plugin.effectsPlaying.isNotEmpty(), "an effect should play at the chest")
    }

    @Test
    fun `contraband is destroyed instead of placed`() {
        val chest = chestAt(0, 64, 0)
        DespawnProcess(ItemStack(Material.NETHERITE_INGOT, 3), plugin)

        server.scheduler.performTicks(3)

        assertTrue(inv(chest).isEmpty, "contraband must never be stored")
        assertEquals(0, plugin.despawnProcesses.size)
    }

    @Test
    fun `with no locations the process dies immediately`() {
        DespawnProcess(ItemStack(Material.DIRT), plugin)
        assertEquals(0, plugin.despawnProcesses.size)
    }

    @Test
    fun `an unloaded world counts as a failed attempt and the process expires`() {
        plugin.locations.store.add(DespawnLocation("no_such_world", 0, 64, 0, owner))
        DespawnProcess(ItemStack(Material.DIRT), plugin)
        assertEquals(1, plugin.despawnProcesses.size)

        server.scheduler.performTicks(3)

        assertEquals(0, plugin.despawnProcesses.size, "process should exhaust its location budget")
    }

    @Test
    fun `a full chest yields a partial result and the process moves on`() {
        val first = chestAt(0, 64, 0)
        fill(first) { inv ->
            for (slot in 0 until inv.size) inv.setItem(slot, ItemStack(Material.STONE, 64))
        }
        val second = chestAt(8, 64, 8)

        DespawnProcess(ItemStack(Material.DIRT, 10), plugin)
        server.scheduler.performTicks(6)

        assertTrue(inv(second).contains(Material.DIRT, 10), "leftovers should land in the second chest")
        assertEquals(0, plugin.despawnProcesses.size)
    }

    @Test
    fun `selfDestroy invalidates a pending process`() {
        chestAt(0, 64, 0)
        val process = DespawnProcess(ItemStack(Material.DIRT), plugin)
        process.selfDestroy()
        assertTrue(process.invalid)
        server.scheduler.performTicks(3)
        assertEquals(0, plugin.despawnProcesses.size)
    }

    @Test
    fun `duplication invariant - items in equals items stored plus leftover`() {
        val chest = chestAt(0, 64, 0)
        // Leave exactly one free slot and a partial stack: 60 dirt fits before overflow.
        fill(chest) { inv ->
            for (slot in 2 until inv.size) inv.setItem(slot, ItemStack(Material.STONE, 64))
            inv.setItem(0, ItemStack(Material.DIRT, 60))
        }

        DespawnProcess(ItemStack(Material.DIRT, 64), plugin)
        server.scheduler.performTicks(3)

        val dirtInChest =
            inv(chest).contents.filterNotNull().filter { it.type == Material.DIRT }.sumOf { it.amount }
        assertEquals(60 + 64, dirtInChest, "no dirt may be duplicated or lost")
    }
}
