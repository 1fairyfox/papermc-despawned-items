# PaperMC Despawned Items — Fabric client mod

Adds a button to the chest / furnace / barrel screen for marking a container as a despawn
target, and a full options screen behind it. Client-side only.

## "Can I just target Fabric 1.21.x so everything is on the same Mojang baseline?"

**Yes — that logic is right**, with one nuance worth knowing.

What client and server actually share is the **network protocol**, and the protocol is
determined by the Minecraft version. A Fabric client and a Paper server on the same version
speak the same wire format, and this mod's channel
(`papermc-despawned-items:targets`) is an ordinary vanilla *custom payload* packet. So a
Fabric 1.21.x client talks to a Paper 1.21.x server natively, with nothing special in between.

The nuance is what "same baseline" does **not** mean:

- **They share no code.** A Fabric mod compiles against the deobfuscated *client* jar via Yarn
  mappings; a Paper plugin compiles against the Bukkit API. There is no common type to pass
  around — which is exactly why the bridge is plain text rather than a shared class.
- **"1.21.x" is one API for the plugin, but not one protocol for the client.** The plugin
  declares `api-version: '1.21'` and runs unmodified across the whole line. A *client*,
  though, can only connect to a server of its exact protocol version — 1.21.1, 1.21.4 and
  1.21.11 are not interchangeable. That is vanilla behaviour, not something this mod adds.

So the practical rule is: **build the mod for the same exact version the server runs.** That is
one line — `minecraft_version` in `gradle.properties` — and it is why the version lives there
rather than being hard-coded.

## Installing it is always safe

The mod sends exactly one packet when you join, and then waits:

| Server | What happens |
|---|---|
| No plugin | Nothing answers. No button is ever drawn. |
| Plugin, client mods switched off (`targets.client-mod.enabled: false`) | Server replies `UNAVAILABLE`. No button is drawn. |
| Plugin, but you lack `despi.client` | Server replies `UNAVAILABLE` with the reason. No button is drawn. |
| Plugin, allowed | Server replies `WELCOME` with your capabilities. The button appears. |

It cannot make the game do anything you could not already do by typing `/despi`. Every
request is re-validated server-side — permission, reach, ownership, and your location limit.

## Using it

- **Click the button** — marks an unmarked container, or toggles a marked one on/off.
- **Shift-click** — opens the options screen: on/off, priority (1–10), whether the target
  accepts banned items, and remove.

Switching a target **off** keeps its registration and settings and just makes the pipeline
skip it. Use *Remove* when you actually mean to unregister.

## Building

```sh
cd client/fabric
./gradlew build      # → build/libs/papermc-despawned-items-client-<version>.jar
./gradlew runClient  # launch a dev client with the mod loaded
```

This is a **separate Gradle build** from the plugin, on purpose: Fabric Loom rewrites the
whole toolchain around a deobfuscated Minecraft client, and folding that into the plugin's
build would mean a Loom or mappings problem could break the plugin's release.

## Why the button lives here and not in the plugin

A container screen has exactly as many slots as the container has, and they are all real
storage. A server plugin cannot add a widget to one — the only server-side way to fake it is
to replace the entire screen with a custom inventory, which risks losing or duplicating the
player's items and fights every storage and sorting mod on the server.

A client mod adds a button to the existing screen and never touches the slots. So: **the
client draws and clicks, the server owns the truth.**
