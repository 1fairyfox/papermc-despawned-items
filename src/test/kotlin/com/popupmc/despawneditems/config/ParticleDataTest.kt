package com.popupmc.despawneditems.config

import org.bukkit.Color
import org.bukkit.Particle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Regression tests (§87) for the particle handling that used to crash: the effect called
 * `spawnParticle` with no data, throwing for any data-bearing particle. [ParticleData]
 * now resolves and validates the data up front.
 */
class ParticleDataTest {

    @Test
    fun `a data-less particle resolves with null data and no warning`() {
        val r = ParticleData.resolve("happy_villager", null, 1.0)
        assertEquals(Particle.HAPPY_VILLAGER, r.particle)
        assertNull(r.data)
        assertNull(r.warning)
    }

    @Test
    fun `a DUST particle gets DustOptions instead of crashing`() {
        val r = ParticleData.resolve("dust", "#FF0000", 2.0)
        assertEquals(Particle.DUST, r.particle)
        val data = r.data
        assertIs<Particle.DustOptions>(data)
        assertEquals(Color.fromRGB(0xFF0000), data.color)
        assertEquals(2.0f, data.size)
        assertNull(r.warning)
    }

    @Test
    fun `a particle needing unsupported data falls back with a warning, not an exception`() {
        val r = ParticleData.resolve("block", null, 1.0) // BLOCK requires BlockData
        assertEquals(Particle.HAPPY_VILLAGER, r.particle)
        assertNull(r.data)
        assertNotNull(r.warning)
    }

    @Test
    fun `an unknown particle key falls back with a warning`() {
        val r = ParticleData.resolve("not_a_real_particle", null, 1.0)
        assertEquals(Particle.HAPPY_VILLAGER, r.particle)
        assertNotNull(r.warning)
    }

    @Test
    fun `namespaced and mixed-case keys resolve`() {
        assertEquals(Particle.HAPPY_VILLAGER, ParticleData.resolve("minecraft:Happy_Villager", null, 1.0).particle)
    }

    @Test
    fun `dust size is clamped to the valid range`() {
        val data = ParticleData.resolve("dust", "#FFFFFF", 999.0).data
        assertIs<Particle.DustOptions>(data)
        assertEquals(4.0f, data.size)
    }

    @Test
    fun `an invalid colour falls back to white`() {
        assertEquals(Color.WHITE, ParticleData.parseColor("not-a-hex"))
        assertEquals(Color.WHITE, ParticleData.parseColor(null))
    }
}
