// Automated release screenshots (mandate 2026-07-22, clauses C1–C3, C6, C10).
//
// Boots a real Paper server with the built plugin, joins it with a Mineflayer "director"
// bot, builds each scene from the server console, points the camera, and captures a frame
// at the exact moment the subject is on screen. Completely background — no window, no
// human, no GPU required.
//
// TWO CAPTURE BACKENDS, selected by SCREENSHOT_ENGINE:
//
//   viewer  (default) prismarine-viewer's web renderer + headless Chrome. Pure Node +
//           Chromium (SwiftShader WebGL), so it runs anywhere CI runs and never needs an
//           X server or a GPU. Renders real block textures, entities and dropped items.
//           It does NOT render Minecraft's particle system — see the honesty note below.
//
//   client  a REAL Minecraft client under Xvfb (HeadlessMC/portablemc + x11grab). This is
//           the only way to photograph actual particle effects, because particles are
//           computed client-side from a network packet and no server-side renderer sees
//           them. Opt-in because it depends on a large external toolchain; when it fails
//           the harness falls back to `viewer` rather than failing the build.
//
// HONESTY NOTE (kept in the code so it can't drift out of the docs): a `viewer`-backend
// "particle effect" frame captures the *moment* the effect fires — the item vanishing into
// the container, at the tick the sound and particles are emitted — not the particle sprites
// themselves. Frames captured by the `client` backend are marked `engine: client` in
// manifest.json and are the ones that show real particles.
//
// usage: node screenshots.mjs <server-work-dir> <output-dir>
import { spawn } from "node:child_process";
import { mkdirSync, writeFileSync, statSync } from "node:fs";
import { resolve, join } from "node:path";
import process from "node:process";
import mineflayer from "mineflayer";
// bot.lookAt requires a real Vec3 — a plain {x,y,z} fails at runtime with
// "point.minus is not a function" (caught by the first green-ish screenshots CI run).
import { Vec3 } from "vec3";

const workDir = resolve(process.argv[2] ?? "smoke-screenshots");
const outDir = resolve(process.argv[3] ?? "screenshots");
const ENGINE = process.env.SCREENSHOT_ENGINE ?? "viewer";
const WIDTH = Number(process.env.SCREENSHOT_WIDTH ?? 1920);
const HEIGHT = Number(process.env.SCREENSHOT_HEIGHT ?? 1080);
const VIEWER_PORT = 3007;

mkdirSync(outDir, { recursive: true });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/**
 * Counts distinct colours actually drawn on the page's canvas.
 *
 * prismarine-viewer does not expose its `viewer` on `window`, so reaching for internals
 * told us nothing (the first probe reported `viewerPresent:false` while four mesher workers
 * were plainly running). Sampling the pixels asks the only question that matters — "is
 * anything drawn?" — without depending on the renderer's private shape at all.
 *
 * A rendered Minecraft scene has hundreds of distinct colours; a bare sky has one.
 */
async function inspect(page) {
  return page.evaluate(() => {
    const canvas = document.querySelector("canvas");
    if (!canvas) return { canvas: false };
    const w = 64;
    const h = 36;
    const scratch = document.createElement("canvas");
    scratch.width = w;
    scratch.height = h;
    const ctx = scratch.getContext("2d");
    let sampled = null;
    try {
      ctx.drawImage(canvas, 0, 0, w, h);
      const data = ctx.getImageData(0, 0, w, h).data;
      const colours = new Set();
      for (let i = 0; i < data.length; i += 4) {
        colours.add(`${data[i]},${data[i + 1]},${data[i + 2]}`);
      }
      sampled = { distinctColours: colours.size, first: [...colours][0] };
    } catch (err) {
      sampled = { error: String(err.message || err) };
    }
    return {
      canvas: true,
      size: `${canvas.width}x${canvas.height}`,
      workers: (window.__diag?.workers ?? []).length,
      errors: (window.__diag?.errors ?? []).slice(0, 6),
      ...sampled,
    };
  });
}

// ---------------------------------------------------------------------------------------
// Server lifecycle
// ---------------------------------------------------------------------------------------

