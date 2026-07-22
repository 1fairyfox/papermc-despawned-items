<img src="assets/icon.png" alt="PaperMC Despawned Items icon" width="120" align="right">

# PaperMC Despawned Items

**Minecraft deletes your stuff. This plugin catches it first.**

Every item dropped on the ground in Minecraft has five minutes to live. When the timer runs
out the server deletes it — the diamonds that fell out of a full inventory, the drops from
the mob farm nobody emptied, the stack somebody died on the way back to. This plugin
intercepts that moment and, instead of deleting the item, **puts it somewhere**: a chest, a
barrel, a furnace, an item frame, an armour stand, a minecart, or just back on the ground as
a placed block.

[![Contributors](https://img.shields.io/github/contributors/1fairyfox/papermc-despawned-items?style=flat-square&logo=github)](https://github.com/1fairyfox/papermc-despawned-items/graphs/contributors)
[![Stars](https://img.shields.io/github/stars/1fairyfox/papermc-despawned-items?style=flat-square&logo=github)](https://github.com/1fairyfox/papermc-despawned-items/stargazers)
[![Forks](https://img.shields.io/github/forks/1fairyfox/papermc-despawned-items?style=flat-square&logo=github)](https://github.com/1fairyfox/papermc-despawned-items/network/members)
![Watchers](https://img.shields.io/github/watchers/1fairyfox/papermc-despawned-items?style=flat-square&logo=github)
[![Last commit](https://img.shields.io/github/last-commit/1fairyfox/papermc-despawned-items?style=flat-square)](https://github.com/1fairyfox/papermc-despawned-items/commits)
![Commits](https://img.shields.io/github/commit-activity/t/1fairyfox/papermc-despawned-items?style=flat-square&label=commits)
[![Version](https://img.shields.io/github/v/tag/1fairyfox/papermc-despawned-items?style=flat-square&label=version)](https://github.com/1fairyfox/papermc-despawned-items/releases)
![Java](https://img.shields.io/badge/java-21-f89820?style=flat-square&logo=openjdk&logoColor=white)
![Paper](https://img.shields.io/badge/Paper-1.21.x-2a9d8f?style=flat-square)
[![CI](https://img.shields.io/github/actions/workflow/status/1fairyfox/papermc-despawned-items/ci.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=CI)](https://github.com/1fairyfox/papermc-despawned-items/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/codecov/c/github/1fairyfox/papermc-despawned-items?style=flat-square&logo=codecov&logoColor=white&label=coverage)](https://codecov.io/gh/1fairyfox/papermc-despawned-items)
[![Code quality](https://img.shields.io/codefactor/grade/github/1fairyfox/papermc-despawned-items?style=flat-square&logo=codefactor&logoColor=white&label=code%20quality)](https://www.codefactor.io/repository/github/1fairyfox/papermc-despawned-items)
[![Quality gate](https://img.shields.io/sonar/quality_gate/1fairyfox_papermc-despawned-items?server=https%3A%2F%2Fsonarcloud.io&style=flat-square&logo=sonarcloud&logoColor=white&label=quality%20gate)](https://sonarcloud.io/summary/new_code?id=1fairyfox_papermc-despawned-items)
[![Tech debt](https://img.shields.io/sonar/tech_debt/1fairyfox_papermc-despawned-items?server=https%3A%2F%2Fsonarcloud.io&style=flat-square&logo=sonarcloud&logoColor=white&label=tech%20debt)](https://sonarcloud.io/summary/new_code?id=1fairyfox_papermc-despawned-items)
[![OpenSSF Scorecard](https://img.shields.io/ossf-scorecard/github.com/1fairyfox/papermc-despawned-items?style=flat-square&label=scorecard)](https://securityscorecards.dev/viewer/?uri=github.com/1fairyfox/papermc-despawned-items)
[![Docs](https://img.shields.io/badge/docs-fairyfox.io-4c9?style=flat-square&logo=readthedocs&logoColor=white)](https://fairyfox.io/papermc-despawned-items/)
[![Pages](https://img.shields.io/github/actions/workflow/status/1fairyfox/papermc-despawned-items/docs.yml?branch=main&style=flat-square&logo=readthedocs&logoColor=white&label=pages)](https://github.com/1fairyfox/papermc-despawned-items/actions/workflows/docs.yml)
[![Open issues](https://img.shields.io/github/issues/1fairyfox/papermc-despawned-items?style=flat-square)](https://github.com/1fairyfox/papermc-despawned-items/issues)
![Closed issues](https://img.shields.io/github/issues-closed/1fairyfox/papermc-despawned-items?style=flat-square)
[![Open PRs](https://img.shields.io/github/issues-pr/1fairyfox/papermc-despawned-items?style=flat-square)](https://github.com/1fairyfox/papermc-despawned-items/pulls)
![Closed PRs](https://img.shields.io/github/issues-pr-closed/1fairyfox/papermc-despawned-items?style=flat-square)
[![License](https://img.shields.io/github/license/1fairyfox/papermc-despawned-items?style=flat-square)](LICENSE)

<!-- Distribution / usage badges — enable each once the plugin is published on that platform.
     Publishing is already automated in .github/workflows/release.yml (Modrinth + CurseForge via
     mc-publish, Hangar via hangar-publish-plugin); each step is gated on its token secret, so
     these go live as soon as the projects exist. Owner steps: notes/plans/platform-targets.md.
[![Hangar downloads](https://img.shields.io/hangar/dt/papermc-despawned-items?style=flat-square&label=hangar)](https://hangar.papermc.io/1fairyfox/papermc-despawned-items)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/papermc-despawned-items?style=flat-square&logo=modrinth&logoColor=white&label=modrinth)](https://modrinth.com/plugin/papermc-despawned-items)
[![Modrinth version](https://img.shields.io/modrinth/v/papermc-despawned-items?style=flat-square&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/papermc-despawned-items/versions)
-->

---

## Screenshots

These are captured **automatically**, from a real Paper server running the current build —
CI boots the server, joins it with a bot, builds each scene and takes the frame at the exact
tick. They are never stale, and nobody hand-picked a flattering angle.

| | |
|---|---|
| ![A registered despawn network](https://fairyfox.io/papermc-despawned-items/screenshots/01-despawn-network.png) | ![Items waiting out their despawn timer](https://fairyfox.io/papermc-despawned-items/screenshots/02-items-on-the-ground.png) |
| **The network** — chests, barrels, cookers, hoppers and entities all accept rescued items. | **The problem** — forty dropped items, five minutes from deletion. |
| ![The landing effect](https://fairyfox.io/papermc-despawned-items/screenshots/04-particle-effect.png) | ![A group of items relocating](https://fairyfox.io/papermc-despawned-items/screenshots/03-group-relocation.png) |
| **The landing effect** — a configurable sound and particle burst marks where an item came to rest. | **A group relocating** — a whole batch rescued at once, at a bounded rate. |

**[→ Full gallery, refreshed every build](https://fairyfox.io/papermc-despawned-items/screenshots.html)**

## What it actually does

When an item entity reaches its despawn timer, the plugin picks a registered location at
random and offers the item to each strategy in turn until something takes it:

1. **Void** — genuinely illegal or technical items (command blocks, debug sticks, structure
   blocks, netherite, plus anything the admin bans) are destroyed rather than duplicated
   into the world. Optionally routed to a **catch-all** chest instead, so staff can see what
   players are dropping.
2. **Cookers** — fuel and smeltables slot into a nearby furnace, blast furnace, or smoker.
   Your coal ends up burning, not gone.
3. **Empty air** — a block item is *placed back into the world*, carrying its stored data
   with it: banner patterns, player skulls, sign text, spawner types, and the contents of
   shulker boxes survive the trip.
4. **Entities** — a single item fits onto an item frame, into a mob's or armour stand's
   equipment slots, into a storage minecart, or as fuel in a furnace minecart.
5. **Containers** — anything left goes into a chest, barrel, hopper, dropper, dispenser,
   shulker box, or trapped chest.

Whatever takes the item, a short configurable sound-and-particle effect plays where it
landed, so the recovery is visible rather than silent.

## Why an admin would install this

**It removes an entire category of support ticket.** "The server ate my stuff" is one of the
most common and least resolvable complaints in Minecraft administration — there is no log, no
rollback, and no way to prove what was lost. This makes the answer "check the recovery chest"
instead of "sorry".

**It is a lag fix, not a lag source.** The usual community answer to ground-item clutter is a
clear-lag plugin that *deletes faster*. This deletes nothing and still removes the entities —
items leave the world into container inventories, which cost the server nothing to tick. And
the pipeline itself is bounded: relocations are queued and drained at `max-per-tick`, never
spawning unbounded work, with measured throughput of roughly `max-per-tick × 20` items/second.

**It survives contact with a real server.** Lookups are O(1) through spatial and owner
indexes; persistence is incremental and off the main thread; there is a hard cap on queue
depth with a configurable drop policy. A 5 000-item burst lands in about 1.3× the theoretical
minimum number of ticks.

**One player can no longer ruin it for everyone.** New in v1.5.0: per-user throttling. The
server-wide budget answers *"how much work may the server do"*; throttling answers *"how much
of it may one player take"*. Pick a strategy — `rate` (per player per time window),
`concurrent` (in flight at once), `fair-share` (weighted round-robin), or `combined` — and set
allowances per rank with permissions. A VIP group can get four times the drain weight of a
default player, with one LuckPerms node.

**It fits whatever storage you already run.** Flat YAML (zero setup), embedded SQLite, or
shared MySQL/MariaDB so a whole network sees the same locations. Switching backends migrates
your data automatically; the JDBC driver and pool download themselves on first start.

**You can tune the economy, not just the mechanics.** A configurable `void.chance` lets you
decide that recovery isn't a *perfect* safety net — say, one item in twenty is still lost —
so item sinks keep working on an economy server. Voided items can still land in a catch-all
for audit.

**It is maintained to a bar you can inspect.** ~200 tests across every layer gate the build at
≥90% line coverage; CI boots a real Paper 1.21.11 server *and* the newest Paper release on
every push, joins them with an actual Minecraft client, and runs the commands end to end.
Static analysis runs with no baseline and no suppressed findings. Releases carry build
provenance.

## Why a player would use it

- **Your drops come back.** Die on the way home, overflow your inventory, forget a shulker on
  the ground — instead of a five-minute countdown to nothing, it lands in your chest.
- **You set it up yourself.** `/despi add this` on the block you're looking at. No asking an
  admin, no config file. You get a personal allowance (default 10 locations, more by rank).
- **Your farms empty themselves.** Put a registered chest near the drop point and the
  overflow that would have despawned files itself away instead.
- **`/recycle` pays you.** Feed the item in your hand straight into the network and earn a
  reward for it. It's a use for the junk you'd otherwise throw in lava.
- **You can see it working.** Every rescue plays a sound and a puff of particles at the
  container, so you know it happened.

## Commands & permissions

| Command | Permission | Purpose |
|---------|-----------|---------|
| `/despi add this [player]` | `despi.use` (self) · `despi.elevated` (others) | register the block you're looking at, subject to your limit |
| `/despi remove …` · `/despi clear …` · `/despi exists …` · `/despi locations …` | `despi.use` (self) · `despi.elevated` (others) | manage locations |
| `/despi purge …` | `despi.use` (own) · `despi.elevated` (others/everyone) | bulk-remove materials from despawn storage |
| `/despi despawn …` · `/despi effects …` · `/despi reload do` · `/despi save do` | `despi.elevated` | admin / testing |
| `/recycle` (also `/despi recycle`) | `recycle.use` | despawn the item in your hand for a reward |

`despi.use` and `recycle.use` default to everyone; `despi.elevated`, `despi.limit.bypass` and
`despi.throttle.bypass` default to ops.

**Per-rank tuning is all permissions** — grant them to LuckPerms groups:

| Node | Effect |
|---|---|
| `despi.limit.<n>` | own up to *n* despawn locations (highest node wins) |
| `despi.throttle.rate.<n>` | *n* relocations per time window |
| `despi.throttle.concurrent.<n>` | *n* relocations in flight at once |
| `despi.throttle.weight.<n>` | fair-share drain weight — the "some users get more" knob |

Both command names are **renameable** (with aliases) via `commands:` in `config.yml`, for when
another plugin already claims `/recycle`. Renames take effect on restart.

## Configuration

`plugins/papermc-despawned-items/config.yml`:

| Section | What it controls |
|---|---|
| `commands` | rename `/despi` and `/recycle`, add aliases (restart required) |
| `sound` / `particles` | the landing effect — keys not enums, coloured `DUST` supported |
| `limits` | default location cap and an `unlimited` switch (permissions override per rank) |
| `performance` | `max-per-tick`, `max-concurrent`, `max-queue`, `drop-when-full` |
| **`throttle`** | per-user throttling: strategy, rate window, concurrency, fair-share weight, over-quota policy |
| **`void`** | void chance, extra banned materials, and the catch-all containers |
| `storage` | `yaml` · `sqlite` · `mysql`, plus connection and pool settings |

`throttle` and `void` ship **inert** — upgrading changes no behaviour until you switch them
on. `/despi reload do` re-reads the file live.

## Install

1. Download `papermc-despawned-items-<version>.jar` from
   [releases](https://github.com/1fairyfox/papermc-despawned-items/releases).
2. Drop it into your server's `plugins/` folder.
3. Start the server. Configure via `plugins/papermc-despawned-items/config.yml`.

Every release also ships an **artifact bundle**: the plugin jar, the default `config.yml` for
that version, the API docs, a source archive, `SHA256SUMS.txt`, the screenshots, and a
build-provenance attestation you can verify with `gh attestation verify`.

For SQLite or MySQL storage, Paper downloads the JDBC driver and HikariCP automatically on
first start (declared as plugin `libraries:`) — no extra jars to install.

## Supported platforms

| | |
|---|---|
| **Supported** | **Paper 1.21.x** (built against 1.21.11, Java 21), **Purpur**, and compatible Paper forks. Also loads on newer **26.x** servers via Paper's forward compatibility — verified in CI on every push. |
| **Declared** | Spigot / Bukkit within `api-version: '1.21'`. |
| **Planned** | **Folia.** Not yet supported and deliberately not declared: Folia removes the single main thread, and the plugin's schedulers and shared state need a real port first. The plan is written up in [`notes/plans/platform-targets.md`](notes/plans/platform-targets.md). |
| **Separate editions, not started** | Fabric, NeoForge, Sponge — each needs its own implementation, not a repackage. Roadmap and honest cost estimates in the same file. |
| **Not supported** | Hybrid Bukkit-on-Forge/Fabric servers (Arclight, Mohist, Magma, Cardboard, Banner). Bug reports welcome when reproducible on a supported platform. |

This is a Kotlin rewrite of the original 2021 Java/Maven plugin, which targeted 1.16.5.

## Build from source

```sh
./gradlew build          # ktlint + detekt + ~200 tests + coverage gate → build/libs/*.jar
./gradlew dokkaGenerate  # → build/dokka/html (API docs)
node scripts/screenshots.mjs <server-dir> <out-dir>   # the screenshot harness, headless
```

JDK 21 is provisioned automatically by the Gradle toolchain (foojay resolver); the wrapper is
committed. `build` fails below 90% line coverage — see
[`notes/plans/testing.md`](notes/plans/testing.md) for what's tested and how.

## Contributing

Contributions welcome — fork and open a pull request against `dev`. Security issues: see
[SECURITY.md](SECURITY.md).

## License

[Apache 2.0](LICENSE) — do what you like, just credit back.
