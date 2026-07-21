package io.fairyfox.papermc.despawneditems.property

import io.fairyfox.papermc.despawneditems.RecycleProgress
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.location.LocationStore
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Kotest-property invariants (testing.md §5): generator-driven fuzz over the pure
 * core — serialization roundtrips, garbage resilience, store consistency under random
 * operation sequences, and the reward threshold's arithmetic.
 */
class PropertyInvariantsTest {
    private val owner: UUID = UUID.randomUUID()

    private val arbLocation =
        arbitrary {
            DespawnLocation(
                // World names may contain almost anything, including ';' (re-joined on parse).
                world = Arb.string(1..24).bind().replace("\n", "").replace("\r", "").ifEmpty { "w" },
                x = Arb.int().bind(),
                y = Arb.int(-2048..2047).bind(),
                z = Arb.int().bind(),
                owner = owner,
            )
        }

    @Test
    fun `serialize-parse roundtrips for arbitrary locations`() =
        runBlocking {
            checkAll(500, arbLocation) { location ->
                assertEquals(location, DespawnLocation.parse(location.serialize(), owner))
            }
        }

    @Test
    fun `parse never throws on arbitrary garbage`() =
        runBlocking {
            checkAll(1000, Arb.string(0..64)) { garbage ->
                DespawnLocation.parse(garbage, owner) // null or a value — never an exception
            }
        }

    @Test
    fun `store stays consistent under random add-remove sequences`() =
        runBlocking {
            checkAll(50, Arb.list(arbLocation, 0..200)) { locations ->
                val store = LocationStore()
                val expected = HashSet<DespawnLocation>()
                for (location in locations) {
                    assertEquals(expected.add(location), store.add(location), "add mirrors set semantics")
                }
                assertEquals(expected.size, store.size)
                assertEquals(expected, store.all().toSet())

                // Remove a random half; the indexes must agree at every step.
                for (location in locations.shuffled().take(locations.size / 2)) {
                    assertEquals(expected.remove(location), store.remove(location), "remove mirrors set semantics")
                }
                assertEquals(expected, store.all().toSet())
                if (expected.isNotEmpty()) {
                    assertTrue(store.randomOrNull() in expected, "random draws only live values")
                } else {
                    assertEquals(null, store.randomOrNull())
                }
            }
        }

    @Test
    fun `recycle progress always rewards exactly every 64th item`() =
        runBlocking {
            checkAll(500, Arb.int(-10..1_000)) { start ->
                var stored = start.coerceAtLeast(0).coerceAtMost(RecycleProgress.ITEMS_PER_REWARD - 1)
                var rewards = 0
                repeat(RecycleProgress.ITEMS_PER_REWARD) {
                    val result = RecycleProgress.advance(stored)
                    stored = result.stored
                    if (result.rewarded) rewards++
                    assertTrue(result.remaining >= 0)
                }
                assertEquals(1, rewards, "exactly one reward per 64 recycles regardless of start point")
            }
        }
}
