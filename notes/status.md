# Project Status

_Current state only._ History → [`sessions/`](sessions/README.md); changelog →
[`version.md`](version.md).

**Version:** `1.0.0` (source of truth: repo-root `VERSION`).

## Current state (read this first)

Onboarded into the fairyfox mesh **and** modernised from the original 2021
Java/Maven plugin to a **Kotlin / Gradle** plugin targeting **Paper 26.1** on
**Java 25**. The full plugin (all `/despi` subcommands, `/recycle`, and the five-stage
despawn pipeline) was ported to idiomatic Kotlin.

- **Builds green** (`./gradlew build`) against the real Paper 26.1.2 API →
  `build/libs/DespawnedItems-1.0.0.jar` (Kotlin stdlib shaded in).
- **Runtime-verified**: loads and enables cleanly on a headless Paper 26.1.2 / Java 25
  server (`DespawnedItems is enabled`, no plugin stack traces).
- Standards adopted: mesh-aware `CLAUDE.md`, `notes/` system, `VERSION`, LF
  `.gitattributes`, security policy + Dependabot + branch-sync/CI/release workflows,
  reconciled README with badges.

## Next

- **Docs site**: theme the Dokka output with the shared fairyfox chrome and publish at
  `fairyfox.io/papermc-despawned-items/` (hub-side Pages step).
- **Hub registration**: add the row to the hub's `registry.yml` + `_data/projects.yml`
  (a hub-side change — this repo does not write to the hub).
- **Hangar publish**: create the Hangar project + `HANGAR_API_TOKEN` secret, then wire
  the commented publish step in `.github/workflows/release.yml`.
- Cut the tagged `v1.0.0` release to `main` once the above are confirmed.

## Health

| Area | Status |
|------|--------|
| Build (Gradle/Kotlin/Paper 26.1) | ✅ green |
| Runtime load on Paper 26.1.2 | ✅ enables cleanly |
| In-game gameplay test | ⏳ blocked — no 26.x client bot yet (node-minecraft-protocol ≤ 1.21.11) |
| Standards adopted (project side) | ✅ |
| Themed docs site (live) | ❌ not yet published |
| Hub registration | ❌ not yet (hub-side) |
