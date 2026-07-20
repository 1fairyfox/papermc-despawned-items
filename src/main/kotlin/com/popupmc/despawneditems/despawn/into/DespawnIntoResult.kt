package com.popupmc.despawneditems.despawn.into

/** Outcome of offering an item to a despawn strategy. */
enum class DespawnIntoResult {
    /** This strategy took nothing; try the next strategy. */
    NONE,

    /** Some of the stack was placed; continue with the remainder elsewhere. */
    PARTIALLY,

    /** The whole stack was placed; the process is done. */
    FULLY,

    /** The item was illegal and was destroyed; the process is done. */
    CONTRABAND,
}
