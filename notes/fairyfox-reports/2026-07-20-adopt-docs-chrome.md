# fairyfox process report — adopt docs-site shared chrome

**Date:** 2026-07-20
**Procedure:** adopt `hub/standards/docs-site` shared chrome bundle into the Dokka docs site
**Adopted bundle VERSION:** 2.2.1
**Trigger:** direct user request ("docs page … totally lacking the entire chrome bundle"),
not the check-for-updates flow — so applied directly under the user's go-ahead.

## What was done

- Vendored the four master assets (`main.css`, `reader.js`, `nav.js`, `coins.js`) + the four
  bundle partials + `VERSION` from the read-only hub clone into `docs-theme/chrome/`.
- Injected the chrome through Dokka's FreeMarker `templatesDir`
  (`docs-theme/dokka-templates/includes/{page_metadata,header,footer}.ftl`) — the
  full-page-generator (Doxygen-style) adapter. Slots filled: `FF_CSS_HREF` →
  `${'$'}{pathToRoot}main.css`, `FF_PROJECT_KEY` = `papermc-despawned-items`,
  `FF_PROJECT_NAME` = `PaperMC Despawned Items`, subnav three-zone (overview · Guide ·
  Changelog · Download · Repository), Docs + subnav-home marked `.active`.
- `vendorChromeAssets` Copy task (`finalizedBy dokkaGenerate`) drops the assets into the
  docs root; referenced per-page via `${'$'}{pathToRoot}` — resolves at any depth, vendored
  (not hot-linked).
- Verified in-browser at root and a deep package page (sticky masthead/subnav, footer,
  reader/coins injected, palette cohesive). Full `./gradlew build` green.

## What was rough / suggestions to improve the procedure

- **No Dokka adapter.** `chrome/adapters/` covers Jekyll, Doxygen, static/SPA but not Dokka.
  Dokka is effectively the Doxygen case (owns the page; hooks via `templatesDir` +
  `customStyleSheets`/`customAssets`), but a one-page `adapters/dokka.md` would save the
  translation. Key notes worth capturing there: (1) override the `includes/*.ftl` (not
  `base.ftl`) so Dokka's viewport layout is untouched; (2) keep the shared footer **inside
  `#main`**, because a body-level footer creates a second window-scroll that unpins the
  sticky masthead in Dokka's 100vh internal-scroll layout; (3) reference vendored assets via
  `${'$'}{pathToRoot}` wrapped in `<@template_cmd name="pathToRoot">`.
- **`main.css` global bleed.** The bundle CSS sets `html{font-size}`, `body{display:flex}`
  and bare element selectors that leak into a generator's body. The "boundary the reference"
  guidance covers it, but a short note on load order (bundle CSS before the generator's, a
  per-stack harmony sheet after) would help the next adopter.

## Deviations recorded

Footer sits in Dokka's content column (not full-bleed under the sidebar); Dokka's own
reference bar kept below the masthead as API controls. Both in `decisions/architecture.md`.
