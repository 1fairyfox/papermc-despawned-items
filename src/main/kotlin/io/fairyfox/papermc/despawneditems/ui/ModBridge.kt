package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.limit.DespawnLimits
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.location.TargetOptions
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

/**
 * The **client-mod bridge** — the server half of the button that a client mod draws inside
 * the chest / furnace / barrel screen.
 *
 * ### Why the button lives in a client mod
 *
 * A container screen in Minecraft has exactly as many slots as the container has, and they
 * are all real storage. A server plugin cannot add a widget to it: the only server-side way
 * to fake one is to replace the whole screen with a custom inventory, which risks losing or
 * duplicating the player's items and fights every storage/sorting mod on the server. A
 * client mod, by contrast, adds a button to the existing screen in a few lines and never
 * touches the slots at all.
 *
 * So the split is: the **client mod draws and clicks**, the **server owns the truth**. This
 * class is that server side, and it is deliberately usable by *any* mod — the protocol is
 * plain text on a namespaced channel, documented below, with no library to link against.
 *
 * ### Channel
 *
 * ```
 * papermc-despawned-items:targets
 * ```
 *
 * **Handshake.** A mod says hello; the server answers with what it will allow. This is what
 * lets a mod render a *real* interface instead of guessing — and lets it hide that interface
 * entirely on a server that does not want it.
 *
 * ```
 * → HELLO <client-protocol-version>
 * ← WELCOME <server-protocol-version> <capability> <capability> …
 * ← UNAVAILABLE <reason>          # server has client-mod support off, or you lack permission
 * ```
 *
 * **Server → client** (state):
 *
 * ```
 * TARGET  <world> <x> <y> <z> <owner-uuid> <enabled> <priority> <contraband>
 * ABSENT  <world> <x> <y> <z>          # that block is not a despawn target
 * DENIED  <world> <x> <y> <z> <reason> # the request was refused, with a human reason
 * ```
 *
 * **Client → server** (requests; every one is re-validated server-side):
 *
 * ```
 * QUERY    <world> <x> <y> <z>          # what is the state of this block?
 * MARK     <world> <x> <y> <z>          # register it as a despawn target
 * UNMARK   <world> <x> <y> <z>          # unregister it
 * TOGGLE   <world> <x> <y> <z>          # flip enabled/disabled, keeping the registration
 * PRIORITY <world> <x> <y> <z> <n>      # set draw weight 1–10
 * CONTRABAND <world> <x> <y> <z> <bool> # opt this target in or out of receiving banned items
 * ```
 *
 * ### The server owner is in charge
 *
 * Every one of these is refused unless **both** `targets.client-mod.enabled` and the
 * player's `despi.client` permission allow it — see [ClientAccess]. A server that wants
 * nothing to do with client mods switches one config key and the protocol goes silent;
 * `/despi` keeps working exactly as before, for everyone, on any client.
 *
 * ### Trust model
 *
 * **Nothing the client sends is trusted.** Every request re-checks, server-side: that the
 * sender has the permission, that the block is close enough to actually be interacting with
 * it, that the player owns the target (or is elevated), and — for `MARK` — that they are
 * under their configured location limit. A modified client can therefore do nothing through
 * this channel that it could not already do by typing the commands.
 *
 * ### Conflict and synergy
 *
 * The channel name is namespaced to this plugin, so it cannot collide. Nothing is ever sent
 * to a client that has not registered the channel, so vanilla and unmodded-client players
 * are completely unaffected. And because the protocol is text on an open channel, a
 * *different* mod — a storage manager, a minimap, an admin HUD — can implement the same
 * three verbs and interoperate without this plugin knowing it exists.
 */
class ModBridge(private val plugin: PaperMcDespawnedItems) : PluginMessageListener {
    val channel: String get() = "$NAMESPACE:$CHANNEL_NAME"

    fun register() {
        runCatching {
            plugin.server.messenger.registerOutgoingPluginChannel(plugin, channel)
            plugin.server.messenger.registerIncomingPluginChannel(plugin, channel, this)
        }.onFailure { plugin.logger.warning("Could not register the mod bridge channel: ${it.message}") }
    }

