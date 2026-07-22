package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.SyncChunkWorldMock
import io.fairyfox.papermc.despawneditems.location.TargetOptions
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Behaviour and — more importantly — **conflict-safety** tests for the in-world toggle
 * button.
 *
 * The riskiest thing about adding an interaction listener to a Minecraft server is that it
 * quietly steals clicks other plugins and mods wanted. Each rule in
 * [TargetInteractListener]'s contract is pinned here, because a regression would show up on
 * someone's server as "your plugin broke my shops" rather than as a failing build.
 */
class TargetButtonTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: SyncChunkWorldMock
    private lateinit var listener: TargetInteractListener
    private lateinit var player: PlayerMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = SyncChunkWorldMock()
        server.addWorld(world)
        listener = TargetInteractListener(plugin)
        player = server.addPlayer()
    }

    @AfterTest
    fun tearDown() = MockBukkit.unmock()

    private fun chest(
        x: Int,
        y: Int,
        z: Int,
    ): Block = world.getBlockAt(x, y, z).also { it.type = Material.CHEST }

    private fun registerTarget(
        block: Block,
        owner: UUID = player.uniqueId,
    ): Location = block.location.also { plugin.locations.add(it, owner) }

    private fun interact(
        block: Block,
        item: ItemStack?,
        hand: EquipmentSlot = EquipmentSlot.HAND,
        action: Action = Action.RIGHT_CLICK_BLOCK,
    ): PlayerInteractEvent = PlayerInteractEvent(player, action, item, block, org.bukkit.block.BlockFace.UP, hand)

    private fun wand(): ItemStack = TargetWand.create(plugin)

    /**
     * True when the listener claimed the interaction. The listener denies both halves of
     * [PlayerInteractEvent] rather than calling the deprecated `setCancelled`, so "did we
     * take this click" is asked the same way here as it is answered there.
     */
    private fun handled(event: PlayerInteractEvent): Boolean = event.useInteractedBlock() == Event.Result.DENY

    // --- the wand itself ---------------------------------------------------------------

    @Test
    fun `a created wand is recognised and a lookalike is not`() {
        val real = wand()
        assertTrue(TargetWand.isWand(plugin, real))

        // Same material, no tag — exactly what another mod's item would look like.
        val lookalike = ItemStack(real.type)
        assertFalse(TargetWand.isWand(plugin, lookalike), "material alone must never identify a wand")

        assertFalse(TargetWand.isWand(plugin, null))
        assertFalse(TargetWand.isWand(plugin, ItemStack(Material.AIR)))
    }

    // --- conflict safety ---------------------------------------------------------------

    @Test
    fun `without a wand the event is left completely untouched`() {
        val block = chest(0, 64, 0)
        registerTarget(block)

        val event = interact(block, ItemStack(Material.STONE))
        listener.onInteract(event)

        assertFalse(handled(event), "ordinary chest interaction must pass straight through")
    }

    @Test
    fun `an empty hand is left untouched`() {
        val block = chest(0, 64, 0)
        registerTarget(block)

        val event = interact(block, null)
        listener.onInteract(event)

        assertFalse(handled(event))
    }

    @Test
    fun `a wand click on a block that is not a target is left untouched`() {
        val block = chest(0, 64, 0) // deliberately NOT registered

        val event = interact(block, wand())
        listener.onInteract(event)

        assertFalse(handled(event), "the wand must behave like an ordinary item everywhere else")
    }

    @Test
    fun `the off hand is ignored so the toggle cannot double-fire`() {
        val block = chest(0, 64, 0)
        val location = registerTarget(block)
        player.isSneaking = true

        val event = interact(block, wand(), hand = EquipmentSlot.OFF_HAND)
        listener.onInteract(event)

        assertFalse(handled(event))
        assertTrue(
            plugin.locations.targetAt(location, player.uniqueId)!!.enabled,
            "an off-hand event must not flip the target",
        )
    }

    @Test
    fun `a left click is ignored`() {
        val block = chest(0, 64, 0)
        registerTarget(block)

        val event = interact(block, wand(), action = Action.LEFT_CLICK_BLOCK)
        listener.onInteract(event)

        assertFalse(handled(event))
    }

    @Test
    fun `a denied block interaction is respected`() {
        val block = chest(0, 64, 0)
        val location = registerTarget(block)
        player.isSneaking = true

        val event = interact(block, wand())
        event.setUseInteractedBlock(Event.Result.DENY) // as a protection plugin would

        listener.onInteract(event)

        assertTrue(
            plugin.locations.targetAt(location, player.uniqueId)!!.enabled,
            "a protection plugin's DENY must win",
        )
    }

    @Test
    fun `the feature does nothing at all when switched off in config`() {
        io.fairyfox.papermc.despawneditems.editConfig(plugin, "targets.button.enabled" to false)
        val block = chest(0, 64, 0)
        registerTarget(block)
        player.isSneaking = true

        val event = interact(block, wand())
        listener.onInteract(event)

        assertFalse(handled(event))
    }

    @Test
    fun `a player cannot toggle someone else's target without the elevated permission`() {
        val block = chest(0, 64, 0)
        val stranger = UUID.randomUUID()
        val location = registerTarget(block, owner = stranger)
        player.isSneaking = true

        val event = interact(block, wand())
        listener.onInteract(event)

        assertFalse(handled(event), "not theirs — the event is not even claimed")
        assertTrue(plugin.locations.targetAt(location, stranger)!!.enabled)
    }

    // --- the actual feature ------------------------------------------------------------

    @Test
    fun `sneak plus right click toggles the target off and on again`() {
        val block = chest(0, 64, 0)
        val location = registerTarget(block)
        player.isSneaking = true

        listener.onInteract(interact(block, wand()))
        assertFalse(plugin.locations.targetAt(location, player.uniqueId)!!.enabled, "first click switches it off")

        listener.onInteract(interact(block, wand()))
        assertTrue(plugin.locations.targetAt(location, player.uniqueId)!!.enabled, "second click switches it back on")
    }

    @Test
    fun `toggling off keeps the registration rather than deleting it`() {
        val block = chest(0, 64, 0)
        val location = registerTarget(block)
        player.isSneaking = true

        listener.onInteract(interact(block, wand()))

        assertEquals(1, plugin.locations.count, "the target is switched off, NOT unregistered")
        assertEquals(0, plugin.locations.enabledCount)
        assertNotNull(plugin.locations.targetAt(location, player.uniqueId))
    }

    @Test
    fun `a non-sneaking wand click opens the options menu`() {
        val block = chest(0, 64, 0)
        registerTarget(block)
        player.isSneaking = false

        val event = interact(block, wand())
        listener.onInteract(event)

        assertTrue(handled(event))
        assertTrue(
            player.openInventory.topInventory.holder is TargetMenuHolder,
            "the opened screen must be ours, identified by holder type",
        )
    }

    // --- the menu ----------------------------------------------------------------------

    @Test
    fun `the menu renders the current state`() {
        val inventory =
            org.bukkit.Bukkit.createInventory(null, TargetMenuSlots.SIZE)
        TargetMenu.render(inventory, TargetOptions(enabled = true, priority = 3))

        assertEquals(Material.LIME_DYE, inventory.getItem(TargetMenuSlots.TOGGLE)?.type, "ON is green")
        assertEquals(3, inventory.getItem(TargetMenuSlots.PRIORITY)?.amount, "priority shows as the stack size")

        TargetMenu.render(inventory, TargetOptions(enabled = false))
        assertEquals(Material.GRAY_DYE, inventory.getItem(TargetMenuSlots.TOGGLE)?.type, "OFF is grey")
    }

    // --- the mod bridge ----------------------------------------------------------------

    @Test
    fun `the bridge encodes a target in the documented line format`() {
        val target =
            io.fairyfox.papermc.despawneditems.location.DespawnLocation(
                "world",
                1,
                2,
                3,
                player.uniqueId,
                TargetOptions(enabled = false, priority = 5, acceptContraband = true),
            )
        val encoded = plugin.modBridge.encode(target)

        assertEquals(
            "TARGET world 1 2 3 ${player.uniqueId} false 5 true",
            encoded,
            "the wire format is a documented contract for other mods — changing it is a breaking change",
        )
    }

    @Test
    fun `the bridge channel is namespaced to this plugin`() {
        assertEquals("papermc-despawned-items:targets", plugin.modBridge.channel)
    }

    @Test
    fun `broadcasting to a player who is not listening is a no-op, not an error`() {
        val target =
            io.fairyfox.papermc.despawneditems.location.DespawnLocation("world", 0, 0, 0, player.uniqueId)
        // The player has registered no channels; this must simply do nothing.
        plugin.modBridge.broadcastTargetChanged(target)
        plugin.modBridge.sendTargetState(player, target)
    }
}
