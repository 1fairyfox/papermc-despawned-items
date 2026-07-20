---
date: 2026-07-19
procedure: onboarding
node: papermc-despawned-items
outcome: partial
hub_version: 0.20.2
hub_commit: 697bc5c
---

# Process Report — onboarding, 2026-07-19

> A full, honest account of onboarding `papermc-despawned-items` into the fairyfox
> mesh, run alongside a Kotlin/Paper-26.1 modernisation of the plugin.

## Outcome in one line

Project-side onboarding + a full Kotlin/Paper-26.1 rewrite are done and verified
(builds green, loads on a real 26.1 server); the live docs-site publish and hub-side
registration remain, so overall this is **partial** — committed on `dev`, release held.

## What was done

- **Survey first (step 1).** Inventoried the repo: already on `main` (no `master`
  rename needed), Apache-2.0, Java 11 / Maven / Paper 1.16.5, an unfinished 2021
  "90% done" rewrite. Repo was moved mid-run from `PopupMC/DespawnedItems` to
  `1fairyfox/papermc-despawned-items` (owner's call, to sit under the shared account).
- **Modernisation (not in the runbook, but the reason for onboarding).** Rewrote all
  ~30 classes from Java to Kotlin on Gradle (Kotlin DSL), targeting Paper
  `26.1.2.build.74-stable` / Java 25. Migrated churned APIs, shaded the Kotlin stdlib
  into the jar, and pinned the toolchain (Gradle 9.6.1, Kotlin 2.4.10, Dokka 2.2.0).
  Iterated the build to green against the real Paper API.
- **Reference clone (step 3).** Cloned the hub read-only into
  `assets/references/fairyfox.io` (git-ignored).
- **Templates by reconciliation (step 4).** `CLAUDE.md` with the mesh-awareness block;
  `VERSION` seeded `1.0.0` (from the real `1.0`, SemVer-shaped, not reset to `0.1.0`);
  merged `.gitignore` toward Gradle (the old Maven ignores no longer applied); copied
  `.gitattributes`; dropped in the `notes/` skeleton and seeded `status.md`,
  `context/project.md`, `decisions/architecture.md`, and the changelog; `SECURITY.md`,
  Dependabot (gradle ecosystem), and SHA-pinned `branch-sync`/`ci`/`release` workflows.
  Reconciled the README and added the badge block.
- **Docs (step 6).** Generated Dokka HTML and re-skinned the generator with a fairyfox
  stylesheet + brand→`fairyfox.io` home link + footer. Verified it builds and the theme
  lands in the output.
- **Runtime verification (beyond the runbook).** Booted a headless Paper 26.1.2 server
  with the built jar: it enables cleanly (`DespawnedItems is enabled`, no plugin
  traces), confirming `api-version: 26.1` and the shaded runtime.
- **Report (step 8) + commit on `dev` (step 9, release held).**

## What went well

- The **survey-first** discipline paid off immediately — catching that the repo was
  already on `main` skipped the whole `master→main` dance.
- "**Seed VERSION from the real number, not 0.1.0**" was unambiguous and correct.
- The **mesh-awareness block** requirement in `CLAUDE.md` is well-flagged as the
  easiest thing to miss; because the runbook shouts about it, it didn't get missed.
- **Partial-is-fine-if-reported** made it comfortable to stop at an honest boundary
  rather than fake completion of the live docs publish.

## What went wrong / friction

- **The runbook assumes a web/static or JS project.** Every concrete example (npm,
  JSDoc/Doxygen, Netlify, `package.json` version badge) is web-shaped. This is a JVM
  server plugin: Maven→Gradle, Javadoc/Dokka, a `.jar` artifact, no deploy target at
  all (it ships to Hangar / a server's `plugins/`, not Pages or Netlify). Nearly every
  template needed a JVM translation. A "JVM/Gradle project" note would help.
- **Dependabot template is npm-only.** Had to swap `package-ecosystem: npm` → `gradle`.
  The template could ship commented ecosystem variants (gradle/maven/cargo/pip).
- **The version badge** in `README-badges.md` defaults to `github/package-json/v` —
  wrong for anything non-npm. The commented `github/v/tag` fallback saved it, but the
  default assumes npm.
- **Docs-site step is the biggest mismatch.** It's written for hand-authored Jekyll or
  common generators (JSDoc/Doxygen/TypeDoc/Sphinx) — **Dokka isn't mentioned**, and the
  "publish at `fairyfox.io/<key>/`" step is genuinely cross-repo: per this mesh's model
  a project only writes its own repo, so the actual Pages/domain wiring is a hub-side
  act. The runbook treats it as a project-side step. Result: I themed the generator
  (project side) but can't do the live publish (hub side) from here — an honest partial
  the runbook's own "verify the served page" bar can't be met until the hub step runs.
- **Client-side runtime testing isn't possible on 26.1 yet.** `node-minecraft-protocol`
  (and therefore Mineflayer) tops out at protocol `1.21.11`; the new year-based `26.x`
  line isn't supported, so an automated in-game client bot can't connect. `minecraft-data`
  *has* 26.1.2 metadata, but the protocol layer lags. Load/enable on a headless server is
  the current testing ceiling.
- **Headless console command testing is unreliable on this build.** Feeding commands via
  piped stdin, *every* command — including vanilla `/version` and `/stop` — throws a
  Paper-internal `CommandSourceStack.getLevel()` NPE in post-command feedback. Confirmed
  environmental (not the plugin) with a control test. Worth knowing before trusting a
  console smoke test as a gate.

## Suggestions / feedback

- Add a short **"JVM / Gradle projects"** aside to `onboarding-existing-project.md` and
  `new-project-setup.md`: Maven→Gradle, `VERSION` from `gradle`/tag not `package.json`,
  Dokka/Javadoc as the doc generator, `.jar` + Hangar/Modrinth as the "deploy" analogue.
- `docs-site/06` should list **Dokka** alongside JSDoc/Doxygen/TypeDoc/Sphinx as a
  "generator-IS-the-site" case, with the theme hook being a Dokka `customStyleSheets` +
  `homepageLink` + `footerMessage` (what I used).
- Clarify in step 6 **which side owns the live publish** when the mesh rule is
  "a project only writes its own repo." Right now the runbook reads as if the project
  enables Pages on the shared domain, which it can't. Name it as a hub-side step and let
  the project's docs-site duty end at "themed generator output, verified locally."
- `dependabot.yml` and `README-badges.md` templates: ship commented non-npm variants so
  a JVM/Rust/Python project isn't editing around npm defaults.

## Environment

Windows 11, PowerShell (Windows-MCP) + JDK 25, Gradle wrapper 9.6.1. Established repo
(own history/README/license/`main` branch) arriving as Java 11 / Maven / Paper 1.16.5,
moved to `1fairyfox` during the run. Generator docs (Dokka), no static site. The bulk of
the effort was the language/API modernisation the onboarding rode alongside, not the
mesh mechanics themselves.
