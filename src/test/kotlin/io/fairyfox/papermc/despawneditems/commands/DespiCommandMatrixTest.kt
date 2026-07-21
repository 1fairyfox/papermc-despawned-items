package io.fairyfox.papermc.despawneditems.commands

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.TargetingPlayerMock
import io.fairyfox.papermc.despawneditems.plain
import org.bukkit.Material
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Dispatch-level command + permission matrix: every `/despi` branch is exercised through
 * the Brigadier registration, as console, elevated player, and unprivileged player.
 */
class DespiCommandMatrixTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var player: TargetingPlayerMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        player = TargetingPlayerMock(server, "Tester")
        server.addPlayer(player)
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

    private fun drain(p: TargetingPlayerMock): List<String> {
        val all = ArrayList<String>()
        while (true) all.add((p.nextMessage() ?: break).plain())
        return all
    }

    // ─── permission matrix ──────────────────────────────────────────────────────

    @Test
    fun `unprivileged player cannot run elevated subcommands`() {
        assertTrue(player.hasPermission("despi.use"), "despi.use defaults to true")
        assertTrue(!player.hasPermission("despi.elevated"), "despi.elevated defaults to op")

        server.dispatchCommand(player, "despi locations count")
        val messages = drain(player)
        assertTrue(messages.none { it.startsWith("Locations:") }, "elevated output must not appear: $messages")
    }

    @Test
    fun `op player can run elevated subcommands`() {
        player.isOp = true
        server.dispatchCommand(player, "despi locations count")
        assertTrue(drain(player).any { it == "Locations: 0" })
    }

    @Test
    fun `player without despi use cannot run despi at all`() {
        player.addAttachment(plugin, "despi.use", false)
        lookAtStone()
        server.dispatchCommand(player, "despi add this")
        assertEquals(0, plugin.locations.count, "no location may be added without despi.use")
    }

    @Test
    fun `console can run sender-friendly elevated commands`() {
        val console = server.consoleSender
        assertTrue(server.dispatchCommand(console, "despi locations count"))
        assertTrue(server.dispatchCommand(console, "despi locations normal-mode"))
        assertTrue(server.dispatchCommand(console, "despi clear player Stranger"))
        assertTrue(server.dispatchCommand(console, "despi remove anywhere owned-by Stranger"))
        assertTrue(server.dispatchCommand(console, "despi exists anywhere owned-by Stranger"))
        assertTrue(server.dispatchCommand(console, "despi locations player Stranger"))
        assertTrue(server.dispatchCommand(console, "despi despawn"))
        assertTrue(server.dispatchCommand(console, "despi despawn count-ongoing"))
        assertTrue(server.dispatchCommand(console, "despi despawn clear-ongoing"))
        assertTrue(server.dispatchCommand(console, "despi despawn create-material dirt"))
        assertTrue(server.dispatchCommand(console, "despi despawn create-material-amount dirt 5"))
        assertTrue(server.dispatchCommand(console, "despi effects count-ongoing"))
        assertTrue(server.dispatchCommand(console, "despi effects clear-ongoing"))
        assertTrue(server.dispatchCommand(console, "despi purge owned-by-everyone materials dirt"))
        assertTrue(server.dispatchCommand(console, "despi purge owned-by-player Stranger materials dirt"))
        assertTrue(server.dispatchCommand(console, "despi reload do"))
        assertTrue(server.dispatchCommand(console, "despi save do"))
    }

    @Test
    fun `player-only commands tell the console to be a player`() {
        val console = server.consoleSender
        server.dispatchCommand(console, "despi add this")
        server.dispatchCommand(console, "despi effects create-here")
        server.dispatchCommand(console, "despi locations solo-mode")
        server.dispatchCommand(console, "despi despawn create-from-hand")
        assertEquals(0, plugin.locations.count)
        assertEquals(0, plugin.effectsPlaying.size)
    }

    // ─── full player command tree ───────────────────────────────────────────────

    @Test
    fun `every player-facing branch dispatches`() {
        player.isOp = true
        lookAtStone()

        assertTrue(server.dispatchCommand(player, "despi add this"))
        assertEquals(1, plugin.locations.count)

        assertTrue(server.dispatchCommand(player, "despi exists here owned-by-me"))
        assertTrue(server.dispatchCommand(player, "despi exists here owned-by-anyone"))
        assertTrue(server.dispatchCommand(player, "despi exists here owned-by Tester"))
        assertTrue(server.dispatchCommand(player, "despi exists anywhere owned-by-me"))
        assertTrue(server.dispatchCommand(player, "despi locations mine"))
        assertTrue(server.dispatchCommand(player, "despi locations here"))

        assertTrue(server.dispatchCommand(player, "despi add this Stranger"))
        assertTrue(server.dispatchCommand(player, "despi remove here owned-by Stranger"))
        assertTrue(server.dispatchCommand(player, "despi remove here owned-by-anyone"))
        assertEquals(0, plugin.locations.count)

        assertTrue(server.dispatchCommand(player, "despi add this"))
        assertTrue(server.dispatchCommand(player, "despi remove here owned-by-me"))
        assertTrue(server.dispatchCommand(player, "despi add this"))
        assertTrue(server.dispatchCommand(player, "despi remove anywhere owned-by-me"))
        assertTrue(server.dispatchCommand(player, "despi add this"))
        assertTrue(server.dispatchCommand(player, "despi clear here"))
        assertTrue(server.dispatchCommand(player, "despi add this"))
        assertTrue(server.dispatchCommand(player, "despi clear mine"))
        assertEquals(0, plugin.locations.count)

        assertTrue(server.dispatchCommand(player, "despi add this"))
        assertTrue(server.dispatchCommand(player, "despi locations solo-mode"))
        assertTrue(server.dispatchCommand(player, "despi locations normal-mode"))

        assertTrue(server.dispatchCommand(player, "despi purge owned-by-me materials dirt"))
        assertTrue(server.dispatchCommand(player, "despi effects create-here"))
        assertTrue(server.dispatchCommand(player, "despi effects count-ongoing"))
        assertTrue(server.dispatchCommand(player, "despi effects clear-ongoing"))
    }

    @Test
    fun `purge in-hand variants dispatch for a player`() {
        player.isOp = true
        lookAtStone()
        assertTrue(server.dispatchCommand(player, "despi add this"))
        player.inventory.setItemInMainHand(org.bukkit.inventory.ItemStack(Material.DIRT, 1))
        assertTrue(server.dispatchCommand(player, "despi purge owned-by-me in-hand"))
        assertTrue(server.dispatchCommand(player, "despi purge owned-by-everyone in-hand"))
        assertTrue(server.dispatchCommand(player, "despi purge owned-by-player Stranger in-hand"))
    }
}
