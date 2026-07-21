# Project Status

_Current state only._ History → [`sessions/`](sessions/README.md); changelog →
[`version.md`](version.md).

**Version:** `1.4.0` (source of truth: repo-root `VERSION`). Released: v1.1.0 → v1.2.0
(Brigadier commands) → v1.3.0 (naming + quality gates) → v1.3.1 (package →
`io.fairyfox.papermc.despawneditems`) → v1.3.2 (docs site wears the shared fairyfox chrome,
bundle v2.2.1) → v1.3.3 (full standards audit: nav/subnav corrected, Downloads page,
mandatory branch protection + PR releases, provenance attestation) → v1.3.4–1.3.6
(docs-site fixes: rendered notes/README pages, self-hosted legal pages + Legal subnav) →
v1.3.7 (badge wall + supply-chain/quality tooling parity with `random-ai-prompt`) →
**v1.4.0** (full-layer test suite 44%→~95% Kover-gated ≥90, 3 bug fixes found by it,
`/despi recycle` + renameable commands, CodeQL restored on Kotlin 2.4.0).
Artifact/plugin-id/data-folder are all `papermc-despawned-items`.

**Release path (since 2026-07-20):** `main` is branch-protected — releases go through a
**PR** (`gh pr create --base main` → checks green → `gh pr merge --merge` → back-merge
`git merge --ff-only main` on dev). Direct `dev → main` pushes no longer work. Tags stay
by-hand (CI does not own tagging). Full audit: `notes/plans/standards-audit-2026-07-20.md`.

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
- ~~Release v1.3.3~~ ✅ shipped 2026-07-21 (UTC) — first release through the protected
  PR path (PR #9 → `--merge` → hand-tag → back-merge, `dev == main`); jar attached with
  build-provenance attestation; Pages docs deployed with the corrected chrome.
- **Light up the account-gated badges** (v1.3.7 added the markup + workflows; these are the
  external steps): enable the **Codecov** GitHub app + add `CODECOV_TOKEN`; import the
  project on **SonarQube Cloud** under org `1fairyfox`, confirm its real `projectKey`, add
  `SONAR_TOKEN` (or just enable Automatic Analysis); add the repo on **CodeFactor**; optional
  `SCORECARD_TOKEN` PAT for the Scorecard Branch-Protection check.
- Hub registration (hub-side; incl. registry `docs:`/`repo:` check — audit item #23) and
  Hangar project + `HANGAR_API_TOKEN` secret — then uncomment the Hangar/Modrinth usage
  badges in README.md and wire the release-publish workflow (hangar-publish-plugin / mc-publish).
- Follow-ups from the standards audit: themed Changelog/Tutorials pages, vendored fox
  icon (see `plans/future.md`).

## Health

| Area | Status |
|------|--------|
| Build (Gradle/Kotlin 2.4.0/Paper 1.21.11, Java 21) | ✅ green — Kotlin pinned to CodeQL's supported max (bump only together) |
| Test suite (JUnit 5 + MockBukkit) | ✅ ~200 tests across every layer, gating build |
| Coverage | ✅ ~95.6% line / ~78.6% branch — **`koverVerify` min 90 gates the build** |
| SAST (CodeQL, java-kotlin traced compile) | ✅ restored — dev runs informational, release-PR run gates |
| CI on `dev` | ✅ passing |
| Refactor (plan: refactor-2026-07.md) | ✅ Phases 1–4 largely done |
| Runtime load on real Paper 1.21.11 | ✅ verified — headless smoke, `libraries:` (HikariCP + JDBC) auto-loaded, enables cleanly, no errors |
| Static analysis (Ktlint + Detekt) + coverage (Kover) | ✅ gate the build; all detekt rules on, no baseline |
| Forward-compat load on Paper 26.1.2 | ✅ verified — headless smoke, enabled cleanly (`Done (10.3s)`), no exceptions despite 26.x registry changes |
| In-game gameplay test | ✅ unblocked (Mineflayer supports 1.21.11) |
| GitHub Pages docs | ⏳ enabled; deploys on release to `main` |
| Standards adopted (project side) | per-standard state: [`reference/adoption-manifest.md`](reference/adoption-manifest.md) — 17 implemented · 6 copied-only (due next adopt pass) · 1 gap · 4 N/A (no bare ✅ — see the checklist-noncompliance report) |
| Hub registration | ❌ not yet (hub-side) |
