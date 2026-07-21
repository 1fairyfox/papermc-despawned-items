<img src="assets/icon.png" alt="PaperMC Despawned Items icon" width="120" align="right">

# PaperMC Despawned Items

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

<!-- Distribution / usage badges — enable each once the plugin is published on that platform
     (see notes/status.md: "Hangar project + HANGAR_API_TOKEN secret"). Both platforms support
     automated publishing from CI (Hangar via the hangar-publish-plugin / Modrinth via mc-publish),
     so wire the release-publish workflow at the same time.
[![Hangar downloads](https://img.shields.io/hangar/dt/papermc-despawned-items?style=flat-square&label=hangar)](https://hangar.papermc.io/1fairyfox/papermc-despawned-items)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/papermc-despawned-items?style=flat-square&logo=modrinth&logoColor=white&label=modrinth)](https://modrinth.com/plugin/papermc-despawned-items)
[![Modrinth version](https://img.shields.io/modrinth/v/papermc-despawned-items?style=flat-square&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/papermc-despawned-items/versions)
-->


A **Paper** (Minecraft) server plugin that catches items about to despawn on the
ground and — instead of deleting them — relocates them into a registered network of
nearby **containers, cookers, entities, or empty space**. Players and admins manage
their own despawn locations; a `/recycle` command lets players feed the item in their
hand straight into the network for a small reward.

> **Built for the 1.21 line.** A Kotlin rewrite targeting **Paper 1.21.x** (built
> against 1.21.11) on **Java 21**, built with Gradle. It loads on newer **26.x** servers
> too via Paper's forward compatibility. It replaces the original 2021 Java/Maven plugin
> (which targeted Minecraft 1.16.5).

## What it does

When an item entity reaches its despawn timer, the plugin tries, in order, to:

1. **Void** genuinely illegal/technical items (command blocks, netherite, …).
2. Slot fuel/smeltables into a nearby **furnace, blast furnace, or smoker**.
3. Place a block item back into **empty air**, carrying over stored block data
   (banners, skulls, container contents, signs, spawners, …) where the API allows.
4. Fit the item onto a single nearby **entity** — an item frame, a mob/armour stand's
   equipment slots, a storage minecart, or a furnace minecart's fuel.
5. Drop it into an ordinary **container** — chest, barrel, hopper, dropper, dispenser,
   shulker box, or trapped chest.

A short, configurable sound-and-particle effect plays wherever an item lands.

## Features

- **Scales to large servers.** Location lookups are O(1) via spatial + owner indexes,
  and the despawn pipeline is throttled — a flood of despawning items is queued and
  drained at a bounded rate per tick instead of storming the server.
- **Pluggable storage.** Flat **YAML** files (default, zero setup), embedded **SQLite**,
  or shared **MySQL/MariaDB** for a network of servers. Switching backends migrates your
  existing data automatically. Writes are incremental and off the main thread.
- **Self-service with limits.** Players manage their own despawn locations up to a cap,
  set per group/rank through `despi.limit.<n>` permissions (LuckPerms-friendly) with a
  configurable default and an admin bypass.
- **Safe by default.** Contraband is destroyed; hazardous blocks are never re-placed;
  particles that need data are validated at load rather than crashing.

## Commands & permissions

| Command | Permission | Purpose |
|---------|-----------|---------|
| `/despi add this [player]` | `despi.use` (self) · `despi.elevated` (for others) | register the block you're looking at, subject to your limit |
| `/despi remove …` · `/despi clear …` · `/despi exists …` · `/despi locations …` | `despi.use` (self) · `despi.elevated` (others) | manage locations |
| `/despi purge …` | `despi.use` (own) · `despi.elevated` (others/everyone) | bulk-remove materials from despawn storage |
| `/despi despawn …` · `/despi effects …` · `/despi reload do` · `/despi save do` | `despi.elevated` | admin / testing |
| `/recycle` | `recycle.use` | despawn the item in your hand for a reward |

`despi.use` and `recycle.use` default to everyone; `despi.elevated` and
`despi.limit.bypass` default to ops. Grant a group `despi.limit.50` for a 50-location cap.

## Configuration

`plugins/papermc-despawned-items/config.yml` sections:

- `sound` / `particles` — the landing effect (keys, not enums; colored `DUST` supported).
- `limits` — `default` cap and `unlimited` toggle (permissions override per group).
- `performance` — `max-per-tick`, `max-concurrent`, `max-queue`, `drop-when-full`.
- `storage` — `type: yaml | sqlite | mysql`, plus MySQL connection + pool settings.

`/despi reload do` re-reads the file live.

## Install

1. Download `papermc-despawned-items-<version>.jar` from the
   [releases](https://github.com/1fairyfox/papermc-despawned-items/releases) (or build it).
2. Drop it into your server's `plugins/` folder.
3. Start the server (**Paper 1.21.11+**, **Java 21+**; also loads on 26.x). Configure via
   `plugins/papermc-despawned-items/config.yml`.

For SQLite or MySQL storage, Paper downloads the JDBC driver and HikariCP automatically
on first start (declared as plugin `libraries:`) — no extra jars to install.

## Build from source

```sh
./gradlew build          # runs the test suite → build/libs/papermc-despawned-items-<version>.jar
./gradlew dokkaGenerate  # → build/dokka/html (API docs)
```

JDK 21 is provisioned automatically by the Gradle toolchain (foojay resolver); the
Gradle wrapper (9.6.1) is committed. The build runs a JUnit 5 + MockBukkit test suite
(unit, property/fuzz, database, performance, and mocked-server integration) — see
[`notes/plans/testing.md`](notes/plans/testing.md).

## Contributing

Contributions welcome — fork and open a pull request against `dev`. Security issues: see
[SECURITY.md](SECURITY.md).

## License

[Apache 2.0](LICENSE) — do what you like, just credit back.