    fun unregister() {
        runCatching {
            plugin.server.messenger.unregisterOutgoingPluginChannel(plugin, channel)
            plugin.server.messenger.unregisterIncomingPluginChannel(plugin, channel)
        }
    }

    // ─── server → client ────────────────────────────────────────────────────────────

    /** Encodes one target as the documented `TARGET …` line. */
    fun encode(target: DespawnLocation): String =
        listOf(
            "TARGET",
            target.world,
            target.x.toString(),
            target.y.toString(),
            target.z.toString(),
            target.owner.toString(),
            target.options.enabled.toString(),
            target.options.priority.toString(),
            target.options.acceptContraband.toString(),
        ).joinToString(" ")

    /** Encodes "this block is not a despawn target". */
    fun encodeAbsent(location: Location): String = "ABSENT ${location.world.name} ${location.blockX} ${location.blockY} ${location.blockZ}"

    private fun send(
        player: Player,
        message: String,
    ) {
        if (!plugin.settings.targetUi.clientModEnabled) return
        if (!player.listeningPluginChannels.contains(channel)) return
        runCatching { player.sendPluginMessage(plugin, channel, message.toByteArray()) }
    }

    /** Tells [player] the state of one target. */
    fun sendTargetState(
        player: Player,
        target: DespawnLocation,
    ) = send(player, encode(target))

    /** Tells [player] that [location] is not registered — so the button can render "off". */
    fun sendAbsent(
        player: Player,
        location: Location,
    ) = send(player, encodeAbsent(location))

    /** Sends whatever the current truth is for [location]: a `TARGET` line or an `ABSENT` one. */
    fun sendStateFor(
        player: Player,
        location: Location,
    ) {
        val target =
            plugin.locations.targetAt(location, player.uniqueId)
                ?: plugin.locations.anyTargetAt(location)
        if (target == null) sendAbsent(player, location) else sendTargetState(player, target)
    }

    /** Announces a change to every listening player. */
    fun broadcastTargetChanged(target: DespawnLocation) {
        if (!plugin.settings.targetUi.clientModEnabled) return
        val message = encode(target).toByteArray()
        for (player in plugin.server.onlinePlayers) {
            if (!player.listeningPluginChannels.contains(channel)) continue
            runCatching { player.sendPluginMessage(plugin, channel, message) }
        }
    }

    // ─── client → server ────────────────────────────────────────────────────────────

    override fun onPluginMessageReceived(
        channelName: String,
        player: Player,
        message: ByteArray,
    ) {
        if (channelName != channel) return
        handle(player, String(message))
    }

    /**
     * Parses and executes one client request. Split out from
     * [onPluginMessageReceived] so tests drive it directly without a network stack.
     */
    fun handle(
        player: Player,
        raw: String,
    ) {
        val parts = raw.trim().split(' ')
        if (parts.isEmpty()) return
        val verb = parts[0].uppercase()

        // The handshake is answered first and separately: a refused client still gets a
        // clear UNAVAILABLE so it can hide its interface rather than showing dead buttons.
        if (verb == "HELLO") {
            handshake(player)
            return
        }

        val denial = ClientAccess.denialFor(plugin, player)
        if (denial != null) {
            send(player, "UNAVAILABLE ${denial.reason}")
            return
        }

        if (parts.size < MIN_REQUEST_FIELDS) return
        val location = parseLocation(parts) ?: return

        // Anti-spoof: a client may only act on a block it could plausibly be looking at.
        // Without this, a modified client could toggle targets anywhere in the world.
        if (player.world != location.world || player.location.distanceSquared(location) > MAX_REACH_SQUARED) {
            deny(player, location, "too far away")
            return
        }

        dispatch(player, verb, location, parts.getOrNull(VALUE_INDEX))
    }

    /** Routes one validated request to its handler. Unknown verbs are ignored so the protocol can grow compatibly. */
    private fun dispatch(
        player: Player,
        verb: String,
        location: Location,
        value: String?,
    ) {
        when (verb) {
            "QUERY" -> sendStateFor(player, location)
            "MARK" -> mark(player, location)
            "UNMARK" -> unmark(player, location)
            "TOGGLE" -> toggle(player, location)
            "PRIORITY" -> setPriority(player, location, value)
            "CONTRABAND" -> setContraband(player, location, value)
            else -> Unit
        }
    }

