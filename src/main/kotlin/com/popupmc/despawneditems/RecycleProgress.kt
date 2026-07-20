package com.popupmc.despawneditems

/**
 * Pure reward-threshold logic for `/recycle`, extracted from the command so it can be
 * unit-tested without a server. Regression guard: the old code tracked progress in
 * scoreboard objectives the plugin never created, so a reward was **never** granted;
 * this makes the "reward every [ITEMS_PER_REWARD]" rule explicit and testable.
 */
object RecycleProgress {
    /** How many recycles earn one reward. */
    const val ITEMS_PER_REWARD = 64

    /**
     * Advances the stored progress by one recycle.
     *
     * @param current the count stored so far (>= 0)
     * @return the new state: [Result.stored] to persist, whether a reward is earned, and
     *   how many recycles [Result.remaining] until the next reward.
     */
    fun advance(current: Int): Result {
        val next = current.coerceAtLeast(0) + 1
        return if (next >= ITEMS_PER_REWARD) {
            Result(stored = 0, rewarded = true, remaining = 0)
        } else {
            Result(stored = next, rewarded = false, remaining = ITEMS_PER_REWARD - next)
        }
    }

    data class Result(val stored: Int, val rewarded: Boolean, val remaining: Int)
}
