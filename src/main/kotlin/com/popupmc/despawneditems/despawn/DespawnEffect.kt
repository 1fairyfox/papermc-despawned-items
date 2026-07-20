package com.popupmc.despawneditems.despawn

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.config.LocationEntry
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

/**
 * Plays the short "item landed here" effect at a despawn location: an optional
 * one-shot sound plus a burst of particles that repeats for a configured number
 * of ticks before self-destructing.
 */
class DespawnEffect(val locationEntry: LocationEntry, private val plugin: DespawnedItems) {

    private var loopsLeft: Int
    private var task: BukkitTask? = null

    init {
        val cfg = plugin.settings.fileConfig
        // Convert the configured duration (seconds) to a loop count given how
        // many ticks apart each particle burst is.
        loopsLeft = (cfg.particleLengthSeconds * SECOND) / cfg.newParticlesEveryNthTick

        // Hold a reference so the effect is not garbage collected mid-play.
        plugin.effectsPlaying.add(this)

        play()
    }

    private fun play() {
        val world = locationEntry.location.world
        val center = locationEntry.location.toCenterLocation()
        val cfg = plugin.settings.fileConfig

        if (cfg.soundEnabled) {
            world.playSound(center, cfg.soundKey, cfg.soundVolume, cfg.soundPitch)
        }

        if (!cfg.particlesEnabled) {
            plugin.effectsPlaying.remove(this)
            return
        }

        val self = this
        val radius = cfg.particleRandomRadius.toDouble()

        task = object : BukkitRunnable() {
            override fun run() {
                // Two bursts spreading out along the +/- X and Z axes. The trailing
                // data argument is null for data-less particles and e.g. DustOptions
                // for DUST — resolved and validated once at config load.
                world.spawnParticle(
                    cfg.particleFX, center,
                    cfg.particleCountEveryNthTick / 2,
                    radius, 0.0, radius, cfg.particleData,
                )
                world.spawnParticle(
                    cfg.particleFX, center,
                    cfg.particleCountEveryNthTick / 2,
                    -radius, 0.0, -radius, cfg.particleData,
                )
                self.loopEnd()
            }
        }.runTaskTimer(plugin, 1L, cfg.newParticlesEveryNthTick.toLong())
    }

    private fun checkSelfDestroy() {
        if (loopsLeft <= 0) {
            plugin.effectsPlaying.remove(this)
            task?.cancel()
        }
    }

    fun forceSelfDestroy() {
        plugin.effectsPlaying.remove(this)
        task?.cancel()
    }

    private fun loopEnd() {
        loopsLeft -= 1
        checkSelfDestroy()
    }

    companion object {
        /** Ticks per second. */
        private const val SECOND = 20
    }
}
