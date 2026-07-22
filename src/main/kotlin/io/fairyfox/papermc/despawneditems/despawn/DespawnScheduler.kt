package io.fairyfox.papermc.despawneditems.despawn

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.throttle.ThrottleDecision
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.random.Random

/**
 * Bounds the automatic despawn pipeline so a flood of despawning items can't storm the
 * server, and shares that bounded budget fairly between players.
 *
 * Two independent layers:
 *
 *  * **Server budget** (`performance:`) — at most `max-per-tick` new relocations start each
 *    tick and never more than `max-concurrent` run at once. This is what stops the *server*
 *    from being overwhelmed.
 *  * **Per-user budget** (`throttle:`) — how much of that server budget any one player may
 *    take, via [io.fairyfox.papermc.despawneditems.throttle.ThrottleManager]. This is what
 *    stops one *player* from starving everyone else.
 *
 * The `void.chance` roll happens here too, at enqueue time rather than per relocation
 * attempt: an item's fate should be decided once, not re-rolled at every candidate location.
 */
class DespawnScheduler(
    private val plugin: PaperMcDespawnedItems,
    private val random: Random = Random.Default,
) {
    /** One queued item plus the player it is attributed to (null = ownerless drop). */
    private data class Queued(val item: ItemStack, val actor: UUID?)

    private val queue: ArrayDeque<Queued> = ArrayDeque()
    private var taskId: Int = -1
    private var ticks: Long = 0

    /** Number of items waiting to be relocated. */
    val queued: Int get() = queue.size

    /** Items voided by the `void.chance` roll since enable (diagnostics + tests). */
    var voidedByChance: Long = 0
        private set

    /** Items dropped because their owner was over quota (diagnostics + tests). */
    var droppedByThrottle: Long = 0
        private set

    fun start() {
        if (taskId != -1) return
        taskId = plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 1L, 1L).taskId
    }

    fun stop() {
        if (taskId != -1) {
            plugin.server.scheduler.cancelTask(taskId)
            taskId = -1
        }
        queue.clear()
    }

    /** Queues [item] for relocation with no attributed owner. */
    fun enqueue(item: ItemStack) = enqueue(item, null)

    /**
     * Queues [item] for relocation on behalf of [actor], honouring the queue cap, the drop
     * policy, and the configured void chance.
     */
    fun enqueue(
        item: ItemStack,
        actor: UUID?,
    ) {
        if (rollForVoid(item)) return

        val perf = plugin.settings.performance
        if (queue.size >= perf.maxQueue) {
            if (perf.dropWhenFull) return // ignore the new item
            queue.removeFirst() // make room by dropping the oldest
        }
        queue.addLast(Queued(item, actor))
    }

    /**
     * Applies `void.chance`. Returns true when the item was consumed by the void (either
     * destroyed outright or handed to a catch-all) and must not be queued.
     */
    private fun rollForVoid(item: ItemStack): Boolean {
        val voiding = plugin.settings.voiding
        if (voiding.chance <= 0.0) return false
        if (random.nextDouble() >= voiding.chance) return false

        voidedByChance++
        if (voiding.catchAllUsable) plugin.catchAll.deliver(item.clone())
        return true
    }

    /** Hands an over-quota item to the catch-all when configured, else lets it go. */
    private fun discardOverQuota(entry: Queued) {
        droppedByThrottle++
        if (plugin.settings.throttle.overLimitToCatchAll && plugin.settings.voiding.catchAllUsable) {
            plugin.catchAll.deliver(entry.item.clone())
        }
    }

    private fun tick() {
        ticks++
        if (ticks % PURGE_INTERVAL_TICKS == 0L) plugin.throttle.purgeIdle()

        if (queue.isEmpty()) return

        // Nothing to relocate into — discard the backlog rather than spinning on it.
        if (plugin.locations.isEmpty()) {
            queue.clear()
            return
        }

        drain()
    }

    /** Starts as many relocations as this tick's server budget and per-user quotas allow. */
    private fun drain() {
        val startedPerActor: MutableMap<UUID?, Int> = HashMap()
        var started = 0

        // Scan at most one full pass of the queue so deferred entries cannot spin the
        // drain forever within a single tick.
        var remainingScans = queue.size

        while (remainingScans > 0 && queue.isNotEmpty() && hasServerBudget(started)) {
            remainingScans--
            if (attempt(queue.removeFirst(), startedPerActor)) started++
        }
    }

    /** Whether the *server-wide* budget still permits another relocation this tick. */
    private fun hasServerBudget(startedThisTick: Int): Boolean {
        val perf = plugin.settings.performance
        return startedThisTick < perf.maxPerTick && plugin.despawnProcesses.size < perf.maxConcurrent
    }

    /**
     * Applies the per-user gates to one queued entry. Returns true when a relocation
     * actually started (so the caller can count it against the server budget).
     */
    private fun attempt(
        entry: Queued,
        startedPerActor: MutableMap<UUID?, Int>,
    ): Boolean {
        val takenThisTick = startedPerActor[entry.actor] ?: 0
        if (takenThisTick >= plugin.throttle.shareFor(entry.actor)) {
            queue.addLast(entry) // had its fair share this tick; try again next tick
            return false
        }

        return when (plugin.throttle.evaluate(entry.actor)) {
            ThrottleDecision.ALLOW -> {
                plugin.throttle.onStart(entry.actor)
                DespawnProcess(entry.item, plugin, entry.actor)
                startedPerActor[entry.actor] = takenThisTick + 1
                true
            }
            ThrottleDecision.DEFER -> {
                queue.addLast(entry)
                false
            }
            ThrottleDecision.DROP -> {
                discardOverQuota(entry)
                false
            }
        }
    }

    private companion object {
        /** How often idle per-actor throttle state is evicted (20 ticks = 1 second). */
        const val PURGE_INTERVAL_TICKS = 600L
    }
}
