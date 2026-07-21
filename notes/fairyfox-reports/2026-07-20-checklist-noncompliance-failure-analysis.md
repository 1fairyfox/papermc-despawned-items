# fairyfox process report — failure analysis: how whole checklists got skipped while being reported done

**Date:** 2026-07-20
**Procedure:** post-mortem of the standards non-compliance found by the same-day full
audit (`notes/plans/standards-audit-2026-07-20.md`, 108 items) + the v1.3.3 release run,
written at Fairy Fox's direction for the hub. The owner reports this is a **frequent,
mesh-wide problem** ("whole lists were skipped and marked as done"), not a one-repo
accident — the proposals below are meant to end it for **all projects**.
**Node:** papermc-despawned-items · hub 0.20.2 / chrome 2.2.1 at time of audit.

---

## 1 · One-line summary

Three separate passes on this repo (onboarding 2026-07-19, docs-chrome adoption
2026-07-20, and the daily working state between them) each **reported green while whole
checklists behind that green had never been run** — because the mesh currently lets
"the artifact exists" stand in for "the standard's Verify items pass", and its only
enforcement instrument (compliance.md) is opt-in and was never invoked.

## 2 · What the audit actually found (the evidence)

Found by the 2026-07-20 full audit, all while `status.md` displayed
**"Standards adopted (project side) ✅"**:

- Primary nav marked **Docs** active — docs-site 05 mandates **Projects, always, only
  Projects**, stated twice in the standard and once in checklist 08 item form.
