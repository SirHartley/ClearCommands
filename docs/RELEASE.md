# Release

How ClearCommands is built, packaged and published. Nothing here runs unless a release is asked for: no version bumps, rebuilt jars, tags or published releases for ordinary work. Changelog entry rules are in [CHANGELOG.md](CHANGELOG.md); code owners are in [ARCHITECTURE.md](ARCHITECTURE.md).

## Pieces

| Piece | Path | Role |
|---|---|---|
| Changelog | `Changelog` | Source of the version and the release notes. The newest version heading is the release |
| Mod metadata | `mod_info.json` | Version stamped into the ZIP's copy; loads `jars/ClearCommands.jar` |
| Version file | `ClearCommands.version` | Online Version Checker file on `master`, forum thread `19210`, registered in `data/config/version/version_files.csv`; stamped at packaging, committed back after a release |
| Release builder | `jars/src/clearcommands/tools/ReleaseBuild.java` | `build` and `package`, run with `java <file>` |
| Builder checks | `jars/src/clearcommands/tools/ReleaseBuildChecks.java` | Headless checks of changelog parsing, stamping, file filtering and the command list |
| Build libraries | `lib/` | `starfarer.api.jar`, `lwjgl.jar`, `lwjgl_util.jar`, `json.jar`, `log4j-1.2.9.jar` (game `0.98a-RC8`) and `lw_Console.jar` (Console Commands), copied from Industrial.Evolution's `lib/` |
| Release jar | `jars/release/ClearCommands.jar` | Built by `build`, gitignored, runtime classes only; shipped as `jars/ClearCommands.jar` |
| Release workflow | `.github/workflows/release.yml` | Manual only: preview without a version, publish with one |

The IDE artifact `ClearCommands:jar` writes `jars/ClearCommands.jar` (gitignored, like the IDE's `jars/production/` output), so an IDE build never replaces the release jar.

The release jar is built in CI from `lib/` and never committed, as in Tidy.Panels. Do not add the house template's committed jar and source hash check.

## Versions

Versions use `major.minor.letter`: the first number for a major release, the second for changes players will notice, the letter for fixes and small patches. Reset the lower parts when a higher one changes. Current public version `1.0.g`. The next change opens a `- UNRELEASED` section above it; ask the user which version it is if it has not been chosen.

## Build the release jar

From the repository root, with JDK 17:

```text
java jars/src/clearcommands/tools/ReleaseBuild.java build
```

- Compiles every `.java` file under `jars/src` against the jars in `lib/`. No game install is needed. `starfarer_obf.jar` is not needed: the code uses only API and Console Commands types.
- Refuses a JDK other than 17 and a Maven `json.jar` (the game's `JSONException` is checked).
- Builds into a fresh `out/release-build-*`, keeps only classes under `com/fs/starfarer/api/commands/` (the `clearcommands/tools` package never ships), and replaces the release jar only after a clean compile.
- The mod has no mod plugin. Console Commands loads the classes named in the `class` column of `data/console/commands.csv`, so the jar must contain every one of them.
- After a game or Console Commands update, copy the new jars into `lib/` and update the table above.

## Package

```text
java jars/src/clearcommands/tools/ReleaseBuild.java package          # preview
java jars/src/clearcommands/tools/ReleaseBuild.java package 1.0.h    # release
```

- Needs a release jar from `build`.
- Preview packages the newest changelog section.
- Release requires the version (`1.0.h`, no `v`) to equal the newest changelog heading, and that section must not be `UNRELEASED`.
- Ships Git-tracked files only: `mod_info.json`, `ClearCommands.version`, `Changelog`, `_clearcommands_icon.png` (LunaLib loads it from the mod root, see `data/config/LunaSettingsConfig.json`), `data/` (`csv json shader ship wpn proj system variant skin`), `graphics/` (`png jpg jpeg`), `sounds/` (`ogg wav`) and the release jar as `jars/ClearCommands.jar`. Source, `lib/`, docs, IDE, agent and workflow files, and any path under a dot, `unused`, `unedited`, `tools` or `docs` directory are left out.
- Fails if a required file (`Changelog`, `mod_info.json`, the version file, the icon, `data/console/commands.csv`, `data/config/LunaSettingsConfig.json`, `data/config/version/version_files.csv`) is missing.
- Output in `out/release/`: `ClearCommands-<version>.zip` (one `ClearCommands/` folder, sorted, fixed timestamps), `release-notes.md`, the stamped version file and `version.txt`.
- Stamping changes the download name from the old `ClearCommands<version>.zip` to `ClearCommands-<version>.zip`. The committed version file still names `1.0.g`'s old asset until the next release.

## Publish

The workflow must be on `master` (the default branch) before it can be started.

1. Finish the newest `Changelog` section and remove `UNRELEASED`.
2. Run `build` and `package <version>` locally as the preview check, then merge to `master`.
3. Start the Release workflow on `master` with `version` set: Actions tab, Release, Run workflow, or `gh workflow run release.yml --ref master -f version=<version>`.
4. The workflow runs the builder checks, builds the jar, packages, creates the release and its tag on the packaged commit, then commits the stamped `ClearCommands.version` to `master`.
5. Check the release ZIP and notes, and that `master`'s version file names the new version.

The workflow has no push, pull request, tag or schedule trigger. Without a version it only uploads a preview artifact. It refuses to overwrite an existing release, and run on an existing version tag it publishes that tag. It compiles from `lib/` and downloads nothing.

## Maintenance

Update this guide when `ReleaseBuild`, `ReleaseBuildChecks`, `.github/workflows/release.yml`, the version file or the jars in `lib/` change.
