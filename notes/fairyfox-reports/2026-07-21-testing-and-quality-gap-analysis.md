# Fairyfox report — testing & quality-gate gap analysis (2026-07-21)

**Project:** papermc-despawned-items · **Procedure:** owner-mandated coverage/quality
maximization (not a sync flow — a gap analysis the hub should read).
**Outcome:** coverage 44.2% → ~95% line (Kover-gated ≥90), 3 shipped bugs found by the
new tests, CodeQL SAST restored, Scorecard repo-side checks maxed.

## What was missing, and why it mattered

1. **"Tests gate the build" hid a 44% reality.** status.md said "✅ ~49 tests, gating
   build" — true, but the suite covered less than half the lines and almost none of the
   command, pipeline, or management layers. A green gate with no *coverage floor* let
   the healthy-looking number stand. The testing standard asks for "multi-layer and
   real" tests but names no measurable floor, so nothing forced the honest look.
2. **Three real bugs sat under the untested half** — all found within hours of writing
   the missing tests:
   - `/despi purge` **never purged containers** (the always-applicable Void strategy
     short-circuited the removal chain with a no-op) — a whole feature silently dead.
   - The `/recycle` reward pool could pick **non-item materials**
     (`DEAD_HORN_CORAL_WALL_FAN`) and crash the reward drop.
   - Entity purge-by-material **never cleared storage-minecart stacks** (exact-amount
     `ItemStack` matching).
   The pattern: every one lived in a layer with 0% coverage. Coverage isn't the point;
   *these* are the point — the metric is just the flashlight.
3. **SAST was dropped instead of held.** CodeQL was removed because Kotlin 2.4.10 was
   "too recent" for its extractor — the *toolchain* out-raced the *security gate* and
   the gate lost. Nothing in the standards said which should win. (Resolution: pin
   Kotlin to CodeQL's supported max — 2.4.0 today — and bump both together.)
4. **Workflow token hygiene drifted.** Two workflows carried top-level `write`
   permissions (docs: `pages`/`id-token`; release: `contents`) that belonged at job
   scope; no wrapper-validation step backed the checked-in gradle-wrapper.jar.
   Individually tiny; collectively they cost Scorecard points every week.
5. **A stale detekt-baseline reference** lingered in `build.gradle.kts` after the
   baseline file was removed — harmless, but it advertised parked findings that no
   longer existed.

## What went wrong, process-wise

- **The checklist existed and was honest** (`notes/plans/testing.md` correctly marked
  ⏳ everywhere) — but nothing *escalated* a page of ⏳ into a release blocker. The
  adoption manifest tracked standards, not the testing checklist's completion ratio.
- **Mock-framework friction was unexplored.** Three MockBukkit deviations (ray
  tracing, async chunks, container-state snapshots) made the hard layers *look*
  untestable, so they stayed untested. ~80 lines of harness (`TestSupport.kt`)
  dissolved all three. The lesson: "the mock can't do it" deserves an hour of probing
  before it becomes a ⏳.
- **Verification of the verifier was missing:** nothing measured the coverage number
  itself, so "tests gate the build" was unfalsifiable-by-glance.

## How to fix this for all projects (hub suggestions)

1. **Amend the testing standard with a measurable floor:** a coverage gate wired into
   the build (line ≥90% as the default; the tool is per-stack — Kover/c8/istanbul/
   gcov), plus the rule *"a feature without a test at its own layer is not done"*.
   The Verify table should ask for the gate's config line, not a vibe.
2. **Add a "SAST outlives toolchain bumps" rule to supply-chain-hardening:** language/
   compiler upgrades must not out-race the SAST analyzer's supported range; pin the
   toolchain at the analyzer's max and bump both together. Failing SAST is a release
   blocker, not a deferrable.
3. **Standardize workflow hygiene as a lintable list:** top-level `permissions:
   contents: read` on every workflow (elevate only at job scope), every action
   SHA-pinned, wrapper/artifact validation where the ecosystem has one. A copyable
   checklist row per workflow file would have caught the drift here.
4. **Make "gate the gates" explicit in the release runbook:** before any release —
   local full build (lint + static analysis + suite + coverage verify), doc-link gate,
   then CI + SAST green on the release PR; merges never happen on red/pending.
5. **Add a "probe the mock first" note to the testing standard:** when a mock
   framework seems to block a layer, spend a bounded probe (subclass seams, source
   reading) before parking the layer as untestable. Ship the harness bridges as a
   documented pattern (this repo's `TestSupport.kt` + `mockbukkit-harness.md` is a
   working example for JVM/Bukkit projects).

## What was rough

- MockBukkit's `ContainerStateMock.update()` silently reverting inventories cost the
  most wall-clock time; only reading MockBukkit's source settled it (probe tests gave
  contradictory-looking evidence first).
- PowerShell-over-MCP quoting mangles `-f` format strings and backtick escapes —
  writing throwaway `.ps1` files and running them with `-File` was the reliable path.
- Two mid-stream scope additions (feature + hardening) folded in cleanly because the
  plan file existed; worth keeping the plan-first rule even under interactive scope
  growth.

## Verification

- Before: 535/1211 lines (44.2%), branch 20.7%. After: ~1190/1244 (~95.6%), branch
  ~78.6%; `koverVerify` (min 90) now gates `check`/`build`.
- Full local gate green post-change: ktlint + detekt (no baseline) + ~200 tests +
  koverVerify + shadowJar, plus the doc-link gate before commit.
- CodeQL restored on Kotlin 2.4.0 (dev runs informational, release-PR run gating) —
  confirmed on the dev push for this change.
