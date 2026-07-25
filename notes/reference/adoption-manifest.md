# Adoption manifest — per-standard state (the evidence behind any "adopted" claim)

One row per hub standard. **Rules:** `copied-only` is NOT adopted — only a recorded
Verify pass flips a row to `implemented`. No summary claim (status.md, registry,
process reports) is permitted without a row here backing it. A `gap`/`partial` row carries a due
marker; an overdue gap on a mandatory standard (git-workflow, supply-chain-hardening) holds
releases. Proposed as mesh standard in
`fairyfox-reports/2026-07-20-checklist-noncompliance-failure-analysis.md` (S1).

Hub baseline: **1.5.1 / commit a6d7e68** (adopted 2026-07-24, up from 0.20.2 / 697bc5c).
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
| dependencies | implemented | 2026-07-24 | Dependabot gradle + github-actions → dev; local gate; open-PR backlog empty |
| deployment | copied-only | — | Docs deploy = Pages on release; formal Verify vs deployment.md still light — kept `copied-only` pending an itemized pass |
| docker | **implemented** | 2026-07-24 | NEW standard. Vendored Dockerfile/compose.yaml/.dockerignore; full `./gradlew build` green in-container (12m33s); `MariaDbStorageTest` runs 3/3 over mounted socket (prior write-off fixed); CLAUDE.md local-first rule |
| docs-lifecycle | implemented | 2026-07-24 | Current-state docs swept today; dated history unedited; single-source |
| docs-site (13 modules) | **implemented** | 2026-07-24 | **Chrome bundle 2.3.0 adopted** (was 2.2.1): the only 2.3.0 delta was **self-hosted fonts** — vendored fraunces/inter/jetbrains-mono-latin.woff2 + fonts.css into docs-theme/chrome (relative URLs for the Pages subpath), swapped all 3 head sites (`_shell.html`, `page_metadata.ftl`, `head.html`) off Google Fonts, wired both build.gradle.kts Copy tasks, `docs-theme/chrome/VERSION`→2.3.0. **`assembleDocsSite` builds green (5m46s in-container)**; built output self-hosts fonts, zero real googleapis hot-links (remaining string hits are standards prose in `<code>`). 1.4.0 web-interface rules already met (subnav three-zone shape, on-site Notes/Systems/Reference, coins.js loaded). Open (pre-existing, recommended): #26 breadcrumb, #46 live-deploy sign-off |
| engineering-quality | **implemented** | 2026-07-24 | 0 TODO/FIXME across 87 .kt files; no detekt baseline; ship contract: Scorecard 7.6 (2026-07-22), debt clear, PR backlog empty. Was `copied-only` |
| farm-operating-model | N/A | — | Story/game farms only; this is a server plugin |
| git-workflow | implemented | 2026-07-24 | main protected, PR path, `--no-ff` tagged; newest tag v1.4.8; dev clean+pushed; history intact |
| legal-docs | **implemented** | 2026-07-24 | NEW reference copy. Self-hosted privacy/terms/cookies/index under docs-theme/…/legal/; accurate to code (no data-practice change since 2026-07-21); footer legal column intact |
| maintenance-sweep | copied-only | — | First whole-repo sweep **still never run** (pre-existing); due within a month of active work — recommend next |
| mandate-ledger | **implemented** | 2026-07-24 | NEW standard. CLAUDE.md "Owner Mandates Become Ledgers"; owner directive + GitHub-details exception transcribed this session |
| new-project-setup | N/A | — | One-time runbook; project predates mesh |
| notes-system | implemented | 2026-07-24 | Full tree; status current; inline changelog; session logs; reference copy refreshed |
| onboarding-existing-project | N/A | — | Completed 2026-07-19; hub-registration + GitHub-details are hub-side (details exception recorded above) |
| planning | implemented | 2026-07-24 | Phased task list + runbook-as-plan; CLAUDE.md phase-by-default; reference copy refreshed |
| process-reports | implemented | 2026-07-24 | 2 reports filed this date; template followed; hub_version anchor advanced to 1.5.1 |
| repo-hygiene | implemented | 2026-07-24 | check-links green (37 OK) incl. new files; tree clean post-commit; auto-delete on; dev+main protected |
| research-capture | **implemented** | 2026-07-24 | Findings in notes same session; load-bearing Docker claim probed (real build + DB test), not assumed. Was `copied-only` |
| self-hosted-assets | implemented | 2026-07-24 | **Fonts now fully self-hosted** (chrome 2.3.0) — Google Fonts hot-links removed from all docs head templates; built site has zero `fonts.g*` requests. Prior "Google-hosted, hub exception" note retired. Fox icon vendored |
| supply-chain-hardening | implemented | 2026-07-24 | SHA-pins (0 unpinned); SECURITY.md; provenance-as-`.intoto.jsonl` asset; main contexts = full CI suite; SAST pinned. §1: one **deliberate documented divergence** — scorecard.yml keeps top-level `read-all` (write-free, vendor-canonical; tightening would starve the scanner's reads); documented in-file + here |
| testing | implemented | 2026-07-24 | ~200 tests/layer; Kover min-90 gates build; regression-per-fix; suite green in-container; probe-the-mock lore |
| versioning | implemented | 2026-07-24 | VERSION 1.5.0 (one SemVer line) ahead of newest main tag v1.4.8 — expected mid-flight on dev; nothing hardcoded |
| working-rhythm | **implemented** | 2026-07-24 | Task-tracked phases; long build backgrounded (dockerd-owned) + surfaced; ask-first honoured (GitHub-details constraint). Was `copied-only` |
