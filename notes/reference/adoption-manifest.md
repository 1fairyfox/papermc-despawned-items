# Adoption manifest — per-standard state (the evidence behind any "adopted" claim)

One row per hub standard. **Rules:** `copied-only` is NOT adopted — only a recorded
Verify pass flips a row to `implemented`. No summary claim (status.md, registry,
process reports) is permitted without a row here backing it. A `gap`/`partial` row carries a due
marker; an overdue gap on a mandatory standard (git-workflow, supply-chain-hardening) holds
releases. Proposed as mesh standard in
`fairyfox-reports/2026-07-20-checklist-noncompliance-failure-analysis.md` (S1).

Hub baseline: **1.6.0 / commit 8c6a50e** (readme standard adopted 2026-08-02, up from 1.5.1 / a6d7e68).
Full Verify pass: `fairyfox-reports/2026-07-24-compliance-audit.md`. Chrome bundle: **2.3.0**
(self-hosted fonts; matches hub).

## Recorded user exceptions

| Date | Exception | Scope | Recorded because |
|------|-----------|-------|------------------|
| 2026-07-24 | **Do not edit the GitHub repo description or topics/tags.** | onboarding "project details complete by default" / hub-registration, GitHub-repo-metadata only | Owner instruction, mid-turn. The badges 20-set, `_data/projects.yml` blurb, and every other detail rule are unaffected; only editing the GitHub repo's own description/topics is out of scope until the owner lifts it. |

## Per-standard state

