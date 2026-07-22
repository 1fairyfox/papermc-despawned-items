<#
.SYNOPSIS
  Spin up a local, real Paper server running the freshly-built plugin in YAML storage
  mode, so you can connect your own Minecraft client and hand-test before a ship.

.DESCRIPTION
  OPTIONAL last-stage manual test (see CLAUDE.md "Optional pre-ship playtest"). It:
    1. builds the plugin jar (unless -NoBuild),
    2. downloads a Paper server jar for the target MC version (fill.papermc.io) if absent,
    3. sets up run/playtest/ with eula=true, online-mode=false (so you can join localhost
       without auth), the plugin in plugins/, and storage.type: yaml (the plugin default),
    4. starts the server with --nogui.
  Then open Minecraft (same major version) → Multiplayer → Direct Connect → localhost.
  Everything lives under run/ (git-ignored); delete it anytime. Stop the server with `stop`
  in its console (or Ctrl+C).

.PARAMETER McVersion   Target Minecraft/Paper version. Default 1.21.11 (the build target).
.PARAMETER NoBuild     Skip ./gradlew build and use the newest jar already in build/libs.
.PARAMETER Port        Server port. Default 25565.

.EXAMPLE
  pwsh scripts/local-playtest.ps1
  pwsh scripts/local-playtest.ps1 -NoBuild -Port 25566
#>
[CmdletBinding()]
param(
  [string]$McVersion = "1.21.11",
  [switch]$NoBuild,
  [int]$Port = 25565
)
$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo
$run = Join-Path $repo "run/playtest"

# --- Locate a Java 21+ runtime (Paper 1.21 needs 21; 26.x needs 25) ---
function Get-Java {
  $cands = @()
  if ($env:JAVA_HOME) { $cands += (Join-Path $env:JAVA_HOME "bin/java.exe") }
  $cands += "java"
  foreach ($j in $cands) {
    try {
      $v = & $j -version 2>&1 | Out-String
      if ($v -match 'version "(\d+)') { if ([int]$Matches[1] -ge 21) { return $j } }
    } catch { continue }  # candidate not runnable — try the next one
  }
  throw "No Java 21+ found. Set JAVA_HOME to a JDK 21 (Temurin) or add java to PATH."
}

# --- 1. Build the plugin (unless skipped) ---
if (-not $NoBuild) {
  Write-Host "==> Building the plugin jar..." -ForegroundColor Cyan
  & "$repo/gradlew.bat" build --no-daemon | Write-Host
  if ($LASTEXITCODE -ne 0) { throw "gradle build failed" }
}
$jar = Get-ChildItem "$repo/build/libs/papermc-despawned-items-*.jar" |
  Where-Object { $_.Name -notmatch '-(jmh|sources|javadoc)\.jar$' } |
  Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw "No plugin jar in build/libs — run without -NoBuild." }
Write-Host "==> Using plugin jar: $($jar.Name)" -ForegroundColor Green

# --- 2. Resolve + download the Paper server jar ---
New-Item -ItemType Directory -Force -Path "$run/plugins" | Out-Null
$paper = Join-Path $run "paper.jar"
if (-not (Test-Path $paper)) {
  Write-Host "==> Resolving Paper $McVersion from fill.papermc.io..." -ForegroundColor Cyan
  $meta = Invoke-RestMethod "https://fill.papermc.io/v3/projects/paper/versions/$McVersion/builds/latest" -Headers @{ "User-Agent" = "papermc-despawned-items-playtest" }
  $url  = $meta.downloads.'server:default'.url
  Write-Host "==> Downloading $url"
  Invoke-WebRequest -Uri $url -OutFile $paper
}

# --- 3. Server config: EULA, offline localhost, plugin in YAML mode ---
Set-Content "$run/eula.txt" "eula=true"
@"
online-mode=false
server-port=$Port
motd=PaperMC Despawned Items - local playtest (YAML)
level-name=world
spawn-protection=0
"@ | Set-Content "$run/server.properties"
Copy-Item $jar.FullName (Join-Path $run "plugins/$($jar.Name)") -Force
# Force YAML storage explicitly (it is the plugin default, but be unambiguous).
$cfgDir = Join-Path $run "plugins/papermc-despawned-items"
New-Item -ItemType Directory -Force -Path $cfgDir | Out-Null
if (-not (Test-Path "$cfgDir/config.yml")) {
  "storage:`n  type: yaml`n" | Set-Content "$cfgDir/config.yml"
}

# --- 4. Launch ---
$java = Get-Java
Write-Host "`n==> Starting Paper $McVersion on localhost:$Port (YAML mode, offline)..." -ForegroundColor Cyan
Write-Host "==> When you see 'Done', open Minecraft $McVersion -> Multiplayer -> Direct Connect -> localhost:$Port" -ForegroundColor Yellow
Write-Host "==> Stop with 'stop' in this console.`n" -ForegroundColor Yellow
Push-Location $run
try { & $java -Xms1G -Xmx2G -jar $paper --nogui } finally { Pop-Location }
