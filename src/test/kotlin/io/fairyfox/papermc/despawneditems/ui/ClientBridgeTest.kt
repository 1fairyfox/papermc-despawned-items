package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.SyncChunkWorldMock
import io.fairyfox.papermc.despawneditems.TargetingPlayerMock
import io.fairyfox.papermc.despawneditems.editConfig
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.location.TargetOptions
import io.fairyfox.papermc.despawneditems.plain
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the two surfaces that replaced the wand: the `/despi target …` commands (which
 * work on every client) and the client-mod bridge (which is optional, gated, and never
 * trusted).
 */
class ClientBridgeTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: SyncChunkWorldMock
    private lateinit var player: TargetingPlayerMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = SyncChunkWorldMock()
        server.addWorld(world)
        player = TargetingPlayerMock(server, "Tester")
        server.addPlayer(player)
    }

    @AfterTest
    fun tearDown() = MockBukkit.unmock()

    private fun chest(
        x: Int = 0,
        y: Int = 64,
        z: Int = 0,
    ): Block = world.getBlockAt(x, y, z).also { it.type = Material.CHEST }

    /**
     * Registers a target, points the player's ray trace at it, and stands them next to it —
     * the bridge re-checks reach server-side, so a test player parked at world origin would
     * be (correctly) refused.
     */
    private fun lookingAtTarget(owner: UUID = player.uniqueId): Location {
        val block = chest()
        player.target = block
        standNextTo(block)
        plugin.locations.add(block.location, owner)
        return block.location
    }

    private fun standNextTo(block: Block) = player.teleport(block.location.clone().add(1.0, 0.0, 0.0))

    private fun run(command: String) = server.dispatchCommand(player, command)

    private fun lastMessage(): String = player.nextMessage().plain()

    // ── /despi target … ─────────────────────────────────────────────────────────────

    @Test
    fun `target toggle switches a target off and on again`() {
        val location = lookingAtTarget()

        run("despi target toggle")
        assertFalse(plugin.locations.targetAt(location, player.uniqueId)!!.enabled)

        run("despi target toggle")
        assertTrue(plugin.locations.targetAt(location, player.uniqueId)!!.enabled)
    }

    @Test
    fun `target disable and enable are explicit and idempotent`() {
        val location = lookingAtTarget()

        run("despi target disable")
        run("despi target disable")
        assertFalse(plugin.locations.targetAt(location, player.uniqueId)!!.enabled, "disable twice stays disabled")

        run("despi target enable")
        run("despi target enable")
        assertTrue(plugin.locations.targetAt(location, player.uniqueId)!!.enabled)
    }

    @Test
    fun `disabling keeps the registration rather than deleting it`() {
        val location = lookingAtTarget()
        run("despi target disable")

        assertEquals(1, plugin.locations.count, "switched off, NOT unregistered")
        assertEquals(0, plugin.locations.enabledCount)
        assertNotNull(plugin.locations.targetAt(location, player.uniqueId))
    }

    @Test
    fun `target priority is set and reported`() {
        val location = lookingAtTarget()
        run("despi target priority 7")
        assertEquals(7, plugin.locations.targetAt(location, player.uniqueId)!!.options.priority)
    }

    @Test
    fun `target contraband opts in and out`() {
        val location = lookingAtTarget()

        run("despi target contraband accept")
        assertTrue(plugin.locations.targetAt(location, player.uniqueId)!!.options.acceptContraband)

        run("despi target contraband refuse")
        assertFalse(plugin.locations.targetAt(location, player.uniqueId)!!.options.acceptContraband)
    }

    @Test
    fun `target info reports the current settings`() {
        lookingAtTarget()
        run("despi target priority 4")
        player.nextMessage() // consume the priority confirmation
        run("despi target info")
        val message = lastMessage()
        assertTrue(message.contains("ON"), "info should say whether it is on: $message")
        assertTrue(message.contains("priority 4"), "info should report the priority: $message")
    }

    @Test
    fun `a block that is not a target explains itself instead of failing silently`() {
        val block = chest()
        player.target = block // looked at, but never registered

        run("despi target toggle")
        assertTrue(lastMessage().contains("not a despawn target"), "the player must be told why nothing happened")
    }

    @Test
    fun `a player cannot change someone else's target without the elevated permission`() {
        val stranger = UUID.randomUUID()
        val location = lookingAtTarget(owner = stranger)

        run("despi target disable")

        assertTrue(plugin.locations.targetAt(location, stranger)!!.enabled, "the stranger's target is untouched")
        assertTrue(lastMessage().contains("belongs to someone else"))
    }

    // ── client access gating ────────────────────────────────────────────────────────

    @Test
    fun `client mods are allowed by default`() {
        assertNull(ClientAccess.denialFor(plugin, player))
    }

    @Test
    fun `a server owner can switch client mods off entirely`() {
        editConfig(plugin, "targets.client-mod.enabled" to false)
        assertEquals(ClientAccess.Denial.SERVER_DISABLED, ClientAccess.denialFor(plugin, player))
    }

    @Test
    fun `a player without the client permission is refused`() {
        val attachment = player.addAttachment(plugin)
        attachment.setPermission(ClientAccess.PERMISSION, false)
        player.recalculatePermissions()

        assertEquals(ClientAccess.Denial.NO_PERMISSION, ClientAccess.denialFor(plugin, player))
    }

    @Test
    fun `capabilities describe what the player may actually do`() {
        val capabilities = ClientAccess.capabilitiesFor(plugin, player)
        assertTrue("manage-own" in capabilities, "despi.use defaults to true")
        assertTrue("recycle" in capabilities, "recycle.use defaults to true")
        assertFalse("manage-others" in capabilities, "despi.elevated defaults to op")
    }

    // ── the wire protocol ───────────────────────────────────────────────────────────

    @Test
    fun `the encoded target line is the documented contract`() {
        val target =
            DespawnLocation(
                "world",
                1,
                2,
                3,
                player.uniqueId,
                TargetOptions(enabled = false, priority = 5, acceptContraband = true),
            )
        assertEquals(
            "TARGET world 1 2 3 ${player.uniqueId} false 5 true",
            plugin.modBridge.encode(target),
            "changing this format is a breaking change for every integrating mod",
        )
    }

    @Test
    fun `the channel is namespaced to this plugin`() {
        assertEquals("papermc-despawned-items:targets", plugin.modBridge.channel)
    }

    @Test
    fun `an absent block encodes as ABSENT so a button can render off`() {
        val location = Location(world, 4.0, 5.0, 6.0)
        assertEquals("ABSENT ${world.name} 4 5 6", plugin.modBridge.encodeAbsent(location))
    }

    @Test
    fun `requests from a client are ignored when the server has client mods off`() {
        editConfig(plugin, "targets.client-mod.enabled" to false)
        val location = lookingAtTarget()

        plugin.modBridge.handle(player, "TOGGLE ${world.name} 0 64 0")

        assertTrue(
            plugin.locations.targetAt(location, player.uniqueId)!!.enabled,
            "a server that refuses client mods must not act on their requests",
        )
    }

    @Test
    fun `a request for a far-away block is refused`() {
        val location = lookingAtTarget()
        player.teleport(Location(world, 500.0, 64.0, 500.0))

        plugin.modBridge.handle(player, "TOGGLE ${world.name} 0 64 0")

        assertTrue(
            plugin.locations.targetAt(location, player.uniqueId)!!.enabled,
            "reach is re-checked server-side, so a modified client cannot act at range",
        )
    }

    @Test
    fun `a client cannot change a target it does not own`() {
        val stranger = UUID.randomUUID()
        val location = lookingAtTarget(owner = stranger)

        plugin.modBridge.handle(player, "TOGGLE ${world.name} 0 64 0")

        assertTrue(plugin.locations.targetAt(location, stranger)!!.enabled)
    }

    @Test
    fun `a client request does toggle a target the player owns`() {
        val location = lookingAtTarget()

        plugin.modBridge.handle(player, "TOGGLE ${world.name} 0 64 0")

        assertFalse(plugin.locations.targetAt(location, player.uniqueId)!!.enabled)
    }

    @Test
    fun `MARK registers a block and UNMARK removes it`() {
        val block = chest()
        player.target = block
        standNextTo(block)

        plugin.modBridge.handle(player, "MARK ${world.name} 0 64 0")
        assertNotNull(plugin.locations.targetAt(block.location, player.uniqueId), "MARK registers the block")

        plugin.modBridge.handle(player, "UNMARK ${world.name} 0 64 0")
        assertNull(plugin.locations.targetAt(block.location, player.uniqueId), "UNMARK removes it")
    }

    @Test
    fun `MARK respects the player's location limit`() {
        editConfig(plugin, "limits.default" to 0)
        val block = chest()
        standNextTo(block)

        plugin.modBridge.handle(player, "MARK ${world.name} 0 64 0")

        assertNull(
            plugin.locations.targetAt(block.location, player.uniqueId),
            "the limit is enforced on the client path exactly as on the command path",
        )
    }

    @Test
    fun `PRIORITY and CONTRABAND are applied and clamped`() {
        val location = lookingAtTarget()

        plugin.modBridge.handle(player, "PRIORITY ${world.name} 0 64 0 99")
        assertEquals(
            TargetOptions.MAX_PRIORITY,
            plugin.locations.targetAt(location, player.uniqueId)!!.options.priority,
            "an out-of-range value from a client is clamped, not trusted",
        )

        plugin.modBridge.handle(player, "CONTRABAND ${world.name} 0 64 0 true")
        assertTrue(plugin.locations.targetAt(location, player.uniqueId)!!.options.acceptContraband)
    }

    @Test
    fun `malformed and unknown requests are ignored rather than throwing`() {
        lookingAtTarget()
        plugin.modBridge.handle(player, "")
        plugin.modBridge.handle(player, "TOGGLE")
        plugin.modBridge.handle(player, "TOGGLE nosuchworld 0 64 0")
        plugin.modBridge.handle(player, "TOGGLE ${world.name} x y z")
        plugin.modBridge.handle(player, "FLARGLE ${world.name} 0 64 0")
        plugin.modBridge.handle(player, "PRIORITY ${world.name} 0 64 0 notanumber")
    }
}
