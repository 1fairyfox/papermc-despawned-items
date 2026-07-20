package com.popupmc.despawneditems.despawn

import com.popupmc.despawneditems.DespawnedItems
import org.bukkit.inventory.ItemStack

/**
 * Bounds the automatic despawn pipeline so a flood of despawning items can't storm the
 * server. Items to relocate are enqueued; a once-per-tick drain starts at most
 * `performance.max-per-tick` new [DespawnProcess]es and never exceeds
 * `performance.max-concurrent` in flight. The queue is capped at `performance.max-queue`
 * with a configurable drop policy.
 *
 * This replaces the old model where every despawning item immediately spawned its own
 * chunk-loading process with no upper bound.
 */
class DespawnScheduler(private val plugin: DespawnedItems) {

    private val queue: ArrayDeque<ItemStack> = ArrayDeque()
    private var taskId: Int = -1

    /** Number of items waiting to be relocated. */
    val queued: Int get() = queue.size

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

    /** Queues [item] for relocation, honouring the configured queue cap and drop policy. */
    fun enqueue(item: ItemStack) {
        val perf = plugin.settings.performance
        if (queue.size >= perf.maxQueue) {
            if (perf.dropWhenFull) return // ignore the new item
            queue.removeFirst() // make room by dropping the oldest
        }
        queue.addLast(item)
    }

    private fun tick() {
        if (queue.isEmpty()) return

        // Nothing to relocate into — discard the backlog rather than spinning on it.
        if (plugin.locations.isEmpty()) {
            queue.clear()
            return
        }

        val perf = plugin.settings.performance
        var started = 0
        while (started < perf.maxPerTick &&
            plugin.despawnProcesses.size < perf.maxConcurrent &&
            queue.isNotEmpty()
        ) {
            DespawnProcess(queue.removeFirst(), plugin)
            started++
        }
    }
}
