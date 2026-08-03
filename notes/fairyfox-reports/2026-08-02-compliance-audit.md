---
date: 2026-08-02
procedure: compliance-audit
mode: full
node: papermc-despawned-items
outcome: completed-with-gaps
hub_version: 1.6.0
target_commit: 62cbd59 (main/dev, v1.5.1)
---

# Process Report — full compliance audit, 2026-08-02

> Full mode: every standard's `## Verify` run against the current v1.5.1 state, with live
> evidence. Report-first — nothing fixed yet; fixes await go-ahead. Standard:
> `notes/reference/compliance.md`.

## Outcome in one line

Overall **compliant with five open gaps** — none structural, but two are ship-contract/
supply-chain items (unhandled Dependabot backlog; Dockerfile base image unpinned) and one is
a self-inflicted dating error in *this session's* own artifacts. Scorecard **7.9** (live).

## Evidence base

- **Full CI just passed on the exact HEAD.** PR #26 (v1.5.1) went green across `build`,
  `Analyze (java-kotlin)`, CodeQL, Sonar, Codecov, and every smoke job (Paper 1.21.11 / 26.1.2 /
  Purpur / latest stable+experimental) + Mineflayer, minutes before this audit — authoritative
  build/test/SAST evidence on `62cbd59`, stronger than a re-run local build.
- `git`: `dev` == `main` == `62cbd59`, tag `v1.5.1`, working tree clean, history intact.
- Live Scorecard read (2026-07-27 scan, commit 62cbd59): **7.9**.
- `gh secret list`, `gh pr list`, branch-protection API, `check-links` (42 OK), source grep.

## The matrix — per standard

