// In-game client acceptance smoke (testing.md §83–87): boot a real Paper server with
// the built plugin, join it with a Mineflayer bot (a real Minecraft client on the
// wire), run plugin commands as a player, and assert the plugin's replies reach the
// client. This is the end-to-end layer no mock can prove: Kotlin → Paper → protocol →
// client and back.
//
// usage: node ingame-smoke.mjs <server-work-dir>
//   The dir must already contain paper.jar + plugins/<plugin>.jar (server-smoke.sh
//   layout — run it first or prepare the dir the same way; this script writes its own
//   eula/server.properties and boots its own server instance).
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
}, 300_000);

let server;
let bot;
let done = false;

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
  }, 5_000);
}

console.log("Booting Paper server…");
server = spawn("java", ["-jar", "paper.jar", "--nogui"], { cwd: dir });
let log = "";
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

function startBot() {
  console.log("Server up — joining with the Mineflayer bot…");
  bot = mineflayer.createBot({ host: "127.0.0.1", port: 25565, username: "SmokeBot", auth: "offline" });

  const expectations = [
    // /recycle with an empty hand: full client→plugin→client roundtrip, no perms needed.
    { command: "/recycle", expect: /nothing in your hand/i },
    // /despi is visible to ordinary players; exists check answers politely.
    { command: "/despi exists anywhere owned-by-me", expect: /no location was found/i },
  ];
  let current = -1;

  function next() {
    current += 1;
    if (current >= expectations.length) {
      console.log("In-game smoke OK — all expected plugin replies reached the client.");
      cleanup(0);
      return;
    }
    console.log(`> ${expectations[current].command}`);
    bot.chat(expectations[current].command);
  }

  bot.on("message", (jsonMsg) => {
    const text = jsonMsg.toString();
    console.log(`[chat→bot] ${text}`);
    if (current >= 0 && expectations[current].expect.test(text)) next();
  });

  bot.once("spawn", () => setTimeout(next, 2_000));
  bot.on("kicked", (reason) => {
    console.error(`::error::bot kicked: ${reason}`);
    cleanup(1);
  });
  bot.on("error", (err) => {
    console.error(`::error::bot error: ${err.message}`);
    cleanup(1);
  });
}
