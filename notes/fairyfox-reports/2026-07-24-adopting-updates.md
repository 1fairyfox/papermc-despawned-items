---
date: 2026-07-24
procedure: adopting-updates
node: papermc-despawned-items
outcome: completed
hub_version: 1.5.1
hub_commit: a6d7e68
---

# Process Report — adopting-updates, 2026-07-24

> A full, honest account of running a fairyfox system procedure. The point is to improve
> the system — so say what was rough even if the run succeeded. Standard:
> `notes/reference/process-reports.md`.

## Outcome in one line

Adopted the hub standards span 0.20.2 → 1.5.1 in full (3 releases: 0.21.0 / 1.4.0 / 1.5.0),
including a real, validated Docker local-first adoption that fixed a previously written-off
Testcontainers↔Docker incompat; paired full compliance pass followed.

## What was done

1. **Refreshed** the git-ignored hub mirror (`697bc5c → a6d7e68`, clean fast-forward). Hub
   VERSION `1.5.1`; last-adopted anchor `0.20.2` from the newest prior report.
2. **Scoped the diff off the new `hub/standards/CHANGELOG.md`** (not a file diff) across the
   span — 0.21.0 (checklists-are-contracts, mandate-ledger), 1.4.0 (complete/phase-by-default,
   20-badge set, docs-site enforcement), 1.5.0 (Docker). Read `authorizations.yml`: the
   standing `adopt-standards-by-default` grant pre-authorises the whole set → adopt-by-default,
   skip the confirmation pause, keep the verification floor.
3. **Applied**: refreshed 17 mirrored reference standards; vendored 5 new standards +
   VERIFY-INDEX; added the Docker mesh rule to CLAUDE.md. Verified via `git diff` that no
   project-specific content was clobbered.
4. **Docker** (1.5.0's headline, and the one genuinely new capability): confirmed Docker
   29.5.3 works locally, vendored `Dockerfile`/`compose.yaml`/`.dockerignore`, and validated a
   full green `./gradlew build` (12m33s) in-container with the Testcontainers MariaDB tests
   actually executing (3/3, previously skipped on the Windows host).
5. **Recorded**: changelog, session log, this report, manifest rows; ran the paired compliance
   pass.

Deviation from the runbook: the manifest's per-row Verify was done in the **paired compliance
pass** (the owner explicitly ordered "updates in-full THEN full compliance"), not inline in
adopt step 3a — the two were run back-to-back as one session, one combined record.

## What went well

- The **standards CHANGELOG.md** (new since our last anchor) is exactly the "what changed
  upstream" signal the runbook wants — reading it beat any file diff, and its per-release
  grouping mapped cleanly onto adopt decisions.
- The **`adopt-standards-by-default` ledger entry** removed all ambiguity about whether to
  wait — the whole span was pre-authorised.
- Most reference copies are **verbatim canonical** with no real local divergence, so a
  wholesale refresh + `git diff` review was safe and fast.
- Much of 0.21.0/1.4.0 was **already enshrined** in CLAUDE.md (checklists, mandate-ledger,
  phase-by-default, ship contract, full-CI gate, 20-badge README) — the adoption was mostly
  "file the reference copy + manifest row," confirming the node had kept pace informally.

## What went wrong / friction

- **Docker validation vs. the agent harness.** The MCP PowerShell tool kills the whole
  process tree at ~125 s, and even `Start-Process`-detached children were reaped when the
  launching call timed out. A 12-minute `./gradlew build` is impossible to run foreground.
  The working pattern was `docker compose up -d` (a **dockerd-owned** container that outlives
  the CLI/session), then poll `docker inspect` + `docker logs`. Worth capturing in the
  `docker` or `agent-tooling` standard as the canonical way to run a long containerised task
  from an agent with a short per-call timeout.
- **DooD networking gotcha** is real and undocumented in `docker.md`: a Testcontainers
  container publishes its port on the host daemon, so `getHost()` returns `localhost` (= the
  build container, not the host). The fix (`extra_hosts: host.docker.internal:host-gateway`
  \+ `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`) is non-obvious and cost a design
  pass to get right. `docker.md`'s "Windows↔Linux gotchas" list mentions CRLF/mounts/platform
  pins but **not** the Testcontainers-over-DooD host-override — the single most likely thing
  to bite a JVM node adopting this standard.
- **Version-anchor optics.** The hub jumped 0.x → 1.5.x during the span; the standards
  CHANGELOG "starts at 0.21.0" while the root VERSION reads 1.5.1, which momentarily looks
  like a mismatch. It reconciles (the CHANGELOG spans the releases), but a one-line note in
  the CHANGELOG header tying its top entry to the root VERSION would remove the double-take.

## Suggestions / feedback

- **`docker.md`**: add a short "running a long build in a container from an agent" note
  (dockerd-owned `up -d` + poll, not a foreground/`run` tied to a short-timeout shell), and
  add the **Testcontainers-over-DooD host-override** to the Windows↔Linux gotchas list with
  the exact `host-gateway` + `TESTCONTAINERS_HOST_OVERRIDE` recipe.
- **`CHANGELOG.md` header**: state "top entry corresponds to root VERSION X.Y.Z" so an
  adopter can sanity-check the anchor at a glance.

## Environment

Windows dev box, PowerShell via Windows-MCP (agent-tooling mesh rule; Cowork bash sandbox
avoided). JVM/Gradle/Kotlin Paper plugin (Gradle 9.6.1 wrapper, Kotlin 2.4.0, Paper API
1.21.11, Java 21). Docker Desktop 29.5.3 (Linux containers, compose v5.1.4). Arrived on `dev`,
clean, up to date with origin, mid-flight on an unreleased v1.5.0. Node already had the
express-auth machinery and most 0.21.0/1.4.0 behaviours in CLAUDE.md.
