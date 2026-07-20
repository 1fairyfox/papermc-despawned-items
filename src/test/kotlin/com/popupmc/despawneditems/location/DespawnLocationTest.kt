package com.popupmc.despawneditems.location

import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pure (no server) tests for [DespawnLocation] (de)serialisation — the on-disk format
 * that every stored location roundtrips through. Roundtrip, edge cases, and fuzz.
 */
class DespawnLocationTest {
    private val owner: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `serialize then parse roundtrips`() {
        val loc = DespawnLocation("world", 10, 64, -30, owner)
        val parsed = DespawnLocation.parse(loc.serialize(), owner)
        assertEquals(loc, parsed)
    }

    @Test
    fun `serialize omits the owner and matches the legacy x y z world format`() {
        assertEquals("10;64;-30;world_nether", DespawnLocation("world_nether", 10, 64, -30, owner).serialize())
    }

    @Test
    fun `blockKey ignores owner so same block different owners collide in the spatial index`() {
        val a = DespawnLocation("world", 1, 2, 3, owner)
        val b = DespawnLocation("world", 1, 2, 3, UUID.randomUUID())
        assertEquals(a.blockKey, b.blockKey)
    }

    @Test
    fun `parse rejects malformed input without throwing`() {
        assertNull(DespawnLocation.parse("", owner))
        assertNull(DespawnLocation.parse("1;2;3", owner), "too few fields")
        assertNull(DespawnLocation.parse("x;2;3;world", owner), "non-integer x")
        assertNull(DespawnLocation.parse("1;2;3;", owner), "empty world")
        assertNull(DespawnLocation.parse("1;2;9999999999999;world", owner), "y overflows Int")
    }

    @Test
    fun `parse tolerates world names containing semicolons`() {
        val parsed = DespawnLocation.parse("1;2;3;odd;world", owner)
        assertNotNull(parsed)
        assertEquals("odd;world", parsed.world)
    }

    @Test
    fun `fuzz - random valid coordinates always roundtrip`() {
        val rng = Random(1234)
        repeat(2_000) {
            val loc =
                DespawnLocation(
                    world = "world_${rng.nextInt(3)}",
                    x = rng.nextInt(-30_000_000, 30_000_000),
                    y = rng.nextInt(-64, 320),
                    z = rng.nextInt(-30_000_000, 30_000_000),
                    owner = owner,
                )
            assertEquals(loc, DespawnLocation.parse(loc.serialize(), owner))
        }
    }

    @Test
    fun `fuzz - random garbage never throws`() {
        val rng = Random(9876)
        val alphabet = "0123456789;-abcXYZ_. "
        repeat(5_000) {
            val junk = buildString { repeat(rng.nextInt(0, 20)) { append(alphabet[rng.nextInt(alphabet.length)]) } }
            // Must not throw regardless of input; result may be null or a value.
            DespawnLocation.parse(junk, owner)
        }
    }
}
