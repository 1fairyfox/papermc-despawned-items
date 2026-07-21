# Plan — badge + security parity with `random-ai-prompt` (2026-07-21)

**Goal.** Bring `papermc-despawned-items` up to the same README badge wall and
supply-chain / quality tooling as the sibling project
[`random-ai-prompt`](https://github.com/1fairyfox/random-ai-prompt), adapted from
that project's JavaScript/Node stack to this one's Kotlin/Gradle/Paper stack.

## What the reference has that we lacked

Reference README carries ~22 badges backed by real infra: CI, CodeQL, OpenSSF
Scorecard, Codecov, SonarCloud (quality gate + tech debt), CodeFactor, docs deploy,
plus the GitHub social/activity badges. We had 5 (CI, version, last-commit, docs,
license) and only `ci.yml` + `docs.yml` + `dependabot.yml`.

## JS → Kotlin adaptations

| Reference (Node) | Here (Kotlin/Gradle) |
|---|---|
| Node ≥24 badge | Java 21 badge (+ Paper 1.21.x badge) |
| `package-json/v` version | `github/v/tag` (already correct) |
| Codecov `flag=node` engine coverage | single Kover coverage (Kover already gates the build) |
| CodeQL `javascript-typescript` | CodeQL `java-kotlin`, build-mode manual (Gradle compile) |
| Netlify deploy badge | GitHub Pages docs-deploy badge (`docs.yml`) |
| SonarCloud (JS) | SonarQube Cloud (Kotlin), scoped to `src`, Kover XML coverage import |
| CodeFactor | CodeFactor (language-agnostic — unchanged) |

## Phases

1. **README badge block** — full wall, Kotlin-adapted, + commented-out Hangar/Modrinth
   usage badges (ready to enable once the plugin is published there).
2. **Security workflows** — `scorecard.yml` (verbatim from reference; language-agnostic),
   `codeql.yml` (`java-kotlin`), `codeql/codeql-config.yml`. Dependabot already covers
   gradle + actions.
3. **Coverage → Codecov** — `koverXmlReport` step + Codecov upload in `ci.yml`.
4. **SonarQube Cloud** — `sonar-project.properties` (Kotlin, `src` only, Kover coverage
   import) + a token-guarded scan step in `ci.yml`.
5. **Docs** — parity check only. Subnav/chrome already deploy correctly (confirmed by
   owner); no injection fix needed.
6. **Naming fix + release** — fix the bare `DespawnedItems` in `SECURITY.md`; verify
   `./gradlew build`; commit on `dev` (changelog + PATCH `VERSION` bump); release via PR.

## Needs the owner (external, cannot be done from the repo)

Badges that stay grey/red until these are done:
- **Codecov** — enable the Codecov GitHub app on the repo; add `CODECOV_TOKEN` secret.
- **SonarQube Cloud** — import the project under org `1fairyfox`; confirm the real
  `projectKey`/`organization` (SonarCloud mints its own — see the reference's warning);
  add `SONAR_TOKEN` secret. Until the secret exists the scan step self-skips.
- **CodeFactor** — add the repo at codefactor.io (no config file needed).
- **OpenSSF Scorecard** — optional `SCORECARD_TOKEN` PAT to score the Branch-Protection
  check (falls back to the default token).
- **Hangar/Modrinth** — publish the plugin, then uncomment the usage badges.
