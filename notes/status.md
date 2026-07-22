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

**Ship contract (since 2026-07-21, enforced in CLAUDE.md):** a "ship"/release, by
default and every time, also (a) drives OpenSSF Scorecard toward its max / never below
the **≥ 7.0** floor, (b) removes tech debt (no stale dep PRs, no deprecation warnings,
Quality Bar intact), and (c) triages + handles every open PR (merge or close-with-reason).
**Phase by default:** any owner/fairyfox request — or a self-set task — is broken into as
many phases as needed. Worked example: `notes/plans/mandate-2026-07-21-ship-contract.md`.

**Full-CI gate (owner 2026-07-21) — now platform-enforced.** No merge to `main` until the
**entire** CI suite is green on the release PR. As of 2026-07-22 `main`'s **required status
checks** are: `build`, `Analyze (java-kotlin)`, `Server smoke (Paper 1.21.11)`, `Server
smoke (Paper 26.1.2)`, `In-game client smoke (Mineflayer)`, with strict up-to-date — so
GitHub itself blocks a merge unless all of them pass. A green local `./gradlew build` is
necessary but not sufficient (the v1.4.4 PR proved it: local build green, but server-smoke
caught a runtime `sqlite-jdbc` version absent from Paper's library-loader mirror).

**OpenSSF Scorecard: 7.6** (read live 2026-07-22, up from 7.1 → 6.9). Comfortably above the
≥ 7.0 floor. Remaining headroom: more releases for Signed-Releases;
Code-Review/Contributors/CII/Fuzzing are solo-capped. **Open PR backlog: empty** (checked
2026-07-22) — nothing to triage before shipping.

## In flight — v1.5.0 (2026-07-22, on `dev`, not yet released)

Owner mandate of 2026-07-22 (two messages). Ledger, clause by clause:
[`plans/mandate-2026-07-22-screenshots-throttling-publishing.md`](plans/mandate-2026-07-22-screenshots-throttling-publishing.md).
Platform survey answer: [`plans/platform-targets.md`](plans/platform-targets.md).

Landed on `dev` (both commits green through the full CI suite unless noted):

- **Per-user throttling** (`throttle:`, off by default) — `rate` / `concurrent` /
  `fair-share` / `combined`, allowances from `despi.throttle.*` permissions.
- **Void chance + catch-all** (`void:`, inert by default) — probabilistic voiding rolled once
  at enqueue, admin-extensible banned materials, one or more catch-all containers.
- **Per-target settings by command** — `/despi target info|enable|disable|toggle|priority
  <1-10>|contraband accept|refuse` on the block you're looking at. Disabling keeps the
  registration; the pipeline just skips it.
- **Optional client-mod protocol** — handshake (`HELLO` → `WELCOME`/`UNAVAILABLE`) plus six
  verbs on `papermc-despawned-items:targets`. **Server owners can switch it off entirely**
  (`targets.client-mod.enabled`), and `despi.client` gates it per rank. The client is never
  trusted: permission, reach, ownership and limits are all re-checked server-side.
  **The client mod itself is not written** — this is only the server half.
- **No wand, no fake chest menu.** An earlier pass added both; the owner rejected the
  approach (an item pretending to be a tool is less predictable than a command) and they are
  deleted. Recorded in `decisions/rejected.md` territory — see the changelog entry.
- **Automated release screenshots — harness done, output NOT yet usable.**
  `scripts/screenshots.mjs` + a reusable `screenshots.yml` called by CI (build artifacts) and
  Docs (gh-pages gallery at `/screenshots.html`). Two backends: `viewer` (headless Chrome)
  and `client` (real MC under Xvfb — the only one that can photograph particles). It boots
  the server, drives all eight scenes and writes eight PNGs — **but every frame is bare
  sky**, so the job deliberately fails itself via a blank-frame guard rather than publish
  empty art. Rule-out table + next probes: the mandate ledger's not-done list. **The
  screenshots job is non-blocking and not a required check, so this does not gate anything.**
- **Artifact bundle + multi-platform publishing** in `release.yml` (Modrinth, CurseForge,
  Hangar — each gated on its token secret, so inert until the owner creates the projects).
- **Purpur** added to the `server-smoke` matrix so that platform claim is proven, not assumed.
- **README rewritten completely** (problem-first, admin reasons, player reasons, honest
  platform matrix).

**Not yet done for v1.5.0:** the release itself (PR into `main`, full CI, tag, back-merge);
Folia support (planned, needs a `PlatformScheduler` + a shared-state audit that the new
throttle maps enlarged); any Group-C platform edition (Fabric/NeoForge/Sponge/Velocity).

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
