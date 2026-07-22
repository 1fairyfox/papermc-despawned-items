# PaperMC Despawned Items — AI Context

> Naming (keep consistent — never the bare `DespawnedItems` form): human-facing name is
> **PaperMC Despawned Items**; the repo slug, the Bukkit plugin id (`plugin.yml` `name`),
> the built jar, and the data folder are all **`papermc-despawned-items`**; the Kotlin
> main class is `PaperMcDespawnedItems` (package `io.fairyfox.papermc.despawneditems`).

A Paper (Minecraft) server plugin that intercepts items about to despawn on the
ground and relocates them into a registered network of nearby containers, cookers,
entities, or empty space instead of deleting them. Also ships `/recycle`.
Modern Kotlin rewrite targeting **Paper 1.21.x** (built against 1.21.11) / **Java 21**.
Built by Fairy Fox (github.com/1fairyfox).

> **Why 1.21.x, not 26.1?** The 1.21 line is the largest single install base and —
> unlike the newer 26.x line — is supported by the MockBukkit test framework, so it
> unlocks full integration testing. A 1.21-built plugin still loads on 26.1 servers
> (Paper forward-compat); watch the 26.x registry changes and verify. Full rationale:
> `notes/plans/refactor-2026-07.md`.

## Start Here

Read `notes/status.md` first — current state, what's in flight, what to do next.

The full notes system is in `notes/` (see `notes/README.md` for the map). It
follows the shared living-notes standard. Highlights:

| File | What's in it |
|------|-------------|
| `notes/status.md` | **Current state** — start here |
| `notes/sessions/` | Per-day session logs (`YYYY-MM/YYYY-MM-DD.md`, newest on top) |
| `notes/version.md` | Changelog index (plain-English, per commit; months under `version/`) |
| `notes/context/` | `project.md` · `architecture.md` · `principles.md` |
| `notes/systems/overview.md` | The system map |
| `notes/reference/` | Quick lookups (git-workflow, versioning, …) |
| `notes/decisions/` | `architecture.md` (choices) · `rejected.md` (don't repeat) |
| `notes/plans/` | `next-steps.md` · `future.md` |

## Quality Bar — Enforced, Not Aspirational (a standing instruction)

Owner mandate (2026-07-21). These are release-blocking rules, not goals:

- **Coverage ≥90% line, Kover-gated.** `koverVerify` (min 90) runs in `check`, so
  `./gradlew build` FAILS below it. Current suite sits ~95%; keep it there. Every new
  feature ships WITH tests at every testable layer (unit → MockBukkit → command
  dispatch → permission matrix → load). No feature lands untested.
- **No parked findings.** No detekt baseline, no skipped tests, no TODO/FIXME left in
  `src/`. A finding is fixed or narrowly `@Suppress`ed at the site with a reason —
  never baselined away.
- **Scorecard is kept at the repo's maximum.** All workflow actions SHA-pinned;
  top-level `permissions: contents: read` on every workflow (elevate at job scope
  only); gradle wrapper validated in CI; CodeQL SAST alive.
- **Kotlin is pinned to CodeQL's supported range** (currently 2.4.0 — CodeQL 2.26.0
  rejects 2.4.10 as "too recent"). Do NOT bump Kotlin past what CodeQL analyzes;
  losing SAST for a patch release is the wrong trade. Bump both together.
- **CodeQL: dev runs are informational; the gate is the release PR into `main`.**
- **Full gate before any release:** `./gradlew build` (ktlint + detekt + full suite +
  koverVerify + jar) AND `node scripts/check-links.mjs` locally green, then CI + CodeQL
  green on the PR. No red-or-pending merges.
- Test-harness lore (MockBukkit deviations — scriptable target blocks, sync chunk
  loads, sticky container states) lives in `src/test/.../TestSupport.kt` and
  `notes/reference/mockbukkit-harness.md`. Read it before writing MockBukkit tests.

## Critical Things Not to Get Wrong

- **Kotlin ↔ Paper overloaded setters.** Where a Paper getter has multiple
  setter overloads (e.g. `EntityEquipment.setHelmet`, block-state setters), Kotlin
  won't synthesize a settable property — it's read-only. Call the explicit
  `setX(...)` method, not `x = ...`.
