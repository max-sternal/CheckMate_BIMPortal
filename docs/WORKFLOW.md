# How CheckMate works

This file describes how CheckMate is meant to work (sections 1-3) and how much of that works
today (section 4). As of 2026-11-20. Open points that are also listed in [STATUS.md](STATUS.md)
are referred to by their number there ("STATUS #2") instead of being repeated.

## 1. Goal

You give CheckMate an IFC model and the requirements it has to meet: one or more LOINs from the
BIM Portal. For each element, CheckMate says whether the requirements are met, and if not, why.

The check is one core with three front ends:

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
- Main classes: [`IfcResources`](../bim-portal/Java_client/hackathon-template/src/main/java/test/fx/IfcResources.java)
  finds the model, [`IfcModelElements`](../bim-portal/Java_client/hackathon-template/src/main/java/test/fx/IfcModelElements.java)
  reads it into [`ModelElement`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/ModelElement.java) records.
- Output: the model's schema and a list of `ModelElement`s. They carry no IFC library types, so the
  check can be tested without an IFC file.

### 2.2 Get the requirements

Four sources, all of which end up as IDS specifications:

- a LOIN ID: the GUID, or any text containing one, such as a link copied from the portal;
- an AIA project ID: the portal exports all LOINs of the project in one ZIP;
- a local IDS file;
- a selection from the portal's LOIN catalogue (see 2.3).

Then:

- Downloads are cached per LOIN and version.
- From each IDS, the specification for the model's schema is picked. If there is none, that is an
  error, not an empty result.
- Main classes: [`EnhancedBimPortalClient`](../bim-portal/Java_client/bim-portal-enhanced/src/main/java/com/pb40/bimportal/client/EnhancedBimPortalClient.java)
  (`exportLoinIds`, `exportProjectIds`), [`LoinIdsCheck`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/LoinIdsCheck.java),
  [`IdsParser`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/IdsParser.java),
  [`IdsSpecification`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/IdsSpecification.java).

### 2.3 Choose the relevant LOINs

- By ID, directly (see 2.2). This is the usual case in a project, where the AIA fixes the LOINs.
- From the catalogue, when it isn't known yet which LOINs apply:
  1. Filter by context: discipline and phase, optionally use case. Every catalogue entry carries
     this metadata.
  2. Sort by fit: first a quick match of each LOIN's object type against the model's
     classification values, then an exact applicability test on the remaining candidates.
  3. LOINs that don't fit are hidden behind "alle anzeigen". The search stays available.
- Several LOINs can be selected and checked in one go.

### 2.4 Check

- Follows the IDS rules. An element is checked when all applicability conditions hold. Each
  requirement then gives one finding per element: entity including `predefinedType`, property,
  value, data type, and cardinality (required, optional, prohibited).
- Data types are compared strictly, as IDS defines them: `IfcText` does not count as `IFCLABEL`.
  Elements that a related type would let through are named in a hint, e.g. "6 Elemente wären
  anwendbar, wenn IfcText statt IFCLABEL akzeptiert würde".
- Model-specific property names are mapped to the ones the LOIN asks for. The report says when a
  mapped name was used.
- When nothing applies, the result is "nicht prüfbar" with a diagnosis: how many elements each
  condition hits, and which elements came closest. It is never reported as passed.
- Main classes: [`IdsValidator`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/IdsValidator.java),
  [`PropertyAliases`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/PropertyAliases.java),
  [`DataTypes`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/DataTypes.java).

### 2.5 Results

- One summary line per LOIN, the findings per element, and the diagnosis when nothing applied.
- The report states the model, the LOIN name and version, and the date.
- GUI: a table across all checked LOINs. Clicking a row shows the details per element.
- CLI: text or JSON output, and an exit code other than 0 when requirements are not met.
- Main classes: [`CheckResult`](../bim-portal/Java_client/hackathon-template/src/main/java/com/bimportal/hackathon/ids/CheckResult.java),
  [`ElementUI`](../bim-portal/Java_client/hackathon-template/src/main/java/test/fx/ElementUI.java),
  [`LoinCheckCli`](../bim-portal/Java_client/hackathon-template/src/main/java/test/fx/LoinCheckCli.java).

### 2.6 Quality

- Tests for the parser and the check, with real portal exports as test files and hand-built
  `ModelElement`s.
