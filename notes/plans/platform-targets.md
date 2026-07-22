# Platform targets — release matrix and roadmap

Owner directive, 2026-07-22 (second message): *"i guess target 1.21.x where possible, can you
target as many of these as possible"*, with a full survey of Purpur / Folia / Sponge / Velocity /
BungeeCord / Fabric / Quilt / NeoForge / Forge / Minestom / PocketMine / Cloudburst-Nukkit and a
recommended priority order.

This file is the standing answer. Ledger rows C20–C24 in
[`mandate-2026-07-22-screenshots-throttling-publishing.md`](mandate-2026-07-22-screenshots-throttling-publishing.md).

## The honest shape of the problem

The plugin is ~2 000 lines of Kotlin against the **Bukkit/Paper API**. Everything it does is
expressed in that API's vocabulary: `ItemDespawnEvent`, `Block.getState()`, `Container`,
`BukkitRunnable`, `World.getChunkAtAsync`. None of that vocabulary exists on Fabric, NeoForge,
Sponge, Minestom or Bedrock. So the targets divide cleanly into three groups, and pretending
otherwise is how projects end up with bug reports they cannot reproduce.

| Group | Targets | What shipping it costs |
|---|---|---|
| **A · Same jar, no code change** | Paper, **Purpur**, Spigot, Bukkit, other Paper forks | Declare + smoke-test. Effectively free. |
| **B · Same codebase, real work** | **Folia** | A scheduler/threading abstraction. Folia removes the single main thread; every `BukkitRunnable`, chunk load and block-state write must move to a region/entity scheduler. Est. 2–4 focused sessions incl. tests. |
| **C · Separate implementation** | Fabric, NeoForge, Quilt, Forge, Sponge, Velocity, Minestom, PocketMine, Cloudburst/Nukkit | A new module per platform with its own event model, inventory API, build, tests and CI. Only the *domain* (despawn policy, throttling, storage, config) is shareable, and only after it is extracted out of the Bukkit types it currently uses. |

## Decided release claim

> **Supported:** Paper 1.21.x, Purpur, and compatible Paper forks.
> **Planned:** Folia (group B).
> **Not supported:** hybrid Bukkit-on-Forge/Fabric servers (Arclight, Mohist, Magma, Cardboard,
> Banner). Bug reports are accepted when reproducible on a supported platform.

Adopted from the owner's own recommendation. Hybrids are explicitly excluded for the reasons the
owner listed (transformed internals, registry/mapping differences, event-ordering surprises).

## Status

| # | Target | Status | Notes |
|---|---|---|---|
| 1 | **Paper 1.21.x** | ✅ shipped | Build target; CI server-smoke on 1.21.11 + in-game Mineflayer acceptance. |
| 2 | **Purpur** | ✅ claimed, ⏳ unsmoked | Paper fork — the existing jar runs unmodified. **Next action:** add a Purpur row to the `server-smoke` matrix so the claim is *proven*, not assumed. Cheap; do it before advertising Purpur anywhere user-facing. |
| 3 | **Spigot / Bukkit** | ✅ claimed via `api-version: '1.21'` | Declared in the Modrinth/CurseForge `loaders` list. No Paper-only API is required for the core path. |
| 4 | **Folia** | ❌ not started — **highest-value next platform** | Needs `PlatformScheduler` (see below). Until then the plugin must NOT declare `folia-supported: true`: on Folia the current code would throw on its first `runTaskLater`. |
| 5 | **Fabric (client mod)** | 🟡 **written, unbuilt** | `client/fabric/` — a separate Loom build providing the container-screen button and options screen over the plugin's `papermc-despawned-items:targets` channel. Source complete; **not yet compiled** (Loom needs to resolve Minecraft + Yarn for the target version). This is a *companion* to the plugin, not a port of it. |
| 5b | **Fabric (server-side port)** | ❌ not started | A genuine port — `ServerTickEvents` + `ItemEntity` age, writing into `Container` block entities — so Fabric *servers* get the feature without Paper. Independent of 5. |
| 6 | **NeoForge** | ❌ not started | Preferred over Forge for current versions, per the owner's own ordering. |
| 7 | **Sponge** | ❌ not started | Independent API; smaller audience. Do on request. |
| 8 | **Velocity** | ❌ not started, and **not applicable as a port** | A proxy has no worlds, chunks or item entities. Only meaningful as an *optional companion* for cross-server shared storage (the MySQL backend already makes network-wide storage work without it). |
| 9 | **BungeeCord** | ❌ not planned | Superseded by Velocity; same non-applicability. |
| 10 | **Quilt** | ❌ not planned before Fabric | Cheap *after* Fabric exists; must be tested, not assumed. |
| 11 | **Forge** | ❌ not planned | Only for legacy modpack versions, which conflicts with the 1.21.x target. |
| 12 | **Minestom** | ❌ not planned | Vanilla mechanics aren't guaranteed; a survival-recovery plugin has little audience there. |
| 13 | **PocketMine-MP** | ❌ not planned | PHP, Bedrock — effectively a rewrite. |
| 14 | **Cloudburst/Nukkit** | ❌ not planned | JVM, but shares no platform code. |

