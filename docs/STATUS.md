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

14. **TODO, to be decided: the hard-coded property name mapping.** `PropertyAliases` (`hackathon-template/src/main/java/com/bimportal/hackathon/ids/PropertyAliases.java`) maps the portal's `Klassifizierung/Objektgruppe|Objektklasse|Objekttyp` to `Pset_Objektinformation/_IDEbene1..3`. That is the convention of the BIM.Hamburg bridge models bundled with the project, written into the code for them. It is not a rule: no spelling rule can tell that these names mean the same, other models with other conventions get no mapping, and IDS knows no aliases (ifctester reports these properties as missing). The report states every use of an alias ("als Pset_Objektinformation/_IDEbene3"). To decide: whether mappings stay at all, and if so how they are supplied - e.g. a mapping file per project or convention, passed with the model - instead of code.

15. Done on `0.3.2` (2026-11-22): **the IFC library.** Replaced the proprietary Apstex IFC Toolbox with a pure-Java, schema-driven STEP reader in `com.bimportal.hackathon.ifc` (`StepParser`, `IfcSchema`, `IfcModelElements`, `IfcResources`, `LoadedModel`), verified 1:1 against `IfcOpenShell 0.9` (0 diffs across 702 elements / 12,695 properties). No proprietary JARs or Python runtime needed.

### MCP server

16. Claude Desktop and Claude Code (Windows) start the server and list its tools (2026-11-22); Claude Desktop only with `JAVA_HOME` in the config's `env` (WORKFLOW 2.7). Not yet tried with Ollama through an MCP host such as `ollmcp`.
17. The MCP SDK is built for Jackson 2.21; the project pins Jackson to 2.15.2 (`build.gradle.kts`), so Gradle hands the SDK 2.15.2. Every message CheckMate uses works and is tested; if an AI app's message ever fails to parse, raise Jackson first.

### Code structure

13. The CheckMate GUI (`App`, `ElementUI`, `RequirementSource`) still lives in the package `test.fx` in the main sources, so the GUI looks like test code (the IFC reader and CLI already moved to `com.bimportal.hackathon.ifc` and `com.bimportal.hackathon.cli`). Rename `test.fx` to `com.bimportal.hackathon.gui`. This touches `build.gradle.kts` (`runGui`, the `test/fx` JavaFX exclusion), the `run-checkmate` skill and the READMEs. Coordinate with SOPHIEfree, who made the recent changes to this code.
