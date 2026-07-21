package io.fairyfox.papermc.despawneditems.bench

import io.fairyfox.papermc.despawneditems.RecycleProgress
import io.fairyfox.papermc.despawneditems.location.BlockKey
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.location.LocationStore
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Microbenchmarks for the plugin's hot paths — the per-tick costs the scheduler pays
 * under load. Informational (not a gate): run `./gradlew jmh`; CI records the numbers
 * weekly so regressions show up as trends.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class HotPathBenchmarks {
    private val store = LocationStore()
    private val owners = Array(100) { UUID.randomUUID() }
    private var counter = 0

    @Setup(Level.Trial)
    fun populate() {
        // 100k locations across 100 owners — a very large network.
        for (i in 0 until 100_000) {
            store.add(DespawnLocation("world", i % 1000, 64 + (i / 1000) % 64, i / 64_000, owners[i % owners.size]))
        }
    }

    @Benchmark
    fun storeRandomDraw(): DespawnLocation? = store.randomOrNull()

    @Benchmark
    fun storeContains(): Boolean = store.contains(DespawnLocation("world", 500, 64, 0, owners[0]))

    @Benchmark
    fun storeOwnersAt(): Set<UUID> = store.ownersAt(BlockKey("world", 500, 64, 0))

    @Benchmark
    fun storeCountOfOwner(): Int = store.countOfOwner(owners[counter++ % owners.size])

    @Benchmark
    fun storeAddRemoveCycle(): Boolean {
        val loc = DespawnLocation("world", -1, -1, counter++ % 1000, owners[0])
        store.add(loc)
        return store.remove(loc)
    }

    @Benchmark
    fun locationParse(): DespawnLocation? = DespawnLocation.parse("123;64;-987;world_the_end", owners[0])

    @Benchmark
    fun locationSerialize(): String = DespawnLocation("world", 123, 64, -987, owners[0]).serialize()

    @Benchmark
    fun recycleAdvance(): RecycleProgress.Result = RecycleProgress.advance(counter++ % 64)
}
