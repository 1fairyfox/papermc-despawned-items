package io.fairyfox.papermc.despawneditems.throttle

/**
 * What the throttler decided to do with one despawning item.
 *
 * The three outcomes are deliberately exhaustive and side-effect free — a policy only
 * ever *classifies*; the [io.fairyfox.papermc.despawneditems.despawn.DespawnScheduler]
 * is what acts on the classification. That split is what makes every policy unit-testable
 * without a server.
 */
enum class ThrottleDecision {
    /** Relocate now: the actor is within every quota that applies to them. */
    ALLOW,

    /**
     * Over quota, but keep the item queued and retry on a later tick. The item is held
     * until the actor's budget refills, so nothing is lost — it just lands later.
     */
    DEFER,

    /**
     * Over quota and over patience: give up on relocating this item. What "give up"
     * means is the admin's choice ([ThrottleSettings.onLimit]) — vanilla despawn, or a
     * hand-off to the catch-all/void path.
     */
    DROP,
    ;

    /** True when the item may start a relocation right now. */
    val allowed: Boolean get() = this == ALLOW
}