- **`getConfig()` name clash.** The plugin's own config holder is exposed as
  `plugin.settings` (a `Config`), NOT `plugin.config` — `config` would collide with
  `JavaPlugin.getConfig()` (Bukkit's `FileConfiguration`).
- **`api-version: '1.21'`** in `plugin.yml` — matches the Paper API dep
  (`io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT` in `build.gradle.kts`). Bumping
  the Paper API dep may require re-checking this.
- **Sound/particle config are keys, not enums.** `config.yml` stores a sound key
  (`block.fire.extinguish`) resolved via the `playSound(String, …)` overload, and a
  particle key resolved case-insensitively. This avoids the enum→registry churn.
- **In-game client automation is available on 1.21.11.** `node-minecraft-protocol`/
  Mineflayer top out at 1.21.11 — which is exactly the target — so automated in-game
  client tests are now possible (a reason to prefer 1.21.11). Validate by (a)
  `./gradlew build` against the real Paper API, (b) booting a headless Paper 1.21.11
  server and confirming the plugin enables, and (c) confirming it also loads on a
  26.1 server (forward-compat check).
- **Reference clone is read-only.** `assets/references/` is git-ignored; never commit
  it, never edit it.

## Build / Run

> **Tooling (mesh rule):** use **PowerShell** (the `Windows-MCP` PowerShell tool) +
> the file tools (Read/Edit/Write/Glob/Grep). **Never the Cowork bash sandbox** — it
> mis-reports truncated files on this environment. Execute verify/commit/release
> directly. Full rule: the shared `agent-tooling` standard.

- **Build the plugin jar:** `./gradlew build` → `build/libs/papermc-despawned-items-<version>.jar`
  (a shaded jar with the Kotlin stdlib inside; drop it in a server's `plugins/`).
- **API docs:** `./gradlew dokkaGenerate` → `build/dokka/html/`.
- **Runtime smoke test:** download a Paper 1.21.11 server jar (fill.papermc.io), put
  the built jar in `plugins/`, run `java -jar paper.jar --nogui`, confirm
  `papermc-despawned-items` enabling cleanly with no plugin stack traces. Repeat on a 26.1 jar to
  confirm forward-compat.
- **Optional pre-ship playtest (owner, opt-in — `scripts/local-playtest.ps1`).** A manual,
  hands-on preview: builds the plugin, downloads Paper, and boots a **local real server in
  YAML storage mode** (offline `online-mode=false`, under git-ignored `run/`) so the owner can
  connect their own Minecraft client to `localhost:25565` and play-test. When set up live, also
  help the owner open/update their Minecraft client to the matching version.
  **Where it sits in the flow: this is a purely LOCAL stage that runs on the built jar
  *before* the release enters the `main`/CI phase** — i.e. during preview / right before a ship,
  *before* `gh pr create --base main`. It never involves `main`, never involves CI, and never
  blocks a release (the required CI smoke jobs remain the hard gate). **Only offered when
  previewing or right before a ship, and only if the owner wants it** — never automatic.
- **Toolchain:** JDK 21 (auto-provisioned via the foojay resolver in
  `settings.gradle.kts`), Gradle wrapper 9.6.1, Kotlin 2.4.x, Paper API 1.21.11.

## Default Workflow — Do These By Default (a standing instruction)

**Plan before you execute.** For non-trivial work write a short structured plan
first in `notes/plans/`, then execute against it. Full rule: the shared `planning`
standard.

**Phase by default — decompose every ask (a standing instruction).** Break **any**
request — one the **owner** gives, one the **fairyfox system** gives, *or a task you
set yourself* — into as many phases as it needs, **by default**. Do not collapse a
multi-part ask into one undifferentiated push: name the phases up front (a task list,
plus a `notes/plans/` entry for anything non-trivial), execute them in order, and
report against them. More phases is the safe default; a single phase is only for the
genuinely atomic. This pairs with the mandate ledger — clauses become phases, phases
become tracked tasks. Under-phasing is exactly how clauses get lost (see
`notes/fairyfox-reports/2026-07-21-mandate-execution-failure-analysis.md`).

Then, after making changes, run this loop **without being asked**:

1. **Build / check** the change (`./gradlew build`).
2. **Test** the affected area; full build before releasing to `main`. Only proceed
   on green.
3. **Commit + push on `dev`**, staging specific files (never `git add -A`). The
   changelog entry rides inside the commit (top of `notes/version/YYYY-MM.md`), and
   bump `VERSION` in the same commit when warranted (PATCH default).
4. When green, **release `dev → main` the git-flow way** — `main` advances only by a
   `--no-ff`, **tagged** merge, never a fast-forward or direct commit. PATCH releases
   directly; MINOR/MAJOR go through a `release/*` branch. **`main` IS branch-protected**
   (since 2026-07-20), so every release goes **via PR**: `gh pr create --base main` →
   `gh pr checks --watch` → `gh pr merge --merge` (never squash/rebase), tag by hand
   (CI does not own tagging here). **Then back-merge**
   (`git checkout dev && git merge --ff-only main`). Full rules:
   `notes/reference/git-workflow.md`.

**What "ship"/"release" includes by default (a standing instruction).** When the owner
says "ship" (or a release is cut), the release is not just the code diff — **by default,
every time**, it also:

- **Drives OpenSSF Scorecard toward its maximum**, and never below the **≥ 7.0 floor**:
  read the live score (`https://api.securityscorecards.dev/projects/github.com/1fairyfox/papermc-despawned-items`),
  fix the cheap gaps this release (unpinned deps, missing branch-protection status
  checks, provenance-as-release-asset), and record before/after in the session log.
  Solo-unfixable checks (Code-Review, Contributors, CII-Best-Practices, Fuzzing) are
  **noted, not chased**.
- **Removes tech debt** instead of parking it — no stale dependency PRs, no deprecation
  warnings left to rot, and nothing that violates the Quality Bar (no skipped tests, no
  `TODO`/`FIXME` in `src/`, no detekt baseline).
- **Triages and handles the open PRs** — every open Dependabot/other PR is either
  test-and-merged or **closed with a reason** (e.g. a bump that contradicts the
  deliberate 1.21.x target). Never release on top of an unhandled PR backlog.

These ride the **same green gate** as the code; they are enforced here and mirrored in
`notes/status.md`. The canonical worked example is
`notes/plans/mandate-2026-07-21-ship-contract.md`.

**Every deployment to `main` passes the FULL CI suite — all of it, including all testing
(a standing instruction, owner 2026-07-21).** No merge to `main` until **every** job on
the release PR is green — `build`, CodeQL, **and every server-smoke, forward-compat, and
in-game (Mineflayer) integration job** — not a subset. A green local `./gradlew build` is
**necessary but not sufficient**: the real-server smoke jobs catch what unit/MockBukkit
tests cannot (e.g. a runtime `libraries:` version that resolves on Maven Central but is
absent from Paper's library-loader mirror, so the plugin fails to load in production).
Wait for the whole suite (`gh pr checks --watch`) and **hold on any failure** — diagnose
and fix, never merge red or partial. (Enforcement mechanism: the required-status-checks on
`main` should list these jobs — an owner action; see `notes/status.md`.)

**Hard safety rules:** never `push --force` / rewrite pushed history; never
`reset --hard` / `rebase` / `clean -fd` / delete a long-lived branch without an
explicit request. Inspect `git status` before and after.

## Checklists Are Contracts (a standing instruction)

When work touches a standard that carries a checklist or `## Verify` table:
**enumerate every item in the plan; record each item's outcome individually**
(pass / fixed / N-A-with-reason / gap-with-due). **Never compress a list into a single
done-mark unless the item-by-item record exists first.** Expansion depth is mechanical —
every touched standard expands to its full Verify table; none is summarized at
table level. Copying a standard into `notes/reference/` is `copied-only`, **not**
adoption — only a recorded Verify pass counts. The per-standard state lives in
`notes/reference/adoption-manifest.md`; keep it current, and back any summary claim
(status.md, reports) with its row. An overdue `gap` on a mandatory standard
(git-workflow, supply-chain-hardening) holds releases like a red build.

## Owner Mandates Become Ledgers (a standing instruction)

Failure this rule exists to prevent (2026-07-21): a multi-part owner mandate
("testing on every feature on every layer … full high-load … full performance") took
THREE prompts to complete because its clauses were compressed into summary tasks,
deferred with plausible-but-untested reasons, and re-disclosed instead of finished.
Full analysis: `notes/fairyfox-reports/2026-07-21-mandate-execution-failure-analysis.md`.

- **Transcribe, don't summarize.** When the owner gives a directive with more than
  one requirement, create a mandate ledger in `notes/plans/` with the owner's words
  QUOTED VERBATIM, one row per clause: quote → interpretation → status
  (`done` / `blocked-with-evidence` / `awaiting-owner`). Completion claims cite the
  ledger row-by-row. A clause that never became a row is the root failure mode.
- **Deferral requires falsification, not plausibility.** No ⏳/⛔ without recorded
  EVIDENCE of an attempt (the probe test, the error text, the version check) and a
  retest trigger. "The tool probably can't" is banned — spend the bounded probe hour
  first. (Track record: "MockBukkit can't ray-trace" → 10-line subclass; "Docker 29
  is incompatible" → one curl + a one-line properties file; "Pitest won't run on
  Gradle 9" → a version bump. Every untested deferral this project has recorded so
  far was false.)
- **"As much as you can" means exhaustion, not a milestone.** Effort-scoping
  language from the owner does not authorize an internal time budget. The phase ends
  when every ledger row is `done` or `blocked-with-evidence` — then keep working the
  blocked rows' alternatives before presenting.
- **Re-read the mandate before claiming completion.** Diff the FINAL state against
  the owner's original message(s), clause by clause — not against the plan file,
  which drifts toward what was convenient to do.
- **The S9 NOT-done list is a decision request, not absolution.** Every item on it
  must exist as an `awaiting-owner` ledger row. If the owner repeats the mandate
  without descoping, every `awaiting-owner` row escalates to do-now — a SECOND
  repetition of the same item is a process failure to be reported, not a task.

**Strict reading of latitude (S8).** When a checklist item offers latitude ("include
the ones that exist", "recommended", "where possible"), the default reading is the
**ambitious** one — build the thing — unless the owner explicitly descopes it.
Descoping by silent omission is a `gap`, not a pass.

**Disclose the not-done list (S9).** Every completion claim ends with an explicit
"NOT done / read leniently / needs the owner's eyes" section. The owner should never
have to extract it by challenge.
Background: `notes/fairyfox-reports/2026-07-20-checklist-noncompliance-failure-analysis.md`.

## Maintaining the Notes — Your Responsibility

| Trigger | Action |
|---------|--------|
| Did work worth recording this session | Append to today's `notes/sessions/YYYY-MM/YYYY-MM-DD.md` |
| Made a substantive commit | Inline changelog entry atop `notes/version/YYYY-MM.md`, same commit |
| Health / next changed | Update `notes/status.md` |
| Made / rejected a decision | `notes/decisions/architecture.md` / `rejected.md` |
| A change warrants a version | Bump `VERSION`, same commit |
| Changed data practices (storage, network, local storage, deps that phone out) | Update `docs-theme/pages/content/legal/*` + bump their "Last updated", same change (legal-docs standard) |

## Cross-project standards & checking the fairyfox system for updates

This project is a **node in the fairyfox system** (the hub mesh): it pulls shared
standards from the system on request — see `notes/reference/cross-project-sync.md`.

**When the user asks you to check *the fairyfox system* for updates** — to sync the
standards, get the latest version, or pull a particular standard/runbook — treat it
as the check-for-updates flow. **To invoke it the request must carry the word
"fairyfox"** — normally **"the fairyfox system"**, or a *fairyfox*-prefixed variant
("fairyfox.io", "fairyfox standards") — *paired with* an update/sync intent (check
for updates · what changed · sync · refresh · pull the latest · get the newest).
Generic handles — "the hub", "the mesh", "the standards", a runbook name, a bare
"system", or an update verb alone — do **not** qualify; the word *fairyfox* must be
present, or don't assume this flow.

The default is **check, report, then wait**: refresh the read-only system clone
under `assets/references/`, diff it against what this project has adopted, and
**report what changed + what adopting it would touch — then stop.** Apply nothing
until the user clearly says go ahead; applying is a separate, confirmed act. Full
procedure: the shared `adopting-updates` runbook (in `notes/reference/`).

**Exception — pre-authorized changes.** The system keeps an express-authorization
ledger (`hub/authorizations.yml`). If an active entry there `covers` the change
you're adopting, the user **already gave the go-ahead at the system** — apply it
directly, skipping the "wait" pause. Still reconcile (don't clobber local
divergence), still write the process report, still commit as a reviewable act, and
still run **full verification before *and* after**. If verification can't be
completed, **do not auto-apply** — fall back to check-report-wait.

**Adopting hub standards/templates is the default.** A standing ledger grant
(`adopt-standards-by-default`) covers all of `hub/standards/` and `hub/templates/`,
so pending standard/template changes are pre-authorized: when you run the fairyfox
flow, **adopt them by default** — report *and apply* — rather than reporting and
waiting. This changes nothing about anti-recursion or the safety floor
(reconcile-not-clobber, divergence re-prompt, process report, full verification
before and after).

**After running any fairyfox system procedure, write a process report** in
`notes/fairyfox-reports/YYYY-MM-DD-<procedure>.md` (from
`hub/templates/fairyfox-report.md`): what was done, what was rough, and suggestions
to improve the procedure. The hub reads these to improve the system.

**Guardrails (don't break these):** on-request only — never auto-pull or schedule
cross-repo syncs; the reference clone is read-only and git-ignored; never apply
changes or rewrite history without an explicit go-ahead (an active
`authorizations.yml` entry that covers the change *is* that go-ahead); reconcile
with local edits, don't clobber them.

> Naming: the user calls it **the fairyfox system** in conversation; the public
> website calls it the **hub**. Both name the same fairyfox.io mesh.
