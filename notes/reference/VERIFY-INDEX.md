# Verify Index (generated — do not hand-edit)

Every standard's `## Verify` table in one place, for a single-read full
[compliance audit](compliance.md). **Generated** by `build-verify-index.mjs` from the
canonical standards — regenerate when a standard changes (`node hub/standards/build-verify-index.mjs`).
The standards themselves remain the source of truth; this is a derived convenience.


## Adopting Updates — bringing hub changes into a project  
<sub>[`adopting-updates.md`](adopting-updates.md#verify-is-it-being-followed)</sub>

- `git status` is clean; `assets/references/` stayed untracked/ignored. Any mirror
  refresh (fast-forward, or re-clone) targeted **only** the git-ignored hub mirror,
  never a tracked branch — and no `reset --hard` was used.
- The adopted change is present in the project's own tree (not just the
  reference clone).
- Each touched standard's `## Verify` ran and its result is recorded in
  `notes/reference/adoption-manifest.md`; no row reads `implemented` without a recorded
  pass, and no `Standards adopted ✅` summary lacks a backing row
  ([checklists-are-contracts](checklists-are-contracts.md)).
- "What changed" was scoped from the hub **changelog** across the version span (last
  adopted `hub_version` → current hub `VERSION`).
- Changelog + session log + (if bumped) `VERSION` ride in the same commit.
- A **process report** for this run is in `notes/fairyfox-reports/` and committed —
  written even on a check-only run, **except** a check-only run on a node that hasn't
  adopted process-reports yet, which reports inline and writes no file
  ([process-reports standard](process-reports.md)).
- If `release.yml` owns tagging, the release **did not** hand-push a tag; otherwise the
  hand tag matches `VERSION`.
- The run ended with a **close-out** stating `dev`/`main` status and that any hub-mirror
  refresh (fast-forward / re-clone) hit the git-ignored mirror only.
- On a **check-only** run, the node's own working tree was glanced at and anything
  alarming (mid-merge, conflicts, detached HEAD, large unpushed divergence) was
  surfaced in the report **without** being acted on.
- If a run **skipped the pause**, an active [`hub/authorizations.yml`](../authorizations.yml)
  entry actually `covers`ed the change (not assumed), and the other safety steps still
  ran — divergence re-prompt, process report, reviewable commit, and **full
  verification (build/tests + standards `## Verify` + project-constraint checks)
  before and after**. If verification couldn't complete, the run fell back to
  check-report-wait rather than auto-applying.
- If the project builds/serves, it builds green.

