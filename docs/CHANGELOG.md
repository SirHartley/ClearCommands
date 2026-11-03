# Changelog guide

How agents write entries in the root `Changelog`. Read it for every implementation task. Version numbers, release steps and how the release tooling reads the file are in [RELEASE.md](RELEASE.md).

## When to write an entry

- Every implementation task adds or amends its entries in the current unreleased section before merge, in the same commit as the change.
- Read-only work, documentation-only changes and build or tooling changes that never ship add nothing. Say why in the PR when a change needs no entry.
- Check whether the feature has been released: look for it under every version heading without `UNRELEASED`.
  - Not released yet: edit its `Added -` entry so it describes the feature as it now stands. No `Fixed -`, `Adjusted -` or `Removed -` line for it.
  - Released: the change gets its own entry.
- Record only completed changes from the actual diff. Never list plans or unfinished work.

## File layout

`Changelog` at the repository root, plain text, no extension. This repository's file has no `Known issues` or `Notes` header sections: it starts with versions, newest first, with a blank line between them. Keep that shape; do not add the header sections unless asked.

```text
1.0.h - UNRELEASED
Fixed - csmartClear no longer clears Threat, Dweller and RAT Abyss fleets

1.0.g
Adjusted - Updated for 0.98a
```

- Version heading: the version alone on its line, optionally followed by ` - UNRELEASED`. No `v` prefix, no date, no Markdown. The release tooling reads these headings, so keep them in this form.
- If no unreleased section exists, add one above the newest version. Ask the user which version comes next if it has not been chosen; do not invent a new version per commit.
- Entries: one change per line, starting `Added - `, `Adjusted - `, `Fixed - ` or `Removed - `, in that order.
- A `Note - ` line directly below an entry gives a limit or something the player must do. A console command the player has to run goes on its own line under it.
- Category headings are optional; a version with a few lines has none. Use the command name when one is needed.

## Wording

The house-style skill's "Changelog entries" section is the authority on wording; this is its short form for this mod.

- Short: stay under 80 characters, past 100 only for a new feature that needs its numbers.
- Capital letter after the dash, no full stop on a one-clause line. A second sentence is fine for a new feature; then both end in a full stop.
- Write commands as the player types them (`csmartClear`, `cfleetReport <FactionID>`), arguments in brackets as in the existing entries.
- Parentheses carry the short extra: `(was 100)`, credits.
- No semicolons, no curly quotes, no hyphen as a dash inside the text.
- Say what the player sees, never class names or the call chain.
- `Added - <command> - <what it does>` keeps this file's existing form for a new command; `Added - Setting to ...` for a setting.
- `Adjusted -` gives numbers as old `->` new with the unit, otherwise a direction word (Reduced, Increased).
- `Fixed -` in the user's forms: `Fixed - csmartClear no longer clears X`, `Fixed - X is now properly Y`, `Fixed - Crash when Y`, or a short noun phrase for small things.
- `Removed - <thing> (<reason or replacement>)`.
- Credits name real people only, in the user's forms (`(thanks, name)`, `(reported by name)`). Never an agent.
- Keep the user's own lines verbatim, including their commentary and spelling. Never imitate it: agent lines have no first person, jokes, caps, exclamation marks outside a credit, or hedges about whether a fix works (untested status goes in the PR).
- No agent or model names, links, validation logs or commit hashes.

## Amending versus adding

- Combine entries when several edits fix the same player-visible problem.
- An unreleased feature removed again before release loses its `Added -` entry; it gets no `Removed -` line.
- Never repeat a change once per file, step or PR.

## Maintenance

Update this guide when the `Changelog` layout or entry format changes, or when the house-style "Changelog entries" section changes. Release mechanics belong in [RELEASE.md](RELEASE.md).
