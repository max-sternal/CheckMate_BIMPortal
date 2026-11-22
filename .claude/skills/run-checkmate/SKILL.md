---
name: run-checkmate
description: Build, run and drive the CheckMate JavaFX GUI (checks an IFC model against BIM Portal LOINs via IDS) and its headless LOIN-check CLI. Use when asked to start, open or run CheckMate / the GUI / the app, validate a LOIN, take a screenshot of the app, click through it, run the headless LOIN check, or run the hackathon-template tests.
---

CheckMate is the JavaFX app `test.fx.App` in `hackathon-template`. It loads a bundled IFC
bridge model, lists AIA projects and LOINs from the BIM Portal and checks the model against the
selected one, a LOIN or project given by its ID, or a local IDS file or project ZIP.
**Drive it with `.claude/skills/run-checkmate/drive.sh`**: it compiles the module and starts
`CheckMateDriver.java`, which launches the real `test.fx.App` in its own JVM and then runs
commands against the scene graph (search, select, click, read the report, screenshot).

Paths:

- Run `drive.sh` from the repo root as `.claude/skills/run-checkmate/drive.sh`. It works
  from any cwd and `cd`s to `bim-portal/Java_client/` itself.
- Run the Gradle commands (`./gradlew ...`) from `bim-portal/Java_client/`.
- All other paths below are relative to `bim-portal/Java_client/`.

Verified on Windows 10 in Git Bash (the Bash tool), with Gradle 8.14 via the wrapper. Not tried
on Linux/macOS.

## Prerequisites

- **JDK 21 with bundled JavaFX** in `JAVA_HOME`, e.g. Zulu 21 FX
  (`zulu21.*-ca-fx-jdk21.*`), is required only for the JavaFX GUI (`runGui` / `drive.sh`). Check
  it with `grep JAVA_VERSION "$JAVA_HOME/release"` and `ls "$JAVA_HOME/lib/javafx.properties"`.
  The `java` on `PATH` may be a different version, so don't run bare `java`. drive.sh uses
  `$JAVA_HOME/bin/java`.
- **No Apstex JARs needed on `0.3.2`.** CheckMate uses its own pure-Java STEP/IFC reader, so the
  CLI, MCP server, and tests build and run on any plain JDK 21 without anything in
  `hackathon-template/lib/`.
- **No credentials needed.** Without a `.env` the client runs in public mode: published LOINs only.
- Network access to the BIM Portal (the LOIN search and the IDS downloads happen live).

## Run (agent path): drive.sh

Batch mode: one driver command per argument. It stops at the first failing command, exits 1
and saves `hackathon-template/build/checkmate-failure.png`.

```bash
.claude/skills/run-checkmate/drive.sh "idle" "search Kappe" "idle" "loins" "select 0" "click Validate LOIN" "idle" "report" "tabs" "size 1400 950" "ss hackathon-template/build/kappe.png" 2>&1 | grep '^>>'
```

Takes 25-35 s (Gradle up-to-date check, IFC parse, catalogue load). Expected output ends with:

```
>> | Tunnelrohbau > Kappe (Ingenieurbau): LPH 1-9 (IFC2X3): 0 von 303 Elementen anwendbar - nicht prüfbar
...
>> screenshot ...\hackathon-template\build\kappe.png (1386x913)
>> done
```

REPL mode: no arguments, one command per line on stdin, EOF ends it:

```bash
.claude/skills/run-checkmate/drive.sh <<'EOF' 2>&1 | grep '^>>'
wait LOIN\(s\) im Portal
search Schaft
idle
loins > Schaft
select > Schaft
click Validate LOIN
idle
report
dump
ss
EOF
```

Driver lines start with `>> `. Everything else on stdout is the app's own logback output,
so `grep '^>>'` gives just the driver's view. Then Read the PNG and look at it.

