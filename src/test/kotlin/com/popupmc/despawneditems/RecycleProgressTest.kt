package com.popupmc.despawneditems

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression tests (§87) for the `/recycle` reward that used to **never** fire (it read
 * scoreboard objectives the plugin never created). Pins the "reward every 64 recycles".
 */
class RecycleProgressTest {
    @Test
    fun `first recycle stores 1 and is not rewarded`() {
        val r = RecycleProgress.advance(0)
        assertEquals(1, r.stored)
        assertFalse(r.rewarded)
        assertEquals(63, r.remaining)
    }

    @Test
    fun `one below the threshold`() {
        val r = RecycleProgress.advance(62)
        assertEquals(63, r.stored)
        assertFalse(r.rewarded)
        assertEquals(1, r.remaining)
    }

    @Test
    fun `reaching the threshold rewards and resets`() {
        val r = RecycleProgress.advance(63)
        assertTrue(r.rewarded, "the 64th recycle must grant a reward — this never happened before the fix")
        assertEquals(0, r.stored)
        assertEquals(0, r.remaining)
    }

    @Test
    fun `negative stored progress is treated as zero`() {
        assertEquals(1, RecycleProgress.advance(-5).stored)
    }

    @Test
    fun `exactly one reward is earned across a full cycle of 64`() {
        var stored = 0
        var rewards = 0
        repeat(RecycleProgress.ITEMS_PER_REWARD) {
            val r = RecycleProgress.advance(stored)
            stored = r.stored
            if (r.rewarded) rewards++
        }
        assertEquals(1, rewards)
        assertEquals(0, stored)
    }
}
