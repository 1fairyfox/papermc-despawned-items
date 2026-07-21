# Architecture Decisions

Key structural choices and why. Newest on top.

### 2026-07-20 — Full standards audit: nav-active + subnav corrected; PR-based releases now mandatory

The full hub-standards audit (`notes/plans/standards-audit-2026-07-20.md`, 100+ items)
corrected two docs-chrome mistakes from the same-day chrome adoption and hardened the
release path:

- **Primary nav marks `Projects` active — never `Docs`.** Docs-site standard 05 is
  unambiguous: a standalone sub-project always sits under `Projects` (only `Projects`,
  every page). The earlier "Docs active" choice is superseded.
- **Subnav canonicalized** to the three-zone shape with only real, chrome-wearing pages:
  `[PaperMC Despawned Items] · API (active) · Download · [Repository ↗]`. The invented
  "Guide" item and the raw-GitHub Changelog/Download links are gone (centre links must be
  chrome-wearing pages). Download now goes to a real themed `downloads.html` (perma-latest
  via `releases/latest`, install notes, all-releases) vendored into the docs root.
  A themed Changelog page is a recorded follow-up, not a raw link.
- **`main` is now branch-protected** (canonical solo config: require PR, 0 approvals,
  strict checks, enforce-admins, linear history off) and `dev` is deletion/force-push
  protected, per supply-chain-hardening §5 (mandatory). **Releases therefore go through
  the PR path** (`gh pr create --base main` → checks → `gh pr merge --merge` → back-merge);
  a direct local `dev → main` push is retired. Tagging stays by-hand (release.yml reacts
  to tags; CI does not own tagging). Release jars now carry build-provenance attestation.
- Repo hygiene: branch auto-delete on; `check-links.mjs` doc-link gate in CI;
  `check-tidy.mjs` for session-end tidiness; 17 hub standards copied into
  `notes/reference/` (4 recorded N/A: farm-operating-model, legal-docs,
  new-project-setup, onboarding-existing-project).

### 2026-07-20 — Docs site wears the shared fairyfox chrome (bundle v2.2.1)

Adopted the hub's **shared chrome bundle** (`hub/standards/docs-site/chrome`, VERSION
**2.2.1**) into the Dokka site so it reads as a page of fairyfox.io instead of a bare
generator island. Dokka is the "full-page generator" case (like Doxygen): it owns the
page but exposes hooks, so the bundle is injected via Dokka's FreeMarker `templatesDir`
(`docs-theme/dokka-templates/includes/{page_metadata,header,footer}.ftl`) — the head
bundle + vendored `main.css`, the masthead + this project's subnav (Docs active), and the
shared footer + `nav.js`/`reader.js`/`coins.js`. The four master assets are **vendored**
under `docs-theme/chrome/` and copied into the docs root at build time (`vendorChromeAssets`,
`finalizedBy dokkaGenerate`) — never hot-linked, so the site renders with fairyfox.io
offline. Referenced per-page via Dokka's `${'$'}{pathToRoot}` so they resolve at any depth.

Sanctioned pattern: **"wear the chrome, boundary the reference"** — the frame is the
verbatim bundle; Dokka's API body stays Dokka, harmonised to the fairyfox palette by
`docs-theme/dokka-fairyfox.css` (tokens reimplemented per-stack, per docs-site standards
01–11). Deliberate deviations: (a) the shared footer sits inside Dokka's content column
(not full-bleed under the sidebar) to avoid a second window-scroll that would unpin the
sticky masthead — Dokka's viewport-locked layout is left intact; (b) Dokka's own reference
bar is kept below the masthead as the API controls (search / theme / source-set filter).
Adopted bundle version recorded here for clean refresh diffs; refresh = re-pull + re-diff
`docs-theme/chrome/VERSION`, not a reimplementation.

### 2026-07-20 — Pluggable storage backends (YAML default, SQLite, MySQL/MariaDB)

Introduced a `LocationRepository` interface with three backends so the plugin meets
large-server expectations: **YAML** flat files (zero-config default, backward-compatible
with existing `userdata/<uuid>.yml`), **SQLite** (embedded, no external server — good
for large single servers), and **MySQL/MariaDB** (shared storage across a network of
servers). SQLite/MySQL go through one dialect-agnostic `JdbcLocationRepository` over a
HikariCP pool. Rationale: rewriting every player file on every change (the old model)
doesn't scale; a DB gives indexed, transactional, incremental, network-shareable
storage. JDBC drivers + HikariCP are loaded at runtime via Paper's `libraries:` loader
rather than shaded, keeping the jar small. Switching backends auto-migrates existing
data. Trade-off: more config surface and an optional DB dependency.

### 2026-07-20 — Retarget Paper 26.1 → 1.21.x (Java 21)

Moved the build target from Paper 26.1 / Java 25 down to the Paper **1.21.x** line
(built against 1.21.11) on **Java 21**. Rationale: the MockBukkit test framework
supports 1.21.x but not the newer 26.x line, so 1.21.x unlocks full integration/e2e
testing (a hard requirement for this refactor); 1.21.11 is also the single most-installed
version and a 1.21-built plugin still loads on 26.1 via Paper forward-compat. Supersedes
the 2026-07-19 "targeting Paper 26.1" decision below. Full rationale + data:
`../plans/refactor-2026-07.md`.

### 2026-07-19 — Rewrite to Kotlin/Gradle targeting Paper 26.1

Chose to modernise the plugin as a **Kotlin rewrite on Gradle** (rather than a
minimal Java-on-Maven version bump) targeting **Paper 26.1 / Java 25**. Rationale:
the 2021 code targeted Paper 1.16.5 / Java 11 and can't load on a modern server, so
substantial API migration was unavoidable either way; Gradle (Kotlin DSL) + Hangar
publishing is the current Paper-community norm, and Kotlin is well-supported on
Hangar. Trade-off: the plugin now ships the Kotlin stdlib (shaded into the jar), and
Kotlin's handling of Paper's overloaded setters requires explicit `setX(...)` calls.

### 2026-07-19 — Sound/particle config as string keys, not enums

Switched `config.yml` to store a Minecraft **sound key** (resolved via the
`World.playSound(Location, String, …)` overload) and a **particle key**, instead of
the old `org.bukkit.Effect`/`Particle` enum names. Rationale: modern Paper moved these
toward registry-backed types, so string keys are stable across versions and avoid the
enum→registry churn. Trade-off: a one-time config-schema change from the 1.16 version.

### 2026-07-19 — Config holder exposed as `plugin.settings`

Named the plugin's own configuration holder `plugin.settings` rather than
`plugin.config`, because a Kotlin `config` property collides with
`JavaPlugin.getConfig()` (Bukkit's `FileConfiguration`).