| command | what it does |
|---|---|
| `idle [sec]` | wait until the report panel stops ending in `...` (catalogue load, search or check finished). Use after startup, `search` and `click Validate LOIN`. |
| `wait <regex>` | wait until the report panel text matches |
| `report` | print the report panel (bottom, "LOIN-PRÜFUNG") |
| `search <term>` | type into the search field and press Enter (server-side search for AIA projects and LOINs, empty = all). A term holding an ID (or a link with one) fetches that LOIN or project, puts it on top of the list and checks it at once; follow with `idle`. |
| `ids <file>` | read a local IDS file or ZIP and check it, as `Load IDS` does after its file dialog; it lands on top of the list. Follow with `idle`. |
| `loins [n\|text]` | list the first n entries (default 20) or the ones whose label contains text, with indices. Projects from a search show as `AIA-Projekt: <name>` above the LOINs; anything by ID or file as `<title>, Version <v> (<source>)`. `select AIA-Projekt` picks the first project. |
| `select <i\|text>` | select one entry by index or name substring (replaces the selection). Selecting describes the entry in the report panel; for a LOIN that downloads its IDS, so follow with `idle`. |
| `add <i\|text>` | add an entry to the selection, to check several at once |
| `filter <which> <text>` | choose a catalogue filter value: `which` is `fachmodell`, `phase` or `anwendungsfall`, `text` a part of the value (`filter phase LPH 5`) or `alle` |
| `showall on\|off` | tick or untick "alle anzeigen, auch nicht passende" |
| `summary` | print the summary table of the last check: verdict, specification, applicable elements, elements without and with defects; row 0 is "Gesamt" when several were checked |
| `row <i\|text>` | select a summary row; the report and the element tabs narrow to it (`row Gesamt` for all again) |
| `click <label>` | fire a button by its text, e.g. `Validate LOIN`, `Filter` |
| `tabs` / `tab <i\|text>` | list the element tabs in the middle pane / select one and print its lines |
| `dump` | control tree: types, texts, prompts, item counts |
| `size <w> <h>` | resize the window |
| `ss [path]` | snapshot the scene to PNG (default `hackathon-template/build/checkmate.png`) |
| `sleep <ms>`, `hold`, `quit` | pause / block until a human closes the window / close |

Relative `ss` paths resolve against `bim-portal/Java_client` (drive.sh `cd`s there). Keep
them under `hackathon-template/build/`, which git ignores (`*.png` is not ignored).

To use a different model, set `CHECKMATE_IFC` in the shell to an absolute path or a link
(`.ifc` or `.ifczip`; a GitHub file link works as copied from the browser). The report panel's
first line names the model in use. From the repo root:

```bash
CHECKMATE_IFC="$PWD/bim-portal/Java_client/hackathon-template/src/main/resources/ifc/bruecke-tbw1-mit-merkmalen.ifc" .claude/skills/run-checkmate/drive.sh "idle" "report"
CHECKMATE_IFC="https://github.com/ibpsa/project1-wp-2-2-bim/blob/master/IFC_Files/MISC/AC20-FZK-Haus.ifc" .claude/skills/run-checkmate/drive.sh "idle 90" "report"
```

If `CHECKMATE_IFC` can't be read, the app shows an error dialog before the window opens and
then starts with the bundled model. The dialog blocks the driver, so check the path first.

## Run: headless LOIN check (no window)

The same check as the button, for LOINs and AIA projects by ID, local IDS files and project ZIPs,
or everything a search term finds. Takes about 13 s. Run it from `bim-portal/Java_client/`:

```bash
JAVA_TOOL_OPTIONS="-Dstdout.encoding=UTF-8" ./gradlew -q --console=plain :hackathon-template:runLoinCheck --args="Kappe"
JAVA_TOOL_OPTIONS="-Dstdout.encoding=UTF-8" ./gradlew -q --console=plain :hackathon-template:runLoinCheck --args="--model test-models/ifc4/Infra-Bridge.ifc Ueberbau > Schaft"
JAVA_TOOL_OPTIONS="-Dstdout.encoding=UTF-8" ./gradlew -q --console=plain :hackathon-template:runLoinCheck --args="--loin be959b9d-daef-5315-9aba-f60b6ad54ccb"
JAVA_TOOL_OPTIONS="-Dstdout.encoding=UTF-8" ./gradlew -q --console=plain :hackathon-template:runLoinCheck --args="--model test-models/ifc4/Infra-Bridge.ifc --ids src/test/resources/ids/local/mehrere-spezifikationen.ids"
JAVA_TOOL_OPTIONS="-Dstdout.encoding=UTF-8" ./gradlew -q --console=plain :hackathon-template:runLoinCheck --args="--model test-models/ifc4/Infra-Bridge.ifc --aia e6f69257-dfda-441b-97bb-e9df44c5cf8b"
```

- `--model` takes a path or a link. Relative paths (`--model`, `--ids`) are relative to
  `hackathon-template/`.
