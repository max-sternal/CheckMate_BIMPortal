# CheckMate status

What is being worked on and what is still open. One line per item. Reference code as
`file:line` where it helps.

## Open points

### GUI: `bim-portal/Java_client/hackathon-template/src/main/java/test/fx/`

1. No control has a fixed ID (`setId`). The `run-checkmate` driver finds controls by their visible text, so renaming a label or button in `ElementUI.java` silently breaks it. Fix both of these together:
   - Give the key controls IDs as constants in `ElementUI`.
   - Move the driver out of `.claude/skills/run-checkmate/` into `hackathon-template/src/test/java/com/bimportal/hackathon/devtools/CheckMateGuiDriver.java`, add a `runGuiDriver` Gradle task, and have the driver use those constants.

   The driver then compiles with the app, and `driver-classpath.init.gradle` is no longer needed.
2. Done (2026-11-20): "Load IFC" opens a file, "Load IFC link" a link (GitHub file links and `.ifczip` included).
3. "Connect MCP" has no handler (`ElementUI.java:112`). The MCP server exists (WORKFLOW 2.7) and works without the GUI; a chat inside the GUI, as in the pitch, is not implemented.
4. Done (2026-11-22): "BIM Portal" opens `https://via.bund.de/bim` (`ElementUI.java:160`).
5. Done (2026-11-20): the window opens at 1280x900, which fits the whole layout.

### LOIN check

6. Every portal LOIN is "nicht prüfbar" on the bundled bridge model (0 of 303 elements applicable). The model is classified after Objektkatalog `Ingenieurbau_V005`, whose level names differ from what the LOINs ask for. This needs a matching model or a classification mapping.
7. The LOIN search matches substrings, so "Schaft" also returns the "Landschaftsbau" LOINs. The GUI could rank or filter by the category path (`Ueberbau > Schaft`).

### Documentation

8. Done (2026-11-21): the repo root has a README with a map of the repo, the required software and a quick start.
9. `bim-portal/README.md` and `bim-portal/Java_client/README.md` are the hackathon starter kit's docs. They point to the `pb40development/bim-portal` repo and to a `bim-portal-client-suite` folder that doesn't exist.
10. `bim-portal/doku/README.md` is empty.
11. Done (2026-11-22): `.github/copilot-instructions.md` covers CheckMate (`ids/`, `ifc/`, `cli/`, `mcp/`, `test/fx`, Gradle tasks, and version branches).

### Repository

12. Done (2026-11-20): `bim-portal/.gitignore` still ignores `*.xml` and `*.ids`, except under `hackathon-template/src/test/resources/`, so test files are tracked.

### Open decisions

14. Done on `0.3.2` (2026-11-22): **the property name mapping is data, not code.** `PropertyMapping` reads a JSON file (`{"name": ..., "aliases": [{"from": "Klassifizierung/Objekttyp", "to": ["Pset_Objektinformation/_IDEbene3"]}]}`); the BIM.Hamburg convention is the bundled default (`src/main/resources/mapping/bim-hamburg.json`). Another file comes with `-Dcheckmate.mapping=<file>` or `CHECKMATE_MAPPING`, `--mapping <file|none|default>` on the CLI, or the MCP tool `load_mapping`. `none` checks exactly as ifctester does. Still open: a GUI control for it, and a mapping file per project next to the model.

15. Done on `0.3.2` (2026-11-22): **the IFC library.** Replaced the proprietary Apstex IFC Toolbox with a pure-Java, schema-driven STEP reader in `com.bimportal.hackathon.ifc` (`StepParser`, `IfcSchema`, `IfcModelElements`, `IfcResources`, `LoadedModel`), verified 1:1 against `IfcOpenShell 0.9` (0 diffs across 702 elements / 12,695 properties). No proprietary JARs or Python runtime needed.

### MCP server

16. Claude Desktop and Claude Code (Windows) start the server and list its tools (2026-11-22); Claude Desktop only with `JAVA_HOME` in the config's `env` (WORKFLOW 2.7). Not yet tried with Ollama through an MCP host such as `ollmcp`.
17. Done on `0.3.2` (2026-11-22): MCP Java SDK 2.2.0 and Jackson 2.22.4 (both `hackathon-template` and `openapi-generated`). The server uses the legacy (2025-11-25) server over stdio; the modern 2026-07-28 server is planned as a second entry point (docs/MCP-APPS-PLAN.md).
18. MCP Apps (docs/MCP-APPS-PLAN.md): done on `0.3.2` are the `io.modelcontextprotocol/ui` capability, `check_model` with `structuredContent` and the view `ui://checkmate/report`. Open: views for `find_loins` and `describe_model`, app-only tools, a test in Claude Desktop and the Antigravity App (the view is only tested at the protocol level), and the port to `0.4`.

### Code structure

13. The CheckMate GUI (`App`, `ElementUI`, `RequirementSource`) still lives in the package `test.fx` in the main sources, so the GUI looks like test code (the IFC reader and CLI already moved to `com.bimportal.hackathon.ifc` and `com.bimportal.hackathon.cli`). Rename `test.fx` to `com.bimportal.hackathon.gui`. This touches `build.gradle.kts` (`runGui`, the `test/fx` JavaFX exclusion), the `run-checkmate` skill and the READMEs. Coordinate with SOPHIEfree, who made the recent changes to this code.
