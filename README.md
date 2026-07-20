# DespawnedItems

[![CI](https://img.shields.io/github/actions/workflow/status/1fairyfox/papermc-despawned-items/ci.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=CI)](https://github.com/1fairyfox/papermc-despawned-items/actions/workflows/ci.yml)
[![Version](https://img.shields.io/github/v/tag/1fairyfox/papermc-despawned-items?style=flat-square&label=version)](https://github.com/1fairyfox/papermc-despawned-items/releases)
[![Last commit](https://img.shields.io/github/last-commit/1fairyfox/papermc-despawned-items?style=flat-square)](https://github.com/1fairyfox/papermc-despawned-items/commits)
[![Docs](https://img.shields.io/badge/docs-fairyfox.io-4c9?style=flat-square&logo=readthedocs&logoColor=white)](https://fairyfox.io/papermc-despawned-items/)
[![License](https://img.shields.io/github/license/1fairyfox/papermc-despawned-items?style=flat-square)](LICENSE)

A **Paper** (Minecraft) server plugin that catches items about to despawn on the
ground and — instead of deleting them — relocates them into a registered network of
nearby **containers, cookers, entities, or empty space**. Players and admins manage
their own despawn locations; a `/recycle` command lets players feed the item in
their hand straight into the network for a small reward.

> **Modernised for 2026.** This is a Kotlin rewrite targeting **Paper 26.1** on
> **Java 25**, built with Gradle and packaged for the PaperMC Hangar workflow. It
> replaces the original 2021 Java/Maven plugin (which targeted Minecraft 1.16.5).

## What it does

When an item entity reaches its despawn timer, DespawnedItems tries, in order, to:

1. **Void** genuinely illegal/technical items (command blocks, netherite, …).
2. Slot fuel/smeltables into a nearby **furnace, blast furnace, or smoker**.
3. Place a block item back into **empty air**, carrying over stored block data
   (banners, skulls, container contents, signs, spawners, …) where the API allows.
4. Fit the item onto a single nearby **entity** — an item frame, a mob/armour
   stand's equipment slots, a storage minecart, or a furnace minecart's fuel.
5. Drop it into an ordinary **container** — chest, barrel, hopper, dropper,
   dispenser, shulker box, or trapped chest.

A short sound-and-particle effect plays wherever an item lands.

## Commands & permissions

| Command | Permission | Who |
|---------|-----------|-----|
| `/despi add this [player]` | `despi.use` | register the block you're looking at |
| `/despi remove …`, `/despi clear …`, `/despi exists …`, `/despi locations …` | `despi.use` (self) / `despi.elevated` (others) | manage locations |
| `/despi purge …` | `despi.use` / `despi.elevated` | bulk-remove materials from despawn storage |
| `/despi despawn …`, `/despi effects …`, `/despi indexes …`, `/despi reload do`, `/despi save do` | `despi.elevated` | admin / testing |
| `/recycle` | `recycle.use` | despawn the item in your hand |

`despi.use` and `recycle.use` default to everyone; `despi.elevated` defaults to ops.

## Install

1. Download `DespawnedItems-<version>.jar` from the
   [releases](https://github.com/1fairyfox/papermc-despawned-items/releases) (or build
   it — see below).
2. Drop it into your server's `plugins/` folder.
3. Start the server (Paper 26.1+, Java 25+). Configure via
   `plugins/DespawnedItems/config.yml`.

## Build from source

```sh
./gradlew build          # → build/libs/DespawnedItems-<version>.jar (Kotlin stdlib shaded in)
./gradlew dokkaGenerate  # → build/dokka/html (API docs)
```

Requires JDK 25. The Gradle wrapper (9.6.1) is committed.

## Contributing

Contributions welcome — fork and open a pull request against `dev`. Security issues:
see [SECURITY.md](SECURITY.md).

## License

[Apache 2.0](LICENSE) — do what you like, just credit back.