- `--loin` and `--aia` take an ID of a LOIN or an AIA project, or any text holding one (a link
  copied from the portal); both accept either kind. `--ids` takes a local IDS file or a ZIP of
  them, such as an AIA project's ZIP. All three may be repeated. A search term holding an ID counts as
  `--loin`; any other search term finds AIA projects and LOINs.
- With several specifications (an AIA project), a tally line comes first:
  `36 Spezifikation(en) für IFC4: 36 nicht erfüllt`.
- `--full` prints the whole report, exactly as the GUI shows it, instead of a few examples per
  specification. All options: `docs/WORKFLOW.md`, section 2.5.
- `--suggest` checks nothing and downloads no requirements: it lists the catalogue's LOINs whose
  object type occurs in the model, best first, each line starting with the ID for `--loin`.
  `--discipline`, `--phase` and `--purpose` filter by a part of the value; they also narrow the
  LOINs a search checks. Quote values with spaces inside `--args`:
  `--args="--suggest --phase 'LPH 5' --discipline Ingenieurbau"`. Without quotes Gradle splits
  them, and `5` becomes a search term.
- Without `--loin`, `--ids` and a search term, it searches for "Kappe". The search term may have
  several words.
- The report starts with `CheckMate-Prüfung vom <date>` and the model, and ends with
  `Ergebnis: <verdict> (Exit-Code n)`. `--json <file>` also writes it as JSON (relative to
  `hackathon-template/`, e.g. `--json build/checkmate-report.json`; not with `--suggest`).
