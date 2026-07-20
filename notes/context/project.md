# Project Overview

## What It Is

DespawnedItems is a **Paper (Minecraft) server plugin**. Instead of letting ground
items vanish when their despawn timer expires, it relocates them into a registered
network of nearby containers, cookers, entities, or empty space. It also provides a
`/recycle` command that despawns the item in a player's hand into the network for a
small scoreboard-tracked reward. Originally an internal plugin for the PopupMC server.

## Goals

- Preserve items that would otherwise be lost to despawn, without server lag.
- Let regular players own and manage their own despawn locations, with admin override.
- Stay current with modern Paper: **Paper 26.1**, **Java 25**, Gradle, Hangar-ready.

## Repository

https://github.com/1fairyfox/papermc-despawned-items (moved from `PopupMC/DespawnedItems`).

## Developer

Built by Fairy Fox (github.com/1fairyfox). Licensed Apache 2.0.

## History

Created 2021 as a Java 11 / Maven plugin for Minecraft 1.16.5 (an unfinished "90% done"
rewrite). Rewritten to Kotlin/Gradle for Paper 26.1 in July 2026 as part of fairyfox
mesh onboarding — see `notes/decisions/architecture.md` and the onboarding report in
`notes/fairyfox-reports/`.
