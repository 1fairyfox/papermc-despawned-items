package io.fairyfox.papermc.despawneditems.location

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Serialisation tests for [TargetOptions] and the option-carrying form of
 * [DespawnLocation].
 *
 * The single most important property here is **backward compatibility**: servers have years
 * of `x;y;z;world` data on disk, and a target with default options must still be written in
 * exactly that form. A regression here silently corrupts real users' data, so it is pinned
 * from several directions.
 */
class TargetOptionsTest {
    private val owner: UUID = UUID.randomUUID()

    // --- backward compatibility ------------------------------------------------------

    @Test
    fun `a default target serialises to the classic format`() {
        val loc = DespawnLocation("world", 1, 64, -3, owner)
        assertEquals("1;64;-3;world", loc.serialize(), "default options must not change the on-disk format")
    }

    @Test
    fun `legacy entries parse with default options`() {
        val parsed = DespawnLocation.parse("12;70;-8;world", owner)
        assertNotNull(parsed)
        assertEquals(TargetOptions.DEFAULT, parsed.options)
        assertTrue(parsed.enabled)
    }

    @Test
    fun `legacy entries with semicolons in the world name still parse`() {
        val parsed = DespawnLocation.parse("1;2;3;my;odd;world", owner)
        assertNotNull(parsed)
        assertEquals("my;odd;world", parsed.world)
        assertEquals(TargetOptions.DEFAULT, parsed.options)
    }

    // --- the option-carrying form ----------------------------------------------------

    @Test
    fun `a non-default target round-trips through serialise and parse`() {
        val original =
            DespawnLocation(
                "world",
                5,
                64,
                7,
                owner,
                TargetOptions(enabled = false, priority = 6, acceptContraband = true),
            )
        val parsed = DespawnLocation.parse(original.serialize(), owner)
        assertEquals(original, parsed)
    }

    @Test
    fun `only non-default fields are written`() {
        val disabledOnly = DespawnLocation("world", 0, 0, 0, owner, TargetOptions(enabled = false))
        assertEquals("@enabled=false|0;0;0;world", disabledOnly.serialize())

        val priorityOnly = DespawnLocation("world", 0, 0, 0, owner, TargetOptions(priority = 4))
        assertEquals("@priority=4|0;0;0;world", priorityOnly.serialize())
    }

    @Test
    fun `options survive a world name containing semicolons`() {
        val original =
            DespawnLocation("a;b;c", -1, 2, -3, owner, TargetOptions(enabled = false, priority = 9))
        val parsed = DespawnLocation.parse(original.serialize(), owner)
        assertEquals(original, parsed, "the options block is a PREFIX precisely so this case works")
    }

    @Test
    fun `a truncated options block is rejected rather than mis-parsed`() {
        assertNull(DespawnLocation.parse("@enabled=false", owner), "no terminator = malformed")
    }

    @Test
    fun `unknown keys and junk values degrade to defaults instead of failing`() {
        val options = TargetOptions.parseFields("enabled=perhaps,priority=lots,unknown=1,contraband=yes")
        assertEquals(TargetOptions.DEFAULT, options, "a hand-edited file must not take the server down")
    }

    @Test
    fun `priority is clamped into range on parse`() {
        assertEquals(TargetOptions.MAX_PRIORITY, TargetOptions.parseFields("priority=9999").priority)
        assertEquals(TargetOptions.MIN_PRIORITY, TargetOptions.parseFields("priority=-4").priority)
    }

    @Test
    fun `empty and malformed field blocks are safe`() {
        assertEquals(TargetOptions.DEFAULT, TargetOptions.parseFields(""))
        assertEquals(TargetOptions.DEFAULT, TargetOptions.parseFields("=nokey,novalue="))
    }

    @Test
    fun `isDefault tracks the default instance`() {
        assertTrue(TargetOptions().isDefault)
        assertFalse(TargetOptions(enabled = false).isDefault)
        assertFalse(TargetOptions(priority = 2).isDefault)
        assertFalse(TargetOptions(acceptContraband = true).isDefault)
    }

    // --- store behaviour -------------------------------------------------------------

    @Test
    fun `enabledCount ignores switched-off targets`() {
        val store = LocationStore()
        store.add(DespawnLocation("world", 0, 0, 0, owner))
        store.add(DespawnLocation("world", 1, 0, 0, owner, TargetOptions(enabled = false)))
        store.add(DespawnLocation("world", 2, 0, 0, owner))

        assertEquals(3, store.size)
        assertEquals(2, store.enabledCount())
    }

    @Test
    fun `randomEnabled never returns a switched-off target`() {
        val store = LocationStore()
        store.add(DespawnLocation("world", 0, 0, 0, owner)) // the only enabled one
        repeat(20) { i ->
            store.add(DespawnLocation("world", i + 1, 0, 0, owner, TargetOptions(enabled = false)))
        }
        repeat(200) {
            val drawn = store.randomEnabledOrNull()
            assertNotNull(drawn)
            assertTrue(drawn.enabled, "a disabled target must never be drawn")
        }
    }

    @Test
    fun `randomEnabled returns null when everything is switched off`() {
        val store = LocationStore()
        repeat(5) { i -> store.add(DespawnLocation("world", i, 0, 0, owner, TargetOptions(enabled = false))) }
        assertNull(store.randomEnabledOrNull())
    }

    @Test
    fun `higher priority targets are drawn more often`() {
        val store = LocationStore()
        store.add(DespawnLocation("world", 0, 0, 0, owner, TargetOptions(priority = 10)))
        store.add(DespawnLocation("world", 1, 0, 0, owner, TargetOptions(priority = 1)))

        var high = 0
        repeat(2_000) { if (store.randomEnabledOrNull()?.x == 0) high++ }

        assertTrue(high > 1_100, "a priority-10 target should clearly out-draw a priority-1 one (got $high/2000)")
    }

    @Test
    fun `update swaps a target's options and keeps every index consistent`() {
        val store = LocationStore()
        val original = DespawnLocation("world", 3, 4, 5, owner)
        store.add(original)

        val updated = original.copy(options = TargetOptions(enabled = false, priority = 7))
        assertTrue(store.update(updated))

        assertEquals(1, store.size, "update must not duplicate the entry")
        assertEquals(0, store.enabledCount())
        assertEquals(updated, store.at(original.blockKey).single())
        assertEquals(setOf(updated), store.ofOwner(owner))
        assertTrue(store.contains(updated))
    }

    @Test
    fun `update is a no-op for an absent or unchanged target`() {
        val store = LocationStore()
        val loc = DespawnLocation("world", 0, 0, 0, owner)
        assertFalse(store.update(loc), "nothing stored yet")

        store.add(loc)
        assertFalse(store.update(loc), "identical options change nothing")
    }
}
