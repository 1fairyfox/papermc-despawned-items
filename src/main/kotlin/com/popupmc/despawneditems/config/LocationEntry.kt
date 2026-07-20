package com.popupmc.despawneditems.config

import com.popupmc.despawneditems.DespawnedItems
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.UUID

/**
 * A single despawn location: a block position plus the UUID of the player who
 * owns it. Serialised to `x;y;z;world` in each player's userdata file.
 *
 * The [matches] overloads compare by value against another entry, a raw
 * [Location], a location + owner, or an owner alone.
 */
class LocationEntry(private val plugin: DespawnedItems) {

    lateinit var location: Location
    lateinit var owner: UUID

    constructor(location: Location, owner: UUID, plugin: DespawnedItems) : this(plugin) {
        this.location = location
        this.owner = owner
    }

    override fun toString(): String {
        return "${location.blockX};${location.blockY};${location.blockZ};${location.world.name}"
    }

    fun matches(other: LocationEntry): Boolean =
        location.blockX == other.location.blockX &&
            location.blockY == other.location.blockY &&
            location.blockZ == other.location.blockZ &&
            location.world == other.location.world &&
            owner == other.owner

    fun matches(loc: Location): Boolean =
        location.blockX == loc.blockX &&
            location.blockY == loc.blockY &&
            location.blockZ == loc.blockZ &&
            location.world == loc.world

    fun matches(loc: Location, owner: UUID): Boolean =
        location.blockX == loc.blockX &&
            location.blockY == loc.blockY &&
            location.blockZ == loc.blockZ &&
            location.world == loc.world &&
            this.owner == owner

    fun matches(owner: UUID): Boolean = this.owner == owner

    private fun loadFromString(str: String, owner: UUID): Boolean {
        val parts = str.split(";")
        if (parts.size < 4) {
            plugin.logger.warning("ERROR: Location entry, wrong number of args $str")
            return false
        }

        return try {
            val x = parts[0].toInt()
            val y = parts[1].toInt()
            val z = parts[2].toInt()
            val world = Bukkit.getWorld(parts[3])

            if (world == null) {
                plugin.logger.warning("ERROR: Location entry world is null $str")
                return false
            }

            this.location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
            this.owner = owner
            true
        } catch (ex: NumberFormatException) {
            plugin.logger.warning("ERROR: Unable to parse location entry $str")
            false
        }
    }

    companion object {
        fun fromString(str: String, owner: UUID, plugin: DespawnedItems): LocationEntry? {
            val entry = LocationEntry(plugin)
            return if (entry.loadFromString(str, owner)) entry else null
        }
    }
}