writeFileSync(join(workDir, "eula.txt"), "eula=true\n");
writeFileSync(
  join(workDir, "server.properties"),
  [
    "level-type=minecraft\\:flat",
    // A flat world of pure grass photographs far better than the default dirt/bedrock
    // stripes, and keeps every scene reproducible frame to frame.
    'generator-settings={"layers":[{"block":"bedrock","height":1},{"block":"dirt","height":3},{"block":"grass_block","height":1}],"biome":"plains"}',
    "online-mode=false",
    "spawn-protection=0",
    "view-distance=8",
    "gamemode=creative",
    "difficulty=peaceful",
    "spawn-monsters=false",
    "do-daylight-cycle=false",
  ].join("\n") + "\n",
);

let server;
let bot;
let capture;
let done = false;
let log = "";
const manifest = [];

const deadline = setTimeout(() => {
  console.error("::error::screenshot run timed out");
  finish(1);
}, 900_000);

function finish(code) {
  if (done) return;
  done = true;
  clearTimeout(deadline);
  writeFileSync(join(outDir, "manifest.json"), JSON.stringify({ engine: ENGINE, scenes: manifest }, null, 2));
  Promise.resolve(capture?.close?.())
    .catch(() => {})
    .then(() => {
      try {
        bot?.quit();
      } catch { /* best effort */ }
      try {
        server?.stdin.write("stop\n");
      } catch { /* best effort */ }
      setTimeout(() => {
        try {
          server?.kill();
        } catch { /* best effort */ }
        process.exit(code);
      }, 8_000);
    });
}

function cmd(line) {
  server.stdin.write(line + "\n");
}