## Client ↔ server, not "build targets" (owner, 2026-07-22)

The owner's reframing, adopted: think of this as **one product spanning client and server**,
not as a matrix of independent build targets. The plugin is the authority; the Fabric mod is a
face for it. They are versioned and released together.

**Does targeting Fabric 1.21.x put everything on one Mojang baseline?** Yes, in the sense that
matters: client and server on the same Minecraft version speak the same network protocol, and
the bridge is an ordinary vanilla custom-payload packet, so a Fabric 1.21.x client talks to a
Paper 1.21.x server natively. Two things it does *not* mean, both recorded in
`client/fabric/README.md`:

1. **No shared code.** Fabric compiles against the deobfuscated client via Yarn; the plugin
   compiles against the Bukkit API. Nothing can be passed between them but bytes — which is
   why the protocol is plain text rather than a shared class.
2. **`1.21.x` is one API for the plugin but not one protocol for the client.** The plugin runs
   across the whole line via `api-version: '1.21'`; a client can only join a server of its
   exact protocol version. So the mod is built per version — one line in
   `client/fabric/gradle.properties`.

## Group B — the Folia plan (next platform to actually build)

Folia replaces the single main thread with per-region threads. The concrete work:

1. **Introduce `PlatformScheduler`** (the owner's sketch, adopted verbatim in shape):
   `runGlobal` · `runAtLocation` · `runAtEntity` · `runAsync` · `runLaterAtLocation`.
2. **Two implementations** — `BukkitPlatformScheduler` (current behaviour) and
   `FoliaPlatformScheduler` (`Bukkit.getRegionScheduler()` / `getAsyncScheduler()`), selected at
   enable time by reflecting for `io.papermc.paper.threadedregions.RegionizedServer`. One jar,
   both platforms.
3. **Migrate every scheduling site**: `DespawnProcess.newLoop`, `DespawnScheduler.start`,
   `DespawnEffect`, `LocationManager`'s async persistence, `RemoveMaterials`.
4. **Audit shared mutable state.** This is the real risk for *this* plugin: `LocationStore`,
   `despawnProcesses`, `effectsPlaying`, and the new `ThrottleManager` maps are all plain
   `HashMap`/`ArrayList` reached from one thread today. Under Folia, items in different regions
   despawn concurrently and would touch all of them at once. They need to become concurrent
   structures, or every mutation needs to be funnelled onto a single owning scheduler.
5. **Declare `folia-supported: true`** in `plugin.yml` only after (4).
6. **CI**: add a Folia server-smoke job (Folia publishes jars via the same fill.papermc.io API)
   and extend the Mineflayer acceptance run to it.

**Do not skip step 4.** The throttle maps added in v1.5.0 make the plugin *more* stateful than it
was, so the concurrency audit got bigger, not smaller.

## Group C — the module layout, when we get there

Adopted from the owner's sketch, adjusted to this project's actual dependencies:

```text
papermc-despawned-items/
├── core/                 # NO Bukkit types: despawn policy, throttling, void/catch-all
│   │                     # rules, config model, storage (YAML/SQLite/MySQL), serialization
├── platform-paper/       # today's plugin, reduced to an adapter
├── platform-folia/       # or a flag inside platform-paper (preferred — one jar)
├── platform-fabric/
├── platform-neoforge/
└── distribution/         # the artifact bundle + publishing
```

The prerequisite for *any* group-C target is extracting `core/`. Today the domain logic is
interleaved with Bukkit types (`ItemStack`, `Material`, `Block`), so the extraction — not the
Fabric mod — is the first real task. Estimated: a full session on its own, with the existing
~200-test suite as the safety net.

## Publishing

`.github/workflows/release.yml` publishes to **Modrinth** and **CurseForge** (via `mc-publish`)
and to **Hangar** (PaperMC's own repository), each gated on its token secret so the workflow
stays green until the owner creates the projects. The declared `loaders` list is
`paper · purpur · spigot · bukkit` — deliberately *not* fabric/neoforge, because listing a loader
we cannot run on is a support-cost trap.

**Owner-side steps still required** (cannot be done from here):

1. Create the Modrinth project → set repo variable `MODRINTH_PROJECT_ID`, secret `MODRINTH_TOKEN`.
2. Create the CurseForge project → `CURSEFORGE_PROJECT_ID`, `CURSEFORGE_TOKEN`.
3. Create the Hangar project → secret `HANGAR_API_TOKEN`, then apply
   `io.papermc.hangar-publish-plugin` in `build.gradle.kts`.
4. Uncomment the Hangar/Modrinth usage badges in `README.md` once each project exists.
