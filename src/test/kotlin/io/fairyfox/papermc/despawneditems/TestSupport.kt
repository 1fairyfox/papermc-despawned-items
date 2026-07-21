package io.fairyfox.papermc.despawneditems

import org.bukkit.Chunk
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.configuration.file.YamlConfiguration
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.block.BlockMock
import org.mockbukkit.mockbukkit.block.state.BlastFurnaceStateMock
import org.mockbukkit.mockbukkit.block.state.ChestStateMock
import org.mockbukkit.mockbukkit.block.state.FurnaceStateMock
import org.mockbukkit.mockbukkit.block.state.SmokerStateMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import java.io.File
import java.util.concurrent.CompletableFuture

// Shared MockBukkit test scaffolding. Two gaps in MockBukkit 4.110 are bridged here so
// the full pipeline is testable: ray-traced target blocks (TargetingPlayerMock) and
// async chunk loads (SyncChunkWorldMock completes them synchronously, which also makes
// DespawnProcess deterministic per tick).

/** A [WorldMock] whose async chunk loads complete immediately (MockBukkit leaves them unimplemented). */
class SyncChunkWorldMock : WorldMock() {
    override fun getChunkAtAsync(
        x: Int,
        z: Int,
        gen: Boolean,
        urgent: Boolean,
    ): CompletableFuture<Chunk> = CompletableFuture.completedFuture(getChunkAt(x, z))
}

/** A [PlayerMock] with a scriptable target block (MockBukkit does not implement ray tracing). */
class TargetingPlayerMock(server: ServerMock, name: String) : PlayerMock(server, name) {
    var target: Block? = null

    override fun getTargetBlockExact(maxDistance: Int): Block? = target

    override fun getTargetBlockExact(
        maxDistance: Int,
        fluidCollisionMode: FluidCollisionMode,
    ): Block? = target
}

/**
 * Installs a container state whose `update()` is a no-op, so inventory mutations persist.
 *
 * On a real server the placed container's inventory writes stick (that's the pattern the
 * original plugin shipped with for years); MockBukkit's `ContainerStateMock.update()`
 * instead restores a stale empty snapshot, wiping the strategy's own writes — a mock
 * deviation this bridges.
 */
fun stickyContainer(block: Block): Container {
    val state =
        when (block.type) {
            Material.CHEST ->
                object : ChestStateMock(block) {
                    override fun update(
                        force: Boolean,
                        applyPhysics: Boolean,
                    ) = true
                }
            Material.FURNACE ->
                object : FurnaceStateMock(block) {
                    override fun update(
                        force: Boolean,
                        applyPhysics: Boolean,
                    ) = true
                }
            Material.BLAST_FURNACE ->
                object : BlastFurnaceStateMock(block) {
                    override fun update(
                        force: Boolean,
                        applyPhysics: Boolean,
                    ) = true
                }
            Material.SMOKER ->
                object : SmokerStateMock(block) {
                    override fun update(
                        force: Boolean,
                        applyPhysics: Boolean,
                    ) = true
                }
            else -> error("stickyContainer does not support ${block.type}")
        }
    (block as BlockMock).setState(state)
    return state
}

/** Strips legacy colour codes (§x) so message assertions compare plain text. */
fun String?.plain(): String = this?.replace(Regex("§."), "") ?: ""

/** Edits the plugin's config.yml on disk and reloads [PaperMcDespawnedItems.settings]. */
fun editConfig(
    plugin: PaperMcDespawnedItems,
    vararg entries: Pair<String, Any?>,
) {
    plugin.saveDefaultConfig()
    val file = File(plugin.dataFolder, "config.yml")
    val yaml = YamlConfiguration.loadConfiguration(file)
    entries.forEach { (path, value) -> yaml.set(path, value) }
    yaml.save(file)
    plugin.settings.load()
}
