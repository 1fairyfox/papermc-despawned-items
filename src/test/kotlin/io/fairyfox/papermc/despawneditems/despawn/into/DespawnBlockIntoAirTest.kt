package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DespawnBlockIntoAirTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var strategy: DespawnBlockIntoAir

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        strategy = DespawnBlockIntoAir(plugin)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun processFor(item: ItemStack) = DespawnProcess(item, plugin)

    @Test
    fun `applies only to empty air with no nearby entities`() {
        assertTrue(strategy.doesApply(world.getBlockAt(0, 64, 0)))

        world.getBlockAt(1, 64, 0).type = Material.STONE
        assertFalse(strategy.doesApply(world.getBlockAt(1, 64, 0)))

        world.spawnEntity(Location(world, 5.5, 64.5, 5.5), EntityType.ZOMBIE)
        assertFalse(strategy.doesApply(world.getBlockAt(5, 64, 5)))
    }

    @Test
    fun `a single block item is placed FULLY`() {
        val block = world.getBlockAt(0, 64, 0)
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.DIRT, 1)), block))
        assertEquals(Material.DIRT, block.type)
    }

    @Test
    fun `a stack places one block and keeps the rest`() {
        val block = world.getBlockAt(0, 64, 0)
        val process = processFor(ItemStack(Material.DIRT, 5))
        assertEquals(DespawnIntoResult.PARTIALLY, strategy.despawnInto(process, block))
        assertEquals(Material.DIRT, block.type)
        assertEquals(4, process.item?.amount)
    }

    @Test
    fun `hazardous and non-block items are refused`() {
        val block = world.getBlockAt(0, 64, 0)
        // In order: known-dangerous list, gravity, redstone family, button family,
        // pressure-plate family, infested family, and not-a-block-at-all.
        val refused =
            listOf(
                Material.TNT,
                Material.SAND,
                Material.REDSTONE_TORCH,
                Material.STONE_BUTTON,
                Material.OAK_PRESSURE_PLATE,
                Material.INFESTED_STONE,
                Material.DIAMOND_SWORD,
            )
        for (material in refused) {
            assertEquals(
                DespawnIntoResult.NONE,
                strategy.despawnInto(processFor(ItemStack(material, 1)), block),
                "$material must not be placed",
            )
            assertTrue(block.type.isAir, "$material must leave the block empty")
        }
    }

    @Test
    fun `a null item is NONE and removeFrom is deliberately a no-op`() {
        val process = processFor(ItemStack(Material.DIRT))
        process.item = null
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(process, world.getBlockAt(0, 64, 0)))

        world.getBlockAt(2, 64, 0).type = Material.DIRT
        strategy.removeFrom(Material.DIRT, world.getBlockAt(2, 64, 0))
        strategy.removeFrom(ItemStack(Material.DIRT), world.getBlockAt(2, 64, 0))
        assertEquals(Material.DIRT, world.getBlockAt(2, 64, 0).type, "placed blocks are not retrieved")
    }

    @Test
    fun `copyBlockToLocation copies a plain block without metadata`() {
        val block = world.getBlockAt(3, 64, 0)
        DespawnBlockIntoAir.copyBlockToLocation(ItemStack(Material.COBBLESTONE), block)
        assertEquals(Material.COBBLESTONE, block.type)
    }

    @Test
    fun `copyBlockToLocation copies a block with plain item meta`() {
        val item = ItemStack(Material.COBBLESTONE)
        item.itemMeta =
            item.itemMeta?.also {
                it.displayName(net.kyori.adventure.text.Component.text("named"))
            }
        val block = world.getBlockAt(4, 64, 0)
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.COBBLESTONE, block.type)
    }
}