## Standard: Agent Tooling & Execution  
<sub>[`agent-tooling.md`](agent-tooling.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| The project's `CLAUDE.md` names PowerShell + file tools and forbids the bash sandbox | read `CLAUDE.md` Build/Run + landmines |
| A root `.gitattributes` with `* text=auto eol=lf` (+ `.bat`/`.cmd` CRLF, binaries) exists | `ls .gitattributes`; read it |
| The working tree isn't drowning in CRLF-only "modified" noise | `git status` after a formatter run is clean of phantom edits |

## Standard: AI Context (`CLAUDE.md`)  
<sub>[`ai-context.md`](ai-context.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| A root `CLAUDE.md` exists | `ls CLAUDE.md` |
| It contains all six required pieces (identity · start-here→`status.md` · landmines · build/run · default workflow · notes-maintenance table) | open it and confirm each |
| Its **Default workflow** matches the current [git-workflow standard](git-workflow.md) — no stale `--ff-only`/"fast-forward `main`" wording | grep `CLAUDE.md` for `ff-only`/`fast-forward` |
| Capabilities are stated honestly (build/test/commit named, not denied) | read the Build/run section |

## Standard: README Badges  
<sub>[`badges.md`](badges.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| The README opens with the badge block, consistent `flat-square` style | open the README |
| **All 20 required badges are present, in the canonical order** (§The required order) | walk the block top-to-bottom against the numbered list |
| Each slot is filled by the **right source for this project** (version tag vs package-json; deploy = Pages vs Netlify; CI/coverage wired) — a swap, never a silent drop | cross-check the project's actual services against each slot |
| **Any omitted slot has a recorded user exception** in `adoption-manifest.md` — no badge dropped on the AI's own "doesn't apply" call | diff present badges vs the required 20; for each missing one, find the dated user-granted exception row (none → `missing`) |
| The **docs** badge points at `fairyfox.io/<key>/` | click/inspect the docs badge |
| Each badge links to its source | inspect the badge links |
| A permanently "unknown"/grey required badge is treated as a **gap to wire the service**, not left as-is | look for grey badges; confirm the backing service is wired (or a recorded exception) |

## Standard: Checklists Are Contracts  
<sub>[`checklists-are-contracts.md`](checklists-are-contracts.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| A "done"/"adopted" summary claim links an itemized record (manifest / audit / Verify results), never a bare ✅ over a multi-item set | grep `status.md`, reports, PRs for unbacked ✅/"adopted"/"all done" over lists |
| Work that touched a standard's `## Verify` table recorded a per-item outcome (pass/fixed/N-A/gap), not a table-level done | the plan or manifest shows one row per Verify item |
| Deferred/blocked items carry an attempt log + a retest trigger | scan `⏳`/`⛔`/"deferred" items for evidence, not just a reason |
| The pass ended with an explicit not-done / read-leniently disclosure | the report/completion note has the section without being asked |
| A recorded lesson added its rule in the same commit | a session-log lesson has a matching `CLAUDE.md`/standard edit in that commit |

## Standard: CI Service Secrets  
<sub>[`ci-secrets.md`](ci-secrets.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Every secret a workflow references is set on the repo | `gh secret list --repo OWNER/NAME`, cross-checked against `secrets.*` uses in `.github/workflows/` |
| No workflow references a secret that isn't set (nothing renders "unknown"/unauthenticated) | grep the workflows for `secrets.` and confirm each has a matching secret |
| The secret names match the canonical set (`SONAR_TOKEN`, `CODECOV_TOKEN`, `SCORECARD_TOKEN`) | inspect the secret list and the workflow references |
| A repo wiring none of the three is a clean N/A, not a gap | confirm no `secrets.{SONAR,CODECOV,SCORECARD}_TOKEN` reference exists to satisfy |

## Standard: Coins (reading-engagement points)  
<sub>[`coins.md`](coins.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| **The coin button is present on every chrome-wearing page (MANDATORY)** — never omitted as "not needed" | load any page; the coin button sits beside the "Aa" button. Missing → `missing`, not a judgment call |
| The coin counter comes from the **shared chrome** (`coins.js` pulled from master), not a re-implementation | diff against master `assets/js/coins.js`; confirm it's loaded after `reader.js` |
| **Persistence — enforced.** Neither the hub nor any sub-project loses a user's coins or reader prefs through carelessness: the store is read-and-merged (not replaced) on load, a key-version change migrates the old data forward (doesn't orphan a key), and nothing clears the wallet except the user (their `spend`, the **Clear my data** button, or a browser reset) | read every place the project touches `fairyfox:coins:a` / `fairyfox:reader:b`; confirm no reset/overwrite-on-load and any migration carries data |
| The project **gates nothing** on coins — the full experience works at zero balance | use/read the project with an empty wallet |
| Any project-added coin moments are **subtle, optional, and engagement-tied** — no grinds, no nags, no paywall shapes | exercise the coin moments; read where `reward`/`spend` are called |
| Coin UI does **not** clutter or detract from the project's own brand/content | look at the pages that add coin moments |
| The local coins store is **disclosed** in the project's Privacy/Cookies pages | read the legal pages ([`legal-docs.md`](legal-docs.md)) |

## Standard: Dependencies — upgrade aggressively  
<sub>[`dependencies.md`](dependencies.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Dependabot is configured, grouped, weekly, targeting `dev` | `.github/dependabot.yml` |
| Toolchain is pinned within the SAST analyzer's supported range | the pin + a comment tying it to the analyzer |
| Runtime + test dep versions move together; runtime deps HEAD-checked against the real runtime resolver | the runtime manifest vs. tested version; a real-runtime smoke job |
| Dependabot bumps that contradict a deliberate target are closed **with a reason**, not blind-merged | closed Dependabot PRs carry a reason |
| A full test gate runs **locally**, not only in CI | the project's `test`/`check` scripts run green locally |
| Dependencies are reasonably current (no long-stale majors without a reason) | `ncu` / `npm outdated`; check for old majors |
| Any pin/deferral has a documented reason + revisit note | grep for `pinned`/`overrides`; `decisions/` notes |
| A dep refresh that changed the artifact rode a PATCH bump + changelog | `VERSION` + `notes/version/` history |

## Standard: Deployment  
<sub>[`deployment.md`](deployment.md#verify-is-it-being-followed)</sub>

- Each project's live URL matches its kind: **static → `fairyfox.io/<key>/` (Pages,
  shared domain); built app → Netlify host**. No built app is parked on Pages, and no
  static site is on a needless separate host, **unless** a recorded exception says so.
- **fairyfox-games** resolves at `fairyfox.io/games/` on Pages (no subdomain, no
  Netlify), and any other exception has a written reason in the project's notes.
- Pages projects carry **no `CNAME`** and inherit the domain (per
  [`docs-site/10`](docs-site/10-domain-and-publishing.md)); the base path equals the
  repo slug so chrome/links resolve.
- The registry URL (`docs:` / public URL) points at the real deploy location, and
  `hub/registry.yml` and `_data/projects.yml` agree.

## Standard: Docker — local-first build, test, setup & install  
<sub>[`docker.md`](docker.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| A project with a **Linux-only or reproducibility-sensitive** build/test/setup ships a **working, committed Docker path** (`Dockerfile`/`compose.yaml`) used **locally** | look for the Docker assets; confirm they build/run |
| Linux build/test is **run locally in Docker first**, with CI as the backstop — not "pushed to see what CI says" | check the workflow docs / `CLAUDE.md`; local run is the primary loop |
| **Docker problems were fixed, not routed around** — no "gave up on Docker, CI-only" where a container was the right tool | look for a disabled/abandoned Dockerfile + a note that the fix was skipped |
| The **setup/install/run** flow is reproducible in a container where that helps | try the documented `docker`/`compose` up path |
| Windows↔Linux gotchas handled (CRLF per `.gitattributes`, mount/permission, platform pin) | build the image on the Windows host; confirm no CRLF/permission breakage |
| A project that legitimately **doesn't need Docker** says so honestly (not a silent skip) | the adoption manifest records `N-A(reason)`, not an unexplained gap |

## Standard: Docs Lifecycle — current-state vs. dated history  
<sub>[`docs-lifecycle.md`](docs-lifecycle.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Current-state docs match reality (no phantom files/paths/claims) | `git grep` a recently-renamed identifier; open the critical-things doc |
| Dated history is **unedited** (no retro-rewrites of logs/changelog/decisions/reports) | `git log` on those files shows appends, not rewrites of old entries |
| Removed features are **bannered**, not silently dropped | look for historical-marker headers rather than deletions |
| Facts aren't **duplicated** across docs (single source + links) | spot-check a fact (version, project list) appears once and is linked |

## Standard: Engineering Quality  
<sub>[`engineering-quality.md`](engineering-quality.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| No hacks/temp-fixes/bad-fallbacks shipped in place of the correct solution | read recent changes; look for TODO-hack markers, silent workarounds |
| Features land **finished**, not rough-in ("later" is not a plan) | spot-check recent features for done-ness (built+reviewed+tested+documented) |
| Code is clean/modern/modular/focused | read a sample of recent modules |
| Public surfaces carry **doc-comments**; docs are current | grep for doc-comments; open the docs |
| Refactors came **with test updates**, not test rot | look at refactor commits for accompanying test changes |
| The project's **source-of-truth fidelity** is respected (only-what-was-asked changed) | for data/format stewards, confirm no incidental rewrites |
| **Ship contract:** the latest release held Scorecard ≥ 7.0, left no stale dep PRs / deprecation warnings / skipped tests, and every open PR is merged-or-closed-with-reason | OpenSSF Scorecard badge/score; `gh pr list`; CI warnings; the release notes |

## Standard: Farm Operating Model  
<sub>[`farm-operating-model.md`](farm-operating-model.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| The daily cadence **grows existing items**, not only adds new ones | session logs / changelog show deepening work, not just additions |
| Items carry **real content/craft**, no filler | open a few items; is there genuine value/craft |
| New items are **distinct**, not re-skins | compare recent additions against the existing set |
| Growth kept it **simple + polished** (nothing convoluted/unstable shipped) | use a recently-grown item; check the polish held |
| Items are **planned-first** and **self-contained** (one folder, relative paths) | look for the blueprint/concept + the folder shape |

## Standard: Git Workflow  
<sub>[`git-workflow.md`](git-workflow.md#verify-is-it-being-followed)</sub>

The check that catches a violation — run on request, report `done`/`partial`/`missing`
(the per-standard slice the [compliance audit](compliance.md) aggregates):

| Passes only when… | How to check |
|-------------------|--------------|
| Stable branch is **`main`**, not `master` | `git branch -a` |
| Every commit on `main` is a `--no-ff` **release merge** carrying a matching `vX.Y.Z` tag — no direct commits | `git log --first-parent --oneline main`; `git tag` |
| **`dev` contains `main`** — the back-merge ran after every release (no drift) | `git rev-list --count origin/dev..origin/main` is **0** |
| No content authored directly on `main` (release polish went via `dev`) | `git log --first-parent main` shows only release/hotfix merges, no stray docs/CI commits |
| On a branch-protected repo, releases went through a **PR merge** (`--merge`, not squash/rebase) | protected `main`; release merges are PR merge commits |
| Pushed history is intact — no force-push / rebase / reset of published commits | history stable across fetches; no `--force` in reflog |
| Spent `feature/`/`release/`/`hotfix/` branches deleted; `main`/`dev` intact | `git branch -a` |
| Each release to `main` rode a green build/test checkpoint (feature branches are not CI-tested — the `dev` merge is the gate) | release followed a green `dev` check |

## Standard: Legal Docs  
<sub>[`legal-docs.md`](legal-docs.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Privacy, Terms, and Cookies pages exist in-repo, self-hosted (not generator links) | `ls` the legal pages; confirm same-origin |
| Each page is **accurate to the code** — no clauses for accounts/cookies/tracking the app lacks | read the pages against the source |
| Pages carry a current **"Last updated"** date | open each page |
| A user-facing app links them from its **primary menu / the `Legal` subnav item** | look at the served app |
| Each page **covers the right content for its kind** (Privacy = what data/none; Terms = as-is/licence/coins-not-money; Cookies = cookies-vs-local-storage) with no boilerplate the project doesn't earn | read each page against the per-page breakdown |
| The **footer legal links come from the vendored chrome footer** (project/legal column), not a hand-built or restructured/derailed footer | diff the footer against [`chrome/footer.html`](docs-site/chrome/footer.html) |
| Defaults honored where applicable (18+ for adult content, honest no-cookies, processors named, third-party IP flagged/removed, project-owned contact) | read the pages |
| The **brand minimum** is met — the shared floor above, not less | check each brand-minimum item against the pages |
| The shared **reader prefs + coins** local storage is disclosed (Privacy + Cookies), coins stated as no-monetary-value, and the shared `/legal/coins/` explainer linked | read the pages |

## Standard: Maintenance Sweep  
<sub>[`maintenance-sweep.md`](maintenance-sweep.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| A **documented sweep procedure** exists (or the hub standard is adopted), not improvised each time | find the runbook / an adopted reference |
| Branch state is clean — only `main`/`dev` (+ deliberate keeps); no merged litter | `git branch -a --merged` shows nothing stray |
| `dev` **contains `main`** and both are green | `git rev-list --count origin/dev..origin/main` == 0 |
| Current-state docs match the code (version line, health numbers, no "pending" for shipped work) | open `status.md`/README against reality; `git grep` a stale token |
| Recent sweeps **surfaced** PRs/branches rather than auto-acting, and recorded real verify numbers | read the sweep's session log / process report |

## Standard: Owner Mandates Become Ledgers  
<sub>[`mandate-ledger.md`](mandate-ledger.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| A multi-part owner directive has a `notes/plans/<date>-mandate.md` with the words transcribed verbatim, one row per clause | open it; compare rows to the original message |
| Completion claims cite ledger rows, and the phase-end diff was against the owner's words (not the plan) | the report references rows + an original-message diff |
| Deferred/blocked rows carry an attempt log + retest trigger | scan `blocked` rows for evidence |
| Open `awaiting-owner` rows are re-presented first next session; a repeated mandate escalated them | the next session's opening action + the ledger history |
| No completion/release claim implied done-ness without a "rows remaining: N" count under an open mandate | the release/completion note states the remaining count |

## New Project Setup — bootstrapping a project into the mesh  
<sub>[`new-project-setup.md`](new-project-setup.md#verify-is-it-being-followed)</sub>

- `git status` is clean; `assets/references/` is untracked/ignored.
- `CLAUDE.md` opens to a real `notes/status.md`, **and actually contains the
  "Cross-project standards & checking the fairyfox system for updates" standing
  instruction** (confirm the text is present, not just that the file exists).
- `VERSION` reads a sane starting number.
- The project resolves in **both** registries (`hub/registry.yml` and
  `_data/projects.yml`) with matching `key` and `branch`, and the `_data/projects.yml` row
  has its **project details filled by default** (blurb/tags/lifecycle/version/activity/links/
  accent/icon) — blanks only on a recorded user exception; social/preview image exempt.
- The README opens with the **full 20-badge set in the canonical order** ([badges](badges.md)),
  each slot the right per-project source; omissions only on a recorded user exception.
- The docs site loads at `fairyfox.io/<key>/`, **wears the shared fairyfox chrome**
  (header, nav + submenu, footer) so it reads as a page of the site, with the
  **brand/Home link as the way home** and **no separate back-button** — **look at the
  served page**; default-theme JSDoc/Doxygen output, **any stack's default theme with only
  cosmetic changes (a couple colours + a footer mention)**, or a merely-resolving `docs:` URL
  is a miss. The coin + reader buttons are present, the subnav is not bare, and the tool's own
  default chrome (footer/header/sidebar/CSS) is removed, not left alongside. If the docs are
  generator-produced, theme the generator itself
  ([`docs-site/06`](docs-site/06-content-and-organization.md#generated-docs-doxygen-jsdoc-typedoc-sphinx-)).
  Bar: [`docs-site/08-compliance-checklist.md`](docs-site/08-compliance-checklist.md).
- The deploy target matches the project's kind (static → Pages on the shared domain;
  built app → Netlify) per the [deployment standard](deployment.md).
- If the project builds/serves, it builds green.
- `notes/fairyfox-reports/` exists and holds this run's setup report, committed —
  [process-reports standard](process-reports.md).

Don't report the project as fully set up unless every item above actually holds —
name any that don't rather than rounding up.

## Standard: The Living-Notes System  
<sub>[`notes-system.md`](notes-system.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| The `notes/` tree has its core pieces (`status.md`, `version.md`, `version/`, `sessions/`, `context/`) | `ls notes/` |
| `status.md` reads as **current-state only** and is actually current | open it; compare to recent work |
| The newest `sessions/` entry covers the latest substantive work | `ls notes/sessions/**` newest |
| Changelog entries ride **inside** their commit — no hash markers/bylines, no separate "update changelog" commits | scan `version/*.md`; `git log` for notes-only commits |
| `reference/adoption-manifest.md` exists with a row per hub standard; no row claims `implemented` without a recorded Verify run | open the manifest; spot-check an `implemented` row's evidence |
| Any `status.md` Health row asserting standards/compliance state links its evidence (manifest/audit), not a bare ✅ | open `status.md`; follow the Health links |

## Onboarding an Existing Project — folding an established repo into the mesh  
<sub>[`onboarding-existing-project.md`](onboarding-existing-project.md#verify-is-it-being-followed)</sub>

Run **every** row and report each with its real status — `done`, `partial`, or
**`missing`**. This same audit is what you run to check a project that's *claimed*
to be onboarded; finding `missing` rows means it isn't. This is the **join-time**
gate; the recurring whole-set check (every standard, re-runnable anytime) is the
[standards compliance audit](compliance.md).

> **If a setup/onboarding report from the same day already exists**, read it first
> (`notes/fairyfox-reports/YYYY-MM-DD-*.md`). A verification run right after setup
> shouldn't re-litigate caveats the setup report already recorded (e.g. a
> collection/monorepo shape, or a static project whose site doubles as its docs) —
> confirm against it rather than re-deriving.

| # | Dimension | Passes only when… |
|---|-----------|-------------------|
| 1 | Working tree | `git status` clean; `assets/references/` untracked/ignored; nothing pre-existing clobbered (README, license, CI, history intact). |
| 2 | Versioning | `VERSION` reflects the project's **real** version (not reset to `0.1.0`), SemVer-shaped. |
| 3 | Branch model | Stable branch is **`main`** (a repo on `master` has been renamed — mandatory; Pages/CI/URL refs fixed). Registry `branch` is the **sync-tracking work branch** (`dev` by default), honest — it is *not* the repo's default branch. |
| 4 | Notes system | The `notes/` tree exists with a real `status.md`; existing docs mapped in, not duplicated. |
| 5 | **Mesh-awareness in `CLAUDE.md`** | The project's `CLAUDE.md` **actually contains** the "Cross-project standards & checking the fairyfox system for updates" standing instruction. **Open the file and confirm the text is there** — don't infer it from the project being registered. |
| 6 | **Docs site is a page of fairyfox** | `fairyfox.io/<key>/` serves a site **wearing the shared fairyfox chrome** (header, nav + submenu, footer) so it reads as a page of the site, with the **brand/Home link as the way home** and **no separate back-button** (project-forward branding is fine). **Look at the actual page.** Default-theme JSDoc/Doxygen output — **or any stack's default theme with only cosmetic changes (a couple recoloured variables + one footer mention)** — or a `docs:` URL that merely resolves, is `missing`, not `partial`; the tool's own default chrome must be removed, not left meshing with the fairyfox chrome. Deploy target matches the project's kind ([deployment](deployment.md)). Bar: [`docs-site/08-compliance-checklist.md`](docs-site/08-compliance-checklist.md). |
| 7 | Hub registration | Resolves in **both** registries with honest `adopts_hub` / `notes_system` flags; node + docs pages present. The `_data/projects.yml` row has its **project details filled by default** (blurb/tags/lifecycle/version/activity/links/accent/icon) — blanks only on a recorded user exception; social/preview image exempt. |
| 7b | README badges | README opens with the **full 20-badge set in the canonical order** ([badges](badges.md)), each slot the right per-project source; any omission carries a recorded user exception (not an AI call). |
| 8 | Process report | `notes/fairyfox-reports/` holds this onboarding's report, committed, with the reconcile friction and an honest outcome — [process-reports](process-reports.md). |
| 9 | **Adoption manifest seeded + first compliance pass** | `notes/reference/adoption-manifest.md` exists with a row per hub standard (`implemented`/`copied-only`/`gap(due)`/`N-A(reason)`), and the full [compliance matrix](compliance.md) was run once — so the day-one state is a recorded per-standard result, and every not-yet-adopted standard is a **dated `gap`**, not prose. No `Standards adopted ✅` without backing rows ([checklists-are-contracts](checklists-are-contracts.md)). |

**Reporting rule:** only call a project **"fully onboarded" when rows 1–9 are all
`done`.** If any row is `partial` or `missing`, say exactly which — e.g. "registered
and noted, but the docs site is unthemed JSDoc and the `CLAUDE.md` mesh block is
missing." A clean hub-side registration is **not** a green light.

**Verifying under a held-release flow.** If you're committing on `dev` and holding
the release to `main` until an explicit go-ahead, the live `fairyfox.io/<key>/` page
can't change until that release. That's fine: **verify the built output locally**
(open it in a browser) on each iteration, then do **one live re-verification of the
served page after the release merge + Pages deploy.** Local-during, live-once-after —
Row 6 still gets its real look at a rendered page.

## Standard: Plan Before Execute  
<sub>[`planning.md`](planning.md#verify-is-it-being-followed)</sub>

- Substantive work (a multi-file change, a release-worthy feature, a standards pass)
  has a **written plan that predates the execution** — typically a file in
  `notes/plans/`, or an in-thread plan agreed before edits began.
- The plan **breaks the work into phases** across research / planning / implementation, with
  as many as the work needs — not one undifferentiated push. Research phases went to the
  primary source (local + online) rather than being skipped or rushed.
- The Default Workflow in the project's `CLAUDE.md` states plan-before-execute **and
  phase-by-default**.
- Trivial one-step changes are not gratuitously bureaucratized — the rule is applied
  with judgment, not as paperwork for its own sake.

## Standard: Process Reports — the system's feedback loop  
<sub>[`process-reports.md`](process-reports.md#verify-is-it-being-followed)</sub>

- The node has a `notes/fairyfox-reports/` folder and the run's report is in it,
  committed to the node's own tree (not left in `assets/references/`).
- The report names the procedure, the outcome, and the hub version/commit it ran
  against, and its friction/suggestions sections are real (not "all good").
- The front matter's `hub_version` is a **real version number** (usable as the next
  adoption's "last adopted" anchor), not a placeholder like "see VERSION at run time".
- A check that became an adopt in one session is a **single combined** report, not two.
- A **node → hub proposal** uses `procedure: propose-standard` with complete front
  matter (incl. `hub_version`/`hub_commit`) and restates the "a node never edits the
  hub" guardrail — no bare/frontmatter-less proposals.
- On the hub side, a review pass appended to `reports_through` (the digested-report
  **filename list**, not a date) only the reports it actually digested, and any
  standard changes it made are hub-side commits.
- The review pass **spot-checked each report's adoption/completion claims against the
  node's manifest + tree** (step 3a) and recorded any unbacked claim as a finding — a
  report is not digested by reading it alone.

## Standard: Repo Hygiene  
<sub>[`repo-hygiene.md`](repo-hygiene.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| A **broken-link gate** over the docs runs in the test gate + CI | find the check script; confirm it's in `npm test`/CI and fails on a dangling link |
| An **uncommitted-file guard** exists and is run before finishing | find the tidy check; confirm `git status` is clean of `??` non-ignored files |
| No **stranded useful files** (uncommitted reports/notes) | `git status` shows only gitignored junk untracked |
| Current-state docs were **swept on the last rename/removal** | `git grep` a recently-renamed identifier for stale prose; links resolve |
| **Branch auto-delete on** with the **work branch deletion-protected** | repo settings; `dev` survived the last release merge |
| The **machine-checkable invariants** (active-nav = Projects, `VERSION` == newest tag, subnav shape) are gated in CI, not left to review | find `check-standards.mjs`-style checks in CI |

## Standard: Research Capture — understanding lands in the notes  
<sub>[`research-capture.md`](research-capture.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Non-trivial findings have a **reference note** in `notes/reference/` (not only in a session log) | scan `notes/reference/`; does recent understanding have a home |
| Load-bearing conclusions were **verified against the real system**, with the probe committed | look for probe/verification scripts alongside the claims |
| Reference notes are **plain-English and teachable**, with the traps named | read one — could a newcomer learn the domain from it |
| A research pass that found a model bug **fixed it before building on top** | trace a recent finding → its fix → then the feature |
| New notes are **wired in** (map row, status, session log, plan link) | follow the links from a recent reference note |

## Standard: Self-Hosted Assets  
<sub>[`self-hosted-assets.md`](self-hosted-assets.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Fonts are **self-hosted**, not hot-linked | grep the built HTML/CSS for `fonts.googleapis.com` / `fonts.gstatic.com` — should be absent |
| No **third-party CDN hot-links** for the site's own assets | grep for `cdnjs`, `jsdelivr`, `unpkg`, etc. in the built output |
| The published site makes **no presentation request off-origin** | load it and watch the network panel — asset requests stay same-origin |
| Any real exception is **disclosed** in the legal pages + recorded with a remediation path | read Privacy/Cookies; find the exception note |

## Standard: Supply-Chain Hardening  
<sub>[`supply-chain-hardening.md`](supply-chain-hardening.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Every workflow has top-level `permissions: contents: read`; write is per-job | grep `.github/workflows/*.yml` for `permissions:` |
| Every `uses:` is SHA-pinned with a version comment; Dependabot `github-actions` on | inspect workflows; `.github/dependabot.yml` |
| Root `SECURITY.md` with a private reporting path exists | `ls SECURITY.md` |
| The release workflow attests build provenance **and attaches the `.intoto.jsonl` as a release asset** (attestation alone scores 0) | grep `release.yml` for `attest-build-provenance` + `gh-release … files:` |
| `main` is protected with the solo config **and its required-status-check contexts list the full CI suite** (not just `build`) | `gh api repos/OWNER/REPO/branches/main/protection` — check `required_status_checks.contexts` |
| SAST (CodeQL) is present and green on releases; the toolchain is pinned to the analyzer's supported range | `.github/workflows` for the SAST job; the toolchain pin + a comment tying it to the analyzer |
| Checked-in build wrappers/artifacts are validated (e.g. gradle wrapper-validation) | grep workflows for wrapper/artifact validation |
| Releases go through a PR merge (reconciled with git-workflow), not a blocked direct push | `git log --first-parent main`; branch-protection settings |
| Dependency-vuln findings are cleared or overridden with a reason | `npm audit` / OSV scan; `overrides` in `package.json` |
| The solo ceiling (~8) and badge lag are noted, not chased | the project's security notes acknowledge them |

## Standard: Testing & Verification  
<sub>[`testing.md`](testing.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Core/logic is **separated from rendering** and unit-tested headlessly | look for pure modules + their tests; confirm no DOM/canvas in the tested core |
| Tests are **multi-layer and real** (not one-or-two token asserts) | read the tests; do they cover state/boundaries/paths, not just construction |
| **Every recent bug fix** carries a regression test | spot-check fixed-bug commits for an accompanying failing-case test |
| The **full suite runs green before release**, each runtime covered | CI config + a recent green run; multi-runtime projects verify each |
| A **truth oracle** is used where one exists (console/reference/golden) | look for the parity/probe tests |
| **Visual changes are previewed** before shipping | the workflow/CLAUDE.md names it; evidence in session logs/PRs |
| A **coverage gate with a floor** (line ≥ 90% default) is wired into the build; overrides carry a reason | the build config's coverage-verify line (Kover/c8/gcov); any lowered floor has a note |
| A "can't test this" layer was **probed** (harness/source-read) before being parked | scan `⏳`/"untestable" layers for an attempt log or a shipped harness bridge |
| The release **gated the gates** — full local gate + doc-link + CI/SAST green on the PR | the release runbook/CLAUDE.md order; a recent release PR's checks |

## Standard: Versioning  
<sub>[`versioning.md`](versioning.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — report
`done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| `VERSION` is a single SemVer line | `cat VERSION` |
| It equals the newest `vX.Y.Z` tag on `main` | `git describe --tags --abbrev=0 main` vs `VERSION` |
| No version is hardcoded anywhere else | grep the tree for a stray `X.Y.Z` |
| MAJOR was never bumped without the project leader's call | tag history shows no auto `→ 1.0.0` |

## Standard: Working Rhythm — how an agent collaborates  
<sub>[`working-rhythm.md`](working-rhythm.md#verify-is-it-being-followed)</sub>

The per-standard slice the [compliance audit](compliance.md) aggregates — `done`/`partial`/`missing`:

| Passes only when… | How to check |
|-------------------|--------------|
| Multi-step work was **task-tracked** with a real breakdown, kept live | look at the task trail / session log for the work |
| Agent-driven runs stay **background/headless**; results are **surfaced** to look at | the workflow/CLAUDE.md names both halves; evidence in how work was presented |
| Features were **briefed before built** — no un-briefed neighbours absorbed into a phase | check recent features trace back to a brief/plan, not adjacency |
| Ambiguity was **raised up front**, not resolved by a guess later undone | look for asked-first moments; absence of large redo-because-unbriefed churn |

---

### No `## Verify` section (runbooks / meta — expected)

- `cross-project-sync.md`
