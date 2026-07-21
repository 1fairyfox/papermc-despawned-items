# Adoption manifest — per-standard state (the evidence behind any "adopted" claim)

One row per hub standard. **Rules:** `copied-only` is NOT adopted — only a recorded
Verify pass flips a row to `implemented`. No summary claim (status.md, registry,
process reports) is permitted without a row here backing it. A `gap` row carries a due
marker; an overdue gap on a mandatory standard (git-workflow, supply-chain-hardening)
holds releases. Proposed as mesh standard in
`fairyfox-reports/2026-07-20-checklist-noncompliance-failure-analysis.md` (S1).

Hub baseline: 0.20.2 / commit 697bc5c (clone of 2026-07-19–20) · chrome bundle 2.2.1.

| Standard | State | Verify last run | Evidence / notes |
|----------|-------|-----------------|------------------|
| adopting-updates | implemented | 2026-07-20 | Flow followed 3× (reports in fairyfox-reports/) |
| agent-tooling | implemented | 2026-07-20 | PowerShell+file-tools rule in CLAUDE.md; .gitattributes present; audit #67 |
| ai-context | implemented | 2026-07-20 | CLAUDE.md carries required pieces incl. mesh block; audit #68 |
| badges | copied-only | — | README badge block exists from onboarding; full Verify vs badges.md due next adopt pass |
| coins | implemented | 2026-07-20 | coins.js in vendored chrome, `fairyfox:coins:a`, nothing gated; audit #17 |
| compliance | implemented | 2026-07-20 | First full matrix pass = plans/standards-audit-2026-07-20.md |
| cross-project-sync | implemented | 2026-07-20 | Read-only git-ignored clone; on-request only; audit #95 |
| dependencies | implemented | 2026-07-20 | Dependabot on (gradle + actions) → dev; suite gates; audit #58/#62 |
| deployment | copied-only | — | Docs deploy = Pages on release; formal Verify vs deployment.md due next adopt pass |
| docs-lifecycle | implemented | 2026-07-20 | Current-state docs swept this pass (status/CLAUDE); audit #107 |
| docs-site (13 modules) | implemented | 2026-07-20 (corrected same day) | 08 checklist run = audit A–I; **first pass was a lenient read caught by the owner** — corrected to Case A (overview default, Notes/Tutorials/Changelog pages, API under /api/) in v1.3.4; see audit "Correction round". Open: #26 breadcrumb (recommended), #46 live-deploy sign-off |
| engineering-quality | copied-only | — | Practices held informally; itemized Verify due next adopt pass |
| farm-operating-model | N/A | — | Story/game farms only; this is a server plugin |
| git-workflow | implemented | 2026-07-21 | v1.3.3 shipped via PR path; dev==main; audit J all green |
| legal-docs | N/A | — | Docs live under fairyfox.io origin; hub serves /legal/ |
| maintenance-sweep | copied-only | — | No sweep run yet; first sweep due within a month of active work |
| new-project-setup | N/A | — | One-time runbook; project predates mesh |
| notes-system | implemented | 2026-07-20 | Full tree live; inline changelog; session logs |
| onboarding-existing-project | N/A | — | Completed 2026-07-19 (report on file; outcome partial → gaps closed 2026-07-20) |
| planning | implemented | 2026-07-20 | Plan-before-execute in CLAUDE.md; audit ran off a written plan |
| process-reports | implemented | 2026-07-20 | 4 reports filed; template followed |
| repo-hygiene | implemented | 2026-07-21 | Gates in CI + session use; auto-delete on; dev protected; audit P |
| research-capture | copied-only | — | Notes-first practiced; itemized Verify due next adopt pass |
| self-hosted-assets | implemented | 2026-07-20 | Fox icon vendored (v1.3.4); zero fairyfox.io asset hot-links in the built site; fonts follow the shared-chrome bundle pattern (Google-hosted, as the master chrome ships — hub-level exception) |
| supply-chain-hardening | implemented | 2026-07-21 | main protected (solo config), PR release proven, attestation ran green on v1.3.3; audit K |
| testing | implemented | 2026-07-20 | Audit O #98–#103; regression-per-fix ongoing practice (#100) |
| versioning | implemented | 2026-07-21 | VERSION 1.3.3 == newest main tag v1.3.3 |
| working-rhythm | copied-only | — | Task-tracking practiced; itemized Verify due next adopt pass |
