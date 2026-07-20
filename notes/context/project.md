# Project Overview

## What It Is

**PaperMC Despawned Items** is a **Paper (Minecraft) server plugin**. Instead of letting
ground items vanish when their despawn timer expires, it relocates them into a registered
network of nearby containers, cookers, entities, or empty space. It also provides a
`/recycle` command that despawns the item in a player's hand into the network for a
reward (progress tracked in the player's `PersistentDataContainer`). Originally an
internal plugin for the PopupMC server.

Repo slug: `papermc-despawned-items`. Bukkit plugin id: `DespawnedItems` (no spaces
allowed). Display/listed name: **PaperMC Despawned Items**.

## Goals

- Preserve items that would otherwise be lost to despawn, **without server lag** — the
  pipeline is throttled per tick, lookups are O(1), and persistence is incremental and
  off-thread.
- Let regular players own and manage their own despawn locations, capped by per-group
  `despi.limit.<n>` permissions, with admin override.
- Offer real storage choices for large/networked servers: YAML, SQLite, or MySQL/MariaDB.
- Be **well tested** (JUnit 5 + MockBukkit, run by CI) — see `../plans/testing.md`.
- Target the **Paper 1.21.x** line (built against 1.21.11, Java 21) so MockBukkit is
  available and the largest install base is covered; still loads on 26.x.

## Repository

https://github.com/1fairyfox/papermc-despawned-items (moved from `PopupMC/DespawnedItems`).

## Developer

Built by Fairy Fox (github.com/1fairyfox). Licensed Apache 2.0.

## History

Created 2021 as a Java 11 / Maven plugin for Minecraft 1.16.5 (an unfinished "90% done"
rewrite). Rewritten to Kotlin/Gradle in July 2026 (fairyfox mesh onboarding), then
refactored for modern architecture, scale, and full testing — see
`notes/decisions/architecture.md`, `notes/plans/refactor-2026-07.md`, and the onboarding
report in `notes/fairyfox-reports/`.
