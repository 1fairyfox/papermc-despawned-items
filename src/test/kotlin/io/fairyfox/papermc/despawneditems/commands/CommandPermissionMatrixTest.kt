package io.fairyfox.papermc.despawneditems.commands

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.TargetingPlayerMock
import io.fairyfox.papermc.despawneditems.plain
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.assertEquals

/**
 * The exhaustive command × permission matrix (testing.md §23 + §59): EVERY `/despi`
 * subcommand and `/recycle`, dispatched through the real Brigadier registration, at
 * every permission level — each combination its own JUnit test node, asserting both
 * the allowed AND the denied direction individually.
 *
 * Levels: NONE (all plugin permissions revoked) · USE (the defaults: `despi.use` +
 * `recycle.use`, no elevation) · ELEVATED (op). A command executes iff its tier is
 * within the level; execution is proven by the command's own distinctive reply.
 */
class CommandPermissionMatrixTest {
    private enum class Tier { USE, RECYCLE, ELEVATED }

    private enum class Level { NONE, USE, ELEVATED }

    private class Ctx(
        val server: ServerMock,
        val plugin: PaperMcDespawnedItems,
        val world: WorldMock,
        val player: TargetingPlayerMock,
    ) {
        val strangerId = Bukkit.getOfflinePlayer("Stranger").uniqueId

        // Both fixtures sit at the player's target block (0,64,0) so the `here`
        // variants find them; different owners can share the block.
        fun addMine() = plugin.locations.add(Location(world, 0.0, 64.0, 0.0), player.uniqueId)

        fun addStrangers() = plugin.locations.add(Location(world, 0.0, 64.0, 0.0), strangerId)

        fun holdDirt() = player.inventory.setItemInMainHand(ItemStack(Material.DIRT, 1))
    }

    private class Case(
        val command: String,
        val tier: Tier,
        marker: String,
        val setup: (Ctx) -> Unit = {},
    ) {
        val marker = Regex(Regex.escape(marker))
    }

    // Every subcommand once, with the fixture that makes its happy path reachable and
    // the distinctive reply that proves it actually executed.
    private fun cases(): List<Case> =
        listOf(
            // ── despi.use tier ──────────────────────────────────────────────────
            Case("despi add this", Tier.USE, "Successfully added location!"),
            Case("despi remove here owned-by-me", Tier.USE, "Location removed", { it.addMine() }),
            Case("despi remove anywhere owned-by-me", Tier.USE, "A location was removed!", { it.addMine() }),
            Case("despi clear mine", Tier.USE, "location(s) were removed", { it.addMine() }),
            Case("despi exists here owned-by-me", Tier.USE, "Location does exist", { it.addMine() }),
            Case("despi exists anywhere owned-by-me", Tier.USE, "A location was found!", { it.addMine() }),
            Case("despi locations mine", Tier.USE, "location(s) were found", { it.addMine() }),
            Case("despi purge owned-by-me materials dirt", Tier.USE, "Begun removal", { it.addMine() }),
            Case("despi purge owned-by-me in-hand", Tier.USE, "Begun removal", {
                it.addMine()
                it.holdDirt()
            }),
            // ── recycle.use tier ────────────────────────────────────────────────
            Case("recycle", Tier.RECYCLE, "nothing in your hand"),
            Case("despi recycle", Tier.RECYCLE, "nothing in your hand"),
            // ── despi.elevated tier ─────────────────────────────────────────────
            Case("despi add this Stranger", Tier.ELEVATED, "Successfully added location!"),
            Case("despi remove here owned-by-anyone", Tier.ELEVATED, "Location removed", { it.addMine() }),
            Case("despi remove here owned-by Stranger", Tier.ELEVATED, "Location removed", { it.addStrangers() }),
            Case("despi remove anywhere owned-by Stranger", Tier.ELEVATED, "A location was removed!", { it.addStrangers() }),
            Case("despi clear here", Tier.ELEVATED, "owner(s) were removed", { it.addMine() }),
            Case("despi clear player Stranger", Tier.ELEVATED, "were removed for Stranger", { it.addStrangers() }),
            Case("despi exists here owned-by-anyone", Tier.ELEVATED, "Location does exist", { it.addMine() }),
            Case("despi exists here owned-by Stranger", Tier.ELEVATED, "Location does exist", { it.addStrangers() }),
            Case("despi exists anywhere owned-by Stranger", Tier.ELEVATED, "A location was found!", { it.addStrangers() }),
            Case("despi locations count", Tier.ELEVATED, "Locations:"),
            Case("despi locations here", Tier.ELEVATED, "owner(s) were found", { it.addMine() }),
            Case("despi locations player Stranger", Tier.ELEVATED, "found for Stranger", { it.addStrangers() }),
            Case("despi locations solo-mode", Tier.ELEVATED, "Solo'd this location", { it.addMine() }),
            Case("despi locations normal-mode", Tier.ELEVATED, "Undid solo"),
            Case("despi purge owned-by-everyone materials dirt", Tier.ELEVATED, "Begun removal", { it.addMine() }),
            Case("despi purge owned-by-everyone in-hand", Tier.ELEVATED, "Begun removal", {
                it.addMine()
                it.holdDirt()
            }),
            Case("despi purge owned-by-player Stranger materials dirt", Tier.ELEVATED, "Begun removal", { it.addStrangers() }),
            Case("despi purge owned-by-player Stranger in-hand", Tier.ELEVATED, "Begun removal", {
                it.addStrangers()
                it.holdDirt()
            }),
            Case("despi despawn", Tier.ELEVATED, "Despawns:"),
            Case("despi despawn count-ongoing", Tier.ELEVATED, "Despawns:"),
            Case("despi despawn create-from-hand", Tier.ELEVATED, "Created forced despawn", { it.holdDirt() }),
            Case("despi despawn create-material dirt", Tier.ELEVATED, "Created forced despawn"),
            Case("despi despawn create-material-amount dirt 5", Tier.ELEVATED, "Created forced despawn"),
            Case("despi despawn clear-ongoing", Tier.ELEVATED, "Cleared"),
            Case("despi effects count-ongoing", Tier.ELEVATED, "Effects Count:"),
            Case("despi effects create-here", Tier.ELEVATED, "Created fake effect"),
            Case("despi effects clear-ongoing", Tier.ELEVATED, "Cleared"),
            Case("despi reload do", Tier.ELEVATED, "reloaded"),
            Case("despi save do", Tier.ELEVATED, "saved"),
        )

