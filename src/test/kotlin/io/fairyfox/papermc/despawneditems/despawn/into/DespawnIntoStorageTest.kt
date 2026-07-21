package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import io.fairyfox.papermc.despawneditems.stickyContainer
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DespawnIntoStorageTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var strategy: DespawnIntoStorage

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        strategy = DespawnIntoStorage(plugin)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun processFor(item: ItemStack) = DespawnProcess(item, plugin)

    private fun blockOf(material: Material): Block =
        world.getBlockAt(0, 64, 0).also {
            it.type = material
            if (material == Material.CHEST) stickyContainer(it)
        }

    /** The sticky state's live inventory (mutations persist; see [stickyContainer]). */
    private fun inv(block: Block): Inventory = (block.state as Container).inventory

    private fun fill(
        block: Block,
        setup: (Inventory) -> Unit,
    ) = setup(inv(block))

    @Test
    fun `applies to every storage block and nothing else`() {
        val storage =
            listOf(
                Material.BARREL,
                Material.CHEST,
                Material.DISPENSER,
                Material.DROPPER,
                Material.HOPPER,
                Material.SHULKER_BOX,
                Material.TRAPPED_CHEST,
            )
        for (material in storage) {
            assertTrue(strategy.doesApply(blockOf(material)), "$material is storage")
        }
        assertFalse(strategy.doesApply(blockOf(Material.FURNACE)))
        assertFalse(strategy.doesApply(blockOf(Material.STONE)))
    }

    @Test
    fun `an empty chest takes the whole stack`() {
        val block = blockOf(Material.CHEST)
        val process = processFor(ItemStack(Material.DIRT, 64))
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(process, block))
        assertTrue(inv(block).contains(Material.DIRT, 64))
    }

    @Test
    fun `a full chest returns PARTIALLY and reconstructs the leftover`() {
        val block = blockOf(Material.CHEST)
        fill(block) { inventory ->
            for (slot in 0 until inventory.size) inventory.setItem(slot, ItemStack(Material.STONE, 64))
        }
        val process = processFor(ItemStack(Material.DIRT, 40))
        assertEquals(DespawnIntoResult.PARTIALLY, strategy.despawnInto(process, block))
        assertEquals(40, process.item?.amount, "the untaken remainder rides on for the next location")
        assertEquals(Material.DIRT, process.item?.type)
    }

    @Test
    fun `a nearly-full chest takes what fits - duplication invariant`() {
        val block = blockOf(Material.CHEST)
        fill(block) { inventory ->
            for (slot in 1 until inventory.size) inventory.setItem(slot, ItemStack(Material.STONE, 64))
            inventory.setItem(0, ItemStack(Material.DIRT, 60))
        }

        val process = processFor(ItemStack(Material.DIRT, 10))
        assertEquals(DespawnIntoResult.PARTIALLY, strategy.despawnInto(process, block))

        val stored = inv(block).contents.filterNotNull().filter { it.type == Material.DIRT }.sumOf { it.amount }
        val leftover = process.item?.amount ?: 0
        assertEquals(60 + 10, stored + leftover, "in = stored + leftover")
    }

    @Test
    fun `a non-container is NONE and a null item is NONE`() {
        assertEquals(
            DespawnIntoResult.NONE,
            strategy.despawnInto(processFor(ItemStack(Material.DIRT)), blockOf(Material.STONE)),
        )
        val process = processFor(ItemStack(Material.DIRT))
        process.item = null
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(process, blockOf(Material.CHEST)))
    }

    @Test
    fun `removeFrom strips a material and an exact item`() {
        val block = blockOf(Material.CHEST)
        fill(block) { inventory ->
            inventory.addItem(ItemStack(Material.DIRT, 10), ItemStack(Material.STONE, 5))
        }

        strategy.removeFrom(Material.DIRT, block)
        assertFalse(inv(block).contains(Material.DIRT))
        assertTrue(inv(block).contains(Material.STONE, 5))

        strategy.removeFrom(ItemStack(Material.STONE, 5), block)
        assertFalse(inv(block).contains(Material.STONE))

        // Non-container removals are silent no-ops.
        strategy.removeFrom(Material.DIRT, blockOf(Material.STONE))
        strategy.removeFrom(ItemStack(Material.DIRT), blockOf(Material.STONE))
        assertNull(blockOf(Material.STONE).state as? Container)
    }
}
