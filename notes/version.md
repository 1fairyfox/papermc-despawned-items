# Changelog (Version History)

Plain-English history, **one entry per commit, newest first.** Split by month
under [`version/`](version/); this file is the index.

> Not the version-*number* scheme — that's `reference/versioning.md`. This is the
> *story*; that's the *label*.

## Months

| Month | Notes |
|-------|-------|
| [2026-07](version/2026-07.md) | Kotlin/Paper 26.1 rewrite + fairyfox mesh onboarding |

## The inline rule

Write each entry **inside the commit it describes** — add it to the top of the
current month's `version/YYYY-MM.md` and stage it in the same commit. No separate
"update changelog" commit; no hash marker.

Entry format:

```
### YYYY-MM-DD — Short human title

One or two paragraphs, plain English. No diff noise.
```
