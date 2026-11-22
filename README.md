# CheckMate

CheckMate checks an IFC model against the requirements of the German federal
[BIM Portal](https://via.bund.de/bim): LOINs, whole AIA projects, or any IDS file. For each element
it reports the results and any requirements it does not evaluate. Requirements can be downloaded
as IDS (buildingSMART's Information Delivery Specification) or supplied locally. The checker
supports a subset of IDS 1.0; selected results are compared with ifctester.

Three front ends share one check: a JavaFX GUI, a command line (CLI), and an MCP server for
compatible AI applications. The [documentation](https://max-sternal.github.io/CheckMate_BIMPortal/)
describes setup, the checking scope, its limitations, and the project's BIM context in German and
English.

## What's where

| Path | What it is |
|---|---|
| `bim-portal/Java_client/hackathon-template/` | CheckMate (Gradle module `hackathon-template`) |
| `…/src/main/java/com/bimportal/hackathon/ids/` | the core: requirements from the portal or IDS files (`PortalRequirements`, `IdsParser`, `IdsArchive`), the check (`IdsValidator`, `Values`, `DataTypes`, `PropertyKey`), the result (`CheckResult`) |
| `…/src/main/java/com/bimportal/hackathon/catalogue/` | choosing LOINs from the portal's catalogue: filters, ranking by fit (`ModelFit`), the description on click |
| `…/src/main/java/com/bimportal/hackathon/check/` | the check as every front end runs it: requirement sources, downloading each once, checking, searching (`CheckService`, `Source`) |
| `…/src/main/java/com/bimportal/hackathon/report/` | the result of a run: verdict, exit code, text and JSON (`CheckReport`, `ReportText`, `ReportJson`) |
| `…/src/main/java/com/bimportal/hackathon/mcp/` | the MCP server for AI apps: the tools (`CheckMateTools`) and the server over stdio (`CheckMateMcpServer`), on the official MCP Java SDK |
| `…/src/main/java/test/fx/` | GUI (`App`, `ElementUI`), CLI (`LoinCheckCli`), and reading IFC models with the Apstex toolbox (`IfcResources`, `IfcModelElements`, `ModelLinks`). `PropertyUtils` and `Geo` are left from the original template. The package name is a leftover (STATUS #13). |
| `…/src/test/` | tests, with real portal downloads and own IFC case files |
| `bim-portal/Java_client/bim-portal-enhanced/` | client for the BIM Portal's API (from the hackathon template) |
| `bim-portal/Java_client/openapi-generated/` | generated API client, don't edit |
| `docs/WORKFLOW.md` | how CheckMate works step by step, a checklist, and decisions |
| `docs/STATUS.md` | open points, including open decisions |
| `website/` | bilingual (DE/EN) Astro Starlight documentation site for GitHub Pages (`0.1`–`0.4`) |
| `.github/workflows/` | GitHub Actions CI (Java core tests + docs build) and GitHub Pages deployment |
| `.claude/skills/run-checkmate/` | Claude Code skill: build, run and drive the GUI |
| `DocsBIMDeutschland/` | background material on the BIM Portal, LOIN and IDS |

The packages `ids`, `catalogue`, `check` and `report` use neither the GUI nor the IFC library. GUI
and CLI only collect their input, pass it to `CheckService`, and show the `CheckReport` it returns
through `ReportText` or `ReportJson`; the MCP server does the same. So every front end gets the
same results, and the core is tested without IFC files.
How the parts work together: [`docs/WORKFLOW.md`](docs/WORKFLOW.md), section 2.

The module's own README, [`hackathon-template/README.md`](bim-portal/Java_client/hackathon-template/README.md),
is maintained separately.

## Requirements

Needed:

- **JDK 21 with JavaFX**, e.g. Azul Zulu 21 "JDK FX", as `JAVA_HOME`. With a plain JDK the GUI, the
  CLI (package `test.fx`) and the MCP server don't compile: the `javafx` packages are missing. `runGui` checks for
  this and says so.
- **Apstex IFC Toolbox JARs** (`com.apstex.ifctoolbox*.jar`, `com.apstex.stepparser*.jar`) in
  `bim-portal/Java_client/hackathon-template/lib/`. They read IFC files. They are not
  redistributable and not in git; the team keeps a copy in the shared Nextcloud folder
  `BIM_Deutschland_Hackathon`. Without them, everything that reads IFC (GUI, CLI, MCP server, IFC
  tests) is left out of the build, and a test says what is missing.
- **Network access** to `https://via.bund.de/bim` for LOINs and AIA projects. No account needed:
  without credentials CheckMate sees everything published.

Included or optional:

- Gradle comes with the wrapper (`gradlew`), nothing to install.
- Credentials for non-public content: copy `bim-portal/Java_client/.env_example` to `.env` and set
  `BIM_PORTAL_USERNAME` and `BIM_PORTAL_PASSWORD`. Never commit `.env`.
- Sample models for more tests (about 5 MB, from public GitHub repositories):
  `./gradlew :hackathon-template:downloadTestModels`. Without them those tests are skipped with a
  message.
- Python 3 with `ifcopenshell` and `ifctester` (`pip install ifcopenshell ifctester`), only to
  regenerate the IFC test files (`src/test/python/make_ifc_fixtures.py`) or to compare results with
  ifctester. CheckMate itself doesn't need Python.

Tested on Windows 10 with Git Bash and Gradle 8.14.

## Quick start

From `bim-portal/Java_client/`:

```bash
./gradlew :hackathon-template:test                 # all tests
./gradlew :hackathon-template:runGui               # the window
./gradlew :hackathon-template:runLoinCheck --args="--loin be959b9d-daef-5315-9aba-f60b6ad54ccb"
./gradlew :hackathon-template:runLoinCheck --args="--suggest --phase 'LPH 5'"
./gradlew :hackathon-template:installMcp           # the MCP server for AI apps
```

The model is the bundled bridge model unless `CHECKMATE_IFC` names a file or a link, or `--model`
does. CLI options and exit codes: [`LoinCheckCli`](bim-portal/Java_client/hackathon-template/src/main/java/test/fx/LoinCheckCli.java)
and `docs/WORKFLOW.md`, section 2.5.

## Using CheckMate from an AI app

With the MCP server, an AI app such as Claude Desktop or Claude Code checks a model for you: ask
"Welche LOINs passen zum Modell?" or "Prüfe das Modell gegen die LOIN
be959b9d-daef-5315-9aba-f60b6ad54ccb", and the AI calls CheckMate and explains the report.

1. Build it: `./gradlew :hackathon-template:installMcp`, again after code changes. The start script
   is `bim-portal/Java_client/hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp.bat`.
2. Register it with its full path. On Windows `cmd /c` starts the `.bat`:
   - Claude Code, in a terminal at the repo root:
     `claude mcp add checkmate -- cmd /c "C:\...\checkmate-mcp.bat"`
   - Claude Desktop: Settings → Developer → Edit Config, add the `checkmate` entry from section 2.7
     with `JAVA_HOME` (the JDK 21) in `env`, then quit the app from the tray and start it again.
3. Check: `claude mcp list` shows `checkmate` as connected; Claude Desktop lists it under
   Settings → Developer.

Ollama, the tools, and what to do when the server doesn't start:
[`docs/WORKFLOW.md`](docs/WORKFLOW.md), section 2.7.

## Open decisions

- **Hard-coded property name mapping.** `PropertyAliases` maps three portal property names to the
  names one modelling convention (BIM.Hamburg, `Pset_Objektinformation/_IDEbene1..3`) uses. It
  only helps models following that convention, and IDS knows no such mapping. How mappings should
  be supplied is undecided: [STATUS #14](docs/STATUS.md).

## License

Apache License 2.0, see [LICENSE](LICENSE). The Apstex IFC Toolbox is not part of it and must not
be redistributed.
