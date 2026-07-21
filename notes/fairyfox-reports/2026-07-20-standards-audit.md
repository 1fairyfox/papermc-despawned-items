# fairyfox process report — full standards audit + gap application

**Date:** 2026-07-20
**Procedure:** complete expectation audit against the hub clone (docs-site standard all
modules, git-workflow, supply-chain-hardening, testing, repo-hygiene, versioning, + the
full 27-standard adoption table), then gap application.
**Trigger:** direct user request ("gather up a list of all the hundred or more
expectations … skip absolutely none") — clone not refreshed (request did not invoke the
fairyfox check-for-updates flow; the clone was already current from the same-day
adopt-docs-chrome pass, chrome 2.2.1).
**Authorization:** standing ledger grant `adopt-standards-by-default`
(hub/standards/ + hub/templates/) + the user's direct instruction. Safety floor kept:
reconcile-not-clobber, full verification before/after, this report, reviewable commit.

## What was done

108-item audit in `notes/plans/standards-audit-2026-07-20.md`; applied: primary-nav
active corrected Docs→Projects, subnav canonicalized (three-zone, no invented names, no
raw-GitHub centre links), required Downloads page built + vendored, `main` branch
protection (canonical solo config) + `dev` deletion protection + auto-delete on →
PR-based releases now in force, build-provenance attestation in release.yml,
check-links/check-tidy gates adopted, 17 standards copied into notes/reference/
(4 N/A with reasons: farm-operating-model, legal-docs, new-project-setup,
onboarding-existing-project). Verified: build + dokkaGenerate green before and after;
regenerated output inspected (active states, single footer, sidebar leak-free,
downloads.html present).

## What was rough / suggestions to improve the procedure

- **The chrome bundle adapter path made it too easy to stop at the chrome.** The
  same-day adopt-docs-chrome pass filled the FF_* slots but picked the active nav item
  and subnav names ad hoc — the bundle's README/slots don't restate the 05 rules
  ("active = Projects, always", the canonical subnav names). Suggestion: put a one-line
  reminder of both rules next to the `{{FF_SUBNAV_ITEMS}}` / active-state slots in
  `chrome/header.html` + `chrome/subnav.html` comments, so the slot-filler can't miss
  them without opening module 05.
- **A per-standard "Verify" index would speed full audits.** Compiling the expectation
  list meant opening every module; a generated one-page index of all `## Verify` tables
  across standards (hub-side, auto-built) would make a node-side full audit ~an hour
  faster and more repeatable.
- **Onboarding under-adopts by default.** Onboarding (2026-07-19) copied 6 standards;
  the ledger's adopt-by-default grant arguably implies onboarding should copy all
  applicable standards up front. Clarify in onboarding-existing-project.md whether the
  initial pass should sweep the whole standards dir.
- **check-links/check-tidy are npm-flavoured but zero-dep** — they ran fine in a Gradle
  repo via bare `node`. Worth a line in repo-hygiene.md saying they're runtime-agnostic.

## Deviations recorded

None new beyond the two already recorded for the chrome adoption (footer-in-column,
Dokka reference bar). The audit's open follow-ups (themed Changelog/Tutorials pages,
vendored fox icon, hub-side registry check) are in `plans/future.md` / `status.md`.
