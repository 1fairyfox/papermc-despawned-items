package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.SyncChunkWorldMock
import io.fairyfox.papermc.despawneditems.config.CatchAllTarget
import io.fairyfox.papermc.despawneditems.editConfig
import io.fairyfox.papermc.despawneditems.stickyContainer
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MockBukkit tests for [CatchAllDelivery] — the safety net that stops banned and
 * randomly-voided items from simply ceasing to exist.
 */
class CatchAllDeliveryTest {
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

    private fun chestAt(
        x: Int,
        y: Int,
        z: Int,
    ): Block =
        world.getBlockAt(x, y, z).also {
            it.type = Material.CHEST
            stickyContainer(it)
        }

    private fun inv(block: Block): Inventory = (block.state as Container).inventory

    private fun coord(
        x: Int,
        y: Int,
        z: Int,
    ) = "${world.name};$x;$y;$z"

    private fun enableCatchAll(
        vararg locations: String,
        mode: String = "first",
    ) = editConfig(
        plugin,
        "void.catch-all.enabled" to true,
        "void.catch-all.mode" to mode,
        "void.catch-all.locations" to locations.toList(),
    )

    @Test
    fun `an item is delivered into the configured catch-all chest`() {
        val chest = chestAt(0, 64, 0)
        enableCatchAll(coord(0, 64, 0))

        var delivered: Boolean? = null
        plugin.catchAll.deliver(ItemStack(Material.DIAMOND, 5)) { delivered = it }

        assertEquals(true, delivered)
        assertTrue(inv(chest).contains(Material.DIAMOND, 5))
    }

    @Test
    fun `delivery is a no-op when the catch-all is disabled`() {
        chestAt(0, 64, 0)
        var delivered: Boolean? = null
        plugin.catchAll.deliver(ItemStack(Material.DIAMOND)) { delivered = it }
        assertEquals(false, delivered, "disabled catch-all reports non-delivery rather than pretending")
    }

    @Test
    fun `delivery reports failure when enabled but no locations are configured`() {
        editConfig(plugin, "void.catch-all.enabled" to true, "void.catch-all.locations" to emptyList<String>())
        assertFalse(plugin.settings.voiding.catchAllUsable, "enabled with nowhere to put things is not usable")

        var delivered: Boolean? = null
        plugin.catchAll.deliver(ItemStack(Material.DIAMOND)) { delivered = it }
        assertEquals(false, delivered)
    }

    @Test
    fun `a full first chest falls through to the next one`() {
        val first = chestAt(0, 64, 0)
        val second = chestAt(1, 64, 0)
        // Pack the first chest solid so nothing more fits.
        val firstInv = inv(first)
        repeat(firstInv.size) { slot -> firstInv.setItem(slot, ItemStack(Material.STONE, 64)) }

        enableCatchAll(coord(0, 64, 0), coord(1, 64, 0))

        var delivered: Boolean? = null
        plugin.catchAll.deliver(ItemStack(Material.DIAMOND, 3)) { delivered = it }

        assertEquals(true, delivered)
        assertTrue(inv(second).contains(Material.DIAMOND, 3), "the overflow lands in the second catch-all")
    }

    @Test
    fun `a non-container target is skipped`() {
        world.getBlockAt(0, 64, 0).type = Material.STONE
        val real = chestAt(1, 64, 0)
        enableCatchAll(coord(0, 64, 0), coord(1, 64, 0))

        var delivered: Boolean? = null
        plugin.catchAll.deliver(ItemStack(Material.GOLD_INGOT, 2)) { delivered = it }

        assertEquals(true, delivered)
        assertTrue(inv(real).contains(Material.GOLD_INGOT, 2))
    }

    @Test
    fun `an unknown world is skipped rather than throwing`() {
        val real = chestAt(0, 64, 0)
        enableCatchAll("no_such_world;0;64;0", coord(0, 64, 0))

        var delivered: Boolean? = null
        plugin.catchAll.deliver(ItemStack(Material.EMERALD)) { delivered = it }

        assertEquals(true, delivered)
        assertTrue(inv(real).contains(Material.EMERALD))
    }

    @Test
    fun `every target full reports non-delivery`() {
        val chest = chestAt(0, 64, 0)
        val chestInv = inv(chest)
        repeat(chestInv.size) { slot -> chestInv.setItem(slot, ItemStack(Material.STONE, 64)) }
        enableCatchAll(coord(0, 64, 0))

        var delivered: Boolean? = null
        plugin.catchAll.deliver(ItemStack(Material.DIAMOND)) { delivered = it }

        assertEquals(false, delivered, "when nothing fits anywhere the caller must be told")
    }

    @Test
    fun `random mode still delivers`() {
        val a = chestAt(0, 64, 0)
        val b = chestAt(1, 64, 0)
        enableCatchAll(coord(0, 64, 0), coord(1, 64, 0), mode = "random")

        plugin.catchAll.deliver(ItemStack(Material.REDSTONE, 7))

        val total = inv(a).contains(Material.REDSTONE, 7) || inv(b).contains(Material.REDSTONE, 7)
        assertTrue(total, "random mode picks one of the configured chests, not none of them")
    }

    @Test
    fun `catch-all target parsing accepts good input and rejects bad`() {
        assertEquals(CatchAllTarget("world", 1, 2, 3), CatchAllTarget.parse("world;1;2;3"))
        assertEquals(CatchAllTarget("nether", -5, 64, -9), CatchAllTarget.parse(" nether ; -5 ; 64 ; -9 "))
        assertNull(CatchAllTarget.parse("world;1;2"), "too few fields")
        assertNull(CatchAllTarget.parse("world;1;2;3;4"), "too many fields")
        assertNull(CatchAllTarget.parse(";1;2;3"), "empty world")
        assertNull(CatchAllTarget.parse("world;x;2;3"), "non-integer coordinate")
        assertNull(CatchAllTarget.parse(""), "empty string")
    }

    @Test
    fun `malformed configured locations are skipped, not fatal`() {
        val chest = chestAt(0, 64, 0)
        enableCatchAll("garbage", "also;bad", coord(0, 64, 0))

        assertEquals(1, plugin.settings.voiding.catchAllTargets.size, "only the well-formed entry survives load")

        var delivered: Boolean? = null
        plugin.catchAll.deliver(ItemStack(Material.COAL)) { delivered = it }
        assertEquals(true, delivered)
        assertTrue(inv(chest).contains(Material.COAL))
    }
}
