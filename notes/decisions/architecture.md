# Architecture Decisions

Key structural choices and why. Newest on top.

### 2026-07-20 — Pluggable storage backends (YAML default, SQLite, MySQL/MariaDB)

Introduced a `LocationRepository` interface with three backends so the plugin meets
large-server expectations: **YAML** flat files (zero-config default, backward-compatible
with existing `userdata/<uuid>.yml`), **SQLite** (embedded, no external server — good
for large single servers), and **MySQL/MariaDB** (shared storage across a network of
servers). SQLite/MySQL go through one dialect-agnostic `JdbcLocationRepository` over a
HikariCP pool. Rationale: rewriting every player file on every change (the old model)
doesn't scale; a DB gives indexed, transactional, incremental, network-shareable
storage. JDBC drivers + HikariCP are loaded at runtime via Paper's `libraries:` loader
rather than shaded, keeping the jar small. Switching backends auto-migrates existing
data. Trade-off: more config surface and an optional DB dependency.

### 2026-07-20 — Retarget Paper 26.1 → 1.21.x (Java 21)

Moved the build target from Paper 26.1 / Java 25 down to the Paper **1.21.x** line
(built against 1.21.11) on **Java 21**. Rationale: the MockBukkit test framework
supports 1.21.x but not the newer 26.x line, so 1.21.x unlocks full integration/e2e
testing (a hard requirement for this refactor); 1.21.11 is also the single most-installed
version and a 1.21-built plugin still loads on 26.1 via Paper forward-compat. Supersedes
the 2026-07-19 "targeting Paper 26.1" decision below. Full rationale + data:
`../plans/refactor-2026-07.md`.

### 2026-07-19 — Rewrite to Kotlin/Gradle targeting Paper 26.1

Chose to modernise the plugin as a **Kotlin rewrite on Gradle** (rather than a
minimal Java-on-Maven version bump) targeting **Paper 26.1 / Java 25**. Rationale:
the 2021 code targeted Paper 1.16.5 / Java 11 and can't load on a modern server, so
substantial API migration was unavoidable either way; Gradle (Kotlin DSL) + Hangar
publishing is the current Paper-community norm, and Kotlin is well-supported on
Hangar. Trade-off: the plugin now ships the Kotlin stdlib (shaded into the jar), and
Kotlin's handling of Paper's overloaded setters requires explicit `setX(...)` calls.

### 2026-07-19 — Sound/particle config as string keys, not enums

Switched `config.yml` to store a Minecraft **sound key** (resolved via the
`World.playSound(Location, String, …)` overload) and a **particle key**, instead of
the old `org.bukkit.Effect`/`Particle` enum names. Rationale: modern Paper moved these
toward registry-backed types, so string keys are stable across versions and avoid the
enum→registry churn. Trade-off: a one-time config-schema change from the 1.16 version.

### 2026-07-19 — Config holder exposed as `plugin.settings`

Named the plugin's own configuration holder `plugin.settings` rather than
`plugin.config`, because a Kotlin `config` property collides with
`JavaPlugin.getConfig()` (Bukkit's `FileConfiguration`).
