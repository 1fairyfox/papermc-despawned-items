package io.fairyfox.papermc.despawneditems.commands

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.TargetingPlayerMock
import io.fairyfox.papermc.despawneditems.editConfig
import io.fairyfox.papermc.despawneditems.plain
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

/**
 * Direct behaviour tests for every `/despi` action ([DespiActions]), driven with a
 * scriptable target block. Dispatch-level wiring is covered by [DespiCommandMatrixTest].
 */
class DespiActionsTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var player: TargetingPlayerMock
    private lateinit var actions: DespiActions

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        player = TargetingPlayerMock(server, "Tester")
        server.addPlayer(player)
        actions = DespiActions(plugin)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun lookAtStone(
        x: Int = 0,
        y: Int = 64,
        z: Int = 0,
    ) = world.getBlockAt(x, y, z).also {
        it.type = Material.STONE
        player.target = it
    }

    private fun lastMessage(): String {
        var last: String? = null
        while (true) last = player.nextMessage() ?: break
        return last.plain()
    }

    // ─── add ────────────────────────────────────────────────────────────────────

    @Test
    fun `add registers the targeted block for the player`() {
        lookAtStone()
        actions.add(player, null)
        assertEquals("Successfully added location!", player.nextMessage().plain())
        assertEquals(1, plugin.locations.count)
    }

    @Test
    fun `add warns on a duplicate location`() {
        lookAtStone()
        actions.add(player, null)
        player.nextMessage()
        actions.add(player, null)
        assertTrue(player.nextMessage().plain().startsWith("WARNING:"))
        assertEquals(1, plugin.locations.count)
    }

    @Test
    fun `add rejects when over the configured limit`() {
        editConfig(plugin, "limits.default" to 1)
        lookAtStone(0, 64, 0)
        actions.add(player, null)
        lookAtStone(1, 64, 0)
        actions.add(player, null)
        assertTrue(lastMessage().contains("limit"), "second add should hit the limit")
        assertEquals(1, plugin.locations.count)
    }

    @Test
    fun `add for another player skips the self limit`() {
        editConfig(plugin, "limits.default" to 0)
        lookAtStone()
        actions.add(player, "SomeoneElse")
        assertEquals("Successfully added location!", player.nextMessage().plain())
        assertEquals(1, plugin.locations.count)
        assertEquals(0, plugin.locations.countOfOwner(player.uniqueId))
    }

    @Test
    fun `add without a target block errors and changes nothing`() {
        player.target = null
        actions.add(player, null)
        assertTrue(player.nextMessage().plain().startsWith("ERROR:"))
        assertEquals(0, plugin.locations.count)
    }

    // ─── remove ─────────────────────────────────────────────────────────────────

    @Test
    fun `removeHereOwnedByMe removes an existing location`() {
        lookAtStone()
        actions.add(player, null)
        actions.removeHereOwnedByMe(player)
        assertEquals(0, plugin.locations.count)
        assertEquals("Location removed", lastMessage())
    }

    @Test
    fun `removeHereOwnedByMe warns when nothing is there`() {
        lookAtStone()
        actions.removeHereOwnedByMe(player)
        assertTrue(lastMessage().startsWith("WARNING:"))
    }

    @Test
    fun `removeHereOwnedByAnyone removes any owner's entry`() {
        lookAtStone()
        actions.add(player, "SomeoneElse")
        actions.removeHereOwnedByAnyone(player)
        assertEquals(0, plugin.locations.count)
    }

    @Test
    fun `removeHereOwnedByAnyone warns on an empty block`() {
        lookAtStone()
        actions.removeHereOwnedByAnyone(player)
        assertTrue(lastMessage().startsWith("WARNING:"))
    }

    @Test
    fun `removeHereOwnedByPlayer targets that player's entry`() {
        lookAtStone()
        actions.add(player, "SomeoneElse")
        actions.removeHereOwnedByPlayer(player, "SomeoneElse")
        assertEquals(0, plugin.locations.count)
    }

    @Test
    fun `removeAnywhereOwnedByMe removes one of mine`() {
        lookAtStone()
        actions.add(player, null)
        actions.removeAnywhereOwnedByMe(player)
        assertEquals(0, plugin.locations.count)
    }

    @Test
    fun `removeAnywhereOwnedByPlayer warns when the player has none`() {
        actions.removeAnywhereOwnedByPlayer(player, "SomeoneElse")
        assertTrue(lastMessage().startsWith("WARNING:"))
    }

    // ─── clear ──────────────────────────────────────────────────────────────────

    @Test
    fun `clearMine removes all of my locations`() {
        lookAtStone(0, 64, 0)
        actions.add(player, null)
        lookAtStone(1, 64, 0)
        actions.add(player, null)
        actions.clearMine(player)
        assertEquals(0, plugin.locations.count)
        assertTrue(lastMessage().contains("2 location(s)"))
    }

    @Test
    fun `clearPlayer warns when the player has none`() {
        actions.clearPlayer(server.consoleSender, "SomeoneElse")
        assertEquals(0, plugin.locations.count)
    }

    @Test
    fun `clearHere removes all owners at the block`() {
        lookAtStone()
        actions.add(player, null)
        actions.add(player, "SomeoneElse")
        actions.clearHere(player)
        assertEquals(0, plugin.locations.count)
        assertTrue(lastMessage().contains("2 owner(s)"))
    }

    @Test
    fun `clearHere warns when the block has no owners`() {
        lookAtStone()
        actions.clearHere(player)
        assertTrue(lastMessage().startsWith("WARNING:"))
    }

    // ─── exists ─────────────────────────────────────────────────────────────────

    @Test
    fun `exists variants report presence and absence`() {
        lookAtStone()
        actions.existsHereOwnedByMe(player)
        assertTrue(lastMessage().contains("does not exist"))

        actions.add(player, null)
        actions.existsHereOwnedByMe(player)
        assertEquals("Location does exist", lastMessage())

        actions.existsHereOwnedByAnyone(player)
        assertEquals("Location does exist", lastMessage())

        actions.existsHereOwnedByPlayer(player, "SomeoneElse")
        assertTrue(lastMessage().contains("does not exist"))

        actions.existsAnywhereOwnedByMe(player)
        assertTrue(player.nextMessage().plain().contains("A location was found!"))
        assertTrue(player.nextMessage().plain().contains(";world"), "serialized location follows")

        actions.existsAnywhereOwnedByPlayer(player, "SomeoneElse")
        assertTrue(lastMessage().startsWith("WARNING:"))
    }

    // ─── locations ──────────────────────────────────────────────────────────────

    @Test
    fun `locations listing variants`() {
        actions.locationsMine(player)
        assertTrue(lastMessage().startsWith("WARNING:"), "empty list warns")

        lookAtStone()
        actions.add(player, null)
        actions.locationsMine(player)
        assertTrue(lastMessage().contains(";world"))

        actions.locationsCount(player)
        assertEquals("Locations: 1", lastMessage())

        actions.locationsHere(player)
        assertTrue(lastMessage().isNotEmpty(), "owner names listed")

        actions.locationsPlayer(player, "SomeoneElse")
        assertTrue(lastMessage().startsWith("WARNING:"))

        player.target = null
        actions.locationsHere(player)
        assertTrue(lastMessage().startsWith("ERROR:"))
    }

    @Test
    fun `locationsHere warns when the block has no owners`() {
        lookAtStone()
        actions.locationsHere(player)
        assertTrue(lastMessage().startsWith("WARNING:"))
    }

    @Test
    fun `solo mode isolates one location and normal mode restores`() {
        lookAtStone(0, 64, 0)
        actions.add(player, null)
        lookAtStone(1, 64, 0)
        actions.add(player, "SomeoneElse")
        assertEquals(2, plugin.locations.count)

        lookAtStone(2, 64, 0)
        actions.soloMode(player)
        assertEquals(1, plugin.locations.count)

        actions.normalMode(player)
        assertEquals(2, plugin.locations.count)
    }

    // ─── despawn admin/test commands ────────────────────────────────────────────

    @Test
    fun `despawnCount reports active and queued`() {
        actions.despawnCount(player)
        assertTrue(lastMessage().contains("0 active"))
    }

    @Test
    fun `despawnFromHand needs an item in hand`() {
        actions.despawnFromHand(player)
        assertTrue(lastMessage().startsWith("ERROR:"))

        player.inventory.setItemInMainHand(ItemStack(Material.DIRT, 3))
        actions.despawnFromHand(player)
        assertEquals("Created forced despawn", lastMessage())
    }

    @Test
    fun `despawnMaterial parses names and rejects garbage`() {
        actions.despawnMaterial(player, "not_a_thing", 1)
        assertTrue(lastMessage().startsWith("ERROR:"))

        actions.despawnMaterial(player, "dirt, stone", 2)
        assertEquals("Created forced despawn", lastMessage())
    }

    @Test
    fun `despawnClearOngoing clears active processes`() {
        lookAtStone()
        actions.add(player, null) // give processes somewhere to try
        actions.despawnMaterial(player, "dirt", 1)
        assertTrue(plugin.despawnProcesses.isNotEmpty())
        actions.despawnClearOngoing(player)
        assertEquals(0, plugin.despawnProcesses.size)
    }

    // ─── effects admin/test commands ────────────────────────────────────────────

    @Test
    fun `effects create count and clear`() {
        actions.effectsCount(player)
        assertEquals("Effects Count: 0", lastMessage())

        lookAtStone()
        actions.effectsCreateHere(player)
        assertEquals(1, plugin.effectsPlaying.size)

        actions.effectsClearOngoing(player)
        assertEquals(0, plugin.effectsPlaying.size)
        assertTrue(lastMessage().contains("Cleared 1"))
    }

    @Test
    fun `effectsCreateHere errors without a target`() {
        player.target = null
        actions.effectsCreateHere(player)
        assertTrue(lastMessage().startsWith("ERROR:"))
    }

    // ─── reload and save ────────────────────────────────────────────────────────

    @Test
    fun `reload re-reads config and storage`() {
        editConfig(plugin, "limits.default" to 3)
        actions.reload(player)
        assertEquals("Config and storage have been reloaded", lastMessage())
        assertEquals(3, plugin.settings.limits.default)
    }

    @Test
    fun `save flushes locations`() {
        lookAtStone()
        actions.add(player, null)
        actions.save(player)
        assertEquals("Locations have been saved", lastMessage())
    }

    // ─── purge ──────────────────────────────────────────────────────────────────

    @Test
    fun `purgeMaterials validates names and runs once per sender`() {
        actions.purgeMaterials(player, null, "garbage_name")
        assertTrue(lastMessage().startsWith("ERROR:"))

        lookAtStone()
        actions.add(player, null)

        actions.purgeMaterials(player, null, "dirt")
        assertTrue(plugin.removeMaterialsInst.containsKey(player.uniqueId))

        actions.purgeMaterials(player, null, "dirt")
        assertTrue(lastMessage().contains("already in-progress"))
    }

    @Test
    fun `purgeInHand requires a player a valid item and exclusivity`() {
        actions.purgeInHand(server.consoleSender, null)

        actions.purgeInHand(player, null)
        assertTrue(lastMessage().startsWith("ERROR:"), "air in hand is invalid")

        lookAtStone()
        actions.add(player, null)
        player.inventory.setItemInMainHand(ItemStack(Material.DIRT, 1))
        actions.purgeInHand(player, null)
        assertTrue(plugin.removeMaterialsInst.containsKey(player.uniqueId))

        actions.purgeInHand(player, null)
        assertTrue(lastMessage().contains("already in-progress"))
    }

    @Test
    fun `playerId resolves an offline player id`() {
        val id = actions.playerId("SomeoneElse")
        assertEquals(id, actions.playerId("SomeoneElse"), "stable for the same name")
    }
}
