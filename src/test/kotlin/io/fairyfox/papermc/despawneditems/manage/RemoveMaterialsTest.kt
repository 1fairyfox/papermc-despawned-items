package io.fairyfox.papermc.despawneditems.manage

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.SyncChunkWorldMock
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.plain
import io.fairyfox.papermc.despawneditems.stickyContainer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tick-spread bulk removal ([RemoveMaterials]) across registered despawn locations. */
class RemoveMaterialsTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: SyncChunkWorldMock
    private lateinit var player: PlayerMock
    private val owner: UUID = UUID.randomUUID()
    private val other: UUID = UUID.randomUUID()

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = SyncChunkWorldMock()
        server.addWorld(world)
        player = server.addPlayer()
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun chestAt(
        x: Int,
        ownedBy: UUID,
        contents: ItemStack? = null,
    ): Block {
        val block = world.getBlockAt(x, 64, 0)
        block.type = Material.CHEST
        stickyContainer(block)
        contents?.let { (block.state as Container).inventory.addItem(it) }
        plugin.locations.add(Location(world, x.toDouble(), 64.0, 0.0), ownedBy)
        return block
    }

    private fun inv(block: Block) = (block.state as Container).inventory

    private fun drain(): List<String> {
        val all = ArrayList<String>()
        while (true) all.add((player.nextMessage() ?: break).plain())
        return all
    }

    @Test
    fun `removes a material from every registered location`() {
        val first = chestAt(0, owner, ItemStack(Material.DIRT, 32))
        val second = chestAt(8, other, ItemStack(Material.DIRT, 16))

        RemoveMaterials(player, listOf(Material.DIRT), null, plugin, null, player.uniqueId)
        server.scheduler.performTicks(6)

        assertFalse(inv(first).contains(Material.DIRT), "first chest purged")
        assertFalse(inv(second).contains(Material.DIRT), "second chest purged")
        assertTrue(drain().any { it == "Completed!" })
        assertFalse(plugin.removeMaterialsInst.containsKey(player.uniqueId), "instance unregistered when done")
    }

    @Test
    fun `an owner filter only touches that owner's locations`() {
        val mine = chestAt(0, owner, ItemStack(Material.DIRT, 8))
        val theirs = chestAt(8, other, ItemStack(Material.DIRT, 8))

        RemoveMaterials(player, listOf(Material.DIRT), null, plugin, owner, player.uniqueId)
        server.scheduler.performTicks(6)

        assertFalse(inv(mine).contains(Material.DIRT))
        assertTrue(inv(theirs).contains(Material.DIRT), "other owner's chest untouched")
    }

    @Test
    fun `an exact item purge removes matching stacks`() {
        val block = chestAt(0, owner, ItemStack(Material.STONE, 12))

        RemoveMaterials(player, null, ItemStack(Material.STONE, 12), plugin, null, player.uniqueId)
        server.scheduler.performTicks(4)

        assertFalse(inv(block).contains(Material.STONE))
    }

    @Test
    fun `refuses to start with neither materials nor item`() {
        chestAt(0, owner)
        RemoveMaterials(player, null, null, plugin, null, player.uniqueId)
        assertTrue(drain().any { it.startsWith("ERROR") }, "both-null must refuse")
    }

    @Test
    fun `completes immediately when there are no locations`() {
        RemoveMaterials(player, listOf(Material.DIRT), null, plugin, null, player.uniqueId)
        assertTrue(drain().any { it == "Completed!" })
    }

    @Test
    fun `completes immediately when the owner has no locations`() {
        chestAt(0, other)
        RemoveMaterials(player, listOf(Material.DIRT), null, plugin, owner, player.uniqueId)
        assertTrue(drain().any { it == "Completed!" })
    }

    @Test
    fun `a new run for the same sender replaces the old one`() {
        chestAt(0, owner, ItemStack(Material.DIRT, 4))
        val first = RemoveMaterials(player, listOf(Material.DIRT), null, plugin, null, player.uniqueId)
        val second = RemoveMaterials(player, listOf(Material.STONE), null, plugin, null, player.uniqueId)
        assertEquals(second, plugin.removeMaterialsInst[player.uniqueId], "second replaces first")
        first.forceSelfDestroy() // idempotent, no crash
        server.scheduler.performTicks(4)
    }

    @Test
    fun `unloaded worlds are skipped without dying`() {
        plugin.locations.store.add(DespawnLocation("no_such_world", 0, 64, 0, owner))
        val block = chestAt(0, owner, ItemStack(Material.DIRT, 4))

        RemoveMaterials(player, listOf(Material.DIRT), null, plugin, null, player.uniqueId)
        server.scheduler.performTicks(6)

        assertFalse(inv(block).contains(Material.DIRT), "loaded location still processed")
        assertTrue(drain().any { it == "Completed!" })
    }

    @Test
    fun `progress is reported on long runs`() {
        for (x in 0 until 25) chestAt(x * 4, owner, ItemStack(Material.DIRT, 1))
        RemoveMaterials(player, listOf(Material.DIRT), null, plugin, null, player.uniqueId)
        server.scheduler.performTicks(30)
        assertTrue(drain().any { it.contains("Still processing") }, "progress message every 20 locations")
    }
}