- ifctester (IfcOpenShell's IDS implementation) is the reference for the expected results.

## 3. Background: what the portal delivers

Measured on 2026-11-20 against the public portal `https://via.bund.de/bim`:

- 1,568 public LOINs. Loading the whole list takes about 8 s. Each entry carries discipline,
  phase, use case, construction type and object type.
- One LOIN's IDS export is about 11 KB and takes about 0.5 s. It is valid IDS 1.0, with one
  specification per schema (IFC2X3, IFC4, IFC4X3_ADD2). It uses only a small part of IDS: property
  facets with fixed values, and entity facets (name, sometimes `predefinedType`). It contains all
  properties the LOIN lists.
- The Ingenieurbau LOINs select elements by `Klassifizierung/Objektgruppe|Objektklasse|Objekttyp`.
  Building LOINs such as "Fenster" select by IFC entity.
- An AIA project's IDS export is a ZIP: one combined IDS for the project, sometimes plus one IDS
  per LOIN. There are 4 public AIA projects, e.g. "Beispiel-AIA Neubau Eisenbahnbrücke" with 36
  LOINs for LPH 5 (IFC4 only).
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
- [x] Every property is read. The toolbox's inverse relations (`IsDefinedBy`) lose entries at random while parsing, so the reader goes by the relations' `RelatedObjects` instead. Before, a different few percent of the properties went missing on each start.
- [x] Single, enumerated and list values, from the element and its type; quantity sets are skipped.
- [x] PredefinedType and "user-defined" the way IfcOpenShell reads them.
- [x] Load errors are reported with the file name (`ModelLoadException`): the GUI shows a dialog, the CLI exits with code 1.
- [ ] A property relation pointing at a set of property sets (IFC4): read, but untested, because IfcOpenShell cannot write such a file.
- [ ] Bounded, table and complex properties, and properties in quantity sets: not read. IDS 1.0 property facets can address both; the portal's LOINs don't.

### Get the requirements

- [x] Load the LOIN list from the portal, paged (GUI).
- [x] Download a LOIN's IDS export and pick the specification for the model's schema.
- [x] Picking the specification works across addenda: an IFC4X3 model gets the IFC4X3_ADD2 block, not the IFC4 one.
- [ ] `ifcVersion` listing several schemas (`"IFC2X3 IFC4"`), which IDS 1.0 allows. The portal writes one per specification, so this matters for local IDS files.
- [x] The parser reads what the portal writes: property facets and the entity name.
- [x] Entity `predefinedType`, as a value or a pattern; `USERDEFINED` asks for a user-defined type, as in ifctester.
- [x] `cardinality`: required, optional and prohibited.
- [ ] Report IDS parts the parser doesn't support instead of dropping them silently (matters for local IDS files).
- [ ] LOIN by ID.
- [ ] AIA project as a source (ZIP export).
- [ ] Local IDS file.
- [ ] Cache for downloaded IDS exports.
- [ ] Search: it matches parts of names, so "Schaft" also finds "Landschaftsbau" (STATUS #7). The CLI doesn't page. A search term of several words works since 2026-11-20 ("Ueberbau > Schaft" finds exactly those 2 LOINs).

### Choose the LOINs

- [ ] Context filters for discipline, phase and use case. The GUI currently drops this metadata.
- [ ] Sort the catalogue by fit to the model.
- [ ] Select several LOINs and check them in one go.

### Check

- [x] Applicability (all conditions must hold), and one finding per element and requirement.
- [x] Diagnosis when nothing applies: hits per condition and the closest elements.
- [x] Mapping of the BIM.Hamburg classification (`Pset_Objektinformation/_IDEbene1..3`).
- [x] Numbers are compared as numbers, so `0` equals `0.`.
- [x] Data types strict, as in IDS, in the applicability and the requirements, with a hint naming the elements a related type would let through (section 5).
- [x] A prohibited property is met when it is missing or has another value or type.
- [x] An entity's `predefinedType` is checked; a mismatch is "PredefinedType weicht ab".
- [ ] Decimal numbers compared with IDS's relative tolerance of 1e-6; currently they are compared exactly.
- [ ] IFC2X3: accept an entity through the element's type (an `IfcBeamType` makes the element an `IfcBeam`), as ifctester does.
- [ ] Text values compared case-sensitively, as ifctester does; currently case is ignored.
- [ ] The diagnosis of an entity facet with a PredefinedType counts every element as "abweichender Wert". It should say how many have the entity but another type (e.g. "11 IfcWindow, keiner mit benutzerdefiniertem Typ").

### Results

- [x] Text report in the GUI and the CLI.
- [ ] Summary table across several LOINs.
- [ ] Show all checked elements: the GUI currently stops at 20 tabs without saying so ([`ElementUI.java:437`](../bim-portal/Java_client/hackathon-template/src/main/java/test/fx/ElementUI.java#L437)).
- [ ] Report header with the model, the LOIN version and the date.
- [ ] CLI: JSON output, and an exit code other than 0 when requirements fail. Load errors (1) and unknown arguments (2) already have one.
- [ ] Disable "Validate LOIN" while a check runs; a double-click currently starts a second check.

### Front ends and quality

- [x] Headless CLI: `runLoinCheck`.
- [x] Window size: the GUI opens at 1280x900 (STATUS #5).
- [ ] GUI cleanup: "BIM Portal" button (STATUS #4), chat placeholder (STATUS #3), the leftover class filter. The class filter's tabs still read IFC2X3 only and through the unreliable inverse relations (`ElementUI.createObjectTab`).
- [ ] MCP server (later).
- [x] Tests for the `ids` package ([`src/test/java/com/bimportal/hackathon/ids/`](../bim-portal/Java_client/hackathon-template/src/test/java/com/bimportal/hackathon/ids)), with real portal exports as test files ([`src/test/resources/ids/portal/`](../bim-portal/Java_client/hackathon-template/src/test/resources/ids/portal/README.md)).
- [x] Tests for reading IFC ([`src/test/java/test/fx/`](../bim-portal/Java_client/hackathon-template/src/test/java/test/fx)): small case files of our own per schema ([`make_ifc_fixtures.py`](../bim-portal/Java_client/hackathon-template/src/test/python/make_ifc_fixtures.py)), the bundled model, and sample models that `gradlew :hackathon-template:downloadTestModels` fetches into the ignored `test-models/` (about 5 MB, list in [`samples.properties`](../bim-portal/Java_client/hackathon-template/src/test/resources/ifc/samples.properties)).
- [x] Missing prerequisites are reported, not failed: without the Apstex JARs the IFC tests are not built, without the sample models they are skipped, and `PrerequisitesTest` says in each case what to add and where.

## 5. Decisions

- 2026-11-20, data types: compared strictly, as IDS defines them, with a hint naming the elements a
  related type would let through. Before, related types such as `IfcText` for `IFCLABEL` were
  accepted, and the applicability ignored the data type.
- 2026-11-20, choosing LOINs: by ID first (LOIN, AIA project, local IDS file). Sorting the
  catalogue by fit comes after that.
