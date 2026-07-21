package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import io.fairyfox.papermc.despawneditems.stickyContainer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmokingRecipe
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DespawnIntoCookerTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var strategy: DespawnIntoCooker
    private var nextX = 0

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        strategy = DespawnIntoCooker(plugin)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun processFor(item: ItemStack) = DespawnProcess(item, plugin)

    private fun cooker(material: Material = Material.FURNACE): Block {
        nextX += 1
        return world.getBlockAt(nextX, 64, 0).also {
            it.type = material
            if (material == Material.FURNACE || material == Material.BLAST_FURNACE || material == Material.SMOKER) {
                stickyContainer(it)
            }
        }
    }

    /** The sticky state's live inventory (mutations persist; see [stickyContainer]). */
    private fun inv(block: Block): Inventory = (block.state as Container).inventory

    private fun fill(
        block: Block,
        setup: (Inventory) -> Unit,
    ) = setup(inv(block))

    @Test
    fun `applies to the three cooker types only`() {
        assertTrue(strategy.doesApply(cooker(Material.FURNACE)))
        assertTrue(strategy.doesApply(cooker(Material.BLAST_FURNACE)))
        assertTrue(strategy.doesApply(cooker(Material.SMOKER)))
        assertFalse(strategy.doesApply(cooker(Material.CHEST)))
    }

    @Test
    fun `stacks onto a matching smelt slot`() {
        val block = cooker()
        fill(block) { it.setItem(0, ItemStack(Material.COBBLESTONE, 10)) }

        val process = processFor(ItemStack(Material.COBBLESTONE, 20))
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(process, block))
        assertEquals(30, inv(block).getItem(0)?.amount)
    }

    @Test
    fun `an overflowing stack caps at max size and returns PARTIALLY`() {
        val block = cooker()
        fill(block) { it.setItem(0, ItemStack(Material.COBBLESTONE, 60)) }

        val process = processFor(ItemStack(Material.COBBLESTONE, 10))
        assertEquals(DespawnIntoResult.PARTIALLY, strategy.despawnInto(process, block))
        assertEquals(64, inv(block).getItem(0)?.amount)
        assertEquals(6, process.item?.amount, "60+10 = 64 stored + 6 leftover")
    }

    @Test
    fun `stacks onto a matching fuel slot`() {
        val block = cooker()
        fill(block) { it.setItem(1, ItemStack(Material.COAL, 5)) }

        val process = processFor(ItemStack(Material.COAL, 5))
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(process, block))
        assertEquals(10, inv(block).getItem(1)?.amount)
    }

    @Test
    fun `two occupied mismatched slots mean NONE`() {
        val block = cooker()
        fill(block) {
            it.setItem(0, ItemStack(Material.COBBLESTONE, 1))
            it.setItem(1, ItemStack(Material.COAL, 1))
        }

        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(processFor(ItemStack(Material.DIRT)), block))
    }

    @Test
    fun `fuel drops into an empty fuel slot`() {
        val block = cooker()
        val process = processFor(ItemStack(Material.COAL, 8))
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(process, block))
        assertEquals(Material.COAL, inv(block).getItem(1)?.type)
    }

    @Test
    fun `a smeltable item drops into the matching cooker's input slot`() {
        server.addRecipe(
            FurnaceRecipe(NamespacedKey(plugin, "t_iron"), ItemStack(Material.IRON_INGOT), Material.RAW_IRON, 0f, 200),
        )
        server.addRecipe(
            BlastingRecipe(NamespacedKey(plugin, "t_gold"), ItemStack(Material.GOLD_INGOT), Material.RAW_GOLD, 0f, 100),
        )
        server.addRecipe(
            SmokingRecipe(NamespacedKey(plugin, "t_beef"), ItemStack(Material.COOKED_BEEF), Material.BEEF, 0f, 100),
        )

        val furnace = cooker(Material.FURNACE)
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.RAW_IRON, 3)), furnace))
        assertEquals(Material.RAW_IRON, inv(furnace).getItem(0)?.type)

        val blast = cooker(Material.BLAST_FURNACE)
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.RAW_GOLD, 3)), blast))
        assertEquals(Material.RAW_GOLD, inv(blast).getItem(0)?.type)

        val smoker = cooker(Material.SMOKER)
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.BEEF, 3)), smoker))
        assertEquals(Material.BEEF, inv(smoker).getItem(0)?.type)

        // Not smeltable in a furnace: no recipe matches, not fuel → NONE.
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(processFor(ItemStack(Material.DIRT)), cooker()))
    }

    @Test
    fun `a null item and a non-container are NONE`() {
        val process = processFor(ItemStack(Material.DIRT))
        process.item = null
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(process, cooker()))
        assertEquals(
            DespawnIntoResult.NONE,
            strategy.despawnInto(processFor(ItemStack(Material.DIRT)), cooker(Material.STONE)),
        )
    }

    @Test
    fun `removeFrom strips by material and by item`() {
        val block = cooker()
        fill(block) { it.setItem(0, ItemStack(Material.COBBLESTONE, 10)) }
        strategy.removeFrom(Material.COBBLESTONE, block)
        assertFalse(inv(block).contains(Material.COBBLESTONE))

        fill(block) { it.setItem(1, ItemStack(Material.COAL, 4)) }
        strategy.removeFrom(ItemStack(Material.COAL, 4), block)
        assertFalse(inv(block).contains(Material.COAL))

        strategy.removeFrom(Material.DIRT, cooker(Material.STONE))
        strategy.removeFrom(ItemStack(Material.DIRT), cooker(Material.STONE))
    }
}
