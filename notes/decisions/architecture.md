# Architecture Decisions

Key structural choices and why. Newest on top.

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
