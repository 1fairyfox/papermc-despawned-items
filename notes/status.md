# Project Status

_Current state only._ History → [`sessions/`](sessions/README.md); changelog →
[`version.md`](version.md).

**Version:** `1.4.3` (source of truth: repo-root `VERSION`). Released: v1.1.0 → v1.2.0
(Brigadier commands) → v1.3.0 (naming + quality gates) → v1.3.1 (package →
`io.fairyfox.papermc.despawneditems`) → v1.3.2 (docs site wears the shared fairyfox chrome,
bundle v2.2.1) → v1.3.3 (full standards audit: nav/subnav corrected, Downloads page,
mandatory branch protection + PR releases, provenance attestation) → v1.3.4–1.3.6
(docs-site fixes: rendered notes/README pages, self-hosted legal pages + Legal subnav) →
v1.3.7 (badge wall + supply-chain/quality tooling parity with `random-ai-prompt`) →
**v1.4.0** (full-layer test suite 44%→~95% Kover-gated ≥90, 3 bug fixes found by it,
`/despi recycle` + renameable commands, CodeQL restored on Kotlin 2.4.0) →
**v1.4.1** (server + client layers automated in CI: Testcontainers MariaDB, headless
Paper 1.21.11 + 26.1.2 smoke, Mineflayer in-game acceptance; Sonar CI scan wired) →
**v1.4.2** (123-node permission matrix, combined load + throughput proofs,
Kotest/Pitest/JMH adopted, spark+JFR profiling) → **v1.4.3** (docs/process: the
"Owner Mandates Become Ledgers" standing instruction + mandate-execution failure analysis).
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
- ~~Headless runtime smoke + forward-compat + in-game test~~ ✅ automated in CI (v1.4.1).
- ~~Account-gated badges~~ ✅ owner uploaded `CODECOV_TOKEN` / `SONAR_TOKEN` /
  `SCORECARD_TOKEN` (2026-07-21); Codecov + Sonar scans wired in `ci.yml`, CodeFactor
  active (it checks PRs). Remaining owner-side: if the Sonar CI scan reports an
  Automatic-Analysis conflict, disable Automatic Analysis on the SonarCloud project
  (Administration → Analysis Method) — mutually exclusive with CI scans.
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
| Runtime load on real Paper 1.21.11 | ✅ **automated in CI** (`server-smoke` job, every push/PR) — was a manual headless smoke |
| MySQL/MariaDB backend | ✅ real-server integration via Testcontainers in CI (local Windows blocked by a TC↔Docker-29.5 incompat — documented) |
| In-game client acceptance | ✅ **automated in CI** — Mineflayer bot joins Paper 1.21.11, runs `/recycle` + `/despi`, asserts replies (validated locally too) |
| Static analysis (Ktlint + Detekt) + coverage (Kover) | ✅ gate the build; all detekt rules on, no baseline |
| Forward-compat load on Paper 26.1.2 | ✅ **automated in CI** (`server-smoke` matrix, Java 25) |
| GitHub Pages docs | ⏳ enabled; deploys on release to `main` |
| Standards adopted (project side) | per-standard state: [`reference/adoption-manifest.md`](reference/adoption-manifest.md) — 17 implemented · 6 copied-only (due next adopt pass) · 1 gap · 4 N/A (no bare ✅ — see the checklist-noncompliance report) |
| Hub registration | ❌ not yet (hub-side) |
