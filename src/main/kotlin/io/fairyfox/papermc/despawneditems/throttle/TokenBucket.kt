package io.fairyfox.papermc.despawneditems.throttle

/**
 * A classic token bucket: [capacity] tokens that refill smoothly over [windowMillis].
 *
 * This is the "max per chunk of time" primitive — a player allowed 60 relocations per
 * minute may burst all 60 at once, then trickles back in at one per second, rather than
 * being hard-cut at a window boundary (which would let a player double their rate by
 * straddling one).
 *
 * Deliberately **clock-injected** rather than reading `System.currentTimeMillis()`
 * internally: tests drive time forward explicitly, so rate behaviour is verified
 * deterministically instead of by sleeping.
 */
class TokenBucket(
    val capacity: Int,
    val windowMillis: Long,
    startMillis: Long = 0L,
) {
    private var tokens: Double = capacity.toDouble()
    private var lastRefill: Long = startMillis

    /** Tokens currently available, for tests and diagnostics. */
    val available: Double get() = tokens

    /**
     * Consumes one token if any is available, refilling first based on elapsed time.
     * Returns true when the caller may proceed.
     */
    fun tryConsume(now: Long): Boolean {
        refill(now)
        if (tokens < 1.0) return false
        tokens -= 1.0
        return true
    }

    /** Refills without consuming — used when peeking at a budget. */
    fun refill(now: Long) {
        if (capacity <= 0 || windowMillis <= 0L) return
        val elapsed = now - lastRefill
        if (elapsed <= 0L) return
        lastRefill = now
        val perMilli = capacity.toDouble() / windowMillis.toDouble()
        tokens = (tokens + elapsed * perMilli).coerceAtMost(capacity.toDouble())
    }

    /** True when the bucket is completely full — i.e. the actor is idle and evictable. */
    fun isFull(now: Long): Boolean {
        refill(now)
        return tokens >= capacity.toDouble()
    }
}