    /** Answers a client's HELLO with either its capabilities or the reason it is refused. */
    private fun handshake(player: Player) {
        val denial = ClientAccess.denialFor(plugin, player)
        if (denial != null) {
            send(player, "UNAVAILABLE ${denial.reason}")
            return
        }
        val capabilities = ClientAccess.capabilitiesFor(plugin, player)
        send(player, "WELCOME ${ClientAccess.PROTOCOL_VERSION} ${capabilities.joinToString(" ")}".trim())
    }

    private fun setPriority(
        player: Player,
        location: Location,
        rawValue: String?,
    ) {
        val value = rawValue?.toIntOrNull() ?: return
        if (plugin.locations.targetAt(location, player.uniqueId) == null) {
            deny(player, location, "not yours to change")
            return
        }
        val updated =
            plugin.locations.updateOptions(location, player.uniqueId) {
                it.copy(priority = value.coerceIn(TargetOptions.MIN_PRIORITY, TargetOptions.MAX_PRIORITY))
            }
        if (updated != null) {
            sendTargetState(player, updated)
            broadcastTargetChanged(updated)
        }
    }

    private fun setContraband(
        player: Player,
        location: Location,
        rawValue: String?,
    ) {
        val value = rawValue?.toBooleanStrictOrNull() ?: return
        if (plugin.locations.targetAt(location, player.uniqueId) == null) {
            deny(player, location, "not yours to change")
            return
        }
        val updated = plugin.locations.updateOptions(location, player.uniqueId) { it.copy(acceptContraband = value) }
        if (updated != null) {
            sendTargetState(player, updated)
            broadcastTargetChanged(updated)
        }
    }

    private fun parseLocation(parts: List<String>): Location? {
        val world = Bukkit.getWorld(parts[WORLD_INDEX]) ?: return null
        val x = parts[X_INDEX].toIntOrNull() ?: return null
        val y = parts[Y_INDEX].toIntOrNull() ?: return null
        val z = parts[Z_INDEX].toIntOrNull() ?: return null
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    /** Registers the block as a despawn target, honouring the player's location limit. */
    private fun mark(
        player: Player,
        location: Location,
    ) {
        if (plugin.locations.targetAt(location, player.uniqueId) != null) {
            sendStateFor(player, location)
            return
        }
        val limitSettings = plugin.settings.limits
        if (!DespawnLimits.canAddAnother(player, plugin.locations.countOfOwner(player.uniqueId), limitSettings)) {
            deny(player, location, "at your despawn-location limit")
            return
        }
        plugin.locations.add(location, player.uniqueId)
        plugin.locations.targetAt(location, player.uniqueId)?.let {
            sendTargetState(player, it)
            broadcastTargetChanged(it)
        }
    }

    private fun unmark(
        player: Player,
        location: Location,
    ) {
        if (plugin.locations.targetAt(location, player.uniqueId) == null) {
            deny(player, location, "not yours to remove")
            return
        }
        plugin.locations.remove(location, player.uniqueId)
        sendAbsent(player, location)
    }

    private fun toggle(
        player: Player,
        location: Location,
    ) {
        val owned = plugin.locations.targetAt(location, player.uniqueId)
        if (owned == null) {
            deny(player, location, "not yours to change")
            return
        }
        val updated = plugin.locations.updateOptions(location, player.uniqueId) { it.copy(enabled = !it.enabled) }
        if (updated != null) {
            sendTargetState(player, updated)
            broadcastTargetChanged(updated)
        }
    }

    private fun deny(
        player: Player,
        location: Location,
        reason: String,
    ) = send(player, "DENIED ${location.world.name} ${location.blockX} ${location.blockY} ${location.blockZ} $reason")

    private companion object {
        const val NAMESPACE = "papermc-despawned-items"
        const val CHANNEL_NAME = "targets"

        const val MIN_REQUEST_FIELDS = 5
        const val WORLD_INDEX = 1
        const val X_INDEX = 2
        const val Y_INDEX = 3
        const val Z_INDEX = 4
        const val VALUE_INDEX = 5

        /** Squared reach limit for a client request (8 blocks — comfortably past vanilla reach). */
        const val MAX_REACH_SQUARED = 64.0
    }
}
