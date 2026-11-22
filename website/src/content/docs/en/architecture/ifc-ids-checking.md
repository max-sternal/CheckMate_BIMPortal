---
title: IFC Parsing & IDS 1.0
description: How CheckMate reads IFC2X3/4/4X3 models, validates against buildingSMART IDS 1.0, and cross-verifies results with IfcOpenShell / ifctester.
---

## 1. IFC Loading (`IfcResources`, `ModelLinks`, `IfcModelElements`)

CheckMate reads **IFC2X3, IFC4, and IFC4X3** models (including `IFC4X3_ADD2`) from multiple sources:
- **Local files:** `.ifc` (STEP `ISO-10303-21`) and `.ifczip`.
- **Links (`http`/`https`):** Automatic rewriting of GitHub `blob` URLs to raw links, automatic **Git LFS pointer resolution**, magic-byte header detection (HTML error pages are reported as such rather than producing an empty model), and a 200 MB download cap.
- **Bundled sample model:** `bruecke-tbw1-mit-merkmalen.ifc` (IFC2X3) ships inside the JAR so CheckMate runs out-of-the-box without local files.

### Technical Highlights in `0.3.1` (APSTEX)
- **Forward relation indexing (`Definitions.of`):** The APSTEX IFC Toolbox randomly drops entries in inverse `IsDefinedBy` links while parsing (on the bundled bridge model, 1–7% of relations were lost across runs). `IfcModelElements` therefore indexes all `IfcRelDefinesByProperties` and `IfcRelDefinesByType` relations forward via `RelatedObjects`.
- **Deterministic ordering:** All elements are sorted by STEP line number so repeated checks on the same model always produce identical reports.
- **IfcOpenShell-aligned semantics:**
  - `SingleValue`, `EnumeratedValue`, and `ListValue` from element and type property sets (element overrides type).
  - `PredefinedType`, `ObjectType`, `ElementType`, and `USERDEFINED` matching `ifcopenshell.util.element.get_predefined_type` / `is_userdefined_type`.
  - Quantity sets (`IfcElementQuantity`), bounded/table/complex properties, and geometry are intentionally skipped in `0.3.1` because current BIM Portal LOINs do not request them.

---

## 2. Deterministic IDS 1.0 Validation (`IdsParser`, `IdsValidator`)

CheckMate downloads LOINs and full AIA projects as **buildingSMART IDS 1.0** (or ZIP archives of IDS files) directly from the BIM Portal — or reads local `.ids`/`.zip` files.

### Fully Evaluated in `0.3.1`
- **Schema matching:** Multi-schema `ifcVersion` lists (`"IFC2X3 IFC4"`) and addenda (`IFC4X3_ADD2` ↔ `IFC4X3`, cleanly separated from `IFC4`).
- **Entity facets:** `name` (case-insensitive) and `predefinedType` (case-sensitive, supporting `simpleValue`, `xs:pattern`, `xs:enumeration`, and `USERDEFINED`). For **IFC2X3**, type-object entities count too (`IfcBeamType` → `IFCBEAM`), matching `ifctester`.
- **Property facets:** Exact `propertySet` and `baseName` matching, cardinalities (`required`, `optional`, `prohibited`), strict data-type checks (`IfcText` ≠ `IFCLABEL`), and typed value comparisons (`1e-6` relative float tolerance, exact text matching, XML Schema booleans, empty/`UNKNOWN` treated as missing).
- **Actionable diagnostics instead of silent failures:**
  - Spelling differences (case, umlauts, separators, leading underscore) or related data types are **never** falsely accepted, yet are reported as explicit hints for fixing the model.
  - When `0` elements match applicability, CheckMate always reports **"nicht prüfbar" (not applicable)** along with per-facet hit counts and the 5 closest elements ("near misses").
- **Catalogue ranking (`ModelFit`):** Scores all 1,568 public portal LOINs against the loaded model using metadata alone, putting fitting LOINs first without downloading IDS files up front.

### Explicitly Reported as "Not Evaluated"
IDS 1.0 constructs not yet implemented in `0.3.1` are **never silently dropped**:
- `attribute`, `classification`, `material`, and `partOf` facets.
- Pattern/enumeration/bounds restrictions (`xs:pattern`, `xs:enumeration`, `minInclusive`, …) on property values, and patterns on `propertySet`/`baseName`.
- Specification cardinality `maxOccurs="0"`.
- SI unit conversion for measure values (e.g., mm model vs. m IDS).

Any unsupported construct in **applicability** marks the specification **"nicht prüfbar"**; in **requirements**, the highest possible verdict is **"erfüllt (soweit ausgewertet)"** (passed as far as evaluated).

### Open Decision: `PropertyAliases` (STATUS #14)
`PropertyAliases.java` currently hard-codes a fallback mapping for the bundled BIM.Hamburg bridge convention (`Klassifizierung/Objektgruppe|Objektklasse|Objekttyp` → `Pset_Objektinformation/_IDEbene1..3`). Every alias usage is explicitly flagged in the report. Moving this to external, project-specific mapping files is tracked as an open decision.

---

## 3. Quality Assurance With `IfcOpenShell` / `ifctester` as Reference

**`IfcOpenShell` / `ifctester` remains the permanent reference oracle** across both the Java test suite (`0.3.x`) and the upcoming TypeScript test suite for **`0.4` (*CheckMate Lite*)**:

- **Custom IFC fixtures (`src/test/python/make_ifc_fixtures.py`):** Generates deterministic test models for IFC2X3, IFC4, and IFC4X3_ADD2 (type inheritance, overrides, enumerations, lists, `USERDEFINED`, skipped quantities).
- **Real portal fixtures (`src/test/resources/ids/portal/`):** Real LOIN exports (`Kappe`, `Schaft`, `Fenster`) and real AIA project ZIPs (`Eisenbahnbrücke` with 36 LOINs, `Straße Demonstrator`).
- **Public reference models (`downloadTestModels`):** 5 buildingSMART/open-source models (IFC2X3, IFC4, IFC4X3) automatically cross-checked against `IfcOpenShell 0.9` element/property counts and `ifctester 0.9` verdicts.
- **Test coverage in `0.3.1`:** **148 core tests** run without APSTEX on GitHub Actions; with APSTEX + sample models, the full suite runs **217 tests**.
