# How CheckMate works

This file describes how CheckMate is meant to work (sections 1-3) and how much of that works
today (section 4). As of 2026-11-22. Open points that are also listed in [STATUS.md](STATUS.md)
are referred to by their number there ("STATUS #2") instead of being repeated.

## 1. Goal

You give CheckMate an IFC model and the requirements it has to meet: one or more LOINs from the
BIM Portal. For each element, CheckMate says whether the requirements are met, and if not, why.

The check is one core with three front ends, which only collect input and present results:

- the JavaFX GUI,
- the headless CLI, for scripts and pipelines,
- later an MCP server, so that an LLM client can answer questions about single elements or the
  whole model.

## 2. How it should work

### 2.1 Load the model

- Input: a local file or a link. A GitHub file link (`github.com/.../blob/...`) is turned into its
  raw link, `.ifczip` is unzipped, and downloads have a size limit. Later, for the CLI: all IFC
  files of a GitHub folder or repository.
- Schemas: IFC2X3, IFC4 and IFC4X3.
- Read per element: entity, GlobalId, name, PredefinedType, and the single and enumerated property
  values of the element itself and of its type.
- A model that can't be read is reported as an error. It never turns into an empty model.
- Main classes: [`IfcResources`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ifc/IfcResources.java)
  finds the model, [`StepParser`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ifc/StepParser.java)
  and [`IfcSchema`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ifc/IfcSchema.java)
  parse STEP in pure Java, and [`IfcModelElements`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ifc/IfcModelElements.java)
  reads it into [`ModelElement`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/ModelElement.java) records.
- Output: the model's schema and a list of `ModelElement`s. They carry no IFC parser types, so the
  check can be tested without an IFC file.

### 2.2 Get the requirements

