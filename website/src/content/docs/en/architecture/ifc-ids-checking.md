---
title: IFC and IDS
description: IFC loading, IDS 1.0 and rules checking, near-miss diagnosis and BCF export in CheckMate 0.4.
---

## Model and requirements

CheckMate `0.4` parses `IFC2X3`, `IFC4` and `IFC4X3` models via `@ifc-lite/parser` and extracts 3D triangle meshes using the `@ifc-lite/geometry` WebAssembly kernel. It accepts local `.ifc` and `.ifczip` files as well as HTTP(S) links (automatically translating GitHub blob links to raw URLs).

Requirements can be loaded from:
- **BIM Portal LOINs or AIA projects** (downloaded as IDS XML or ZIP archives via `@checkmate/bimportal`),
- **local IDS 1.0 files or ZIP archives** (with automatic deduplication of identical `(LOIN GUID, IFCVersion)` specifications in AIA project archives),
- **`.rules.json` rule sets** (`@ifc-lite/rules`), which can also be generated directly from any IDS document in the UI.

## What is checked

Using `@ifc-lite/ids` (`validateIDS`, `checkFacet`) together with CheckMate's `MappedDataAccessor`, CheckMate evaluates IDS 1.0 facets (`entity`, `property`, `attribute`, `classification`, `material`, `partOf`) and XSD restrictions (`simpleValue`, `pattern`, `enumeration`, `bounds`, `length`).

On top of standard IDS validation, CheckMate adds:
1. **Transparent property mapping (`MappedDataAccessor`)**: Checks the requested IDS address first and falls back to configured aliases from `bim-hamburg.json` or a custom JSON file, annotating every alias match as `(als ...)`.
2. **Zero-applicability & near-miss diagnosis (`diagnoseZeroApplicability`)**: When a specification matches `0` elements (`nicht prüfbar`), CheckMate evaluates each applicability facet individually, detects related IFC data types (e.g. `IFCTEXT statt IFCLABEL`) and spelling variations (case, umlauts, separators), and lists the closest **near-miss elements (`N/M Facetten`)**.
3. **BCF 2.1 export (`@ifc-lite/bcf`)**: Failed checks can be exported as a `.bcfzip` archive for BIM coordination tools.

## Limits and interpretation

A passing result confirms only the evaluated machine-readable requirements for the loaded model. It does not replace engineering review or certify overall compliance. Specifications with `0` applicable elements are always reported as **`nicht prüfbar` (exit code `4`)**, never as a pass.
