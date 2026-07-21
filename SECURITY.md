# Security Policy

## Reporting a vulnerability

Please report security issues **privately** — do not open a public issue for a
suspected vulnerability.

- Use GitHub's **private vulnerability reporting** for this repo:
  **Security → Report a vulnerability** (Settings → Code security → enable
  *Private vulnerability reporting* if it isn't on).
- Or email **junehanabi@gmail.com** with details and, if possible, a reproduction.

You'll get an acknowledgement as soon as it's seen. Please allow a reasonable window
for a fix before any public disclosure.

## Supported versions

Only the latest released version of **PaperMC Despawned Items** is supported. Fixes ship
in a new release rather than as back-ports.

## Scope

PaperMC Despawned Items is a server-side Paper plugin. It stores per-player despawn
locations on the server's disk under the plugin's data folder (`plugins/papermc-despawned-items/userdata/`)
and reads its own `config.yml`. It makes no outbound network connections and relays
nothing to any third party. Relevant security surface is limited to: the command
permissions (`despi.use`, `despi.elevated`, `recycle.use`) gating who can register,
inspect, or purge despawn locations, and the trust boundary of the server operator who
installs the plugin.
