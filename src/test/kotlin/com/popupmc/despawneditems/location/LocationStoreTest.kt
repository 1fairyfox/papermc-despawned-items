package com.popupmc.despawneditems.location

import java.util.UUID
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pure (no server) tests for the indexed [LocationStore]. */
class LocationStoreTest {
    private lateinit var store: LocationStore
    private val alice: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000a")
    private val bob: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000b")

    private fun loc(
        x: Int,
        y: Int,
        z: Int,
        owner: UUID,
        world: String = "world",
    ) = DespawnLocation(world, x, y, z, owner)

    @BeforeTest
    fun setUp() {
        store = LocationStore()
    }

    @Test
    fun `add then contains, size, and no duplicate`() {
        val a = loc(1, 2, 3, alice)
        assertTrue(store.add(a))
        assertTrue(store.contains(a))
        assertEquals(1, store.size)
        assertFalse(store.add(a), "adding the same owner+block again is a no-op")
        assertEquals(1, store.size)
    }

    @Test
    fun `same block different owners coexist in the spatial index`() {
        val a = loc(1, 2, 3, alice)
        val b = loc(1, 2, 3, bob)
        store.add(a)
        store.add(b)
        assertEquals(setOf(alice, bob), store.ownersAt(a.blockKey))
        assertEquals(2, store.at(a.blockKey).size)
        assertEquals(2, store.size)
    }

    @Test
    fun `owner index and per-owner count`() {
        store.add(loc(1, 1, 1, alice))
        store.add(loc(2, 2, 2, alice))
        store.add(loc(3, 3, 3, bob))
        assertEquals(2, store.countOfOwner(alice))
        assertEquals(1, store.countOfOwner(bob))
        assertEquals(0, store.countOfOwner(UUID.randomUUID()))
    }

    @Test
    fun `remove keeps all indexes consistent`() {
        val a = loc(1, 2, 3, alice)
        val b = loc(1, 2, 3, bob)
        store.add(a)
        store.add(b)
        assertTrue(store.remove(a))
        assertFalse(store.contains(a))
        assertEquals(setOf(bob), store.ownersAt(a.blockKey))
        assertEquals(0, store.countOfOwner(alice))
        assertEquals(1, store.size)
        assertFalse(store.remove(a), "removing again is a no-op")
    }

    @Test
    fun `removeOwner and removeAt`() {
        store.add(loc(1, 1, 1, alice))
        store.add(loc(2, 2, 2, alice))
        val shared = loc(5, 5, 5, alice)
        store.add(shared)
        store.add(loc(5, 5, 5, bob))

        assertEquals(3, store.removeOwner(alice))
        assertEquals(0, store.countOfOwner(alice))
        assertEquals(setOf(bob), store.ownersAt(shared.blockKey))

        assertEquals(1, store.removeAt(shared.blockKey))
        assertTrue(store.isEmpty())
    }

    @Test
    fun `randomOrNull draws only stored values and is null when empty`() {
        assertNull(store.randomOrNull())
        val a = loc(1, 2, 3, alice)
        store.add(a)
        repeat(50) { assertEquals(a, store.randomOrNull()) }
    }

    @Test
    fun `dirty tracking records changed owners and clears`() {
        store.add(loc(1, 1, 1, alice))
        store.add(loc(2, 2, 2, bob))
        assertEquals(setOf(alice, bob), store.dirtyOwnersSnapshot())
        store.clearDirty(listOf(alice))
        assertEquals(setOf(bob), store.dirtyOwnersSnapshot())
    }

    @Test
    fun `replaceAll loads without marking dirty`() {
        store.replaceAll(listOf(loc(1, 1, 1, alice), loc(2, 2, 2, bob)))
        assertEquals(2, store.size)
        assertTrue(store.dirtyOwnersSnapshot().isEmpty(), "loading is not a change to persist")
    }

    @Test
    fun `integrity - randomized add or remove keeps size in sync with contains`() {
        val rng = Random(42)
        val live = HashSet<DespawnLocation>()
        repeat(5_000) {
            val candidate = loc(rng.nextInt(0, 25), rng.nextInt(0, 5), rng.nextInt(0, 25), if (rng.nextBoolean()) alice else bob)
            if (rng.nextBoolean()) {
                val added = store.add(candidate)
                assertEquals(added, live.add(candidate))
            } else {
                val removed = store.remove(candidate)
                assertEquals(removed, live.remove(candidate))
            }
            assertEquals(live.size, store.size)
        }
        // Every survivor is still findable through the public API.
        live.forEach { assertTrue(store.contains(it)) }
    }
}
