package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DespawnIntoVoidTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var strategy: DespawnIntoVoid

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        strategy = DespawnIntoVoid(plugin)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun processFor(item: ItemStack) = DespawnProcess(item, plugin)

    @Test
    fun `applies to every block`() {
        assertTrue(strategy.doesApply(world.getBlockAt(0, 64, 0)))
        world.getBlockAt(1, 64, 0).type = Material.CHEST
        assertTrue(strategy.doesApply(world.getBlockAt(1, 64, 0)))
    }

    @Test
    fun `contraband items are destroyed`() {
        for (material in listOf(Material.NETHERITE_INGOT, Material.COMMAND_BLOCK, Material.ANCIENT_DEBRIS, Material.DEBUG_STICK)) {
            val item = ItemStack(material, 5)
            val result = strategy.despawnInto(processFor(item), world.getBlockAt(0, 64, 0))
            assertEquals(DespawnIntoResult.CONTRABAND, result, "$material is contraband")
            assertTrue(item.type.isAir || item.amount == 0, "$material must be voided")
        }
    }

    @Test
    fun `ordinary items pass through untouched`() {
        val item = ItemStack(Material.DIRT, 5)
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(processFor(item), world.getBlockAt(0, 64, 0)))
        assertEquals(5, item.amount)
    }

    @Test
    fun `a null process item is NONE and removeFrom is a no-op`() {
        val process = processFor(ItemStack(Material.DIRT))
        process.item = null
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(process, world.getBlockAt(0, 64, 0)))
        strategy.removeFrom(Material.DIRT, world.getBlockAt(0, 64, 0))
        strategy.removeFrom(ItemStack(Material.DIRT), world.getBlockAt(0, 64, 0))
    }
}