    private fun allowed(
        level: Level,
        tier: Tier,
    ): Boolean =
        when (tier) {
            Tier.USE, Tier.RECYCLE -> level != Level.NONE
            Tier.ELEVATED -> level == Level.ELEVATED
        }

    @TestFactory
    fun `every command at every permission level`(): List<DynamicTest> =
        cases().flatMap { case ->
            Level.entries.map { level ->
                val expectation = if (allowed(level, case.tier)) "executes" else "is denied"
                DynamicTest.dynamicTest("[$level] /${case.command} $expectation") {
                    runCase(case, level)
                }
            }
        }

    private fun runCase(
        case: Case,
        level: Level,
    ) {
        val server = MockBukkit.mock()
        try {
            val plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
            val world = server.addSimpleWorld("world")
            val player = TargetingPlayerMock(server, "Tester")
            server.addPlayer(player)
            world.getBlockAt(0, 64, 0).type = Material.STONE
            player.target = world.getBlockAt(0, 64, 0)

            when (level) {
                Level.NONE -> {
                    player.addAttachment(plugin, "despi.use", false)
                    player.addAttachment(plugin, "recycle.use", false)
                }
                Level.USE -> Unit // the plugin.yml defaults: despi.use + recycle.use
                Level.ELEVATED -> player.isOp = true
            }

            case.setup(Ctx(server, plugin, world, player))
            server.dispatchCommand(player, case.command)

            val messages = ArrayList<String>()
            while (true) messages.add((player.nextMessage() ?: break).plain())
            val executed = messages.any { case.marker.containsMatchIn(it) }

            assertEquals(
                allowed(level, case.tier),
                executed,
                "[$level] /${case.command} → messages: $messages",
            )
        } finally {
            MockBukkit.unmock()
        }
    }

    /**
     * The commands together as a group: a whole player workflow on one server, with
     * an ordinary player and an admin, ending with a `/recycle` whose item actually
     * lands in the registered container — commands and pipeline working as one.
     */
    @TestFactory
    fun `group scenario`(): List<DynamicTest> =
        listOf(
            DynamicTest.dynamicTest("player + admin workflow ends with a recycled item in the registered chest") {
                val server = MockBukkit.mock()
                try {
                    val plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
                    val world = io.fairyfox.papermc.despawneditems.SyncChunkWorldMock()
                    server.addWorld(world)

                    val player = TargetingPlayerMock(server, "Player")
                    server.addPlayer(player)
                    val admin = TargetingPlayerMock(server, "Admin")
                    server.addPlayer(admin)
                    admin.isOp = true

                    // The player registers a chest they're looking at.
                    val chestBlock = world.getBlockAt(0, 64, 0)
                    chestBlock.type = Material.CHEST
                    io.fairyfox.papermc.despawneditems.stickyContainer(chestBlock)
                    player.target = chestBlock
                    server.dispatchCommand(player, "despi add this")
                    assertEquals(1, plugin.locations.count, "player's chest registered")

                    // The admin registers the same chest for Stranger and audits it.
                    admin.target = chestBlock
                    server.dispatchCommand(admin, "despi add this Stranger")
                    assertEquals(2, plugin.locations.count, "admin added for Stranger")
                    server.dispatchCommand(admin, "despi locations here")
                    server.dispatchCommand(admin, "despi locations count")

                    // The admin removes Stranger's entry again.
                    server.dispatchCommand(admin, "despi remove here owned-by Stranger")
                    assertEquals(1, plugin.locations.count, "back to just the player's entry")

                    // The player recycles a held item; it must land in their chest.
                    player.inventory.setItemInMainHand(ItemStack(Material.COBBLESTONE, 7))
                    server.dispatchCommand(player, "recycle")
                    server.scheduler.performTicks(4)

                    val chest = chestBlock.state as org.bukkit.block.Container
                    assertEquals(
                        true,
                        chest.inventory.contains(Material.COBBLESTONE, 7),
                        "the recycled stack must arrive in the registered chest",
                    )
                    assertEquals(0, plugin.despawnProcesses.size, "pipeline drained")

                    // The player cleans up their own registration.
                    server.dispatchCommand(player, "despi clear mine")
                    assertEquals(0, plugin.locations.count)
                } finally {
                    MockBukkit.unmock()
                }
            },
        )
}
