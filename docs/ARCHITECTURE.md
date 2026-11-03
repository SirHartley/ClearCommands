# Architecture

Technical routing for the current implementation, checked against game `0.98a-RC8`. Code paths below are relative to `jars/src/com/fs/starfarer/api/commands/`; data paths are repository-relative. Search the named owner before adding another implementation.

| Reference | Scope |
|---|---|
| [CLAUDE.md](../CLAUDE.md) | Workflow, build gate and document upkeep |
| [RELEASE.md](RELEASE.md) | Version numbers, release builder, package contents and publishing |
| [CHANGELOG.md](CHANGELOG.md) | Change-note format and upkeep for every implementation task |

## Start here

| Change / symptom | Route |
|---|---|
| A command is missing from the console | `data/console/commands.csv` row -> `class` column -> class in the jar; [registration](#registration-and-lifecycle) |
| `csmartClear` clears too much or too little | `clearCommands_smartClear.runCommand` (faction counts, limit) -> `clearFleetsOfFaction` (exclusion set) -> `clearCommands_clearFleets.softClear`; [what the clear commands keep](#what-the-clear-commands-keep) |
| A fleet that should be kept was removed by any clear command | `clearCommands_clearFleets.softClear` checks; [what the clear commands keep](#what-the-clear-commands-keep) |
| `cclearFleets` does not find a faction | `clearCommands_clearFleets.runCommand` faction lookup (id, display name, lowercase display name) |
| Modular ships not removed, or the wrong ones | `clearCommands_clearModularShips.runCommand` scope (`all`, `allwithstations`, default `VariantSource.REFIT`) |
| `cfleetReport` counts or groups look wrong | `clearCommands_fleetReport.runCommand` -> `getFleets` / `getAllRoutes` -> `getKeyWordGrouping` |
| `listNearbyFleets` misses a fleet | `clearCommands_listNearbyFleets.runCommand` distance map; [contracts](#commands) |
| Another mod wants its fleets kept | `$clearCommands_no_remove` in `ids/clearCommands_Ids.NO_CLEAR_KEY` |

## Registration and lifecycle

| Registry / hook | Owners |
|---|---|
| `mod_info.json` | Id `clearCommands`, no `modPlugin`, jar `jars/ClearCommands.jar`, `utility` because nothing is saved. No `dependencies` entry: Console Commands is required in practice, since nothing else loads the classes |
| `data/console/commands.csv` | Console Commands registry: `command`, `class`, `tags` (`clearCommands,campaign`), `syntax`, `help`. One row per command class |
| `data/config/LunaSettingsConfig.json` | LunaLib settings icon for `clearCommands`: `_clearcommands_icon.png` at the mod root. No LunaLib settings exist |
| `data/config/version/version_files.csv` | Version Checker entry for `ClearCommands.version` |

There is no load order. Console Commands builds each command class from `commands.csv` and calls `BaseCommand.runCommand(String args, CommandContext context)` when the player enters it. Every command returns `WRONG_CONTEXT` outside the campaign (`context.isInCampaign()`). Nothing is registered at startup, no script or listener is added, and nothing is written to saves.

| Command | Class | Scope |
|---|---|---|
| `csmartClear` | `clearCommands_smartClear` | Every location; factions with an abnormal fleet count |
| `cclearFleets <faction>` | `clearCommands_clearFleets` | Every location; one faction |
| `cclearAllFleets` | `clearCommands_clearAllFleets` | Every location; every faction |
| `cclearModularShips [all/allWithStations]` | `clearCommands_clearModularShips` | Current location; ships with module slots |
| `cfleetReport [faction]` | `clearCommands_fleetReport` | Every location; console report of fleets, assignments, listeners, routes |
| `listNearbyFleets [range]` | `clearCommands_listNearbyFleets` | Current location; fleets within `range` (default 30000) of the player |

### Build and tools

`jars/src/clearcommands/tools/ReleaseBuild.java` (`build`, `package`) and `ReleaseBuildChecks.java` compile against `lib/` and never ship in the release jar; they, `lib/` and `.github/workflows/release.yml` are owned by [RELEASE.md](RELEASE.md).

## Data identity

| Stable id | Use |
|---|---|
| `clearCommands` | Mod id in `mod_info.json` and `LunaSettingsConfig.json` |
| `$clearCommands_no_remove` | Fleet memory flag read by `softClear` and `clearCommands_clearModularShips`; set by other mods, never by this one |
| Class names in `commands.csv` | Console Commands loads by these names; renaming a class needs the row changed in the same commit |

## Source owners

Use `rg --files jars/src/com/fs/starfarer/api/commands` for the complete inventory.

| File | Owner / connection |
|---|---|
| `clearCommands_clearFleets.java` | `softClear(LocationAPI, FactionAPI, Set<CampaignFleetAPI>)`: the one fleet removal path and its keep checks, used by all three clear commands. `null` faction means every faction, `null` set means no extra exclusions |
| `clearCommands_smartClear.java` | Faction count limit and the exclusion set (route sources, assignments) passed to `softClear` |
| `clearCommands_clearAllFleets.java` | Calls `softClear` for every location with no faction and no exclusions |
| `clearCommands_fleetReport.java` | The report, plus the shared static helpers `getFleets`, `getAllRoutes`, `addToStringMap` and `sortByLargestValue` that `clearCommands_smartClear` imports |
| `clearCommands_clearModularShips.java` | Its own removal of single fleet members with `removeFleetMemberWithDestructionFlash`; does not use `softClear` |
| `clearCommands_listNearbyFleets.java` | Read-only list; marks ships with module slots `[MODULAR SHIP]` |
| `ids/clearCommands_Ids.java` | `NO_CLEAR_KEY` |

## What the clear commands keep

`softClear` skips a fleet when any of these hold:

- `isPlayerFleet()` or `isStationMode()`;
- a faction was given and the fleet is not of it, or the fleet is in the exclusion set;
- memory flags `MemFlags.ENTITY_MISSION_IMPORTANT`, `MemFlags.FLEET_IGNORES_OTHER_FLEETS`, `MemFlags.FLEET_DO_NOT_IGNORE_PLAYER`, `$clearCommands_no_remove` or `$ziggurat`;
- any member variant (fighters included) has `Tags.SHIP_UNIQUE_SIGNATURE`, `Tags.SHIP_CAN_NOT_SCUTTLE` or `Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER`.

Otherwise it calls `Global.getSector().reportFleetDespawned(fleet, FleetDespawnReason.OTHER, null)` and `loc.removeEntity(fleet)`.

`csmartClear` adds an exclusion set: the active fleets of `RouteManager` routes with source `econ`, `nex_specialForces` or `IndEvo_Shipping`, and fleets whose current assignment description is `Orbiting` (`ORBIT_PASSIVE`, `ORBIT_AGGRESSIVE`) or `Defending` (`DEFEND_LOCATION`). It counts fleets per faction id, sorts the counts largest first, averages the counts ranked between 25% and 50% of the faction list, doubles that as the limit, and clears every faction above the limit or above 100 fleets. Factions `Factions.THREAT`, `Factions.DWELLER` and ids containing `rat_abyssals` print `Skipping Fleets of Faction`, but the loop has no `continue`, so they are cleared anyway (open bug).

`cclearModularShips` skips the player fleet, `ENTITY_MISSION_IMPORTANT` and `$clearCommands_no_remove`, then removes members whose variant has module slots: by default only `VariantSource.REFIT` non-stations, with `all` every non-station, with `allwithstations` stations too.

## Contracts

### Engine

- `SectorAPI.getAllLocations()` already contains hyperspace (`CampaignEngine.getAllLocations`), so the separate hyperspace pass in the clear commands, `getFleets` and `getAllRoutes` finds nothing new; counts are not doubled because `softClear` works on a copy of the live fleet list and the helpers collect into sets.
- A fleet left with no members despawns itself on its next advance with `FleetDespawnReason.NO_MEMBERS` (`CampaignFleet.advance`), so `cclearModularShips` does not remove empty fleets by hand.
- Assignment matching in `csmartClear` compares `FleetAssignment.getDescription()` text, not the enum; a new assignment that shares a description is matched too.

### Commands

- `listNearbyFleets` keys a `TreeMap` by distance (farthest first), so two fleets at exactly the same distance show as one.
- `cfleetReport` and `cclearFleets` look a faction up by id, display name or lowercase display name, in that order, and take the first match.
- The `help` text for `cfleetReport` says it prints to the log; it prints to the console.

## Dead or dormant

| Component | State |
|---|---|
| `clearCommands_fleetReport.splitStrings` `dl` parameter | Unused; the split is always on `_` |
| `clearCommands_clearModularShips` commented-out location lines | Dead |
