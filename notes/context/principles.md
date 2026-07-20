# Principles

Standing guardrails — what this project values and what to avoid.

## Values

- **Data integrity above all.** This plugin moves players' items; never duplicate and
  never silently lose them. Container writes and stack math are covered by tests; the
  duplication invariant (`in == out + consumed`) is a first-class concern.
- **Scale without lag.** Bounded per-tick work, O(1) lookups, incremental off-thread
  persistence. A change that regresses the store to O(n) should fail a performance test.
- **Fail safe, never crash the server.** Bad config, malformed data, unloaded worlds, and
  unsupported particles degrade gracefully with a warning — not an exception mid-tick.
- **Approachable for admins.** Sensible defaults, clear config with comments, permission
  nodes that work with LuckPerms groups, and a live `/despi reload`.
- **Tested and honest.** Follow the testing standard in `../plans/testing.md`; every
  fixed bug gets a permanent regression test.

## What to avoid

- No hacks or "temporary" fixes; no secrets in the repo.
- No global mutable static state that captures a plugin instance across reloads (this bit
  us once — strategies and the location store are instance-scoped now).
- No blocking I/O on the main thread — persistence flushes off-thread; DB work uses the
  pool.
- No unparameterised SQL, ever (injection safety).
- Don't claim Folia support until it's actually tested (scheduling seam is kept, but the
  claim isn't made).
