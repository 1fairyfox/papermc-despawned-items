# DespawnedItems — AI Context

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

- **Build the plugin jar:** `./gradlew build` → `build/libs/DespawnedItems-<version>.jar`
  (a shaded jar with the Kotlin stdlib inside; drop it in a server's `plugins/`).
- **API docs:** `./gradlew dokkaGenerate` → `build/dokka/html/`.
- **Runtime smoke test:** download a Paper 1.21.11 server jar (fill.papermc.io), put
  the built jar in `plugins/`, run `java -jar paper.jar --nogui`, confirm
  `DespawnedItems is enabled` with no plugin stack traces. Repeat on a 26.1 jar to
  confirm forward-compat.
- **Toolchain:** JDK 21 (auto-provisioned via the foojay resolver in
  `settings.gradle.kts`), Gradle wrapper 9.6.1, Kotlin 2.4.x, Paper API 1.21.11.

## Default Workflow — Do These By Default (a standing instruction)

**Plan before you execute.** For non-trivial work write a short structured plan
first in `notes/plans/`, then execute against it. Full rule: the shared `planning`
standard.

Then, after making changes, run this loop **without being asked**:

1. **Build / check** the change (`./gradlew build`).
2. **Test** the affected area; full build before releasing to `main`. Only proceed
   on green.
3. **Commit + push on `dev`**, staging specific files (never `git add -A`). The
   changelog entry rides inside the commit (top of `notes/version/YYYY-MM.md`), and
   bump `VERSION` in the same commit when warranted (PATCH default).
4. When green, **release `dev → main` the git-flow way** — `main` advances only by a
   `--no-ff`, **tagged** merge, never a fast-forward or direct commit. PATCH releases
   directly; MINOR/MAJOR go through a `release/*` branch. **Then back-merge**
   (`git checkout dev && git merge --ff-only main`). If `main` is branch-protected,
   release via a PR. Full rules: `notes/reference/git-workflow.md`.

**Hard safety rules:** never `push --force` / rewrite pushed history; never
`reset --hard` / `rebase` / `clean -fd` / delete a long-lived branch without an
explicit request. Inspect `git status` before and after.

## Maintaining the Notes — Your Responsibility

| Trigger | Action |
|---------|--------|
| Did work worth recording this session | Append to today's `notes/sessions/YYYY-MM/YYYY-MM-DD.md` |
| Made a substantive commit | Inline changelog entry atop `notes/version/YYYY-MM.md`, same commit |
| Health / next changed | Update `notes/status.md` |
| Made / rejected a decision | `notes/decisions/architecture.md` / `rejected.md` |
| A change warrants a version | Bump `VERSION`, same commit |

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