CheckMate downloads the requirements from the portal as IDS files and compares them with the
model; it never writes anything to the portal. (The portal's interface calls these downloads
"exports", hence the client's `exportLoinIds` and `exportProjectIds`.)

Four sources, all of which end up as IDS specifications:

- a LOIN ID: the GUID, or any text containing one, such as a link copied from the portal;
- an AIA project ID: the portal provides all LOINs of the project as one ZIP of IDS files. All
  of them are read, each LOIN and schema once;
- a local IDS file, or a ZIP of them;
- LOINs selected from the portal's catalogue (see 2.3).

Only the requirements of the LOINs to be checked are downloaded. Then:

- Downloads are cached per LOIN and version.
- From each IDS, every specification written for the model's schema is checked. A LOIN's IDS file
  from the portal has one per schema; other IDS files may have several, and one specification may name several
  schemas. If there is none for the schema, that is an error, not an empty result.
- Parts of IDS that CheckMate does not evaluate are listed in the report, never dropped silently.
  In the applicability they make the specification "nicht prüfbar", because without them it would
  select too many elements. In the requirements the rest is checked, but the specification cannot
  pass.
- Main classes: [`EnhancedBimPortalClient`](../bim-portal/Java_client/bim-portal-enhanced/src/main/java/com/pb40/bimportal/client/EnhancedBimPortalClient.java)
  (`exportLoinIds`, `exportProjectIds`), [`PortalRequirements`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/PortalRequirements.java)
  (LOINs and AIA projects by ID, search),
  [`IdsArchive`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/IdsArchive.java)
  (ZIPs of IDS files),
  [`IdsParser`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/IdsParser.java),
  [`IdsDocument`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/IdsDocument.java),
  [`IdsSpecification`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/IdsSpecification.java).

### 2.3 Choose the relevant LOINs

- By ID, directly (see 2.2): a LOIN, or an AIA project with all its LOINs. This is the usual case
  in a project, where the AIA fixes the LOINs.
- From the catalogue, when it isn't known yet which LOINs apply. This works on the catalogue's
  metadata alone; no requirements are downloaded to choose:
  1. The LOIN list with its metadata is loaded once (all LOINs, about 3 MB). Every entry carries
     object type, discipline, construction type, phases, use cases, organisation and version.
  2. Filter by context: discipline and phase, optionally use case. The portal can't filter by
     these itself, so this happens in CheckMate.
  3. Sort by fit: the most promising LOINs first. A LOIN fits when its object type (e.g.
     `Kappe`, or a part of `Stütze - Pfeiler`) occurs in the model: as a classification value
     (`Objekttyp`, `Objektklasse`, `Objektgruppe`, `_IDEbene1..n`, a user-defined type) or, weaker,
     as a word of an element's name. Compared without case and with umlauts written out. Elements
     that also carry the LOIN's group (the part of its name before `>`) put it ahead of others of
     the same object type. The list shows the count, e.g. "6 Element(e) mit 'Lager' in der
     Klassifizierung, 6 davon in Gruppe 'Lager'". LOINs that don't fit are hidden behind "alle
     anzeigen"; when none fits, all are shown. The search stays available.
  4. Clicking a LOIN shows a short description: name, version, organisation, object type,
     discipline, phases, use cases, its fit, and from its IDS file what it selects by, what it
     asks for and how many elements of the model it would apply to. That one IDS file (about
     11 KB) is downloaded for this and kept for the check. The portal's LOIN detail would be the
     natural source, but the generated client loses its IFC classes and property names.
  5. Several LOINs can be selected (Ctrl/Shift) and checked in one go. Only what was clicked or
     selected is downloaded.
- Main classes: [`CatalogueEntry`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/catalogue/CatalogueEntry.java),
  [`CatalogueFilter`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/catalogue/CatalogueFilter.java),
  [`ModelFit`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/catalogue/ModelFit.java),
  [`LoinDescription`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/catalogue/LoinDescription.java).
  CLI: `--suggest` lists the fitting LOINs with their IDs, `--discipline`, `--phase` and
  `--purpose` filter.

### 2.4 Check

- Follows the IDS rules. An element is checked when all applicability conditions hold. Each
  requirement then gives one finding per element: entity including `predefinedType`, property,
  value, data type, and cardinality (required, optional, prohibited).
- Data types are compared strictly, as IDS defines them: `IfcText` does not count as `IFCLABEL`.
  Elements that a related type would let through are named in a hint, e.g. "6 Elemente wären
  anwendbar, wenn IfcText statt IFCLABEL akzeptiert würde".
- Values are compared by the property's data type, as ifctester does: text exactly, including
  case (a value differing only in case is reported as "nur Groß-/Kleinschreibung weicht ab");
  integers as numbers; real numbers and measures within a relative tolerance of 1e-6; booleans as
  XML Schema writes them. A property without a value counts as missing.
- In IFC2X3 an element also has the entity of its type object: an `IfcBuildingElementProxy` typed
  by an `IfcBeamType` passes an `IFCBEAM` facet.
- Property set and property names are compared exactly, including case, as in IDS. A property
  written differently - only in case, or with umlauts written out, other separators or a leading
  underscore (`pset_slabcommon`, `Feld-Nummer`, `_Feldnummer`, `Laenge`) - is never accepted, but
  named so it can be fixed: in the finding ("vorhanden als 'pset_test/Status' (nur
  Groß-/Kleinschreibung weicht ab)"), in the diagnosis, and in a hint naming the elements it kept
  out of the applicability.
- Model-specific property names are mapped to the ones the LOIN asks for. The report says when a
  mapped name was used. **This mapping is hard-coded for one modelling convention and an open
  decision (STATUS #14).**
- When nothing applies, the result is "nicht prüfbar" with a diagnosis: how many elements each
  condition hits, and which elements came closest. It is never reported as passed.
- Main classes: [`IdsValidator`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/IdsValidator.java),
  [`PropertyAliases`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/PropertyAliases.java),
  [`DataTypes`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/DataTypes.java),
  [`Values`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/Values.java).

### 2.5 Results

- One summary line per LOIN, the findings per element, and the diagnosis when nothing applied.
- Each specification gets a verdict: erfüllt, nicht erfüllt, erfüllt (soweit ausgewertet), nicht
  prüfbar.
- The report starts with the date and the model, then names each set of requirements with its
  version and source: "CheckMate-Prüfung vom 21.11.2026 23:05", "Modell: Infra-Bridge.ifc -
  Schema IFC4, 77 Objekte", "Anforderungen: ...".
- Each specification is named with its schema and the phases (LPH) it is for: "Ueberbau > Kappe
  (Ingenieurbau): LPH 8-9 (IFC2X3, LPH 8-9)". The portal states the phases in each
  specification's `instructions` text ("Projektphasen: LPH 9: ..., LPH 8: ..."), not in an IDS
  field; IDS files without it show no phases.
- Every front end gets its report from the same place: `CheckService` checks, `ReportText` writes
  the text (in full for the GUI, briefly for the CLI and the MCP server), `ReportJson` the JSON.
- GUI: a table above the report, one row per specification and one for all of them: verdict,
  applicable elements, elements without and with defects. Clicking a row narrows the report and
  the element tabs to it. Element tabs and report share a split pane whose divider can be dragged.
- CLI options (Gradle task `runLoinCheck`, e.g. `--args="--loin <ID> --json build/r.json"`):
  - `--model <file or link>`: the model; without it the one `CHECKMATE_IFC` names, or the bundled
    one. Relative paths are relative to `hackathon-template/`.
  - `--loin <ID or link>`, `--aia <ID or link>`: a LOIN or AIA project from the portal; both take
    either kind and may be repeated. Search words holding an ID count as `--loin`.
  - `--ids <file>`: a local IDS file or a ZIP of them; may be repeated.
  - search words: check every AIA project and LOIN the portal finds for them. Without any
    requirements given, the search term is "Kappe".
  - `--discipline`, `--phase`, `--purpose`: narrow a search by the catalogue's metadata; a part of
    the value is enough. Values with spaces need quotes: `--phase 'LPH 5'`.
  - `--suggest`: list the LOINs that fit the model, with their IDs; checks nothing.
  - `--full`: the whole report, exactly as the GUI shows it, instead of a few examples per
    specification.
  - `--json <file>`: also write the report as JSON.
- CLI: the text report, `--json <file>` for the same as JSON, and an exit code: 0 everything met;
  1 the model or some requirements could not be read or checked; 2 unknown argument; 3 at least
  one specification not met; 4 nothing violated, but not everything confirmed (not applicable,
  partly evaluated, or nothing checked). Through `gradlew` every non-zero code becomes Gradle's
  own 1; the real one is in Gradle's message ("finished with non-zero exit value 3"), in the
  report's last line and in the JSON's `exitCode`.
- Main classes: [`CheckService`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/check/CheckService.java),
  [`CheckReport`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/report/CheckReport.java),
  [`ReportText`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/report/ReportText.java),
  [`ReportJson`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/report/ReportJson.java),
  [`CheckResult`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/CheckResult.java),
  [`ElementUI`](../bim-portal/Java_client/hackathon-template/src/main/java/test/fx/ElementUI.java),
  [`LoinCheckCli`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/cli/LoinCheckCli.java).

### 2.6 Quality

- Tests for the parser and the check, with real IDS files from the portal as test files and
  hand-built `ModelElement`s.
- ifctester (IfcOpenShell's IDS implementation) is the reference for the expected results.

### 2.7 AI apps: the MCP server

- Purpose: an AI app asks CheckMate about a model: "Which LOINs fit this bridge?", "Does it meet
  the Kappe LOIN for LPH 8?", "Why does this element fail?". The AI decides which tool to call and
  puts the answer in its own words; CheckMate does the checking. The AI sees only the tool
  answers, never the IFC file.
- MCP (Model Context Protocol) is how the app finds the tools (name, description, parameters) and
  calls them. The app starts CheckMate as a program of its own and talks to it over stdin/stdout,
  one JSON message per line. Nothing listens on the network.
- Install: `./gradlew :hackathon-template:installMcp` (from `bim-portal/Java_client/`) writes
  `hackathon-template/build/install/checkmate-mcp/`, with the start script
  `bin/checkmate-mcp.bat` (Windows) or `bin/checkmate-mcp` and every JAR in `lib/`. Run it again
  after code changes. It needs only Java 21 (no Apstex JARs and no JavaFX at runtime); the start
  script takes the Java in `JAVA_HOME`, else the first `java` on the path. It can't run through
  Gradle, which writes to stdout too.
- Tools (answers in German, as the CLI writes them):

  | Tool | What it does | Arguments |
  |---|---|---|
  | `load_model` | load a model from a path or a link; replaces the one before | `source` |
  | `describe_model` | name, schema, number of elements, most frequent entities | - |
  | `list_elements` | `GlobalId \| entity \| name \| PredefinedType` | `entity`, `name`, `limit` |
  | `find_loins` | AIA projects and LOINs from the catalogue, those fitting the model first, with IDs | `search`, `discipline`, `phase`, `purpose`, `limit`, `include_unfitting` |
  | `describe_requirements` | what a LOIN or AIA project selects and requires, how many elements it would apply to | `id` |
  | `check_model` | the report as the CLI prints it, `examples` unmet requirements per specification (default 3, 0 for the full report) | `ids`, `ids_files`, `examples` |
  | `load_mapping` | show or switch the JSON property mapping (`none`, `default`, a file) | `source` |
  | `server_status` | model, mapping and verdict of the last check | - |
  | `element_report` | what the last check said about an element, and its properties | `element` (GlobalId or part of the name) |

- One session per server process: the model in use (until `load_model`, the one `CHECKMATE_IFC`
  names or the bundled one), the last check for `element_report`, and every requirement downloaded
  so far, each downloaded once.
- An error is an answer, not a crash: the tool returns the message with `isError`, so the AI can
  pass it on ("IFC-Datei nicht gefunden: ...").
- Every answer has at most 50,000 characters (about 15,000 tokens). The full report of an AIA
  project has about 500,000; it is cut at a line end, and the verdict line is kept. The default
  brief report of the 36 specifications of an AIA project has about 16,000 characters.
- Logs go to stderr, which AI apps keep as the server's log. stdout carries only the protocol.
- Connecting an AI app. Use the full path of the start script (shortened here). On Windows the app
  starts the `.bat` through `cmd /c`; it can't start a `.bat` file itself.
  - Claude Desktop: Settings → Developer → Edit Config opens `claude_desktop_config.json` (the
    Microsoft Store install keeps it elsewhere than `%APPDATA%\Claude\`, so open it this way).
    Add to it:
    ```json
    { "mcpServers": { "checkmate": {
        "command": "cmd",
        "args": ["/c", "C:\\...\\hackathon-template\\build\\install\\checkmate-mcp\\bin\\checkmate-mcp.bat"],
        "env": { "JAVA_HOME": "C:\\...\\zulu21-jdk-fx" } } } }
    ```
    `JAVA_HOME` belongs in `env` even when it is set in Windows: the app passes the server its
    `PATH` but not `JAVA_HOME`, and if an older `java` comes first on the path, the server stops
    with `UnsupportedClassVersionError`. `"CHECKMATE_IFC": "C:\\...\\model.ifc"` in `env` sets the
    model to start with. Then quit the app completely (tray icon → Quit) and start it again; the
    server is listed under Settings → Developer.
  - Claude Code, in a terminal in the folder where Claude Code sessions start (the repo root):
    `claude mcp add checkmate -- cmd /c "C:\...\bin\checkmate-mcp.bat"`. This registers the
    server for that folder only; `--scope user` registers it for every folder. Not
    `--scope project`: it writes the machine's path into `.mcp.json` in the repo. Claude Code passes
    the server its own environment, so a `JAVA_HOME` set in Windows is enough; otherwise add
    `-e JAVA_HOME=C:\...\zulu21-jdk-fx`. `claude mcp list` shows whether it connects; a new
    session lists it under `/mcp`.
  - Ollama: Ollama runs models but does not speak MCP itself; an MCP host in between does, e.g.
    `ollmcp` (`pip install mcp-client-for-ollama`) with the same `mcpServers` entry, or Open WebUI.
    The model must support tool calls (e.g. Qwen 3, Llama 3.1). Small models have small context
    windows, so keep `examples` low.
- When the server doesn't start, read its log first. Claude Desktop writes it to
  `mcp-server-checkmate.log` in its `logs` folder (`%APPDATA%\Claude\logs\`, for the Store install
  `%LOCALAPPDATA%\Packages\Claude_<id>\LocalCache\Local\Claude\logs\`), including what the server
  writes to stderr.
- Known limits: tried with Claude Desktop and Claude Code on Windows and with a script that speaks
  the protocol (every tool, errors); not yet with Ollama (STATUS #16). Tools only: no MCP resources
  or prompts, no progress messages during a long check. Paths are read on the machine the server
  runs on.
- Main classes: [`CheckMateMcpServer`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/mcp/CheckMateMcpServer.java)
  (tools, parameters, transport), [`CheckMateTools`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/mcp/CheckMateTools.java)
  (what each tool does, on `CheckService`, tested without the protocol).

## 3. Background: what the portal delivers

Measured on 2026-11-20 against the public portal `https://via.bund.de/bim`:

- 1,568 public LOINs. Loading the whole list takes about 7 s and 3 MB (2 pages). Each entry
  carries object type, discipline, construction type, phases, use cases, organisation and version;
  the `description` is empty for all but 51. The search only filters by name text, not by these
  fields (measured 2026-11-21).
- The detail of one LOIN (`GET /loin/{guid}`, about 8 KB) adds the IFC class (`dataFormats`), the
  required properties by name, the classification it selects by, use cases and dates. The
  generated client loses part of it: `dataFormats` arrive as `null` (the portal writes
  `"@class": "ifc"`, the client expects `IFCDto`), and the property groups come without their
  properties.
- One LOIN's IDS file is about 11 KB and takes about 0.5 s. It is valid IDS 1.0, with one
  specification per schema (IFC2X3, IFC4, IFC4X3_ADD2). It uses only a small part of IDS: property
  facets with fixed values, and entity facets (name, sometimes `predefinedType`). It contains all
  properties the LOIN lists. Its `<info>` carries the LOIN's name and version.
- Each LOIN has two IDs: the `guid`, which stays the same across versions, and the `id` of each
  version's record. The IDS download and the detail endpoint only answer to the `guid`; for the
  `id` they return 404 "Die LOIN existiert nicht". CheckMate looks an unknown ID up among the
  catalogue's `id`s (measured 2026-11-21).
- The Ingenieurbau LOINs select elements by `Klassifizierung/Objektgruppe|Objektklasse|Objekttyp`.
  Building LOINs such as "Fenster" select by IFC entity.
- An AIA project's IDS download is a ZIP: `IDS/AIA_<project>_<guid>_<version>.ids` with every LOIN
  of the project, and for 3 of the 4 public projects also `IDS/LOIN_<name>_<guid>_<version>.ids`
  per LOIN. The combined file holds IFC4 only; in the two road projects each LOIN's own file also
  holds IFC2X3 and IFC4X3_ADD2. A specification's `identifier` is `<LOIN guid> <n>`, where n only
  counts within a file. One project ("Beispiel AIA Bundesbauten") holds two different LOINs of the
  same name. Downloading one takes 1-5 s (measured 2026-11-21).
- The 4 public AIA projects: "AIA FB Straße Demonstrator FM Verkehrsanlage" and "BAB 1
  Grundhafter Ausbau, Bergkamen" (19 LOINs each, LPH 3, by classification), "Beispiel AIA
  Bundesbauten" (32 LOINs, by entity), "Beispiel-AIA Neubau Eisenbahnbrücke" (36 LOINs, LPH 5).
  Every specification of the railway project selects any user-defined `IfcBuildingElementProxy`;
  which LOIN an element belongs to only shows in the property set names. So each such proxy is
  checked against all 36 LOINs: on the IFC4 bridge sample, 9 proxies x 36, all failing, in
  CheckMate and in ifctester alike.
- On a copy of the bridge model with planted errors, our check and ifctester 0.9 give the same
  verdicts. Before the data type rule of section 5 was implemented, they differed in exactly that
  rule.

## 4. Checklist

`[x]` works as described above, `[ ]` missing or only partly there.

### Load the model

- [x] The bundled IFC2X3 bridge model loads; `CHECKMATE_IFC` or `-Dcheckmate.ifc` points at another file.
- [x] Load from a file ("Load IFC") or a link ("Load IFC link") in the GUI, in the background. The new model replaces the old one; a check already running keeps the model it started on.
- [x] Links: a GitHub file link becomes its raw file, a Git LFS pointer is followed to the file, and the download is recognised by its first bytes, so a web page or an error page is reported as such. 200 MB limit; the download is deleted after reading.
- [x] `.ifczip`, from a file or a link.
- [x] CLI: `--model <file or link>`; `CHECKMATE_IFC` takes a link too.
- [x] If the configured model can't be read at startup, the GUI says why and starts with the bundled model.
- [ ] All IFC files of a GitHub folder or repository (CLI, later).
- [x] IFC2X3, IFC4 and IFC4X3; checked against IfcOpenShell on the bundled model and five downloaded samples.
- [x] Every property is read by the pure-Java, schema-driven STEP reader (`com.bimportal.hackathon.ifc`), resolving property and type relations via `RelatedObjects` (verified 1:1 against IfcOpenShell 0.9 with 0 diffs across 702 elements / 12,695 properties).
- [x] Single, enumerated and list values, from the element and its type; quantity sets are skipped.
- [x] PredefinedType and "user-defined" the way IfcOpenShell reads them.
- [x] Load errors are reported with the file name (`ModelLoadException`): the GUI shows a dialog, the CLI exits with code 1.
- [ ] A property relation pointing at a set of property sets (IFC4): read, but untested, because IfcOpenShell cannot write such a file.
- [ ] Bounded, table and complex properties, and properties in quantity sets: not read. IDS 1.0 property facets can address both; the portal's LOINs don't.

### Get the requirements

- [x] Load the LOIN list from the portal, paged (GUI).
- [x] Download a LOIN's IDS file and pick the specification for the model's schema.
- [x] Picking the specification works across addenda: an IFC4X3 model gets the IFC4X3_ADD2 block, not the IFC4 one.
- [x] `ifcVersion` listing several schemas (`"IFC2X3 IFC4"`), which IDS 1.0 allows.
- [x] Every specification for the model's schema is checked, one result each; a LOIN's IDS file from the portal has one, other IDS files may have several.
- [x] The parser reads what the portal writes: property facets and the entity name.
- [x] Entity name and `predefinedType` as a value, a pattern (several patterns are alternatives) or an `xs:enumeration`; `USERDEFINED` asks for a user-defined type, as in ifctester.
- [x] `cardinality`: required, optional and prohibited.
- [x] IDS parts CheckMate doesn't evaluate are listed in the report ("Nicht ausgewertet ..."): in the applicability the specification is then "nicht prüfbar", in the requirements it cannot pass. The portal's IDS files contain none.
- [ ] Not evaluated yet: attribute, classification, material and partOf facets; property values given as pattern, enumeration or bounds (only presence and data type are checked); property set or name given as a pattern.
- [ ] Specification cardinality: `maxOccurs="0"` (no element may apply) is listed as not evaluated; `minOccurs="1"` (at least one must apply) is not distinguished from optional.
- [x] LOIN or AIA project by ID: the GUID, or any text holding one (a link, a note). GUI: in the search field, checked at once. CLI: `--loin` or `--aia` (both take either kind), repeatable, or a search term holding an ID. A version record's `id` is resolved through the project list, then the LOIN catalogue.
- [x] AIA project as a source: by ID, from the search (projects are listed above the LOINs), or as a downloaded ZIP. The combined file and the LOIN files are merged, each LOIN and schema once, so an IFC2X3 model can be checked against the road projects although their combined file is IFC4 only.
- [x] Local IDS file or ZIP of IDS files: GUI "Load IDS", checked at once; CLI `--ids`, repeatable. Not IDS, not found, or not readable is reported with the file name.
- [ ] One project, one model, but every specification selecting the same elements (as in the railway project) gives a long report; the LOIN that an element belongs to would have to come from its property set names.
- [ ] IDS file from a link.
- [ ] Cache for downloaded IDS files, and for the LOIN list, which is loaded at every start.
- [ ] Search: it matches parts of names, so "Schaft" also finds "Landschaftsbau" (STATUS #7). A search term of several words works ("Ueberbau > Schaft" finds exactly those 2 LOINs), the CLI pages like the GUI, and both also find AIA projects since 2026-11-21.

### Choose the LOINs

- [x] Context filters for discipline, phase and use case (GUI: three lists with the values the catalogue holds; CLI: `--discipline`, `--phase`, `--purpose`, a part of the value is enough).
- [x] Sort the catalogue by fit to the model, from the metadata alone. On the bundled bridge 103 of 1,568 LOINs fit, 59 with the filters LPH 5 and Ingenieurbau; the first are "Bewehrung", "Gruendung", "Widerlager" and "Lager".
- [x] A short description of a LOIN on click, including how many elements its requirements would apply to.
- [x] Select several LOINs and check them in one go; only what is clicked or selected is downloaded, once per session.
- [x] CLI: `--suggest` lists the fitting LOINs with their IDs, without downloading any.
- [ ] Cache the LOIN list between starts (3 MB, about 7 s at every start).
- [ ] Building models with English or no names: nothing fits by object type there (e.g. the buildingSMART samples). The IFC class of each LOIN would help, but only the LOIN detail has it, and the client loses it (section 3).

### Check

- [x] Applicability (all conditions must hold), and one finding per element and requirement.
- [x] Diagnosis when nothing applies: hits per condition and the closest elements.
- [ ] **TODO, open decision (STATUS #14):** mapping of the BIM.Hamburg classification (`Pset_Objektinformation/_IDEbene1..3`) - works, but hard-coded for that one convention.
- [x] Property set and property names compared exactly; other spellings (case, umlauts, separators, leading underscore) are named in the finding, the diagnosis and a hint, never accepted.
- [x] Values by data type, as ifctester: text exactly (a case-only difference is named), integers exactly as numbers (`42` = `42.0`), reals and measures within a relative tolerance of 1e-6, booleans `true`/`1`/`false`/`0`. A number stored as `IfcLabel` is text.
- [x] Data types strict, as in IDS, in the applicability and the requirements, with a hint naming the elements a related type would let through (section 5).
- [x] A prohibited property is met when it is missing or has another value or type.
- [x] An entity's `predefinedType` is checked; a mismatch is "PredefinedType weicht ab".
- [x] A property without a value (empty, or `UNKNOWN` of an `IfcLogical`) counts as missing: "vorhanden, aber ohne Wert".
- [x] IFC2X3: an element also has the entity of its type object (`IfcBeamType` → `IFCBEAM`, `IfcDoorStyle` → `IFCDOOR`), as ifctester does. Only for IFC2X3 models.
- [x] The diagnosis of an entity facet says how many elements have the entity and which PredefinedTypes they carry instead, e.g. "2 Element(e) dieser Entity, keines mit benutzerdefiniertem Typ (gefunden: WINDOW 1, ohne 1)", or "kein Element dieser Entity im Modell".
- [x] Elements in the order of the file (by STEP line), so two reports on the same model read the same.
- [ ] Values with a unit are compared as stored; ifctester converts them to SI units first (a model in millimetres against an IDS in metres).

### Results

- [x] Text report in the GUI and the CLI.
- [x] Summary table across several LOINs (GUI), with a "Gesamt" row; clicking a row narrows report and element tabs. The text report starts with a tally and one line per specification with its verdict.
- [x] All checked elements: 20 get a tab each, with what every specification said about them; one more tab lists the rest with how many lines are open.
- [x] Report header with date and model; each set of requirements with name, version and source; each specification with its phases (LPH).
- [x] One `CheckService` and one `ReportText` for every front end; the CLI's `--full` prints exactly the GUI's report.
- [x] CLI: `--json <file>`, and exit codes 0 / 1 / 2 / 3 / 4 (section 2.5).
- [ ] Pass the exit code through to scripts: `gradlew` turns every non-zero code into 1. A start script (the `application` plugin's `installDist`) for `LoinCheckCli` would keep it; that changes `build.gradle.kts`. Deferred (2026-11-22) until CheckMate runs in scripts or pipelines.
- [ ] Disable "Validate LOIN" while a check runs; a double-click currently starts a second check.

### Front ends and quality

- [x] Headless CLI: `runLoinCheck`.
- [x] Window size: the GUI opens at 1280x900 (STATUS #5). Many element tabs no longer squeeze the LOIN list.
- [ ] GUI cleanup: chat placeholder (STATUS #3) and deciding whether to keep the class filter tab (`ElementUI.createObjectTab` now reads `ModelElement` across IFC2X3, IFC4 and IFC4X3). The "BIM Portal" button opens `https://via.bund.de/bim` (STATUS #4).
- [x] MCP server for AI apps: stdio, seven tools on `CheckService`, `installMcp` (section 2.7).
- [x] Connect the MCP server to Claude Desktop and Claude Code (Windows).
- [ ] Try the MCP server with Ollama through an MCP host (STATUS #16).
- [x] Tests for the `ids` package ([`src/test/java/com/bimportal/hackathon/ids/`](../bim-portal/Java_client/hackathon-template/src/test/java/com/bimportal/hackathon/ids)), with real IDS files from the portal as test files ([`src/test/resources/ids/portal/`](../bim-portal/Java_client/hackathon-template/src/test/resources/ids/portal/README.md)).
- [x] Tests for reading IFC ([`src/test/java/com/bimportal/hackathon/ifc/`](../bim-portal/Java_client/hackathon-template/src/test/java/com/bimportal/hackathon/ifc)): small case files of our own per schema ([`make_ifc_fixtures.py`](../bim-portal/Java_client/hackathon-template/src/test/python/make_ifc_fixtures.py)), the bundled model, and sample models that `gradlew :hackathon-template:downloadTestModels` fetches into the ignored `test-models/` (about 5 MB, list in [`samples.properties`](../bim-portal/Java_client/hackathon-template/src/test/resources/ifc/samples.properties)).
- [x] Missing prerequisites are reported, not failed: without JavaFX only the GUI (`test/fx`) is skipped while all IFC, CLI, MCP and core tests run; without the sample models the tests needing them are skipped, and `PrerequisitesTest` says in each case what to add and where.

## 5. Decisions

- 2026-11-20, data types: compared strictly, as IDS defines them, with a hint naming the elements a
  related type would let through. Before, related types such as `IfcText` for `IFCLABEL` were
  accepted, and the applicability ignored the data type.
- 2026-11-20, choosing LOINs: by ID first (LOIN, AIA project, local IDS file). Sorting the
  catalogue by fit comes after that.
- 2026-11-21, IDS parts CheckMate does not evaluate: listed in the report, never dropped. In the
  applicability they make the specification "nicht prüfbar"; in the requirements the rest is
  checked and the specification cannot pass.
- 2026-11-21, AIA project ZIPs: every IDS file in the ZIP is read, the combined file first; a
  specification is skipped when one for the same LOIN (the GUID in its identifier) and schema was
  already taken. Not by name, since names repeat.
- 2026-11-21, choosing from the catalogue: by metadata only (filters, sorting by object type, a
  short description on click). Requirements are downloaded only for the LOINs selected for a
  check, never for the whole catalogue or a candidate list. The earlier plan of an exact
  applicability test on downloaded candidates is dropped.
- 2026-11-21, values: compared by data type as ifctester does, text including case. Where only
  the case differs, the finding says so, the same way the data type hint explains a strict verdict.
- 2026-11-21, property names: exactly as asked, as in IDS. Other spellings are shown so they can
  be fixed, not accepted. The alias mapping stays until a decision (STATUS #14).
- 2026-11-22, one core for every front end: GUI, CLI and the MCP server only collect input and
  present results; checking (`CheckService`) and writing the report (`ReportText`, `ReportJson`)
  happen once, in the core.
- 2026-11-22, start script and IFC library: no start script until CheckMate runs in scripts or
  pipelines. On `0.3.2`, replaced the proprietary Apstex IFC Toolbox with a pure-Java schema-driven
  STEP reader (`com.bimportal.hackathon.ifc`, schema tables for IFC2X3/4/4X3), keeping CheckMate
  100% Apache 2.0 with zero external IFC runtime (STATUS #15).
- 2026-11-22, MCP server: the official MCP Java SDK (`mcp-core` and `mcp-json-jackson2` 2.1.0;
  2.2.0 depends on a release candidate of Reactor), over stdio, without Spring. The tools answer
  with the text report, not JSON: the AI reads it and answers in its own words. The server has its
  own start script, since it can't run through Gradle.
- 2026-11-21, exit codes: 3 when a specification is not met, 4 when nothing is violated but not
  everything is confirmed. "Nicht prüfbar" is never 0, in line with the rule that it is never
  reported as passed. Read errors (1) take precedence, since the report is then incomplete.
- 2026-11-21, the description on click comes from the clicked LOIN's IDS file, not the portal's
  detail: one download per click, reused for the check, and it can say how many elements the
  LOIN would apply to.
