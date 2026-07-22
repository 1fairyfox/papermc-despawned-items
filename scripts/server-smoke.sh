#!/usr/bin/env bash
# Headless real-server smoke (testing.md §19–22): boot a Paper server with the built
# plugin jar and prove it enables cleanly — no load errors, no stack traces, no
# self-disable. Used by CI for 1.21.11 (the target), 26.x (forward-compat), and the
# two rolling channels below.
#
# usage: server-smoke.sh <mc-version> <plugin-jar-glob>
#   <mc-version> may be a concrete version (e.g. 1.21.11) OR one of two rolling tokens
#   resolved live from fill.papermc.io:
#     latest-stable        — newest Paper version whose latest build channel is STABLE
#     latest-experimental  — newest Paper version overall (a dev/beta build)
set -euo pipefail

MC_VERSION="${1:?usage: server-smoke.sh <mc-version> <plugin-jar>}"
PLUGIN_JAR="${2:?usage: server-smoke.sh <mc-version> <plugin-jar>}"

UA="papermc-despawned-items-ci/smoke"
API="https://fill.papermc.io/v3/projects/paper"

# Resolve a rolling channel token to a concrete version. Versions come newest-first
# (the API orders major groups and their patch arrays newest-first; jq preserves order).
resolve_channel() {
  local mode="$1" versions v ch
  versions="$(curl -fsSL -H "User-Agent: $UA" "$API" | jq -r '.versions | to_entries | map(.value[]) | .[]')"
  if [ "$mode" = "experimental" ]; then
    printf '%s\n' "$versions" | head -1
    return 0
  fi
  # stable: first version whose latest build is on the STABLE channel
  while IFS= read -r v; do
    [ -n "$v" ] || continue
    ch="$(curl -fsSL -H "User-Agent: $UA" "$API/versions/$v/builds/latest" | jq -r '.channel')"
    if [ "$ch" = "STABLE" ]; then printf '%s\n' "$v"; return 0; fi
  done <<< "$versions"
  echo "::error::could not resolve a STABLE Paper version" >&2
  return 1
}

case "$MC_VERSION" in
  latest-stable)       MC_VERSION="$(resolve_channel stable)" ;;
  latest-experimental) MC_VERSION="$(resolve_channel experimental)" ;;
esac
echo "Target Paper version: $MC_VERSION"
WORK="smoke-${MC_VERSION}"

mkdir -p "$WORK/plugins"
echo "Resolving Paper $MC_VERSION from fill.papermc.io…"
URL="$(curl -fsSL "https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds/latest" | jq -r '.downloads["server:default"].url')"
curl -fsSL -o "$WORK/paper.jar" "$URL"
cp $PLUGIN_JAR "$WORK/plugins/"

cd "$WORK"
echo "eula=true" > eula.txt
{
  echo "level-type=minecraft\\:flat"
  echo "online-mode=false"
  echo "spawn-protection=0"
  echo "view-distance=4"
} > server.properties

java -jar paper.jar --nogui > server-out.log 2>&1 &
PID=$!

BOOTED=1
for _ in $(seq 1 150); do
  if grep -q 'Done (' server-out.log 2>/dev/null; then BOOTED=0; break; fi
  if ! kill -0 "$PID" 2>/dev/null; then break; fi
  sleep 2
done

echo "──── server log ────"
cat server-out.log
echo "────────────────────"

fail() {
  echo "::error::$1"
  kill "$PID" 2>/dev/null || true
  exit 1
}

[ $BOOTED -eq 0 ] || fail "server never reached 'Done' on $MC_VERSION"
grep -q 'Enabling papermc-despawned-items' server-out.log || fail "plugin was never enabled"
grep -q '\[papermc-despawned-items\] Enabled' server-out.log || fail "plugin did not log its Enabled line"
if grep -qiE 'Could not load plugin|Error occurred while enabling|Disabling papermc-despawned-items' server-out.log; then
  fail "plugin errored during enable on $MC_VERSION"
fi

kill "$PID" 2>/dev/null || true
wait "$PID" 2>/dev/null || true
echo "Server smoke OK on Paper $MC_VERSION"
