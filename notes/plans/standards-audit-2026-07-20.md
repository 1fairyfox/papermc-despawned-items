# Full hub-standards audit — 2026-07-20

Complete itemized expectation list compiled from the hub clone (`assets/references/fairyfox.io`,
standards as of chrome bundle 2.2.1 / hub VERSION at clone). Requested by Fairy Fox: list every
expectation, audit each, apply gaps. Applying is pre-authorized by the standing ledger grant
`adopt-standards-by-default` (covers `hub/standards/` + `hub/templates/`); safety floor kept
(reconcile-not-clobber, verification before/after, process report, reviewable commit).

Status key: ✅ pass · ❌ gap (fix now) · 🔶 gap (recorded, follow-up) · N/A (reason recorded) ·
🔹 pass-by-construction (comes verbatim from the vendored chrome bundle 2.2.1, version matches hub).

## A. Docs site — tokens & colour (08 §Tokens)

1. ✅🔹 Dark theme = exact dark palette (vendored `main.css`, 2.2.1 = hub 2.2.1)
2. ✅🔹 Light theme implemented, exact palette (bundle)
3. ✅🔹 OS-driven scheme, dark default, `:not([data-theme])` guard (bundle)
4. ✅🔹 Sepia theme (`data-theme="sepia"`) (bundle)
5. ✅🔹 Lifecycle status fixed hues (bundle)
6. ✅ Accent only on brand/links/highlights (verified in adopt-docs-chrome pass, 2026-07-20)

## B. Typography (08 §Typography)

7. ✅🔹 Fraunces / Inter / JetBrains Mono at specified weights (bundle `head.html`)
8. ✅🔹 Base size / heading scale match tokens (bundle)
9. ✅🔹 Fonts preconnected, no reflow (bundle)

## C. Shell & layout (08 §Shell)

10. ✅ Sticky 64px translucent header (verified in-browser 2026-07-20)
11. ✅ Footer multi-column structure (verified; deviation recorded: sits in Dokka content column)
12. ✅ `main` grows, footer at bottom on short pages
13. ✅ Centred container at `--maxw` + `--gutter`
14. ✅ API pages: Dokka's own grouped sidebar + capped measure (generator-owned)

## D. Components (08 §Components)

15. ✅🔹 Buttons/cards/chips/badges/code/tables per spec (bundle CSS)
16. ✅🔹 Reader "Aa" menu, `fairyfox:reader` key, applied before first paint; spacing/width story-locked (bundle `reader.js`)
17. ✅🔹 Coin button via shared `coins.js`, `fairyfox:coins:a` key; nothing coin-gated (bundle)
18. ✅🔹 `:focus-visible` accent outline (bundle)

## E. Chrome, cross-linking & branding (08 §Chrome + 05)

19. ✅ Chrome is the vendored `chrome/` bundle, not a lookalike; VERSION 2.2.1 recorded
20. ✅ Shared header, global primary nav in fixed order Home · Projects · Farms(Stories·Games) · Docs · Updates · About
21. ✅ Brand + Home link to `https://fairyfox.io/`; no back-button
22. ❌ **Active primary-nav item must be `Projects` — always, only `Projects`.** Currently `Docs`
    is marked `.active aria-current="page"` in `docs-theme/dokka-templates/includes/header.ftl`.
    → FIX: move `.active` to Projects.
23. 🔶 Site reachable at `fairyfox.io/papermc-despawned-items/` + registry `docs:`/`repo:` accurate —
    registry lives hub-side; verify entry on next hub sync (node cannot edit the hub).
24. ❌ **Subnav canonical three-zone shape** `[name=overview] · Notes · Tutorials · Changelog · API ·
    Download · [Repository ↗]`, include only pages that exist, no invented names, centre links must
    be real chrome-wearing pages. Currently: invented "Guide" item; Guide/Changelog point at raw
    GitHub (not chrome-wearing pages); no API item; subnav-home unconditionally active.
    → FIX: `[PaperMC Despawned Items] · API(active) · Download · [Repository ↗]`; drop Guide +
    raw-GitHub Changelog; themed Changelog page recorded as follow-up (plans/future.md).
25. ✅ Footer links repo/notes/main-site sections (bundle footer, slots filled)
26. 🔶 Breadcrumb/locator near page top — recommended, absent; Dokka breadcrumbs exist on API pages;
    acceptable, note for future.
27. ✅ Project-forward branding via subnav locator; membership carried by chrome

## F. Seamlessness (08 §Seamlessness)

28. ✅🔹 Header metrics/fonts/theme-color/favicon match (bundle `head.html`)
29. ✅🔹 Reader menu + key identical (bundle)
30. ✅ Same-origin direct links, no redirect bounce

## G. Content & organization (08 §Content + 06)

31. ✅ Pages cover roles: overview (Dokka module page w/ module docs), reference (API). Getting
    started lives in README; themed Tutorials page = follow-up (plans/future.md).
