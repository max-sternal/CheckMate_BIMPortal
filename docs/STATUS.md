# CheckMate status

What is being worked on and what is still open. One line per item. Reference code as
`file:line` where it helps.

## Current handoff (2026-11-23)

- `0.3.3` is the active development branch (local only, see below). It inherits everything from
  `0.3.2` and adds the chat in the GUI (GUI point 6).
- `0.3.2` is the released development state: Java 25 (LTS), Gradle 9.1.0, Mockito 5.18.0 /
  ByteBuddy 1.17.5, a pure-Java IFC reader without APSTEX, configurable JSON property mappings,
  GUI, CLI and MCP server.
- The MCP server uses SDK 2.2.0 and Jackson 2.22 (databind/core/JSR310 2.22.4, annotations
  2.22). The report MCP App and app-only `app_report_page` are implemented and protocol-tested;
  the view has not yet been visually tested in Claude Desktop or Antigravity.
  See [MCP-APPS-PLAN.md](MCP-APPS-PLAN.md).
- The DE/EN `0.3.2` documentation, including the Hamburg mapping and worked check, is prepared in
  `website/` but is **not the current public version**. GitHub Pages serves `v0.3.1` as latest and
  has a version selector for `v0.1.0`, `v0.2.0` and `v0.3.1`. The older two pages were written
  retrospectively; their tags preserve the historical commits.
- Tags and releases exist for `v0.1.0`, `v0.2.0` and `v0.3.1`; there is no `v0.3.2` release yet.
  The versioned Pages workflow runs from `0.3.2`, but builds latest from the tag named by
  `CHECKMATE_DOCS_LATEST` (`v0.3.1`). The old single-version workflow is disabled. See
  [`website/README.md`](../website/README.md) before publishing; do not switch versions until
  release is approved.
- `0.3.3` branched off `0.3.2` on 2026-11-23 and is local only, not yet pushed. Planned there:
  an LLM chat inside the GUI through an API key (below), direct LOIN-XML support, BCF export, and
  additional BIM Portal endpoints (Fachmodelle, Projekte, Vorlagen, Kontextinfos).
  `0.4` is a remote development branch that has moved on to a TypeScript/IFClite monorepo
  (`@checkmate/core`, `/cli`, `/mcp`, `/web`) and ported the `0.3.2` feature set in `4e49ea7`;
  it carries the online viewer / 3D direction.
- CI for `0.3.2` uses Temurin JDK 25 without bundled JavaFX; optional tests may be skipped.

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
6. Built on `0.3.3` (2026-11-23), **answering path not yet verified**: the chat from point 3,
   driven by an LLM over an API key, so CheckMate can be shown without a separate MCP host. An
   alternative to Claude Desktop, not a replacement for the MCP server - see
   [WORKFLOW.md](WORKFLOW.md) 2.8. `com.bimportal.hackathon.chat.CheckMateChat` holds the
   conversation, the tool definition and the tool-use loop (at most 6 calls per question) on the
   official Anthropic Java SDK with `claude-opus-5-5`; `ElementUI.askChat` and
   `ElementUI.checkForChat` are the GUI side.
   - `check_model` is wired to the GUI's own check, so an answer fills the summary table, the
     report panel and the element tabs as the buttons do. It does **not** go through
     `CheckMateTools`, as first planned: that class keeps its own model and its own last report, so
     the panel would drift from it. The shared layer is one below - `CheckService` and
     `ReportText`, which the MCP server uses too.
   - The tool answer is the short report, capped at 50,000 characters as in the MCP server, so no
     whole report enters a prompt; the panel keeps the full one.
   - The model is configuration, not code: `CHECKMATE_LLM_MODEL` names it (default
     `claude-opus-5-5`) and `CHECKMATE_LLM_BASE_URL` an endpoint other than Anthropic's own that
     speaks the Messages format, e.g. OpenRouter, where one key reaches the models of many vendors
     and travels as a bearer token. Tool, schema and loop are untouched by this, so the same
     question can be put to several models - which of them call `check_model` correctly is a result
     worth having, and the place where small and free models are expected to fail.
   - `ANTHROPIC_API_KEY` from the environment or `.env` (`*.env` is ignored in
     `bim-portal/.gitignore`), never in the repository. Without a key the chat stays inactive and
     the rest of the GUI works unchanged, so the app keeps running without credentials.
   - **Open:** no question has been answered end to end. A deliberately invalid key reaches the API
     and comes back as `401 invalid x-api-key`, so the SDK, the transport and the request shape are
     proven; the tool loop and the model's behaviour are not. Also open: a GUI control for the
     mapping (point 14), `find_loins` as a second tool so the model can search the catalogue
     itself, and server-side refusal fallbacks, which would need the SDK's beta endpoint - today a
     refusal is caught and shown as a message.
   - Two build defects came out of wiring it up, both fixed and both older than this feature:
     the GUI vanished from `build/classes` whenever Gradle ran on a JDK without bundled JavaFX
     (`c3240d5`), and `bim-portal-enhanced`'s jar bundled its whole runtime classpath, so Jackson
     loaded as 2.18.1 from it and the Anthropic SDK refused to start (`a8da1f0`).

