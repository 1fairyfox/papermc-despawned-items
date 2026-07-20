package com.popupmc.despawneditems.despawn

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.config.LocationEntry
import java.util.Random

/**
 * A draw-without-replacement index over all despawn locations. Each call to
 * [randomChestCoord] returns a random location and removes it from the pool,
 * rebuilding the pool automatically once it is exhausted.
 */
class DespawnIndexes(private val plugin: DespawnedItems) {

    val locationEntryIndexes: MutableList<Int> = mutableListOf()
    private val random = Random()

    init {
        rebuildIndexes()
    }

    fun rebuildIndexes() {
        locationEntryIndexes.clear()
        for (i in plugin.settings.fileLocations.locationEntries.indices) {
            locationEntryIndexes.add(i)
        }
    }

    /** Returns a random, not-yet-drawn despawn location. */
    fun randomChestCoord(): LocationEntry {
        if (locationEntryIndexes.isEmpty()) rebuildIndexes()

        val randomIndex = random.nextInt(locationEntryIndexes.size)
        val locationIndex = locationEntryIndexes[randomIndex]
        val entry = plugin.settings.fileLocations.locationEntries[locationIndex]
        locationEntryIndexes.removeAt(randomIndex)
        return entry
    }
}