| Standard | State | Verify last run | Evidence / notes |
|----------|-------|-----------------|------------------|
| adopting-updates | implemented | 2026-07-24 | Flow run again this date (report on file); reference copy refreshed to 1.5.1 |
| agent-tooling | implemented | 2026-07-24 | PowerShell+file-tools used throughout; `.gitattributes` LF-normalized; no CRLF noise in diff |
| ai-context | implemented | 2026-07-24 | CLAUDE.md carries required pieces + mesh block + new Docker Build/Run rule |
| badges | **implemented** | 2026-07-24 | **Full 20-badge set in canonical order** verified in README (was `copied-only`); runtime Java/Paper before CI; distribution badges commented; docs badge → `fairyfox.io/<key>/`; no omission → no exception needed |
| checklists-are-contracts | **implemented** | 2026-07-24 | NEW standard. Behaviour already in CLAUDE.md ("Checklists Are Contracts"); reference copy vendored; per-item recording + not-done disclosure practised in the 2026-07-24 audit |
| ci-secrets | **implemented** | 2026-07-24 | NEW standard. `gh secret list`: SONAR/CODECOV/SCORECARD all set; names canonical; each referenced by a workflow |
| coins | implemented | 2026-07-24 | Counter from shared chrome; nothing gated. 2.3.0 counter refresh rides the docs-site row |
| compliance | implemented | 2026-07-24 | Full matrix pass = `fairyfox-reports/2026-07-24-compliance-audit.md`; reference copy refreshed |
| cross-project-sync | implemented | 2026-07-24 | Read-only git-ignored mirror; on-request; ledger read from clone; anti-recursion held |
| dependencies | implemented | 2026-08-02 | Dependabot gradle + github-actions → dev; local gate. Backlog of 9 triaged this date: 6 green bumps merged, #33 (paper-api 26.x) + #34 (ktlint 14, build-red) + #32 (Kotlin 2.4.10 vs CodeQL pin) closed-with-reason; `dependabot.yml` now ignores paper-api major/minor + `org.jetbrains.kotlin*` so both landmines can't recur |
| deployment | copied-only | — | Docs deploy = Pages on release; formal Verify vs deployment.md still light — kept `copied-only` pending an itemized pass |
| docker | **implemented** | 2026-08-02 | Vendored Dockerfile/compose.yaml/.dockerignore; full `./gradlew build` green in-container (12m33s, 2026-07-24); `MariaDbStorageTest` 3/3 over mounted socket. 2026-08-02: base image **SHA-pinned** (`eclipse-temurin:21-jdk@sha256:da9d3a4f…`), verified by `docker build` + digest match; CLAUDE.md local-first rule |
| docs-lifecycle | implemented | 2026-07-24 | Current-state docs swept today; dated history unedited; single-source |
| docs-site (13 modules) | **implemented** | 2026-07-24 | **Chrome bundle 2.3.0 adopted** (was 2.2.1): the only 2.3.0 delta was **self-hosted fonts** — vendored fraunces/inter/jetbrains-mono-latin.woff2 + fonts.css into docs-theme/chrome (relative URLs for the Pages subpath), swapped all 3 head sites (`_shell.html`, `page_metadata.ftl`, `head.html`) off Google Fonts, wired both build.gradle.kts Copy tasks, `docs-theme/chrome/VERSION`→2.3.0. **`assembleDocsSite` builds green (5m46s in-container)**; built output self-hosts fonts, zero real googleapis hot-links (remaining string hits are standards prose in `<code>`). 1.4.0 web-interface rules already met (subnav three-zone shape, on-site Notes/Systems/Reference, coins.js loaded). Open (pre-existing, recommended): #26 breadcrumb, #46 live-deploy sign-off |
| engineering-quality | **implemented** | 2026-08-02 | 0 TODO/FIXME; no detekt baseline; ship contract re-verified this date: Scorecard **7.9** (live), debt clear, **PR backlog now empty** (9 triaged — the v1.5.1 ship had missed this; v1.5.2 closes it and adds the pre-ship triage discipline) |
| farm-operating-model | N/A | — | Story/game farms only; this is a server plugin |
| git-workflow | implemented | 2026-07-24 | main protected, PR path, `--no-ff` tagged; newest tag v1.4.8; dev clean+pushed; history intact |
| legal-docs | **implemented** | 2026-07-24 | NEW reference copy. Self-hosted privacy/terms/cookies/index under docs-theme/…/legal/; accurate to code (no data-practice change since 2026-07-21); footer legal column intact |
| maintenance-sweep | **implemented** | 2026-07-24 | **First whole-repo sweep run** this date: working tree clean; `dev` contains `main` (ancestor check) + green (in-container build + docs build + check-links); only `dev`/`main` branches (no stale); one docs-currency fix surfaced + applied (status.md `Version:` header 1.4.3 → 1.5.0/v1.4.8). No destructive git action needed. Recorded in `sessions/2026-07/2026-07-24.md` |
| mandate-ledger | **implemented** | 2026-07-24 | NEW standard. CLAUDE.md "Owner Mandates Become Ledgers"; owner directive + GitHub-details exception transcribed this session |
| new-project-setup | N/A | — | One-time runbook; project predates mesh |
| notes-system | implemented | 2026-07-24 | Full tree; status current; inline changelog; session logs; reference copy refreshed |
| onboarding-existing-project | N/A | — | Completed 2026-07-19; hub-registration + GitHub-details are hub-side (details exception recorded above) |
| planning | implemented | 2026-07-24 | Phased task list + runbook-as-plan; CLAUDE.md phase-by-default; reference copy refreshed |
| process-reports | implemented | 2026-08-02 | Reports filed 2026-07-24 (×2) + 2026-08-02 (adopting-updates + compliance-audit); template followed; hub_version anchor advanced to 1.6.0 |
| readme | **implemented** | 2026-08-02 | NEW standard (hub 1.6.0). Added worded **docs link** (`fairyfox.io/papermc-despawned-items/`) near top, an organized **"Get it"** section (Documentation · Download/Releases · Source; Modrinth/Hangar/CurseForge noted as pending — projects not yet created, so legitimately absent per "nothing to link yet"), and a **mesh footer** near the bottom. `## Verify` run: docs link ✅, Get it grouped+labelled ✅, live-app N/A (server plugin; no deployed web app — docs Pages is the docs link), publish targets none-live-yet ✅, mesh footer ✅, no dropped destination → no exception needed. `check-links` green (40 files) |
| repo-hygiene | implemented | 2026-08-02 | check-links green (42 OK); tree clean post-commit; auto-delete on; dev+main protected; stale `release/1.5.0` confirmed removed from origin |
| research-capture | **implemented** | 2026-07-24 | Findings in notes same session; load-bearing Docker claim probed (real build + DB test), not assumed. Was `copied-only` |
| self-hosted-assets | implemented | 2026-07-24 | **Fonts now fully self-hosted** (chrome 2.3.0) — Google Fonts hot-links removed from all docs head templates; built site has zero `fonts.g*` requests. Prior "Google-hosted, hub exception" note retired. Fox icon vendored |
| supply-chain-hardening | implemented | 2026-08-02 | SHA-pins: all Actions + **Dockerfile base image now digest-pinned** (Scorecard Pinned-Dependencies 9→10); SECURITY.md; provenance-as-`.intoto.jsonl` asset; **main required checks = all 7 CI jobs** (added Purpur + latest-Paper smoke this date — full-CI-gate now platform-enforced); SAST pinned. §1: one **deliberate documented divergence** — scorecard.yml keeps top-level `read-all` (write-free, vendor-canonical); documented in-file + here |
| testing | implemented | 2026-07-24 | ~200 tests/layer; Kover min-90 gates build; regression-per-fix; suite green in-container; probe-the-mock lore |
| versioning | implemented | 2026-07-24 | VERSION 1.5.0 (one SemVer line) ahead of newest main tag v1.4.8 — expected mid-flight on dev; nothing hardcoded |
| working-rhythm | **implemented** | 2026-07-24 | Task-tracked phases; long build backgrounded (dockerd-owned) + surfaced; ask-first honoured (GitHub-details constraint). Was `copied-only` |