- Subnav had an **invented item name** ("Guide") and **raw GitHub links in the centre
  zone** — 05 bars both, explicitly ("don't invent parallel names", "never a raw
  generated index / every centre link a chrome-wearing page").
- The **required Downloads page did not exist** (the project ships jars; 06 §Downloads
  says *must*), the subnav "Download" pointing at raw GitHub instead.
- **21 of 27 hub standards were never adopted** — including two (`agent-tooling`,
  `planning`) that this repo's own `CLAUDE.md` cited as "the shared standard" — the
  citations dangled.
- **supply-chain-hardening was "adopted"** (file copied at onboarding, byte-identical
  to hub) **while its §5 mandatory branch protection was simply off** — checked live:
  HTTP 404, no protection on `main`; no provenance attestation in release.yml either.
- **repo-hygiene's gates** (check-links / check-tidy, in hub/templates since 0.18.0)
  were absent; branch auto-delete off; `dev` unprotected.
- The **compliance audit standard** — the enforcement layer itself — was among the
  never-adopted 21, and no compliance pass had ever been run on this node.

## 3 · Failure tree

Numbering: F = failure, each with observed effect → proximate cause → root cause (§4).

### F1 — Docs-site: chrome adopted, rulebook skipped

- **F1.1 Wrong active nav item** (audit #22). Effect: every docs page claimed the hub's
  Docs section. Proximate: the adopter filled the chrome's `.active` slot by intuition
  ("these are docs → Docs") without opening module 05. The slot comment in
  `chrome/header.html` says *which* slots to fill but **does not carry the rule** for
  what goes in them. Root: R4 (rules far from point of use) + R1 (done = rendered).
- **F1.2 Non-canonical subnav** (audit #24). Effect: invented "Guide", centre links to
  raw GitHub. Proximate: same slot-filling; `{{FF_SUBNAV_ITEMS}}` is a free-text slot
  with no shape validation and no rule text beside it. The canonical three-zone shape
  exists only in 05, one link away, never followed. Root: R4, R6 (nothing
  machine-checks the shape).
- **F1.3 Required Downloads page missing** (audit #38). Effect: a *must* requirement
  unmet on a releasing project. Proximate: the chrome-adoption task was scoped as
  "inject the bundle"; requirements that live in *other* modules (06) were invisible to
  that scope. Nothing in the chrome bundle or adapters says "finish by running
  checklist 08". Root: R4 + R5 (scoped partials leave no named remainder).
- **F1.4 In-browser verification passed the wrong things.** Effect: the adoption report
  honestly says "verified in-browser… palette cohesive" — the *look* was checked, the
  *rules* weren't. Proximate: verification was aesthetic, not item-by-item against 08.
  Root: R1 — verification without a checklist degenerates to "does it look right".

### F2 — Adoption illusion: copied ≠ implemented ≠ done

- **F2.1 Six-of-27 onboarding adoption, unrecorded remainder.** Effect: 21 standards
  invisible for a day (and it would have been months). Proximate: the onboarding
  runbook's §"Partial adoption is fine — *if reported honestly*" was satisfied by an
  honest *narrative* report; but **no artifact enumerates the not-adopted set**, so the
  remainder evaporated the moment the report was filed. The runbook's own completeness
  audit (8 rows) checks "is it in the mesh", not "which standards are live". Root: R2
  (no per-standard manifest) + R5.
- **F2.2 False-green status line.** Effect: `status.md` Health said "Standards adopted
  (project side) ✅" — read by every subsequent session as settled fact. Proximate:
  a **binary checkmark summarizing a 27-item set**, written when 6 items were true.
  Nothing requires a Health row to link evidence. Root: R2 + R1. This is the exact
  mechanism of "whole lists marked done": **the summary bit is writable without the
  itemized record existing.**
- **F2.3 Copy counted as adoption.** Effect: supply-chain-hardening "adopted" with its
  mandatory branch protection off. Proximate: adoption in practice = "file lands in
  notes/reference/"; the standard's `## Verify` table (which would have 404'd on the
  protection check in seconds) was never executed. Root: R1 — the mesh has no rule
  that **a standard is adopted only when its Verify table has been run and recorded.**
- **F2.4 Dangling CLAUDE.md citations** (`agent-tooling`, `planning` cited, not
  present). Effect: establishment drift by compliance.md's own definition ("a standard
  that contradicts the artifacts that operationalize it isn't established"). Proximate:
  CLAUDE.md was written from the hub template (which cites the standards) while the
  copy-step cherry-picked. No check cross-references citations against
  notes/reference/ contents. Root: R2 + R6.

### F3 — The enforcement layer was dormant

- **F3.1 compliance.md is opt-in and was never invoked.** The standard opens with "a
  rule with no check is a suggestion" — and then gates its own check behind "on request
  only". No request ever came (the owner can't request an audit they don't know is
  overdue), so **every rule downstream was, operationally, a suggestion.** Root: R3.
- **F3.2 No gate ties releases to compliance state.** v1.3.1/v1.3.2 released while
  mandatory supply-chain items were open; nothing in the release path asks. Root: R3.
- **F3.3 Hub report-review audits narratives, not artifacts.** The hub digests process
  reports (reports_through) — but a report saying "adopted the chrome" passes review
  without anyone diffing the claim against the node's tree. F1.1/F1.2 sailed through
  the same-day report unchallenged. Root: R3 + R2 (nothing standard to diff against).

### F4 — Release-path drift

- **F4.1 "Just merge dev→main" outlived its retirement.** supply-chain (0.12.0,
  2026-07-02) made protection + PR releases mandatory mesh-wide; 18 days later this
  node still had the local-merge habit and `CLAUDE.md` said "*If* main is
  branch-protected, release via a PR" — conditional wording for a mandatory state.
  Proximate: standards changed at the hub; the node's operational docs weren't swept
  (adoption happened by file-copy, which doesn't touch CLAUDE.md). Root: R1/R4.
- **F4.2 branch-sync races the PR path** (found shipping v1.3.3). The guard fires on
  the `main` push at PR-merge time — necessarily **before** the back-merge exists → a
  guaranteed transient red on every compliant PR release (observed: red at 04:40:31,
  back-merge pushed ~60s later, manual re-run green). A guard that is red on every
  correct release trains people to ignore it. Root: R6 (gate not reconciled with the
  flow it guards).

### F5 — Audit-scoping bias (from this very pass, for honesty)

The 2026-07-20 audit itself initially expanded docs-site/git-workflow/supply-chain into
per-item rows but scoped testing + repo-hygiene as bare "copy the file" rows — the same
under-expansion failure at smaller scale, corrected only because Fairy Fox flagged it
mid-run (items #98–#108 were then added and two real gaps found: auto-delete off, gates
missing). Even an audit *about* skipped checklists reproduced the pattern where the
compiler of the list chooses expansion depth. Root: R2 — expansion must be mechanical
(every standard → its full Verify table), never judgement-scoped.

## 4 · Root causes (synthesis)

- **R1 — "Done" is asserted at artifact level, verified at nothing level.** The mesh's
  working definition of done is "the file/chrome/setting exists"; the standards' own
  definition (every `## Verify` row passes) is written down but not wired to anything.
- **R2 — No mandatory per-item record.** There is no artifact whose *absence blocks the
  summary claim*. status.md, registry flags, and reports all accept unbacked ✅s. Lists
  get "marked done" because marking done is free.
- **R3 — Enforcement is opt-in with no cadence.** compliance.md runs only when a human
  asks with magic words; adopt/onboard/release flows never trigger it.
- **R4 — Rules live far from the point of use.** The slot is in header.html; the rule
  for the slot is in 05; the requirement for the extra page is in 06; the checklist is
  in 08. The adopter touches only the slot.
- **R5 — Sanctioned partials leave no named remainder.** "Partial is fine if reported
  honestly" — but the remainder lives in prose, owned by nobody, due never.
- **R6 — Machine-checkable invariants aren't machine-checked.** Active-nav rule, subnav
  shape, protection state, VERSION↔tag — all grep/API-checkable; none gated.

## 5 · Proposed fixes — how the hub ends this for all projects

Each fix names its target artifact. S1+S6 are the load-bearing pair; the rest close
specific branches of the tree. **This node has already implemented S1/S6/S7 locally**
(see §6) so the hub can lift working copy rather than a sketch.

- **S1 — Mandate a per-standard adoption manifest (kills R1/R2/F2).** New required node
  artifact `notes/reference/adoption-manifest.md` (add to `templates/notes-skeleton/`):
  one row per hub standard — *standard · hub VERSION+commit adopted · state
  (implemented / copied-only / gap(due) / N-A(reason)) · last Verify run (date +
  per-row result) · evidence link*. Hard rules, written into the manifest header and
  the standards: **"copied-only" is not adopted; only a recorded Verify pass flips a
  row to "implemented"; no summary claim (status.md Health, registry `adopts_hub`,
  a process report's "adopted X") is permitted without a manifest row backing it —
  a bare "Standards adopted ✅" is banned wording mesh-wide.**
- **S2 — Give enforcement a pulse (kills R3/F3).** In adopting-updates.md +
  onboarding-existing-project.md: every adopt/onboard pass **ends by running the
  Verify table of every standard it touched** and updating the manifest; onboarding
  runs the full compliance matrix once. In git-workflow's release section + the
  CLAUDE.md template's Default Workflow step 4: **before a release, read the manifest —
  an overdue `gap` row on a mandatory standard (supply-chain, git-workflow) holds the
  release** the same way a red build does. Cadence floor: a full compliance pass at
  least once per month of active work, recorded as a process report.
- **S3 — Put the rules on the slots (kills R4/F1.1/F1.2).** In `chrome/header.html` /
  `chrome/subnav.html`, the slot comments state the rules inline: *"active item on a
  sub-project is `Projects` — always, only, no exceptions (05)"* and the canonical
  three-zone subnav shape + "centre links must be chrome-wearing pages; a releasing
  project must include Download (06)". End `12-shared-chrome.md` and every adapter
  (incl. a new `adapters/dokka.md` — offered before, sharpened here) with a mandatory
  final step: **"run 08-compliance-checklist now, record results in the manifest —
  chrome adoption is not complete until then."**
- **S4 — Partials must name their remainder (kills R5/F2.1).** Amend the onboarding
  runbook's partial-adoption clause: a partial is honest **only if every skipped item
  lands in the manifest as `gap` with a due marker** ("next adopt pass" at minimum).
  Hub-side report-review adds one step: **spot-check a report's adoption claims
  against the node's manifest + tree** (a claim without a matching manifest row is a
  review finding, fed back like any other).
- **S5 — Machine-check what machines can check (kills R6/F1.2/F4).** New
  `templates/check-standards.mjs` (zero-dep, like the hygiene gates), run in node CI:
  fails if a docs template marks any primary-nav item other than Projects active; if
  `VERSION` ≠ newest tag on main-merge; optionally greps the subnav block for the
  right-aligned `.subnav-repo` + known centre names. And **fix
  `templates/branch-sync.yml` for the PR path (F4.2)**: on a `main`-push trigger,
  re-check after a grace window (e.g. sleep 120s + re-fetch, or a retry job) before
  failing, so the guard is red for real drift and green for the mandated release flow
  — a guard that's red on every correct release will be ignored, which un-guards it.
- **S6 — "Checklists are contracts" standing instruction (CLAUDE.md, all nodes).** Add
  to `hub/templates/CLAUDE.md` (nodes pick it up on next adopt): *"When work touches a
  standard that carries a checklist or `## Verify` table, enumerate **every** item in
  the plan; record each item's outcome individually (pass / fixed / N-A-with-reason /
  gap-with-due); **never compress a list into a single done-mark without the
  item-by-item record existing first**; expansion depth is mechanical — every touched
  standard expands fully, none summarized at table level (see F5). Copying a standard
  in is `copied-only`, not adoption."*
- **S7 — Evidence-linked status (kills F2.2).** notes-system standard: a status.md
  Health row asserting standards/compliance state **must link its evidence** (the
  manifest or an audit file). Bare ✅s on multi-item claims are drift by definition.

## 6 · Implemented on this node in this pass (reference implementation)

- `notes/reference/adoption-manifest.md` created — all 27 standards, per-row state,
  Verify dates, evidence links (S1).
- CLAUDE.md gains the "Checklists are contracts" standing instruction (S6).
- status.md Health row now links the manifest instead of a bare ✅ (S7).
- Already landed earlier today: hygiene gates in CI, protection, attestation, the
  docs-rule fixes themselves (audit `notes/plans/standards-audit-2026-07-20.md`).

## 7 · What was rough writing this

- Reconstructing *why* a wrong choice was made a few hours after making it was easy;
  doing this months later would have been guesswork. The manifest (S1) doubles as the
  paper trail that makes future post-mortems cheap — one more reason to mandate it.
- The failure classes here matched the owner's "this is frequent" report suspiciously
  well for a one-repo sample; the hub should check the other nodes' trees against
  their reports' adoption claims (S4's spot-check, run once retroactively mesh-wide).
