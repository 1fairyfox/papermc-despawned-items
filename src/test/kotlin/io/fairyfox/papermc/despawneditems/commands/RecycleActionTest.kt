package io.fairyfox.papermc.despawneditems.commands

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.RecycleProgress
import io.fairyfox.papermc.despawneditems.plain
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** `/recycle` and `/despi recycle`, including PDC progress and the reward drop. */
class RecycleActionTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var player: PlayerMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        player = server.addPlayer()
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun progressKey() = NamespacedKey(plugin, "recycle_progress")

    private fun drain(): List<String> {
        val all = ArrayList<String>()
        while (true) all.add((player.nextMessage() ?: break).plain())
        return all
    }

    @Test
    fun `recycling with an empty hand explains itself`() {
        assertTrue(server.dispatchCommand(player, "recycle"))
        assertTrue(drain().any { it.contains("nothing in your hand") })
    }

    @Test
    fun `recycling consumes the held item and advances progress`() {
        player.inventory.setItemInMainHand(ItemStack(Material.DIRT, 5))
        server.dispatchCommand(player, "recycle")

        assertTrue(player.inventory.itemInMainHand.type.isAir, "hand should be emptied")
        val stored = player.persistentDataContainer.get(progressKey(), PersistentDataType.INTEGER)
        assertEquals(1, stored)
        val messages = drain()
        assertTrue(messages.any { it == "Done!" })
        assertTrue(messages.any { it.contains("left before a random item") })
    }

    @Test
    fun `the threshold recycle grants a dropped reward and resets progress`() {
        player.persistentDataContainer.set(
            progressKey(),
            PersistentDataType.INTEGER,
            RecycleProgress.ITEMS_PER_REWARD - 1,
        )
        player.inventory.setItemInMainHand(ItemStack(Material.COBBLESTONE, 1))
        server.dispatchCommand(player, "recycle")

        assertEquals(0, player.persistentDataContainer.get(progressKey(), PersistentDataType.INTEGER))
        assertTrue(drain().any { it.contains("You earned a random item") })
        assertTrue(
            player.world.entities.filterIsInstance<Item>().isNotEmpty(),
            "a reward item should be dropped at the player",
        )
    }

    @Test
    fun `despi recycle is an equivalent alternative`() {
        player.inventory.setItemInMainHand(ItemStack(Material.DIRT, 1))
        assertTrue(server.dispatchCommand(player, "despi recycle"))
        assertTrue(player.inventory.itemInMainHand.type.isAir)
        assertEquals(1, player.persistentDataContainer.get(progressKey(), PersistentDataType.INTEGER))
    }

    @Test
    fun `recycle is gated on its permission`() {
        player.addAttachment(plugin, "recycle.use", false)
        player.inventory.setItemInMainHand(ItemStack(Material.DIRT, 1))
        server.dispatchCommand(player, "recycle")
        assertEquals(Material.DIRT, player.inventory.itemInMainHand.type, "item must not be consumed")

        server.dispatchCommand(player, "despi recycle")
        assertEquals(Material.DIRT, player.inventory.itemInMainHand.type, "despi recycle equally gated")
    }
}
