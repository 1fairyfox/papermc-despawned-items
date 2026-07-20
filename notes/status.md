# Project Status

_Current state only._ History → [`sessions/`](sessions/README.md); changelog →
[`version.md`](version.md).

**Version:** `1.0.0` (source of truth: repo-root `VERSION`).

## Current state (read this first)

**Major refactor largely complete** (2026-07-20) on `dev` — see
`notes/plans/refactor-2026-07.md` (plan) and `notes/version/2026-07.md` (per-commit log).
The plugin was modernised beyond the API: new indexed data model, pluggable storage, a
throttled pipeline, permission-based limits, and a real test suite.

Done on `dev` (all green, CI passing):
- **Retargeted** Paper 26.1/Java 25 → **Paper 1.21.11 / Java 21** (foojay auto-provisions
  the JDK); existing code compiled with zero API changes.
- **Correctness fixes:** `/recycle` rewards (now PDC), particle-data crashes, stale static
  strategy list, `RemoveMaterials` IOOBE, `/despi reload` now re-reads config.
- **Scale:** indexed `LocationStore` (O(1) lookups), incremental off-thread persistence,
  throttled `DespawnScheduler`.
- **Storage backends:** YAML (default) + SQLite + MySQL/MariaDB (HikariCP, runtime
  `libraries:`), with YAML→DB migration.
- **Limits:** per-user caps via `despi.limit.<n>` group permissions + bypass.
- **Tests:** JUnit 5 + MockBukkit suite (unit, property/fuzz, database, performance,
  mocked-server integration) — ~49 tests, gating `build`; CI green on `dev`.
- **Docs/branding:** display name → "PaperMC Despawned Items", rewritten README, filled
  context notes, GitHub Pages Dokka workflow (deploys on release to `main`).

## Next

- Consider a Brigadier command rewrite (optional polish; current commands work + tested).
- Headless runtime smoke on a real 1.21.11 server; confirm forward-compat load on 26.1.
- Cut a tagged **MINOR** release (`1.1.0`) `dev → main` once smoke-verified — this
  triggers the Pages docs deploy and (when wired) Hangar publish.
- Hub registration (hub-side) and Hangar project + `HANGAR_API_TOKEN` secret.

## Health

| Area | Status |
|------|--------|
| Build (Gradle/Kotlin/Paper 1.21.11, Java 21) | ✅ green |
| Test suite (JUnit 5 + MockBukkit) | ✅ ~49 tests, gating build |
| CI on `dev` | ✅ passing |
| Refactor (plan: refactor-2026-07.md) | ✅ Phases 1–4 largely done |
| Runtime load on real Paper 1.21.11 | ⏳ re-verify (headless smoke) |
| Forward-compat load on Paper 26.1 | ⏳ to verify |
| In-game gameplay test | ✅ unblocked (Mineflayer supports 1.21.11) |
| GitHub Pages docs | ⏳ enabled; deploys on release to `main` |
| Standards adopted (project side) | ✅ |
| Hub registration | ❌ not yet (hub-side) |