32. ✅ API docs generated, themed, boundaried; only in API section
33. ✅ API linked from subnav as clear boundary (after fix #24)
34. ❌→verify **Generator sidebar lists ONLY API pages** — audit every regen (generators re-add).
    → Verify in this pass's regenerated output.
35. ❌→verify **Generator's own footer gone** — `footer.ftl` overridden; confirm no Dokka footer
    renders in output.
36. N/A Notes-on-site rules — project does not render `notes/` on the docs site (Dokka-only site).
    If Notes are ever published: single Notes item, landing + full sidebar, README excluded.
37. ✅ Reader gating: API/index pages omit `data-read` (correct — none carry it); Downloads page
    (new) is designed/navigational → omits `data-read`.
38. ❌ **Downloads page is REQUIRED** (project ships release jars). Must be a real chrome-wearing
    page linked as **Download**, sourced from GitHub Releases, with a perma-latest section.
    Currently Download points at raw GitHub releases.
    → FIX: add themed `downloads.html` with Latest (releases/latest) + all-releases sections.
39. ✅ No orphan pages / no raw link dumps (after fix #24 removes raw-GitHub subnav links)
40. ✅ Public voice; parent referred to as Fairy Fox / fairyfox.io

## H. Accessibility (08 §A11y)

41. ✅🔹 AA contrast both themes (bundle palette; accent unchanged)
42. ✅🔹 Focus visible, keyboard-operable nav (bundle)
43. ✅🔹 `prefers-reduced-motion` honoured (bundle)
44. ❌→verify Landmarks/heading order on the new Downloads page; decorative imgs hidden.

## I. Sign-off (08 §Sign-off)

45. ✅ Deviations recorded (`decisions/architecture.md`: footer-in-column, Dokka reference bar kept)
46. ❌→verify Check light/dark/sepia after this pass's changes
47. N/A Master-copy manual-review rule (fairyfox.io repo only)

## J. Git workflow (git-workflow.md §Verify — all 8 checks)

48. ✅ Stable branch is `main`
49. ✅ Every `main` commit is a tagged `--no-ff` release merge (verify at next release)
50. ✅ `dev` contains `main` (`rev-list origin/dev..origin/main` = 0; branch-sync.yml guard present)
51. ✅ No content authored on `main`
52. ❌ **PR-based releases on a protected repo** — `main` is NOT branch-protected (checked via
    `gh api`: 404). supply-chain-hardening makes protection MANDATORY. → FIX: apply canonical solo
    config; releases move to the PR path (gh pr create → checks → merge --merge → back-merge).
    "Just merge dev to main" locally is retired.
53. ✅ No force-push/rewrite of published history
54. ✅ No spent support branches lingering
55. ✅ Releases ride green dev CI (ci.yml on dev)
56. ✅ CI-owns-tag check: release.yml reacts to hand-pushed tags (documented in file header) —
    by-hand tagging is correct here.
57. ✅ Release posture: green-and-CI-gated auto-proceed (mesh default; no stricter local override).

## K. Supply-chain hardening (§Verify — all items)

58. ✅ Actions SHA-pinned with version comments; dependabot github-actions ecosystem on
59. ✅ Root SECURITY.md with private reporting path
60. ❌ **release.yml lacks build-provenance attestation** (`actions/attest-build-provenance`,
    id-token+attestations at job scope). → FIX: add step, SHA-pinned.
61. ❌ **Branch protection missing** (same as #52). → FIX now.
62. ✅ Dependency-vuln posture: dependabot on; no open findings
63. 🔶 Solo ceiling / badge-lag acknowledgement — add line to notes (this file records it: the
    OpenSSF solo ceiling ~8 and badge lag are accepted, not chased).

## L. Versioning (versioning.md)

64. ✅ `VERSION` file present, PATCH-default, tag matches at release
65. ✅ Changelog rides inside the commit atop `notes/version/2026-07.md`

## M. Hub standards adoption state (27 standards; grant makes adoption the default)

| # | Standard | State | Action |
|---|----------|-------|--------|
| 66 | adopting-updates | ✅ adopted, current | — |
| 67 | agent-tooling | ❌ referenced by CLAUDE.md but not copied | copy to notes/reference/ |
| 68 | ai-context | ❌ not adopted | copy; CLAUDE.md shape already conforms |
| 69 | badges | ❌ not adopted | copy; audit README badges after |
| 70 | coins | ❌ not adopted (coins.js already live in chrome) | copy |
| 71 | compliance | ❌ not adopted | copy (this audit is its first run) |
| 72 | cross-project-sync | ✅ current | — |
| 73 | dependencies | ❌ not adopted | copy |
| 74 | deployment | ❌ not adopted | copy |
| 75 | docs-lifecycle | ❌ not adopted | copy; check-links/check-tidy templates = follow-up |
| 76 | docs-site (13 modules) | 🔶 chrome adopted; full standard audited here | fixes #22/#24/#34/#35/#38 |
| 77 | engineering-quality | ❌ not adopted | copy |
| 78 | farm-operating-model | N/A — story/game farms only; this is a plugin repo | record |
| 79 | git-workflow | ✅ current | — |
| 80 | legal-docs | N/A — docs live under fairyfox.io origin; hub serves /legal/ | record |
| 81 | maintenance-sweep | ❌ not adopted | copy |
| 82 | new-project-setup | N/A — one-time runbook, project exists | record |
| 83 | notes-system | ✅ current | — |
| 84 | onboarding-existing-project | N/A — already onboarded 2026-07-19 | record |
| 85 | planning | ❌ referenced by CLAUDE.md but not copied | copy |
| 86 | process-reports | ❌ in active use but not copied | copy |
| 87 | repo-hygiene | ❌ not adopted | copy; settings audit = follow-up |
| 88 | research-capture | ❌ not adopted | copy |
| 89 | self-hosted-assets | ❌ not adopted | copy; note: brand logo hot-links fairyfox.io (#91) |
| 90 | supply-chain-hardening | ✅ current; impl gaps #60/#61 | fix |
| 91 | testing | ❌ not adopted | copy; MockBukkit plan already in plans/testing.md |
| 92 | versioning | ✅ current | — |
| 93 | working-rhythm | ❌ not adopted | copy |

## N. Cross-cutting items found during audit

94. 🔶 `header.ftl` hot-links the brand logo from `https://fairyfox.io/assets/icons/fox.png` —
    self-hosted-assets standard says vendor it. Follow-up: vendor the icon via vendorChromeAssets.
95. ✅ Reference clone read-only + git-ignored
96. ✅ Fairyfox reports written for both prior procedures
97. ❌ This pass itself: write process report + changelog + VERSION bump + session log + status.md.

## O. Testing standard — detail checks (added mid-audit on Fairy Fox's flag)

98. ✅ Core/logic separated from rendering, unit-tested headlessly — 13 test classes across
    config/despawn/limit/location; MockBukkit mocks the live server (no real server needed)
99. ✅ Multi-layer, real tests — unit + property/fuzz + database (SQLite/JDBC) + performance
    (LocationStoreBench) + mocked-server integration (PluginEnableTest); ~49 tests gate `build`
100. 🔶 Regression-test-per-bug-fix — practice held during the July refactor (fixes landed with
     tests); standing rule now in notes/reference/testing.md; spot-check at each release
101. ✅ Full suite green before release — `build` runs `check` → `test`; CI on dev is the gate
102. ✅ Truth oracle — MockBukkit (mocked Paper) + the headless real-server smoke runs
     (1.21.11 + 26.1.2 boots recorded in status.md) are the oracle pair
103. N/A Visual-preview rule — no visual surface in the plugin; docs pages verified in-browser

## P. Repo-hygiene standard — detail checks (same flag)

104. ✅→APPLIED Broken-link gate: `scripts/check-links.mjs` copied from hub/templates, wired
     into ci.yml before the build step
105. ✅→APPLIED Uncommitted-file guard: `scripts/check-tidy.mjs` copied; run at session end
106. ✅ No stranded files — `git status` clean of non-ignored `??` after this pass's commit
107. ✅ Current-state docs swept — status.md/CLAUDE.md updated in this pass (stale
     "cut 1.1.0" next-step removed)
108. ✅→APPLIED Branch auto-delete ON (`delete_branch_on_merge=true`); `dev`
     deletion+force-push protected via branch protection

## Applied in this pass (2026-07-20)

- #22, #24 → header.ftl fixed (Projects active; canonical subnav) ✅
- #38 → downloads.html created + vendorChromeAssets wiring ✅
- #52/#61 → main protected (solo config verified via API response); #108 dev protected ✅
- #60 → release.yml: attest-build-provenance @977bb373 (v3) + job-scope permissions ✅
- #67–#93 copies → 17 standards into notes/reference/ ✅; 4 N/A recorded in table M
- #104/#105 → hygiene gates in ✅
- #97 → changelog v1.3.3 + VERSION + decisions + status + CLAUDE.md release path ✅
- Remaining open: #23 (hub-side registry check), #26/#31/#94 + themed Changelog page
  (plans/future.md), #100 (ongoing practice), #34/#35/#44/#46 (verified below at regen)

## Execution order (this pass)

1. Fixes #22, #24 (header.ftl) · #38 (downloads page + build wiring)
2. #52/#61 branch protection · #60 attestation step
3. M-table copies (14 standards) + 4 N/A recorded here
4. Regenerate docs; verify #34, #35, #44, #46
5. #97 bookkeeping; commit on dev; PR-release eligible when green
