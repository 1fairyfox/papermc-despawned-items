package com.popupmc.despawneditems.location

import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

/**
 * Performance guardrails for the hot-path [LocationStore]. These assert *scaling
 * behaviour* with deliberately generous absolute bounds (to avoid CI flakiness) — the
 * point is to fail the build if someone reintroduces an O(n) query or O(n) persistence
 * pass, not to benchmark the machine. Precise numbers belong to JMH (see testing.md).
 */
class LocationStoreBenchTest {
    private fun owner(i: Int) = UUID(0L, i.toLong())

    @Test
    fun `100k inserts are fast and 1M lookups do not scale with size`() {
        val store = LocationStore()
        val n = 100_000

        val addTime =
            measureTime {
                for (i in 0 until n) store.add(DespawnLocation("world", i, 64, i, owner(i % 500)))
            }
        assertEquals(n, store.size)
        assertTrue(addTime.inWholeMilliseconds < 4_000, "100k O(1) adds took $addTime (expected < 4s)")

        val rng = Random(1)
        val lookupTime =
            measureTime {
                repeat(1_000_000) {
                    val i = rng.nextInt(n)
                    store.contains(DespawnLocation("world", i, 64, i, owner(i % 500)))
                }
            }
        // If contains() were O(n) this would take minutes; O(1) keeps it well under a few seconds.
        assertTrue(lookupTime.inWholeMilliseconds < 4_000, "1M lookups on a 100k store took $lookupTime")
    }

    @Test
    fun `random draw is constant-time on a large store`() {
        val store = LocationStore()
        repeat(100_000) { store.add(DespawnLocation("world", it, 64, it, owner(it % 500))) }
        val t = measureTime { repeat(1_000_000) { store.randomOrNull() } }
        assertTrue(t.inWholeMilliseconds < 3_000, "1M random draws took $t")
    }

    @Test
    fun `owner count and bulk removal stay cheap at scale`() {
        val store = LocationStore()
        val heavy = owner(999)
        repeat(50_000) { store.add(DespawnLocation("world", it, 64, it, heavy)) }
        repeat(50_000) { store.add(DespawnLocation("world_nether", it, 64, it, owner(it % 500))) }

        val t =
            measureTime {
                assertEquals(50_000, store.countOfOwner(heavy))
                assertEquals(50_000, store.removeOwner(heavy))
            }
        assertEquals(0, store.countOfOwner(heavy))
        assertTrue(t.inWholeMilliseconds < 3_000, "owner count + 50k removal took $t")
    }
}
