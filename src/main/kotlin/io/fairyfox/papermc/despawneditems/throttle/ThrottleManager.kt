package io.fairyfox.papermc.despawneditems.throttle

import io.fairyfox.papermc.despawneditems.config.ThrottleSettings
import io.fairyfox.papermc.despawneditems.config.ThrottleStrategy
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Per-actor throttling of the despawn pipeline.
 *
 * The global [io.fairyfox.papermc.despawneditems.config.PerformanceSettings] budget answers
 * *"how much relocation work may the **server** do this tick"*. This answers the different
 * question the owner asked for: *"how much of that budget may **this player** take"* — so a
 * single player dumping a double chest of junk cannot consume the whole pipeline while
 * everyone else's items quietly expire.
 *
 * Three composable strategies, selected by `throttle.strategy`:
 *
 * | Strategy | What it bounds | Owner's phrasing |
 * |---|---|---|
 * | [ThrottleStrategy.RATE] | relocations per actor per time window (token bucket) | "max per chunk of time" |
 * | [ThrottleStrategy.CONCURRENT] | relocations in flight per actor at once | "max per each one" |
 * | [ThrottleStrategy.FAIR_SHARE] | drain order across actors, weighted | "some users get more than others" |
 * | [ThrottleStrategy.COMBINED] | all three at once | — |
 *
 * State is per-actor and lazily created; [purgeIdle] evicts actors whose budgets are full
 * and who have nothing in flight, so a long-running server does not accumulate a map entry
 * per player who ever dropped an item.
 *
 * @param settingsSupplier read fresh each call so `/despi reload` takes effect immediately.
 * @param onlineLookup injected player lookup — tests pass a stub and need no server.
 * @param clock injected time source in milliseconds — tests drive it deterministically.
 */
class ThrottleManager(
    private val settingsSupplier: () -> ThrottleSettings,
    private val onlineLookup: (UUID) -> Player? = { null },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val buckets: MutableMap<UUID, TokenBucket> = HashMap()
    private val inFlight: MutableMap<UUID, Int> = HashMap()

    /** Actors seen since the last fair-share rotation, in insertion order. */
    private val rotation: ArrayDeque<UUID> = ArrayDeque()

    /** Number of actors currently holding throttle state (diagnostics + tests). */
    val trackedActors: Int get() = buckets.size + inFlight.keys.count { it !in buckets }

    /** Relocations currently in flight for [actor]. */
    fun inFlightFor(actor: UUID?): Int = if (actor == null) 0 else inFlight[actor] ?: 0

    /**
     * Classifies one despawning item belonging to [actor] (null = ownerless drop).
     *
     * Pure with respect to the caller: a [ThrottleDecision.ALLOW] **does** consume a rate
     * token, because allowing is the act of spending budget. `DEFER`/`DROP` spend nothing.
     */
    fun evaluate(actor: UUID?): ThrottleDecision {
        val settings = settingsSupplier()
        if (!settings.enabled || settings.strategy == ThrottleStrategy.NONE) return ThrottleDecision.ALLOW

        val quota = ThrottleQuotas.resolve(actor, settings, onlineLookup)
        if (quota.bypass || actor == null) return ThrottleDecision.ALLOW

        val now = clock()

        if (settings.strategy.appliesConcurrent && inFlightFor(actor) >= quota.maxConcurrent) {
            return settings.onLimitDecision
        }

        if (settings.strategy.appliesRate) {
            val bucket =
                buckets.getOrPut(actor) {
                    TokenBucket(quota.ratePerWindow, settings.windowSeconds * MILLIS_PER_SECOND, now)
                }
            if (!bucket.tryConsume(now)) return settings.onLimitDecision
        }

        if (settings.strategy.appliesFairShare) noteForRotation(actor)

        return ThrottleDecision.ALLOW
    }

    /** Records that a relocation started for [actor] (concurrency accounting). */
    fun onStart(actor: UUID?) {
        if (actor == null) return
        inFlight[actor] = (inFlight[actor] ?: 0) + 1
    }

    /** Records that a relocation finished for [actor]. Never goes negative. */
    fun onFinish(actor: UUID?) {
        if (actor == null) return
        val next = (inFlight[actor] ?: 0) - 1
        if (next <= 0) inFlight.remove(actor) else inFlight[actor] = next
    }

    /**
     * Weighted round-robin ordering hint: how many items may be drained for [actor] in a
     * single drain pass. Weight 3 drains three times as fast as weight 1 — the mechanism
     * behind "some users get more despawned items than others".
     */
    fun shareFor(actor: UUID?): Int {
        val settings = settingsSupplier()
        if (!settings.enabled || !settings.strategy.appliesFairShare) return Int.MAX_VALUE
        if (actor == null) return settings.defaultWeight.coerceAtLeast(1)
        val quota = ThrottleQuotas.resolve(actor, settings, onlineLookup)
        if (quota.bypass) return Int.MAX_VALUE
        return quota.weight.coerceAtLeast(1)
    }

    /** Drops per-actor state that is fully refilled and idle, bounding memory. */
    fun purgeIdle() {
        val now = clock()
        buckets.entries.removeAll { (actor, bucket) ->
            bucket.isFull(now) && (inFlight[actor] ?: 0) == 0
        }
        rotation.retainAll { it in buckets || (inFlight[it] ?: 0) > 0 }
    }

    /** Clears all throttle state — used on reload and plugin disable. */
    fun reset() {
        buckets.clear()
        inFlight.clear()
        rotation.clear()
    }

    private fun noteForRotation(actor: UUID) {
        if (!rotation.contains(actor)) rotation.addLast(actor)
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