- Exit codes: 0 every specification met; 1 the model or any of the requirements couldn't be read
  or checked (unknown ID, no IDS file to download, not an IDS file, no specification for the
  model's schema), the others are still checked; 2 an unknown argument; 3 at least one
  specification not met; 4 nothing violated, but not everything confirmed ("nicht prüfbar",
  partly evaluated, or nothing checked). The bundled model gives 4 for every portal LOIN.
- Gradle prints `Execution failed for task ':hackathon-template:runLoinCheck'` and `BUILD FAILED`
  on any non-zero exit code, and `./gradlew` itself then exits with 1. That is the check's
  result, not a build problem. The real code is in Gradle's message (`finished with non-zero exit
  value 3`), in the last report line and in the JSON's `exitCode`.

## Run (human path)

Opens the window for a person to use. It blocks until the window is closed. Run it from
`bim-portal/Java_client/`:

```powershell
.\gradlew.bat :hackathon-template:runGui
```

From the Bash tool, run that with `run_in_background`. The entry class is
`hackathon-template/src/main/java/test/fx/App.java`. The UI itself is `ElementUI.java`.

## Test

From `bim-portal/Java_client/`:

```bash
./gradlew --console=plain :hackathon-template:test
```

All core, IFC, CLI, and MCP tests build and run on any plain JDK 21 without Apstex or JavaFX;
`BasicExampleIntegrationTest` is skipped without credentials (`assumeTrue`), and without the
downloaded sample models (`./gradlew :hackathon-template:downloadTestModels`) the tests that need
them are skipped with a message. Results are in `hackathon-template/build/test-results/test/`.

## Gotchas

- **Every LOIN comes back "nicht prüfbar" (`0 von 303 Elementen anwendbar`) on the bundled model.**
  This is expected, not a driver bug. The model classifies by Objektkatalog `Ingenieurbau_V005`,
  and the portal's LOINs ask for other level names. The report then lists the facet counts and
  the nearest elements, and the middle pane shows those elements as tabs.
- **The portal search is a substring match.** `search Schaft` returns 171 LOINs, mostly
  "Land*schaft*sbau", and `select Schaft` picks the wrong one. Anchor on the category
  separator: `loins > Schaft` / `select > Schaft` gives `Ueberbau > Schaft (Ingenieurbau)`.
- **Without JavaFX the build doesn't fail.** On a plain JDK 21 (without bundled JavaFX),
  `build.gradle.kts` skips only `test/fx` (the GUI) with a lifecycle notice while still building
  the IFC reader, CLI, MCP server, and tests. `drive.sh` checks `JAVA_HOME` for JDK 21 FX first
  and exits 2.
- **Some non-21 JDKs also bundle JavaFX** (e.g. Zulu 17 FX). A JavaFX check alone accepts them,
  and Gradle then dies with `invalid source release: 21`. drive.sh also checks
  `JAVA_VERSION="21` in `$JAVA_HOME/release`.
- **The window opens at 1280x900**, which fits the whole layout; `size` only for other sizes.
- **Take screenshots with `ss`, not desktop capture.** `ss` renders the scene graph, so it
  works even when the window is covered. Capturing the screen region via
  `GetWindowRect` + `CopyFromScreen` from PowerShell grabbed the wrong area: the coordinates are
  DPI-unscaled, so half the image was the Explorer window behind.
- **Closing the window shuts the FX toolkit down** (implicit exit). After that,
  `Platform.runLater` never runs. The driver tracks the window's showing state and fails fast
  with "the window was closed" instead of hanging.
- **Buttons not to click in automation.** `Load IFC` and `Load IDS` open a file dialog and
  `Load IFC link` a text dialog; the driver can't operate them. Set `CHECKMATE_IFC` instead of
  loading a model, and use the `ids <file>` command instead of `Load IDS`. `BIM Portal` opens a
  browser (`https://via.bund.de/bim`). `Connect MCP` has no handler.
- **An ID the portal doesn't know takes about 7 s** before the report says so: the ID is looked
  up among the project list's and the catalogue's version record IDs first. Use `idle 60` after
  `search <id>`.
- **The list shows only LOINs that fit the model** (their object type occurs in it), sorted by
  fit, with the AIA projects above them. `showall on` shows the rest; when none fits, all are
  shown anyway. `loins` labels end with the fit, e.g. `- 6 Element(e) mit 'Kappe' in der
  Klassifizierung`; `select` by a part of the name still works.
- **AIA projects are IFC4 in their combined file.** The railway project ("Beispiel-AIA Neubau
  Eisenbahnbrücke") has nothing for the bundled IFC2X3 model; use
  `CHECKMATE_IFC=$PWD/bim-portal/Java_client/hackathon-template/test-models/ifc4/Infra-Bridge.ifc`
  (after `downloadTestModels`). The road projects also bring IFC2X3 and IFC4X3.
- **`.env` vars are not exported to drive.sh's JVM.** Only Gradle `JavaExec` tasks copy `.env`
  into the environment, and `IfcResources` reads `CHECKMATE_IFC` from `System.getenv` only. So
  export it in the shell. Credentials still work because `ConfigLoader` finds `.env` itself
  by walking up from the cwd.
- **Startup timing.** The bundled IFC parses in a few seconds, and the full catalogue
  (1568 LOINs, paged) loads in about 7 s. The first `idle` covers both.
- **Umlauts from the Gradle CLI task.** In Git Bash they show as `�`, because the forked JVM
  writes cp1252. Set `JAVA_TOOL_OPTIONS="-Dstdout.encoding=UTF-8"` (it prints harmless
  `Picked up JAVA_TOOL_OPTIONS` lines). Git Bash may not ship `iconv`. drive.sh already
  passes `-Dstdout.encoding=UTF-8`.

## Troubleshooting

- **`drive.sh: JAVA_HOME='...' is not a JDK 21 with bundled JavaFX`**: point `JAVA_HOME` at a
  JDK 21 that bundles JavaFX (see Prerequisites).
- **`error: invalid source release: 21`** from `:openapi-generated:compileJava`: Gradle ran on a
  JDK older than 21. Fix `JAVA_HOME`.
- **`❌ No .env file found in search paths`** at configuration time: harmless, it means public mode.

## How the driver is wired

- `driver-classpath.init.gradle` is a Gradle init script passed with `-I`. It adds
  `:hackathon-template:writeDriverArgs`, which compiles the module and writes
  `hackathon-template/build/checkmate-driver.args` (a java `@argfile` with `-cp`, using forward
  slashes because a backslash is an escape character inside argfiles). `build.gradle.kts`
  stays untouched.
- `CheckMateDriver.java` runs in single-file source mode (`java @args CheckMateDriver.java`).
  It calls `Application.launch(test.fx.App.class)` on a side thread, finds the stage via
  `Window.getWindows()`, and does every scene access through `Platform.runLater`. Lookups go by
  visible text: the read-only `TextArea` is the report, the `ListView` next to the `LOINs` label
  is the LOIN list, the search field's prompt starts with `LOIN`, and buttons match by label.
  If `ElementUI` changes those, update the lookups. The `ids` command calls the private
  `ElementUI.offerAndCheck` and the package-private `RequirementSource.Loaded` through
  reflection; renaming either breaks it.
