---
date: 2026-07-24
procedure: compliance-audit
node: papermc-despawned-items
outcome: completed
hub_version: 1.5.1
hub_commit: a6d7e68
mode: full
---

# Process Report — full compliance audit, 2026-07-24

> Paired with the same-day [adopting-updates report](2026-07-24-adopting-updates.md): the owner
> ordered "updates in-full, THEN full compliance checks after updates are done." This is that
> second pass — every standard's `## Verify`, one recorded outcome per row (no bare ✅ over a
> set), against the node as it stands after the 0.20.2 → 1.5.1 adoption. Standard:
> `notes/reference/compliance.md`, mode **full**.

## Outcome in one line

24 of 26 applicable standards **done**; **docs-site** is **partial** (shared chrome 2.2.1 vs
hub 2.3.0 + the 1.4.0 web-interface enforcement not yet re-applied) and **maintenance-sweep**
remains **copied-only** (first sweep still never run). No release-blocking gap on `dev`.

## The matrix (every standard, per-row)

Evidence gathered live from the tree, git, `gh api`, and the in-container build.

| # | Standard | Result | Evidence / gap named |
|---|----------|--------|----------------------|
| 1 | git-workflow | **done** | `main` protected, `--no-ff` tagged PR releases; newest tag `v1.4.8`; `dev` clean + pushed (`593ab45`); history intact; no `master`. |
| 2 | versioning | **done** | `VERSION` = one SemVer line `1.5.0`, **ahead of newest `main` tag v1.4.8 — expected mid-flight on dev** (in-flight v1.5.0); nothing hardcoded. |
| 3 | notes-system | **done** | Full `notes/` tree; `status.md` swept to current today; inline changelog atop `version/2026-07.md`; session log added. |
| 4 | ai-context | **done** | Root `CLAUDE.md` carries all required pieces incl. the mesh block; now also the Docker Build/Run mesh rule. |
| 5 | cross-project-sync | **done** | Mirror read-only + git-ignored (`assets/references/` untracked); on-request only; `authorizations.yml` read from the clone; anti-recursion held. |
| 6 | process-reports | **done** | Two reports filed today (adopting-updates + this); `hub_version` anchor advanced 0.20.2 → 1.5.1. |
| 7 | checklists-are-contracts | **done** (new, filed) | Behaviour already in CLAUDE.md ("Checklists Are Contracts"); reference copy + manifest row now filed; this pass records per-item outcomes and discloses a not-done list. |
| 8 | mandate-ledger | **done** (new, filed) | CLAUDE.md "Owner Mandates Become Ledgers"; this session transcribed the owner directive + the mid-turn GitHub-details constraint as a recorded exception. |
| 9 | **docs-site** | **partial** | Vendored chrome **2.2.1 vs hub 2.3.0**; the 1.4.0 web-interface enforcement (mandatory base coin counter, firm subnav baseline, on-site Notes interface, whole-bundle-or-nothing incl. vendored font-awesome + fonts, Dokka adapter) not yet re-applied. Needs a chrome-bundle refresh + a fresh `08-compliance-checklist` pass **with a visual sign-off** (the mesh flags a rushed default-theme read as the failure mode). Scoped as the top follow-on. |
| 10 | deployment | **done** | Docs deploy = GitHub Pages on release to `main`; matches project kind (server plugin, no app). |
| 11 | planning | **done** | Work run as named phases (task list mirroring the adopting-updates runbook) + this written record; CLAUDE.md carries phase-by-default. |
| 12 | supply-chain-hardening | **done** (1 minor note) | SHA-pinned actions (0 unpinned); `SECURITY.md` present; provenance attached AS `.intoto.jsonl` release asset (release.yml); `main` protection contexts = **full CI suite** (build, CodeQL, both server smokes, Mineflayer), enforce_admins on, force/deletion off, linear off; SAST pinned to CodeQL's range. **Minor:** `scorecard.yml` uses top-level `permissions: read-all` (Scorecard's vendor-canonical form) rather than `contents: read` — optional tighten, not a gap. |
| 13 | dependencies | **done** | Dependabot `gradle` + `github-actions` → `dev`; local build gate; open-PR backlog empty (no stale majors). |
| 14 | docker | **done** (new) | Vendored `Dockerfile`/`compose.yaml`/`.dockerignore`; full `./gradlew build` **green in-container (12m33s)**; previously-skipped `MariaDbStorageTest` runs **3/3** over the mounted socket; CLAUDE.md carries the local-first rule. Prior write-off falsified + fixed. |
| 15 | legal-docs | **done** | Self-hosted `privacy`/`terms`/`cookies`/`index` under `docs-theme/pages/content/legal/`; accurate to code (verified 2026-07-21; no data-practice change since — Docker is dev tooling, nothing phones out); footer legal column not derailed. |
| 16 | coins | **done** (rides chrome refresh) | Counter served by the shared chrome; nothing gated on coins. The 1.4.0 "base counter mandatory" rule is satisfied by the present chrome; the 2.3.0 counter refresh rides the docs-site follow-on (#9). |
| 17 | agent-tooling | **done** | PowerShell + file tools used throughout (no Cowork bash sandbox); root `.gitattributes` (LF-normalized) present; no CRLF noise in the diff. |
| 18 | badges | **done** | README opens with the **full 20-badge set in canonical order** (contributors→license), flat-square, runtime Java/Paper before CI, distribution badges commented until published; docs badge → `fairyfox.io/papermc-despawned-items/`. No omission → no exception needed. |
| 19 | ci-secrets | **done** (new) | `gh secret list`: `SONAR_TOKEN`, `CODECOV_TOKEN`, `SCORECARD_TOKEN` all set; names canonical; each referenced by a workflow. |
| 20 | testing | **done** | ~200 tests across every layer; **coverage floor (Kover min 90) gates the build**; regression-per-fix practised; suite green (proven in-container this session); probe-the-mock lore in `mockbukkit-harness.md`. |
| 21 | engineering-quality | **done** | **0 TODO/FIXME across 87 `.kt` files**; no detekt baseline; ship contract state: Scorecard **7.6** (last read 2026-07-22 — re-verify at the actual release), tech debt clear, **PR backlog empty**. Ship-contract items re-run at release time. |
| 22 | repo-hygiene | **done** | `check-links` green (37 files OK) incl. the new reference files; tree clean post-commit (no stranded files); work-branch auto-delete + `main` protection. |
| 23 | docs-lifecycle | **done** | Current-state docs (status.md) swept to reality today; dated history (session logs, changelog) left unedited; single-source (VERSION). |
| 24 | research-capture | **done** | Findings landed in notes same session (session log, both reports, refreshed reference copies); the load-bearing Docker claim was **probed, not assumed** (real build + DB-test run). |
| 25 | working-rhythm | **done** | Multi-step work task-tracked in phases; the 12-min build backgrounded (dockerd-owned container) + surfaced; ask-first honoured (respected the mid-turn "don't touch GitHub details" constraint immediately). |
| 26 | self-hosted-assets | **done** (fonts ride chrome refresh) | Built site has no third-party asset hot-links (per prior audit); fonts follow the shared-chrome bundle. The 2.3.0 bundle's newly-vendored font-awesome + font woff2 files ride the docs-site follow-on (#9). |
| — | farm-operating-model | **N/A** | Server plugin, no farm tier. |
| — | new-project-setup | **N/A** | One-time runbook; project predates the mesh. |
| — | onboarding-existing-project | **N/A** (1 exception recorded) | Completed 2026-07-19. 1.4.0's "project details complete by default in `_data/projects.yml`" + hub registration are hub-side; the owner has **excepted GitHub repo description/topics** editing (2026-07-24) — recorded in the manifest, not a gap. |
| — | maintenance-sweep | **copied-only** | First whole-repo sweep **still never run** (pre-existing, not caused by this adoption). Due within a month of active work — recommend running one next. |

## Not done / read leniently / needs the owner's eyes (S9)

1. **docs-site chrome 2.2.1 → 2.3.0 (the one real pending adoption).** Delicate rendered-site
   work: re-vendor the whole 2.3.0 chrome bundle (4 HTML parts + `coins.js` + CSS + vendored
   fonts + font-awesome) into `docs-theme/`, apply the 1.4.0 web-interface rules (mandatory
   coin counter, firm subnav baseline, on-site Notes interface), then re-run the docs-site
   `08-compliance-checklist` **with a visual sign-off**. I deliberately did **not** rush this in
   the sync pass — a hasty chrome swap without the visual pass is exactly the "lenient read"
   failure the mesh recorded before. **Recommend as the next phase; will execute on your
   go-ahead.**
2. **maintenance-sweep never run.** Pre-existing `copied-only`. Recommend running the first
   full sweep soon.
3. **`scorecard.yml` top-level `read-all`.** Optional tighten to `contents: read` (Scorecard
   ships `read-all` by default; per-job already elevates). Cosmetic Scorecard-hygiene.
4. **v1.5.0 release to `main` — owner decision, intentionally held.** The adoption is committed
   to `dev`; the milestone release was left for you (see the adopting-updates report's release
   posture). It's a MINOR carrying substantial in-flight feature work beyond this sync, and
   `status.md` already parks "the release itself." Say the word and I'll cut it the git-flow
   way (release/1.5.0 → PR → full CI → tag → back-merge).
5. **GitHub repo description/topics — excepted by you (2026-07-24), not touched.** Recorded as a
   dated user exception in the manifest.

## Suggestions

Same two as the adopting-updates report: document the "long containerised build from a
short-timeout agent → dockerd-owned `up -d` + poll" pattern and the Testcontainers-over-DooD
host-override in `docker.md`.
