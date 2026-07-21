# fairyfox process report — proposal: make the docs-site subnav standard adaptive

**Date:** 2026-07-21
**Procedure:** cross-project consistency finding + proposed standard change (docs-site 05,
"canonical three-zone subnav"). Raised to the hub per the owner's request ("tell fairyfox
hub … with a proposed working effective solution").
**Trigger:** direct owner request — parity work between `papermc-despawned-items` and the
sibling `random-ai-prompt`. Not the check-for-updates flow.

## The finding — two nodes, two different "canonical" subnavs

Both projects claim to follow docs-site standard 05's *canonical three-zone subnav*, yet
they ship materially different centre/right zones:

| Zone | `random-ai-prompt` (live) | `papermc-despawned-items` (was) | chrome/subnav.html example |
|------|---------------------------|----------------------------------|----------------------------|
| Home | Random AI Prompt | PaperMC Despawned Items | `{{FF_PROJECT_NAME}}` |
| Centre | Overview · **Project Notes · Systems · Reference** · Changelog · API · Download | Notes · Tutorials · Changelog · API · Download · Legal | Notes · Tutorials · Changelog · API · Download |
| Right | Repository ↗ · **Notes ↗** | Repository ↗ | Repository ↗ |

The written canonical *example* in `chrome/subnav.html` is the narrow one, so a node that
copies it faithfully (papermc did) ends up **visibly behind** a node that expanded it
(random-ai-prompt). That is not a project bug — it is an **under-specified standard**: the
example encodes one project's page set as if it were the fixed shape, and the "include the
ones it has" latitude got read two different ways. The owner correctly flagged papermc as
"missing stuff" against the sibling.

## Proposed working solution — an adaptive three-zone contract

Keep the three zones fixed; make the **centre membership a function of what the project
actually has**, and name the two conventions the sibling already established so every node
converges:

1. **Home** (left): project name, doubling as overview/home. *(unchanged)*
2. **Overview** (centre, first): an explicit pill that also targets the home/overview page.
   Redundant with Home by design — it reads as the first "section" and matches the sibling.
3. **Notes, broken out by section** (centre): replace a single `Notes` pill with
   **`Project Notes`** (the notes landing) **+ one door per notes section the project has**
   (`Systems`, `Reference`, and any others in the notes tree — Context, Decisions, Plans…).
   A node exposes exactly the sections that exist; empty sections are omitted, never 404.
4. **Project pages** (centre): the ones that exist, in order — `Tutorials?` · `Changelog` ·
   `API` · `Download?` · `Legal?`. `?` = include only if the page exists. `Download` is
   still mandatory for any project that offers downloads; `Legal` is mandatory per legal-docs.
5. **Repository ↗ + Notes ↗** (right): both nodes keep living notes in-repo, so a
   **`Notes ↗`** direct link to `…/tree/main/notes` joins `Repository ↗` as a fixed pair.

### Recommended canonical order (fill in what exists)

```
[Name=home] · Overview · Project Notes · <Section doors…> · Tutorials · Changelog · API · Download · Legal · [Repository ↗] · [Notes ↗]
```

## Reference implementation (already shipped in papermc, this release)

- **Generator**: `renderDocsSite` now emits a **section landing page** per notes section
  that has notes (`notes/systems/index.html`, `notes/reference/index.html`) — a heading + the
  section's note list, reusing the standard sidebar. Only emitted when the section is
  non-empty, so a pill never points at a 404. Active-state keys `OVERVIEW/SYSTEMS/REFERENCE`
  added to the shell fill.
- **Both chrome surfaces updated identically**: the hand-page shell
  (`docs-theme/pages/_shell.html`) and the Dokka API header
  (`docs-theme/dokka-templates/includes/header.ftl`) now carry the full centre set + `Notes ↗`.
- Verified: `assembleDocsSite` green; the new subnav renders on the root hand page **and** on
  a Dokka API page; every centre/sidebar link resolves (no dangling targets).

This maps cleanly onto the JSDoc stack too: random-ai-prompt's
`tutorial-notes__systems__index.html` **are** the per-section landings — JSDoc generates them
from tutorial categories, papermc generates them from notes subdirectories. Same contract,
two generators.

## Asks of the hub

1. **Update `docs-site` standard 05 + the `chrome/subnav.html` canonical comment** to the
   adaptive contract above (centre = Overview + Project Notes + section doors + existing
   project pages; right = Repository ↗ + Notes ↗). Replace the fixed example with the
   "fill in what exists, in this order" rule so nodes stop diverging.
2. **Add a `chrome/adapters/` note** on generating per-section landing pages (the missing
   piece that made papermc collapse everything into one `Notes` door): JSDoc → tutorial
   categories; Dokka/custom → generated section `index.html`.
3. Consider a tiny **conformance check** (a shared script) that asserts a node's subnav
   contains a door for every non-empty notes section — the machine check that would have
   caught this divergence before the owner had to.

## What was rough

- The standard states a *fixed* example but grants *"include the ones that exist"* latitude;
  those two pull in opposite directions and produced the divergence. Naming the conventions
  (Overview pill, section doors, Notes ↗) removes the ambiguity — latitude on *membership*,
  not on *shape*.
