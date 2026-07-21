// In-game client acceptance + load/profiling smoke (testing.md §83–87, §65–73):
// boots a real Paper server (with a Java Flight Recorder capture), joins it with a
// Mineflayer bot — a real Minecraft client on the wire — and walks the permission
// story end to end: denied while unprivileged, opped via console, registering a real
// looked-at block, recycling a really-held item. Then a load phase storms the
// despawn pipeline from console while the bot keeps commanding, and samples spark
// MSPT numbers so CI logs carry real performance data. The JFR profile is written to
// <workdir>/profile.jfr for artifact upload.
//
// usage: node ingame-smoke.mjs <server-work-dir>
import { spawn } from "node:child_process";
import { writeFileSync } from "node:fs";
import { resolve } from "node:path";
import process from "node:process";
import mineflayer from "mineflayer";

const workDir = process.argv[2];
if (!workDir) {
  console.error("usage: node ingame-smoke.mjs <server-work-dir>");
  process.exit(2);
}

const dir = resolve(workDir);
writeFileSync(resolve(dir, "eula.txt"), "eula=true\n");
writeFileSync(
  resolve(dir, "server.properties"),
  ["level-type=minecraft\\:flat", "online-mode=false", "spawn-protection=0", "view-distance=4"].join("\n") + "\n",
);

const deadline = setTimeout(() => {
  console.error("::error::in-game smoke timed out");
  cleanup(1);
}, 420_000);

let server;
let bot;
let done = false;
let log = "";

function cleanup(code) {
  if (done) return;
  done = true;
  clearTimeout(deadline);
  try {
    bot?.quit();
  } catch {}
  try {
    server?.stdin.write("stop\n");
  } catch {}
  setTimeout(() => {
    try {
      server?.kill();
    } catch {}
    process.exit(code);
  }, 8_000);
}

function consoleCmd(cmd) {
  server.stdin.write(cmd + "\n");
}

console.log("Booting Paper server (with JFR recording)…");
server = spawn(
  "java",
  ["-XX:StartFlightRecording=filename=profile.jfr,dumponexit=true,settings=profile", "-jar", "paper.jar", "--nogui"],
  { cwd: dir },
);
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
    cleanup(1);
  }
});

/** Waits until the bot has received a chat line matching re (or times out). */
function expectChat(re, label, timeoutMs = 15_000) {
  return new Promise((resolveP, rejectP) => {
    const timer = setTimeout(() => rejectP(new Error(`timeout waiting for ${label} (${re})`)), timeoutMs);
    const handler = (jsonMsg) => {
      const text = jsonMsg.toString();
      if (re.test(text)) {
        clearTimeout(timer);
        bot.removeListener("message", handler);
        resolveP(text);
      }
    };
    bot.on("message", handler);
  });
}

/** Asserts a pattern does NOT appear within windowMs of running fn. */
async function expectSilence(re, label, fn, windowMs = 3_000) {
  let seen = null;
  const handler = (jsonMsg) => {
    const text = jsonMsg.toString();
    if (re.test(text)) seen = text;
  };
  bot.on("message", handler);
  fn();
  await sleep(windowMs);
  bot.removeListener("message", handler);
  if (seen) throw new Error(`${label}: expected denial but saw: ${seen}`);
  console.log(`OK (denied): ${label}`);
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function run() {
  bot.on("message", (m) => console.log(`[chat→bot] ${m.toString()}`));

  await sleep(2_000);

  // 1 · Permission denial while unprivileged: elevated output must never arrive.
  await expectSilence(/Locations:/, "unprivileged /despi locations count", () =>
    bot.chat("/despi locations count"),
  );

  // 2 · Ordinary permissions work out of the box.
  bot.chat("/recycle");
  await expectChat(/nothing in your hand/i, "empty-hand /recycle");
  bot.chat("/despi exists anywhere owned-by-me");
  await expectChat(/no location was found/i, "exists anywhere (none yet)");

  // 3 · Console op grant unlocks the elevated tree — verified from the client side.
  consoleCmd("op SmokeBot");
  await sleep(1_500);
  bot.chat("/despi locations count");
  await expectChat(/Locations: \d+/, "opped /despi locations count");

  // 4 · Register the block the bot is REALLY looking at (server-side ray trace).
  const below = bot.entity.position.offset(0, -1, 1);
  await bot.lookAt(below, true);
  await sleep(500);
  bot.chat("/despi add this");
  await expectChat(/Successfully added location!/, "/despi add this");
  bot.chat("/despi locations mine");
  await expectChat(/location\(s\) were found/i, "/despi locations mine");

  // 5 · A really-held item recycles: console gives it, pickup is automatic.
  consoleCmd("give SmokeBot minecraft:cobblestone 1");
  await sleep(2_500);
  bot.chat("/recycle");
  await expectChat(/Done!/, "/recycle with item in hand");

  // 6 · LOAD PHASE: storm the pipeline from console while the bot keeps commanding.
  console.log("LOAD: creating 200 forced despawns of 64 dirt each while the bot stays chatty…");
  for (let i = 0; i < 200; i++) consoleCmd("despi despawn create-material-amount dirt 64");
  for (let i = 0; i < 10; i++) {
    bot.chat("/despi despawn count-ongoing");
    await expectChat(/Despawns:/, `mid-load count #${i}`);
    await sleep(400);
  }

  // 7 · Real performance numbers from spark (bundled with Paper).
  const before = log.length;
  consoleCmd("spark tps");
  await sleep(4_000);
  consoleCmd("spark health");
  await sleep(4_000);
  const sparkOut = log
    .slice(before)
    .split("\n")
    .filter((l) => /TPS|MSPT|Tick durations|CPU usage|Memory usage|[0-9]+\.[0-9]+,/i.test(l));
  for (const line of sparkOut) console.log(`PROFILE ${line.trim()}`);

  // 8 · The server must still be responsive after the storm.
  bot.chat("/despi locations count");
  await expectChat(/Locations: \d+/, "post-load responsiveness");

  console.log("In-game smoke OK — permissions, gameplay, load and profiling all verified.");
  cleanup(0);
}

function startBot() {
  console.log("Server up — joining with the Mineflayer bot…");
  bot = mineflayer.createBot({ host: "127.0.0.1", port: 25565, username: "SmokeBot", auth: "offline" });
  bot.once("spawn", () =>
    run().catch((err) => {
      console.error(`::error::${err.message}`);
      cleanup(1);
    }),
  );
  bot.on("kicked", (reason) => {
    console.error(`::error::bot kicked: ${reason}`);
    cleanup(1);
  });
  bot.on("error", (err) => {
    console.error(`::error::bot error: ${err.message}`);
    cleanup(1);
  });
}