console.log(`Booting Paper for screenshots (engine=${ENGINE}, ${WIDTH}x${HEIGHT})…`);
server = spawn("java", ["-Xmx2G", "-jar", "paper.jar", "--nogui"], { cwd: workDir });
server.stdout.on("data", (chunk) => {
  const text = chunk.toString();
  log += text;
  process.stdout.write(text);
  if (!bot && /Done \(/.test(log)) startBot();
});
server.stderr.on("data", (c) => process.stderr.write(c.toString()));
server.on("exit", (code) => {
  if (!done) {
    console.error(`::error::server exited early (code ${code})`);
    finish(1);
  }
});

// ---------------------------------------------------------------------------------------
// Capture backends
// ---------------------------------------------------------------------------------------

/**
 * prismarine-viewer + headless Chrome. Serves the bot's view on localhost and screenshots
 * the canvas. Works with zero native dependencies beyond Chromium.
 */
async function viewerBackend(bot) {
  const { mineflayer: mineflayerViewer } = await import("prismarine-viewer");
  const puppeteer = (await import("puppeteer")).default;

  // prismarine-viewer pushes world data to the browser from `chunkColumnLoad` EVENTS. It
  // does not walk chunks the bot already has, so starting it after the world is loaded
  // leaves the browser with an empty scene — which renders as bare sky.
  //
  // Evidence (CI run 29948445257): chunks loaded, WebGL confirmed working
  // ("ANGLE … SwiftShader"), zero page errors, camera at eight different positions — and
  // all eight frames byte-identical. An empty scene is the only explanation left.
  //
  // So: start the viewer FIRST, then force fresh chunk events by walking the bot out of
  // its loaded set and back.
  mineflayerViewer(bot, { port: VIEWER_PORT, firstPerson: false, viewDistance: 6 });
  await sleep(2_000);

  try {
    await bot.waitForChunksToLoad();
    console.log("chunks loaded around the director");
  } catch (err) {
    console.warn(`::warning::waitForChunksToLoad failed: ${err.message}`);
  }
  await sleep(6_000); // let the mesher build the initial geometry

  const browser = await puppeteer.launch({
    headless: "new",
    args: [
      "--no-sandbox",
      "--disable-setuid-sandbox",
      // SwiftShader gives us real WebGL without a GPU — the whole point of this backend.
      "--use-gl=angle",
      "--use-angle=swiftshader",
      "--enable-unsafe-swiftshader",
      "--ignore-gpu-blocklist",
      "--enable-webgl",
      "--disable-dev-shm-usage",
      `--window-size=${WIDTH},${HEIGHT}`,
    ],
  });
  const page = await browser.newPage();
  // Forward the page's own diagnostics into the job log — without this a blank frame is
  // completely opaque, which cost a full CI cycle to discover.
  page.on("console", (m) => console.log(`[page:${m.type()}] ${m.text()}`));
  page.on("pageerror", (e) => console.warn(`::warning::[page error] ${e.message}`));
  page.on("requestfailed", (r) => console.warn(`::warning::[page request failed] ${r.url()}`));

  // prismarine-viewer builds chunk meshes inside a WEB WORKER. Errors thrown in a worker
  // do NOT reach page.on('pageerror'), which is exactly how we ended up with "zero errors,
  // empty scene". Install a collector before any page script runs so worker-adjacent
  // failures and unhandled rejections are captured too.
  await page.evaluateOnNewDocument(() => {
    window.__diag = { errors: [], workers: [] };
    window.addEventListener("error", (e) => window.__diag.errors.push(String(e.message || e)));
    window.addEventListener("unhandledrejection", (e) => window.__diag.errors.push("rejection: " + String(e.reason)));
    const RealWorker = window.Worker;
    window.Worker = function (...args) {
      const w = new RealWorker(...args);
      window.__diag.workers.push(String(args[0]));
      w.addEventListener("error", (e) => window.__diag.errors.push("worker: " + String(e.message || e)));
      return w;
    };
    window.Worker.prototype = RealWorker.prototype;
  });

  await page.setViewport({ width: WIDTH, height: HEIGHT, deviceScaleFactor: 1 });
  await page.goto(`http://127.0.0.1:${VIEWER_PORT}`, { waitUntil: "networkidle2", timeout: 60_000 });

  // Prove WebGL actually exists in this browser before blaming the world data.
  const webgl = await page.evaluate(() => {
    const c = document.createElement("canvas");
    const gl = c.getContext("webgl2") || c.getContext("webgl");
    if (!gl) return "none";
    const dbg = gl.getExtension("WEBGL_debug_renderer_info");
    return dbg ? String(gl.getParameter(dbg.UNMASKED_RENDERER_WEBGL)) : "available";
  });
  console.log(`WebGL renderer: ${webgl}`);

  await sleep(12_000); // texture atlas + world mesh upload under software GL is slow

  // Decisive probe: is the browser scene actually populated? This one number separates
  // "the viewer never got world data" from "it has data and isn't drawing it", and every
  // further fix is guesswork until it is known.
  console.log(`VIEWER DIAG ${JSON.stringify(await inspect(page))}`);

  return {
    engine: "viewer",
    async shot(file) {
      // Screenshot the canvas itself when we can find it: it rules out page chrome and
      // makes a blank result unambiguously "the scene is empty" rather than "wrong element".
      const canvas = await page.$("canvas");
      if (canvas) {
        await canvas.screenshot({ path: file, type: "png" });
      } else {
        await page.screenshot({ path: file, type: "png" });
      }
    },
    async close() {
      await browser.close();
    },
  };
}

/**
 * A real Minecraft client under Xvfb. Only this backend photographs actual particles.
 * Requires DISPLAY to already point at a running Xvfb and a launcher on PATH; the caller
 * (CI) sets that up. Frames are grabbed straight off the X display with ffmpeg.
 */
async function clientBackend() {
  const display = process.env.DISPLAY;
  if (!display) throw new Error("client backend needs DISPLAY (Xvfb) to be set");
  const launcher = process.env.MC_LAUNCHER; // e.g. "portablemc" or a HeadlessMC jar wrapper
  if (!launcher) throw new Error("client backend needs MC_LAUNCHER on PATH");

  const client = spawn(
    launcher,
    (process.env.MC_LAUNCHER_ARGS ?? "start 1.21.11 --username Camera --server 127.0.0.1 --server-port 25565").split(" "),
    { cwd: workDir, env: process.env },
  );
  client.stdout?.on("data", (c) => process.stdout.write(`[client] ${c}`));
  client.stderr?.on("data", (c) => process.stderr.write(`[client] ${c}`));
  await sleep(90_000); // launcher download + client start + world join

  return {
    engine: "client",
    async shot(file) {
      await new Promise((res, rej) => {
        const ff = spawn("ffmpeg", ["-y", "-f", "x11grab", "-video_size", `${WIDTH}x${HEIGHT}`, "-i", display, "-frames:v", "1", file]);
        ff.on("exit", (code) => (code === 0 ? res() : rej(new Error(`ffmpeg exited ${code}`))));
      });
    },
    async close() {
      client.kill();
    },
  };
}

async function makeCapture(bot) {
  if (ENGINE === "client") {
    try {
      return await clientBackend();
    } catch (err) {
      console.warn(`::warning::client backend unavailable (${err.message}); falling back to viewer`);
    }
  }
  return viewerBackend(bot);
}

/** Captures one frame and records it in the manifest. */
async function shot(name, caption) {
  const file = join(outDir, `${name}.png`);
  await capture.shot(file);
  const bytes = statSync(file).size;
  manifest.push({ name, file: `${name}.png`, caption, engine: capture.engine, bytes });
  const pos = bot?.entity?.position;
  console.log(
    `captured ${name}.png (${bytes} bytes, camera ${pos ? `${pos.x.toFixed(1)},${pos.y.toFixed(1)},${pos.z.toFixed(1)}` : "?"}) — ${caption}`,
  );
}

// ---------------------------------------------------------------------------------------
// Scene helpers
// ---------------------------------------------------------------------------------------

const ORIGIN = { x: 0, y: -59, z: 0 }; // flat-world surface for the generator above

/** Places a block from the console (the bot stays a camera, never a builder). */
function setBlock(x, y, z, block) {
  cmd(`setblock ${x} ${y} ${z} ${block}`);
}

/** Points the camera at a spot and lets the renderer settle. */
async function look(x, y, z, settleMs = 1_500) {
  await bot.lookAt(new Vec3(x, y, z), true);
  await sleep(settleMs);
}

/**
 * Teleports the camera to a vantage point and faces it at a target.
 *
 * Uses `/tp <x> <y> <z> <yaw> <pitch>` with the angles computed here rather than a
 * teleport followed by `bot.lookAt`: one server-side command sets position *and* rotation
 * atomically, so there is no window in which the frame could be taken mid-turn. The
 * resulting position is read back and logged, because a teleport whose Y silently fails to
 * apply is exactly the kind of thing that produces a photograph of the inside of a hill.
 */
async function camera(from, at) {
  const dx = at.x - from.x;
  const dy = at.y - from.y;
  const dz = at.z - from.z;
  const horizontal = Math.sqrt(dx * dx + dz * dz);
  const yaw = (-Math.atan2(dx, dz) * 180) / Math.PI;
  const pitch = (-Math.atan2(dy, horizontal) * 180) / Math.PI;

  cmd(`tp Director ${from.x} ${from.y} ${from.z} ${yaw.toFixed(1)} ${pitch.toFixed(1)}`);
  await sleep(1_800);

  const pos = bot?.entity?.position;
  if (pos && Math.abs(pos.y - from.y) > 1.5) {
    console.warn(
      `::warning::camera Y did not take: asked for ${from.y}, bot reports ${pos.y.toFixed(1)}`,
    );
  }
  await sleep(700);
}

// ---------------------------------------------------------------------------------------
// The scene plan — each entry is one published screenshot
// ---------------------------------------------------------------------------------------

async function sceneNetworkOverview() {
  const { x, y, z } = ORIGIN;
  // A tidy row of every relocation target the plugin understands.
  setBlock(x, y + 1, z, "minecraft:chest");
  setBlock(x + 2, y + 1, z, "minecraft:barrel");
  setBlock(x + 4, y + 1, z, "minecraft:furnace[lit=true]");
  setBlock(x + 6, y + 1, z, "minecraft:blast_furnace[lit=true]");
  setBlock(x + 8, y + 1, z, "minecraft:smoker[lit=true]");
  setBlock(x + 10, y + 1, z, "minecraft:hopper");
  cmd(`summon minecraft:armor_stand ${x + 12} ${y + 1} ${z} {ShowArms:1b}`);
  await sleep(2_000);

  await camera({ x: x + 6, y: y + 6, z: z + 12 }, { x: x + 6, y: y + 2, z });
  await shot("01-despawn-network", "A registered despawn network: chests, barrels, cookers, hoppers and entities all accept items that would otherwise be deleted.");
}

async function scenePileOfItems() {
  const { x, y, z } = ORIGIN;
  // A group of items on the ground, mid-life — the "group of items disappearing" shot.
  for (let i = 0; i < 40; i++) {
    const dx = (Math.random() * 6 - 3).toFixed(2);
    const dz = (Math.random() * 6 - 3).toFixed(2);
    const item = ["diamond", "iron_ingot", "gold_ingot", "emerald", "redstone", "lapis_lazuli"][i % 6];
    cmd(`summon minecraft:item ${Number(x) + Number(dx)} ${y + 2} ${Number(z) + Number(dz)} {Item:{id:"minecraft:${item}",count:${1 + (i % 8)}}}`);
  }
  await sleep(3_000);

  await camera({ x: x + 5, y: y + 4, z: z + 5 }, { x, y: y + 1, z });
  await shot("02-items-on-the-ground", "Forty dropped items waiting out their despawn timer — the moment before vanilla would delete every one of them.");
}

async function sceneGroupDespawn() {
  const { x, y, z } = ORIGIN;
  // Force the whole group through the pipeline at once and photograph the landing.
  cmd("despi despawn create-material-amount diamond 64");
  cmd("despi despawn create-material-amount emerald 64");
  cmd("despi despawn create-material-amount gold_ingot 64");
  await sleep(600); // land inside the effect's lifetime, not after it

  await camera({ x: x + 3, y: y + 3, z: z + 3 }, { x, y: y + 1, z });
  await shot("03-group-relocation", "A group of items despawning together — each one relocated into the network instead of deleted.");
}

async function sceneParticleEffect() {
  const { x, y, z } = ORIGIN;
  // Big, slow, bright particles so the effect is unmistakable in a still frame.
  cmd("despi effects set particle happy_villager");
  cmd("despi despawn create-material-amount diamond 32");
  // The effect emits for `particles.length-seconds`; half a second in is its visual peak.
  await sleep(500);

  await camera({ x: x + 2, y: y + 2.5, z: z + 2 }, { x, y: y + 1, z });
  await shot("04-particle-effect", "The landing effect: a sound and a particle burst mark exactly where a rescued item came to rest.");
  await sleep(400);
  await shot("05-particle-effect-close", "The same effect from close range — configurable particle, colour, radius, duration and sound.");
}

async function sceneRecycle() {
  const { x, y, z } = ORIGIN;
  cmd("give Director minecraft:diamond_block 16");
  await sleep(1_500);
  await camera({ x: x + 3, y: y + 2, z: z + 3 }, { x, y: y + 1, z });
  bot.chat("/recycle");
  await sleep(700);
  await shot("06-recycle", "/recycle: a player feeds the item in their hand straight into the network and earns a reward for it.");
}

async function sceneCatchAll() {
  const { x, y, z } = ORIGIN;
  const cx = x + 20;
  setBlock(cx, y + 1, z, "minecraft:chest");
  setBlock(cx + 1, y + 1, z, "minecraft:chest");
  setBlock(cx + 2, y + 1, z, "minecraft:chest");
  await sleep(1_500);
  await camera({ x: cx + 1, y: y + 4, z: z + 6 }, { x: cx + 1, y: y + 1, z });
  await shot("07-catch-all", "Catch-all containers: banned items and items lost to the void roll land here for staff to audit instead of ceasing to exist.");
}

async function sceneThrottle() {
  const { x, y, z } = ORIGIN;
  cmd("despi despawn create-material-amount cobblestone 64");
  await sleep(300);
  await camera({ x: x + 8, y: y + 8, z: z + 8 }, { x, y: y + 1, z });
  await shot("08-under-load", "Under load: the pipeline is throttled per server and per player, so a flood of despawning items is drained at a bounded rate instead of storming the server.");
}

// ---------------------------------------------------------------------------------------
// Director
// ---------------------------------------------------------------------------------------

const SCENES = [
  ["network overview", sceneNetworkOverview],
  ["items on the ground", scenePileOfItems],
  ["group relocation", sceneGroupDespawn],
  ["particle effect", sceneParticleEffect],
  ["recycle", sceneRecycle],
  ["catch-all", sceneCatchAll],
  ["under load", sceneThrottle],
];

async function run() {
  await sleep(2_000);
  cmd("op Director");
  cmd("gamerule doDaylightCycle false");
  cmd("time set noon");
  cmd("weather clear");
  cmd("gamemode spectator Director"); // a spectator camera never appears in its own shot
  await sleep(2_000);

  const { x, y, z } = ORIGIN;
  cmd(`tp Director ${x + 6} ${y + 6} ${z + 12}`);
  await sleep(1_500);

  capture = await makeCapture(bot);

  // Force a fresh set of chunkColumnLoad events now that the viewer is listening: walk
  // well outside the loaded set and come back. Without this the browser scene stays empty
  // (see the comment in viewerBackend).
  cmd(`tp Director ${x + 2000} ${y + 6} ${z + 2000}`);
  await sleep(5_000);
  cmd(`tp Director ${x + 6} ${y + 6} ${z + 12}`);
  await sleep(8_000);

  // Register a container as a real despawn location so relocation actually happens in the
  // photographed scenes. Guarded: a hiccup here must not cost us all seven scenes (it did
  // exactly that on the first CI run).
  try {
    setBlock(x, y + 1, z, "minecraft:chest");
    await sleep(800);
    cmd(`tp Director ${x} ${y + 3} ${z}`);
    await sleep(800);
    await bot.lookAt(new Vec3(x, y + 1, z), true);
    await sleep(500);
    bot.chat("/despi add this");
    await sleep(800);
  } catch (err) {
    console.warn(`::warning::scene setup failed (${err.message}); capturing anyway`);
  }

  let failures = 0;
  for (const [label, scene] of SCENES) {
    try {
      console.log(`--- scene: ${label}`);
      await scene();
    } catch (err) {
      failures++;
      console.warn(`::warning::scene "${label}" failed: ${err.message}`);
    }
  }

  if (manifest.length === 0) {
    console.error("::error::no screenshots were captured");
    finish(1);
    return;
  }

  // Blank-frame guard. The first working run produced eight files of *identical* byte
  // length — a flat sky with nothing rendered. "Files exist" is not "screenshots work",
  // so the harness now says so itself instead of shipping empty art.
  const distinct = new Set(manifest.map((s) => s.bytes));
  if (distinct.size === 1 && manifest.length > 1) {
    console.error(
      `::error::all ${manifest.length} frames are byte-identical (${[...distinct][0]} bytes) — the scene is almost certainly not rendering`,
    );
    console.log(`Captured ${manifest.length} screenshot(s); ${failures} scene(s) failed.`);
    finish(1);
    return;
  }

  console.log(`Captured ${manifest.length} screenshot(s); ${failures} scene(s) failed.`);
  finish(0);
}

function startBot() {
  console.log("Server up — joining with the director bot…");
  bot = mineflayer.createBot({ host: "127.0.0.1", port: 25565, username: "Director", auth: "offline" });
  bot.once("spawn", () =>
    run().catch((err) => {
      console.error(`::error::${err.message}`);
      finish(1);
    }),
  );
  bot.on("kicked", (reason) => {
    console.error(`::error::director kicked: ${reason}`);
    finish(1);
  });
  bot.on("error", (err) => {
    console.error(`::error::director error: ${err.message}`);
    finish(1);
  });
}
