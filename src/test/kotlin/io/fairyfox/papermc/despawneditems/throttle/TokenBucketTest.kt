package io.fairyfox.papermc.despawneditems.throttle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit tests for [TokenBucket]. Time is injected, so rate behaviour is asserted
 * deterministically rather than by sleeping — these run in microseconds and never flake.
 */
class TokenBucketTest {
    @Test
    fun `a fresh bucket is full and allows a full burst`() {
        val bucket = TokenBucket(capacity = 5, windowMillis = 1_000L)
        repeat(5) { assertTrue(bucket.tryConsume(0L), "burst token ${it + 1} should be available") }
        assertFalse(bucket.tryConsume(0L), "the 6th token in the same instant is refused")
    }

    @Test
    fun `tokens refill smoothly across the window`() {
        val bucket = TokenBucket(capacity = 10, windowMillis = 1_000L)
        repeat(10) { bucket.tryConsume(0L) }
        assertFalse(bucket.tryConsume(0L))

        // A tenth of the window has passed → exactly one token back.
        assertTrue(bucket.tryConsume(100L), "one token refills after a tenth of the window")
        assertFalse(bucket.tryConsume(100L), "but only one")
    }

    @Test
    fun `refill never exceeds capacity`() {
        val bucket = TokenBucket(capacity = 3, windowMillis = 1_000L)
        bucket.refill(1_000_000L)
        assertEquals(3.0, bucket.available, "an idle bucket saturates at capacity, it does not accrue debt")
    }

    @Test
    fun `a zero capacity bucket refuses everything`() {
        val bucket = TokenBucket(capacity = 0, windowMillis = 1_000L)
        assertFalse(bucket.tryConsume(0L))
        assertFalse(bucket.tryConsume(10_000L), "and stays refusing however long we wait")
    }

    @Test
    fun `a non-positive window never refills`() {
        val bucket = TokenBucket(capacity = 2, windowMillis = 0L)
        assertTrue(bucket.tryConsume(0L))
        assertTrue(bucket.tryConsume(0L))
        assertFalse(bucket.tryConsume(1_000_000L), "windowMillis <= 0 disables refill rather than dividing by zero")
    }

    @Test
    fun `time going backwards does not grant tokens`() {
        val bucket = TokenBucket(capacity = 2, windowMillis = 1_000L, startMillis = 5_000L)
        assertTrue(bucket.tryConsume(5_000L))
        assertTrue(bucket.tryConsume(5_000L))
        assertFalse(bucket.tryConsume(1_000L), "a clock that jumps backwards must not refill the bucket")
    }

    @Test
    fun `isFull reports idleness for eviction`() {
        val bucket = TokenBucket(capacity = 2, windowMillis = 1_000L)
        assertTrue(bucket.isFull(0L))
        bucket.tryConsume(0L)
        assertFalse(bucket.isFull(0L))
        assertTrue(bucket.isFull(10_000L), "after a long idle period the bucket is full again and evictable")
    }
}
