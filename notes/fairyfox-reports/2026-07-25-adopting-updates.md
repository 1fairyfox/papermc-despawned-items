---
date: 2026-07-25
procedure: adopting-updates
node: papermc-despawned-items
outcome: completed
hub_version: 1.6.0
hub_commit: 8c6a50e
---

# Process Report — adopting-updates, 2026-07-25

> A full, honest account of running a fairyfox system procedure. The point is to improve
> the system — so say what was rough even if the run succeeded. Standard:
> `notes/reference/process-reports.md`.

## Outcome in one line

Adopted the single project-facing change in the hub span 1.5.1 → 1.6.0 — the **new `readme.md`
standard** (worded README cross-links) — applied to `README.md`, verified, committed on `dev`;
held the `main` release because `dev` carries the unreleased v1.5.0 milestone (owner's call).

## What was done

1. **Refreshed** the git-ignored hub mirror (`a6d7e68 → 8c6a50e`, clean fast-forward). Hub
   VERSION `1.6.0`; last-adopted anchor `1.5.1` from the newest prior report.
2. **Scoped the diff off `hub/standards/CHANGELOG.md`** across the span. Of the three hub
   releases in it, only **1.6.0** is project-facing (the new `readme.md` standard); 1.5.2 and
   1.5.3 are hub-side edits (`registry.yml`, `_data/projects.yml`, the driver's onboarding and
   icon) that never touch a node. Read `authorizations.yml`: the standing
   `adopt-standards-by-default` grant covers `hub/standards/` → the new standard is
   pre-authorized, so adopt-by-default (skip the confirmation pause, keep the verification floor).
3. **Glanced at the working tree** (check-step 3): clean, on `dev`, up to date with
   `origin/dev`, no unpushed divergence, no mid-merge / detached HEAD. Healthy.
4. **Applied to `README.md`** — reconcile-not-clobber, folded into the existing structure:
   - a worded **docs link** near the top (`📖 Documentation — fairyfox.io/papermc-despawned-items/`),
     in addition to the existing docs *badge*;
   - an organized **"Get it" section** (Documentation · Download/Releases · Source), with a
     prose note that Modrinth/Hangar/CurseForge rows land when those project pages exist —
     they don't yet (distribution badges are still commented out), so they're legitimately
     absent per "nothing to link yet," not a dropped destination;
   - a **mesh footer** near the bottom linking back to fairyfox.io / all projects / this
     project / shared docs.
5. **Ran `## Verify`** for the readme standard (all pass; live-app row N/A — a server plugin
   has no deployed web app, and the Pages docs site is already the docs link) and recorded the
   row in `adoption-manifest.md` (`implemented`). Verification floor before/after: `node
   scripts/check-links.mjs` green (40 files) both sides.
6. **Recorded**: changelog entry, session log, this report, manifest row + baseline bump to
   1.6.0. Committed on `dev`.

## Held deliberately (not a gap)

The `main` release was **not** run. `dev` is mid-flight on the unreleased **v1.5.0** MINOR
milestone (per-user throttling, void chance, per-target commands, client-mod protocol,
multi-platform publishing — see `status.md`), and its release "into `main`, full CI, tag,
back-merge" is an outstanding **owner decision** for the whole milestone. A README-standard
adoption must not be the thing that ships an entire unreleased MINOR. So the change rides on
`dev` and will go to `main` when the owner ships v1.5.0. Noted here and to the owner rather
than silently releasing.

## What went well

- **The CHANGELOG-first scoping worked exactly as designed.** One read told new-vs-changed
  and, crucially, told me two of the three releases in the span were hub-internal and
  irrelevant to a node — no file diff, no wasted apply attempts.
- **`adopt-standards-by-default`** removed the wait-decision cleanly; the change is small,
  self-contained, and verifiable, so applying-by-default was low-risk.
- The node **already had the docs *badge* and the download link**, so the standard was mostly
  additive (worded docs link + Get it grouping + footer) with no conflict to reconcile.

## What went wrong / friction

- **`readme.md` doesn't say how the "live app" row maps to a non-web project.** For a server
  plugin there's no deployed web app; the closest live thing is the Pages *docs* site, which
  is already the required docs link. The standard's Verify row ("live-app present when the
  project has one … Netlify app, or Pages at `fairyfox.io/<key>/`") reads as if Pages counts
  as the live-app row, which double-counts the docs link. A one-line note — "for a
  library/plugin with no deployed app, the Pages docs site satisfies the docs link and there
  is no separate live-app row; mark it N/A" — would remove the judgment call.
- **Publish-target "coming soon" wording is unspecified.** The standard says a
  not-yet-existing destination is legitimately absent, but a plugin that has publishing *wired
  but not yet live* is a common in-between. I wrote a prose "also coming to…" note (no dead
  link) to make the absence legibly intentional rather than an oversight; the standard/template
  could bless that pattern explicitly so nodes don't each invent their own.

## Suggestions / feedback

- **`readme.md`**: add an explicit N/A path for the live-app row when the project is a
  library/plugin/CLI with no deployed web surface (Pages docs = the docs link, no separate
  live-app row), so the Verify row isn't ambiguous for non-web nodes.
- **`README-links.md` template**: bless a "publishing wired, not yet live" prose line for
  destinations that exist as automation but not yet as a page, so the honest in-between is a
  documented pattern, not an ad-hoc call.

## Environment

Windows dev box, PowerShell via Windows-MCP (agent-tooling mesh rule; Cowork bash sandbox
avoided). JVM/Gradle/Kotlin Paper plugin. Arrived on `dev`, clean, up to date with origin,
mid-flight on an unreleased v1.5.0. Node already had the express-auth machinery and the docs
badge/download link; the readme standard was additive.