| Standard | State | Evidence |
|----------|-------|----------|
| git-workflow | **done** | `main` protected (enforce_admins, PR-required, strict up-to-date, force-push/deletion off); v1.5.1 shipped `--no-ff` PR + hand tag; history intact. *Minor:* required-checks list omits the newer `Purpur`/`latest-paper` smoke jobs (see gap #5). |
| versioning | **done** | `VERSION` = `1.5.1`, single SemVer line = newest `main` tag `v1.5.1`; PATCH for the docs-only change; nothing hardcoded. |
| notes-system | **partial** | Tree intact, status.md refreshed this session, inline changelog present — but this session's entries are **misdated** (gap #1). |
| ai-context | **done** | `CLAUDE.md` carries all required pieces + mesh block; workflow matches git-flow. |
| cross-project-sync | **done** | Read-only git-ignored mirror; on-request; ledger read from clone; anti-recursion held on today's adopt run. |
| process-reports | **partial** | Adopting-updates report filed this session + this audit — but the adopting report is **misdated 2026-07-25** (gap #1). |
| checklists-are-contracts | **done** | This audit records per-row, names gaps, discloses the not-done list; no bare ✅. |
| mandate-ledger | **done** | No open multi-part owner mandate this session; behaviour in CLAUDE.md. |
| docs-site (13 modules) | **done** | Chrome bundle `2.3.0`; self-hosted fonts; on-site notes/reference. No docs-site change this session. |
| deployment | **done** | Docs = Pages on release; matches project kind (server plugin, no web app). |
| planning | **done** | Phased task list used for both the adopt and this audit; CLAUDE phase-by-default. |
| supply-chain-hardening | **partial** | SHA-pins: 46/46 GH + 16/16 third-party actions pinned; SECURITY.md; provenance on last 5 releases; `main` protected; scorecard.yml `read-all` documented divergence. **Gap:** Dockerfile base image not pinned by hash (gap #2). |
| dependencies | **partial** | Dependabot on, grouped, → `dev`; local gate. **Gap:** 9 open Dependabot PRs unhandled (gap #3). |
| docker | **partial** | Vendored Dockerfile/compose; local-first build proven 2026-07-24. **Gap:** base image `eclipse-temurin:21-jdk` unpinned (gap #2, shared with supply-chain). |
| legal-docs | **done** | Self-hosted privacy/terms/cookies; accurate to code (no data-practice change since 2026-07-21). |
| coins | **done** | Counter from shared chrome; nothing gated. |
| agent-tooling | **done** | PowerShell + file tools used throughout; `.gitattributes` `text=auto eol=lf`; no CRLF noise. |
| badges | **done** | 20-set present in order (25 shields lines incl. issue/PR pairs; distribution badges commented pending publish); docs badge → `fairyfox.io/<key>/`. |
| ci-secrets | **done** | `gh secret list`: SONAR/CODECOV/SCORECARD all set; names canonical; each referenced. |
| testing | **done** | ~200 tests/layer, Kover min-90 gates build; full suite green on the v1.5.1 CI run. |
| engineering-quality | **partial** | 0 TODO/FIXME (grep), no detekt baseline, Scorecard 7.9 ≥ 7.0, debt clear. **Gap:** ship contract's "triage every open PR before releasing" not met at the v1.5.1 ship — 9 Dependabot PRs were open (gap #3). |
| repo-hygiene | **partial** | check-links green (42); tree clean; branch auto-delete on. **Gap:** `origin/release/1.5.0` still present (gap #4). |
| docs-lifecycle | **partial** | Current-state docs match reality after this session's status.md fix; dated history unedited — but the new entries carry the wrong date (gap #1). |
| research-capture | **done** | Adopt run's load-bearing scoping recorded; this audit's evidence captured in the report. |
| working-rhythm | **done** | Task-tracked; CI watched by polling (short-timeout shell); ask-first honoured. |
| self-hosted-assets | **done** | Fonts self-hosted (chrome 2.3.0); no CDN hot-links; check confirms. |
| farm-operating-model | **N/A** | Server plugin, not a story/game farm. |
| maintenance-sweep | **done (light)** | This audit doubles as the sweep: branch state checked, `dev` contains `main` + green, docs-currency fix applied (status.md). Surfaced gaps #1–#5 rather than auto-acting. |
| lifecycle runbooks | **done** | Adopt runbook's PR release path was followed exactly today. |
| readme | **done** | Worded docs link (L46), organized Get it section, mesh footer (L281); adopted + Verified today. |

## Gaps (report-first — nothing fixed yet)

**#1 — Dating error in this session's own artifacts (mine to fix).** Today is **2026-08-02**,
but I dated this session's files `2026-07-25` (I anchored off the previous report's 2026-07-24
and incremented by one instead of reading the clock). Affected:
`notes/fairyfox-reports/2026-07-25-adopting-updates.md`, `notes/sessions/2026-07/2026-07-25.md`
(should live under `2026-08/2026-08-02.md`), the changelog entry (should be in
`notes/version/2026-08.md`, not `2026-07.md`), and the `2026-07-25` dates in
`adoption-manifest.md`. Already shipped in v1.5.1, so the correction is a follow-up commit.
`docs-lifecycle`/`notes-system`/`process-reports` all read `partial` solely because of this.

**#2 — Dockerfile base image not pinned by hash.** Scorecard Pinned-Dependencies 9/10:
`eclipse-temurin:21-jdk` in `Dockerfile:12` isn't SHA-pinned. Cheap fix (pin to the digest
Scorecard printed), lifts the check to 10 and closes the one real supply-chain/docker gap.

**#3 — 9 open Dependabot PRs unhandled (ship-contract gap).** PRs #27–#35 (github-actions +
gradle groups) are open. The ship contract says a release triages every open PR (merge or
close-with-reason) and "never release on top of an unhandled PR backlog" — v1.5.1 shipped with
these open. Includes PR #33 (`paper-api → 26.2.build.84-stable`) which **contradicts the
deliberate 1.21.x target** and should be closed-with-reason, not merged; the rest are ordinary
bumps to test-and-merge.

**#4 — `origin/release/1.5.0` branch lingering.** The v1.5.0 MINOR release branch was never
deleted after its merge. git-workflow/repo-hygiene want work branches auto-deleted; safe to
remove (its history is in `main`).

**#5 — Required-status-checks omit newer smoke jobs (owner-side, minor).** `main` requires
`build`, `Analyze`, `Server smoke (1.21.11)`, `Server smoke (26.1.2)`, `Mineflayer` — but CI now
also runs `Server smoke (Purpur)` and `Latest Paper smoke (stable+experimental)`, which aren't
required. The full-CI-gate mandate implies they should be. Owner action (branch-protection
config); not release-blocking since they ran and passed on #26.

## What went well

- The **just-passed v1.5.1 CI run** made the heaviest rows (build/test/SAST/smoke) authoritative
  without a local rebuild — auditing right after a green ship is efficient.
- **Structurally the node is solid:** every standard is established and enforced; the gaps are
  operational (backlog, a base-image pin, a stale branch) or self-inflicted (dates), not missing
  machinery.
- Scorecard **7.9**, up from 7.6 — Signed-Releases now 10 (v1.5.1 added a 5th signed release).

## What went wrong / friction

- **I misread the date** — the single most avoidable miss, and it cascaded into three `partial`
  rows. The clock was in context the whole time; I should read it, not infer it from the last
  report.
- **Auditing found the PR backlog that the *ship* should have caught.** The ship contract is in
  CLAUDE.md, yet v1.5.1 went out over 9 open PRs. The pre-ship PR-triage step needs to be a hard
  checklist item on the release path, not a thing the later audit rediscovers.

## Recommended fixes (await go-ahead)

1. **#1 (mine):** re-date this session's artifacts to 2026-08-02, move the session log to
   `2026-08/`, move the changelog entry to `version/2026-08.md`, fix manifest dates — one
   follow-up PATCH (v1.5.2) or fold into the next release.
2. **#3:** triage #27–#35 — merge the action/gradle bumps that pass CI, **close #33** (26.x
   paper-api) with the 1.21.x-target reason.
3. **#2:** SHA-pin the Dockerfile base image (→ Pinned-Dependencies 10).
4. **#4:** delete `origin/release/1.5.0`.
5. **#5 (owner):** add the Purpur + latest-paper smoke jobs to `main`'s required checks.

## Environment

Windows dev box, PowerShell via Windows-MCP (agent-tooling mesh rule; Cowork bash sandbox
avoided). Target `62cbd59` (main==dev, v1.5.1). Live reads: Scorecard API, `gh` (secrets, PRs,
branch protection). No local build re-run — the v1.5.1 CI run on this HEAD is the build/test
evidence.
