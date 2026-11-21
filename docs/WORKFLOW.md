# How CheckMate works

This file describes how CheckMate is meant to work (sections 1-3) and how much of that works
today (section 4). As of 2026-11-21. Open points that are also listed in [STATUS.md](STATUS.md)
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
  3. Sort by fit: the most promising LOINs first, by how well each LOIN's object type matches the
     model's classification values and entity names. LOINs that don't fit are hidden behind "alle
     anzeigen". The search stays available.
  4. Clicking a LOIN shows a short description from the portal's detail of that one LOIN (about
     8 KB): object type, IFC class, the properties it asks for, phases and use cases. The reader
     decides from that whether it makes sense.
  5. Several LOINs can be selected. Only the selected ones are downloaded and checked.

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

- Tests for the parser and the check, with real IDS files from the portal as test files and
  hand-built `ModelElement`s.
- ifctester (IfcOpenShell's IDS implementation) is the reference for the expected results.

## 3. Background: what the portal delivers

Measured on 2026-11-20 against the public portal `https://via.bund.de/bim`:

- 1,568 public LOINs. Loading the whole list takes about 7 s and 3 MB (2 pages). Each entry
  carries object type, discipline, construction type, phases, use cases, organisation and version;
  the `description` is empty for all but 51. The search only filters by name text, not by these
  fields (measured 2026-11-21).
- The detail of one LOIN (`GET /loin/{guid}`, about 8 KB) adds the IFC class (`dataFormats`), the
  required properties by name, the classification it selects by, use cases and dates.
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
- [x] Every property is read. The toolbox's inverse relations (`IsDefinedBy`) lose entries at random while parsing, so the reader goes by the relations' `RelatedObjects` instead. Before, a different few percent of the properties went missing on each start.
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

- [ ] Context filters for discipline, phase and use case. The GUI currently drops this metadata.
- [ ] Sort the catalogue by fit to the model, from the metadata alone.
- [ ] A short description of a LOIN on click, from the portal's detail of that LOIN.
- [ ] Select several LOINs and check them in one go; only the selected ones are downloaded.

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
- [ ] The elements come in a different order on each start (the toolbox's object set has no fixed order), so two reports on the same model list findings differently. Sort them, e.g. by GlobalId.

### Results

- [x] Text report in the GUI and the CLI.
- [ ] Summary table across several LOINs. So far: with several specifications, the report starts with a tally ("36 Spezifikation(en) für IFC4: 36 nicht erfüllt") and one line per specification with its verdict (erfüllt, nicht erfüllt, erfüllt (soweit ausgewertet), nicht prüfbar).
- [x] All checked elements: 20 get a tab each, with what every specification said about them; one more tab lists the rest with how many lines are open.
- [ ] Report header: the model, the requirements' name, version and source are there; the date is missing.
- [ ] CLI: JSON output, and an exit code other than 0 when requirements fail. Already: 1 when the model or any requirements can't be read or checked, 2 for unknown arguments.
- [ ] Disable "Validate LOIN" while a check runs; a double-click currently starts a second check.

### Front ends and quality

- [x] Headless CLI: `runLoinCheck`.
- [x] Window size: the GUI opens at 1280x900 (STATUS #5). Many element tabs no longer squeeze the LOIN list.
- [ ] GUI cleanup: "BIM Portal" button (STATUS #4), chat placeholder (STATUS #3), the leftover class filter. The class filter's tabs still read IFC2X3 only and through the unreliable inverse relations (`ElementUI.createObjectTab`).
- [ ] MCP server (later).
- [x] Tests for the `ids` package ([`src/test/java/com/bimportal/hackathon/ids/`](../bim-portal/Java_client/hackathon-template/src/test/java/com/bimportal/hackathon/ids)), with real IDS files from the portal as test files ([`src/test/resources/ids/portal/`](../bim-portal/Java_client/hackathon-template/src/test/resources/ids/portal/README.md)).
- [x] Tests for reading IFC ([`src/test/java/test/fx/`](../bim-portal/Java_client/hackathon-template/src/test/java/test/fx)): small case files of our own per schema ([`make_ifc_fixtures.py`](../bim-portal/Java_client/hackathon-template/src/test/python/make_ifc_fixtures.py)), the bundled model, and sample models that `gradlew :hackathon-template:downloadTestModels` fetches into the ignored `test-models/` (about 5 MB, list in [`samples.properties`](../bim-portal/Java_client/hackathon-template/src/test/resources/ifc/samples.properties)).
- [x] Missing prerequisites are reported, not failed: without the Apstex JARs the IFC tests are not built, without the sample models they are skipped, and `PrerequisitesTest` says in each case what to add and where.

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
