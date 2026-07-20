# Project Status

_Current state only._ History → [`sessions/`](sessions/README.md); changelog →
[`version.md`](version.md).

**Version:** `1.0.0` (source of truth: repo-root `VERSION`).

## Current state (read this first)

**Major refactor in flight** (2026-07-20) — see `notes/plans/refactor-2026-07.md`
for the full plan and phase breakdown. Goal: modernise the code (not just the API),
split big files, add a full test suite, and make it scale to large servers.

**Retargeted Paper 26.1 / Java 25 → Paper 1.21.x (built on 1.21.11) / Java 21** so
the MockBukkit test framework (1.21.x only, not 26.x yet) is available and the plugin
covers the largest install base while still loading on 26.1 (forward-compat, to
verify). `settings.gradle.kts` now auto-provisions JDK 21 via the foojay resolver.

- **Builds green** (`./gradlew build`) against the real Paper 1.21.11 API →
  `build/libs/DespawnedItems-1.0.0.jar` (Kotlin stdlib shaded in). The existing code
  compiled against 1.21 with zero API changes needed.
- Standards adopted: mesh-aware `CLAUDE.md`, `notes/` system, `VERSION`, LF
  `.gitattributes`, security policy + Dependabot + branch-sync/CI/release workflows,
  reconciled README with badges.

## Next

- Execute the refactor phases in `notes/plans/refactor-2026-07.md`: (1) core refactor
  + correctness + scale, (2) test suite + CI, (3) config/commands/permissions/limits,
  (4) docs/Dokka/README.
- Re-run the headless runtime smoke on a 1.21.11 server, and confirm forward-compat
  load on a 26.1 server.

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
| Build (Gradle/Kotlin/Paper 1.21.11, Java 21) | ✅ green |
| Runtime load on Paper 1.21.11 | ⏳ re-verify after retarget |
| Forward-compat load on Paper 26.1 | ⏳ to verify |
| In-game gameplay test | ✅ now unblocked (Mineflayer supports 1.21.11) |
| Refactor (plan: refactor-2026-07.md) | 🔟 in progress — Phase 1 |
| Test suite | ❌ not yet (Phase 2) |
| Standards adopted (project side) | ✅ |
| Themed docs site (live) | ❌ not yet published |
| Hub registration | ❌ not yet (hub-side) |
