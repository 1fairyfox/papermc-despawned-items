@file:Suppress("UnstableApiUsage")

package io.fairyfox.papermc.despawneditems.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Registers `/despi` and `/recycle` using Paper's modern **Brigadier** command API via
 * the lifecycle event system — replacing the old `plugin.yml` command + hand-rolled
 * string parsing. Brigadier gives typed arguments, permission-scoped visibility, native
 * tab-completion, and client-side validation. All behaviour lives in [DespiActions].
 */
object DespiCommand {
    private const val USE = "despi.use"
    private const val ELEVATED = "despi.elevated"
    private const val RECYCLE_USE = "recycle.use"

    fun register(plugin: PaperMcDespawnedItems) {
        val actions = DespiActions(plugin)
        // Names/aliases are read once here — a rename needs a server restart, which the
        // config documents (the lifecycle event fires during startup only).
        val names = plugin.settings.commands
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                buildDespi(actions, names.despiName).build(),
                "Manage despawn locations and despawning",
                names.despiAliases,
            )
            event.registrar().register(
                buildRecycle(actions, names.recycleName).build(),
                "Recycle the item in your hand for a reward",
                names.recycleAliases,
            )
        }
    }

    // ─── /despi tree ────────────────────────────────────────────────────────────

    private fun buildDespi(
        a: DespiActions,
        name: String,
    ) = Commands.literal(name)
        .requires { it.sender.hasPermission(USE) }
        .then(recycleBranch(a))
        .then(addBranch(a))
        .then(removeBranch(a))
        .then(clearBranch(a))
        .then(existsBranch(a))
        .then(locationsBranch(a))
        .then(purgeBranch(a))
        .then(despawnBranch(a))
        .then(effectsBranch(a))
        .then(literal("reload").requires(elevated).then(literal("do").executes(sender { s, _ -> a.reload(s) })))
        .then(literal("save").requires(elevated).then(literal("do").executes(sender { s, _ -> a.save(s) })))

    /** `/despi recycle` — the same behaviour as the standalone recycle command. */
    private fun recycleBranch(a: DespiActions) =
        literal("recycle")
            .requires { it.sender.hasPermission(RECYCLE_USE) }
            .executes(player { p, _ -> a.recycle(p) })

    private fun addBranch(a: DespiActions) =
        literal("add").then(
            literal("this")
                .executes(player { p, _ -> a.add(p, null) })
                .then(
                    playerArg().requires(elevated)
                        .executes(player { p, c -> a.add(p, str(c, "player")) }),
                ),
        )

    private fun removeBranch(a: DespiActions) =
        literal("remove")
            .then(
                literal("here")
                    .then(literal("owned-by-me").executes(player { p, _ -> a.removeHereOwnedByMe(p) }))
                    .then(literal("owned-by-anyone").requires(elevated).executes(player { p, _ -> a.removeHereOwnedByAnyone(p) }))
                    .then(
                        literal("owned-by").requires(elevated).then(
                            playerArg().executes(
                                player {
                                        p,
                                        c,
                                    ->
                                    a.removeHereOwnedByPlayer(p, str(c, "player"))
                                },
                            ),
                        ),
                    ),
            )
            .then(
                literal("anywhere")
                    .then(literal("owned-by-me").executes(player { p, _ -> a.removeAnywhereOwnedByMe(p) }))
                    .then(
                        literal("owned-by").requires(elevated).then(
                            playerArg().executes(
                                sender {
                                        s,
                                        c,
                                    ->
                                    a.removeAnywhereOwnedByPlayer(s, str(c, "player"))
                                },
                            ),
                        ),
                    ),
            )

    private fun clearBranch(a: DespiActions) =
        literal("clear")
            .then(literal("mine").executes(player { p, _ -> a.clearMine(p) }))
            .then(literal("here").requires(elevated).executes(player { p, _ -> a.clearHere(p) }))
            .then(literal("player").requires(elevated).then(playerArg().executes(sender { s, c -> a.clearPlayer(s, str(c, "player")) })))

    private fun existsBranch(a: DespiActions) =
        literal("exists")
            .then(
                literal("here")
                    .then(literal("owned-by-me").executes(player { p, _ -> a.existsHereOwnedByMe(p) }))
                    .then(literal("owned-by-anyone").requires(elevated).executes(player { p, _ -> a.existsHereOwnedByAnyone(p) }))
                    .then(
                        literal("owned-by").requires(elevated).then(
                            playerArg().executes(
                                player {
                                        p,
                                        c,
                                    ->
                                    a.existsHereOwnedByPlayer(p, str(c, "player"))
                                },
                            ),
                        ),
                    ),
            )
            .then(
                literal("anywhere")
                    .then(literal("owned-by-me").executes(player { p, _ -> a.existsAnywhereOwnedByMe(p) }))
                    .then(
                        literal("owned-by").requires(elevated).then(
                            playerArg().executes(
                                sender {
                                        s,
                                        c,
                                    ->
                                    a.existsAnywhereOwnedByPlayer(s, str(c, "player"))
                                },
                            ),
                        ),
                    ),
            )

    private fun locationsBranch(a: DespiActions) =
        literal("locations")
            .then(literal("mine").executes(player { p, _ -> a.locationsMine(p) }))
            .then(literal("count").requires(elevated).executes(sender { s, _ -> a.locationsCount(s) }))
            .then(literal("here").requires(elevated).executes(player { p, _ -> a.locationsHere(p) }))
            .then(literal("solo-mode").requires(elevated).executes(player { p, _ -> a.soloMode(p) }))
            .then(literal("normal-mode").requires(elevated).executes(sender { s, _ -> a.normalMode(s) }))
            .then(
                literal("player").requires(elevated).then(
                    playerArg().executes(
                        sender {
                                s,
                                c,
                            ->
                            a.locationsPlayer(s, str(c, "player"))
                        },
                    ),
                ),
            )

    private fun purgeBranch(a: DespiActions) =
        literal("purge")
            .then(purgeTargets(a, "owned-by-me") { s, _ -> (s as? Player)?.uniqueId })
            .then(literal("owned-by-everyone").requires(elevated).let { attachPurge(it, a) { _, _ -> null } })
            .then(
                literal("owned-by-player").requires(elevated)
                    .then(attachPurge(playerArg(), a) { _, c -> a.playerId(str(c, "player")) }),
            )

    private fun purgeTargets(
        a: DespiActions,
        name: String,
        owner: (CommandSender, CommandContext<CommandSourceStack>) -> java.util.UUID?,
    ) = attachPurge(literal(name), a, owner)

    private fun <T : ArgumentBuilder<CommandSourceStack, T>> attachPurge(
        node: T,
        a: DespiActions,
        owner: (CommandSender, CommandContext<CommandSourceStack>) -> java.util.UUID?,
    ): T =
        node
            .then(literal("in-hand").executes(sender { s, c -> a.purgeInHand(s, owner(s, c)) }))
            .then(literal("materials").then(greedy("names").executes(sender { s, c -> a.purgeMaterials(s, owner(s, c), str(c, "names")) })))

    private fun despawnBranch(a: DespiActions) =
        literal("despawn").requires(elevated)
            .executes(sender { s, _ -> a.despawnCount(s) })
            .then(literal("count-ongoing").executes(sender { s, _ -> a.despawnCount(s) }))
            .then(literal("create-from-hand").executes(player { p, _ -> a.despawnFromHand(p) }))
            .then(literal("clear-ongoing").executes(sender { s, _ -> a.despawnClearOngoing(s) }))
            .then(
                literal("create-material").then(
                    greedy("names")
                        .executes(sender { s, c -> a.despawnMaterial(s, str(c, "names"), 1) }),
                ),
            )
            .then(
                literal("create-material-amount")
                    .then(
                        Commands.argument("names", StringArgumentType.word()).then(
                            Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(
                                    sender {
                                            s,
                                            c,
                                        ->
                                        a.despawnMaterial(s, str(c, "names"), IntegerArgumentType.getInteger(c, "amount"))
                                    },
                                ),
                        ),
                    ),
            )

    private fun effectsBranch(a: DespiActions) =
        literal("effects").requires(elevated)
            .then(literal("count-ongoing").executes(sender { s, _ -> a.effectsCount(s) }))
            .then(literal("create-here").executes(player { p, _ -> a.effectsCreateHere(p) }))
            .then(literal("clear-ongoing").executes(sender { s, _ -> a.effectsClearOngoing(s) }))

    // ─── /recycle ───────────────────────────────────────────────────────────────

    private fun buildRecycle(
        a: DespiActions,
        name: String,
    ) = Commands.literal(name)
        .requires { it.sender.hasPermission(RECYCLE_USE) }
        .executes(player { p, _ -> a.recycle(p) })

    // ─── builder helpers ─────────────────────────────────────────────────────────

    private val elevated = java.util.function.Predicate<CommandSourceStack> { it.sender.hasPermission(ELEVATED) }

    private fun literal(name: String) = Commands.literal(name)

    /** A `player` string argument suggesting online player names. */
    private fun playerArg() =
        Commands.argument("player", StringArgumentType.word())
            .suggests { _, builder ->
                Bukkit.getOnlinePlayers().forEach { builder.suggest(it.name) }
                builder.buildFuture()
            }

    private fun greedy(name: String) = Commands.argument(name, StringArgumentType.greedyString())

    private fun str(
        ctx: CommandContext<CommandSourceStack>,
        name: String,
    ): String = StringArgumentType.getString(ctx, name)

    /** Executor that requires a player sender; sends an error otherwise. */
    private fun player(block: (Player, CommandContext<CommandSourceStack>) -> Unit) =
        Command<CommandSourceStack> { ctx ->
            val sender = ctx.source.sender
            if (sender !is Player) {
                CommandFeedback.error(sender, "Only players can use this command")
                return@Command 0
            }
            block(sender, ctx)
            Command.SINGLE_SUCCESS
        }

    /** Executor for any sender. */
    private fun sender(block: (CommandSender, CommandContext<CommandSourceStack>) -> Unit) =
        Command<CommandSourceStack> { ctx ->
            block(ctx.source.sender, ctx)
            Command.SINGLE_SUCCESS
        }
}