### LOIN check

6. The saved portal IDS "Ueberbau > Kappe" is "nicht prüfbar" on the bundled bridge model (0 of 303 elements applicable). The default Hamburg mapping finds differently named properties, but values and data types still differ (e.g. `Kappe` vs `Ueberbau`, `IFCTEXT` vs `IFCLABEL`). See the [worked example](../website/src/content/docs/guides/example.md); a mapping does not resolve value or type differences.
7. The LOIN search matches substrings, so "Schaft" also returns the "Landschaftsbau" LOINs. The GUI could rank or filter by the category path (`Ueberbau > Schaft`).

### Documentation

8. Done (2026-11-21): the repo root has a README with a map of the repo, the required software and a quick start.
9. `bim-portal/README.md` and `bim-portal/Java_client/README.md` are the hackathon starter kit's docs. They point to the `pb40development/bim-portal` repo and to a `bim-portal-client-suite` folder that doesn't exist.
10. `bim-portal/doku/README.md` is empty.
11. Done (2026-11-22): `.github/copilot-instructions.md` covers CheckMate (`ids/`, `ifc/`, `cli/`, `mcp/`, `test/fx`, Gradle tasks, and version branches).

### Repository

12. Done (2026-11-20): `bim-portal/.gitignore` still ignores `*.xml` and `*.ids`, except under `hackathon-template/src/test/resources/`, so test files are tracked.

### Open decisions

14. Done on `0.3.2` (2026-11-22): **the property name mapping is data, not code.** `PropertyMapping` reads a JSON file (`{"name": ..., "aliases": [{"from": "Klassifizierung/Objekttyp", "to": ["Pset_Objektinformation/_IDEbene3"]}]}`); the BIM.Hamburg convention is the bundled default (`src/main/resources/mapping/bim-hamburg.json`). Another file comes with `-Dcheckmate.mapping=<file>` or `CHECKMATE_MAPPING`, `--mapping <file|none|default>` on the CLI, or the MCP tool `load_mapping`. `none` disables aliases and uses the IDS property names. Still open: a GUI control for it, and a mapping file per project next to the model.

15. Done on `0.3.2` (2026-11-22): **the IFC library.** Replaced the proprietary Apstex IFC Toolbox with a pure-Java, schema-driven STEP reader in `com.bimportal.hackathon.ifc` (`StepParser`, `IfcSchema`, `IfcModelElements`, `IfcResources`, `LoadedModel`), verified 1:1 against `IfcOpenShell 0.9` (0 diffs across 702 elements / 12,695 properties). No proprietary JARs or Python runtime needed.

### MCP server

16. Claude Desktop and Claude Code (Windows) start the server and list its tools (2026-11-22); Claude Desktop only with `JAVA_HOME` in the config's `env` (WORKFLOW 2.7). Not yet tried with Ollama through an MCP host such as `ollmcp`.
17. Done on `0.3.2` (2026-11-22): MCP Java SDK 2.2.0 and Jackson 2.22 (databind/core/JSR310 2.22.4; annotations 2.22) in `hackathon-template` and `openapi-generated`. The server uses the legacy (2025-11-25) server over stdio; the modern 2026-07-28 server is planned as a second entry point (docs/MCP-APPS-PLAN.md). Two follow-ups were needed on `0.3.3`: Spring Boot's BOM forced `jackson-annotations` back to 2.18.1 next to databind 2.22.4 (`5ce0d6f`), and `bim-portal-enhanced`'s jar bundled Jackson 2.18.1 classes that shadowed the resolved ones at run time (`a8da1f0`). Check `jackson.databind.cfg.PackageVersion.VERSION` at run time, not only `gradlew dependencies`.
18. MCP Apps (docs/MCP-APPS-PLAN.md): done on `0.3.2` are the `io.modelcontextprotocol/ui` capability, `check_model` with `structuredContent`, all three views (`report`, `loins`, `model`) with app-only `app_report_page` for more findings, JSON resources and prompts. Open: seeing the views in Claude Desktop and Antigravity (protocol-tested only) - the largest remaining item before a `0.3.2` release - and a possible port to `0.4`.

### Code structure

13. The CheckMate GUI (`App`, `ElementUI`, `RequirementSource`) still lives in the package `test.fx` in the main sources, so the GUI looks like test code (the IFC reader and CLI already moved to `com.bimportal.hackathon.ifc` and `com.bimportal.hackathon.cli`). Rename `test.fx` to `com.bimportal.hackathon.gui`. This touches `build.gradle.kts` (`runGui`, the `test/fx` JavaFX exclusion), the `run-checkmate` skill and the READMEs. Coordinate with SOPHIEfree, who made the recent changes to this code.
