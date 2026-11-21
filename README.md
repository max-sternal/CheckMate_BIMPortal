# CheckMate

CheckMate checks an IFC model against the requirements of the German federal
[BIM Portal](https://via.bund.de/bim): LOINs, whole AIA projects, or any IDS file. For each element
it says whether the requirements are met, and if not, why. It downloads the requirements as IDS
(buildingSMART's Information Delivery Specification) and follows IDS 1.0 the way ifctester does.

Three front ends share one check: a JavaFX GUI, a command line (CLI), and later an MCP server so
an AI assistant can answer questions about a model.

## What's where

| Path | What it is |
|---|---|
| `bim-portal/Java_client/hackathon-template/` | CheckMate (Gradle module `hackathon-template`) |
| `…/src/main/java/com/bimportal/hackathon/ids/` | the core: reading IDS, the check; no GUI, no IFC library |
| `…/src/main/java/com/bimportal/hackathon/catalogue/` | choosing LOINs from the portal's catalogue |
| `…/src/main/java/com/bimportal/hackathon/report/` | the result of a run: verdict, exit code, JSON |
| `…/src/main/java/test/fx/` | GUI, CLI, and reading IFC models (name is a leftover, STATUS #13) |
| `…/src/test/` | tests, with real portal downloads and own IFC case files |
| `bim-portal/Java_client/bim-portal-enhanced/` | client for the BIM Portal's API (from the hackathon template) |
| `bim-portal/Java_client/openapi-generated/` | generated API client, don't edit |
| `docs/WORKFLOW.md` | how CheckMate works step by step, a checklist, and decisions |
| `docs/STATUS.md` | open points, including open decisions |
| `.claude/skills/run-checkmate/` | Claude Code skill: build, run and drive the GUI |
| `DocsBIMDeutschland/` | background material on the BIM Portal, LOIN and IDS |

The module's own README, [`hackathon-template/README.md`](bim-portal/Java_client/hackathon-template/README.md),
is maintained separately.

## Requirements

Needed:

- **JDK 21 with JavaFX**, e.g. Azul Zulu 21 "JDK FX", as `JAVA_HOME`. With a plain JDK the GUI and
  the CLI (package `test.fx`) don't compile: the `javafx` packages are missing. `runGui` checks for
  this and says so.
- **Apstex IFC Toolbox JARs** (`com.apstex.ifctoolbox*.jar`, `com.apstex.stepparser*.jar`) in
  `bim-portal/Java_client/hackathon-template/lib/`. They read IFC files. They are not
  redistributable and not in git; the team keeps a copy in the shared Nextcloud folder
  `BIM_Deutschland_Hackathon`. Without them, everything that reads IFC (GUI, CLI, IFC tests) is
  left out of the build, and a test says what is missing.
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
```

The model is the bundled bridge model unless `CHECKMATE_IFC` names a file or a link, or `--model`
does. CLI options and exit codes: [`LoinCheckCli`](bim-portal/Java_client/hackathon-template/src/main/java/test/fx/LoinCheckCli.java)
and `docs/WORKFLOW.md`, section 2.5.

## Open decisions

- **Hard-coded property name mapping.** `PropertyAliases` maps three portal property names to the
  names one modelling convention (BIM.Hamburg, `Pset_Objektinformation/_IDEbene1..3`) uses. It
  only helps models following that convention, and IDS knows no such mapping. How mappings should
  be supplied is undecided: [STATUS #14](docs/STATUS.md).

## License

Apache License 2.0, see [LICENSE](LICENSE). The Apstex IFC Toolbox is not part of it and must not
be redistributed.
