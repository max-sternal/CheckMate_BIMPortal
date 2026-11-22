---
title: Examples & mapping
description: Properties of the Hamburg bridge model and project-specific JSON mappings in CheckMate 0.4 and 0.3.2.
---

## Why a mapping?

An IDS requirement identifies a property by property set and base name. A model may store equivalent information under different names. A mapping allows CheckMate to look up agreed fallback addresses in the model — without modifying either the model or the IDS file.

The bundled BIM.Hamburg bridge model uses this mapping:

| Requirement (BIM Portal IDS) | Address in the model |
|---|---|
| `Klassifizierung/Objektgruppe` | `Pset_Objektinformation/_IDEbene1` |
| `Klassifizierung/Objektklasse` | `Pset_Objektinformation/_IDEbene2` |
| `Klassifizierung/Objekttyp` | `Pset_Objektinformation/_IDEbene3` |

The file [`resources/mapping/bim-hamburg.json`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4/resources/mapping/bim-hamburg.json) is active by default. CheckMate always checks the requested IDS address first; only if it is absent does `MappedDataAccessor` resolve aliases. Every alias hit is explicitly recorded in the report as `(als Pset_Objektinformation/...)`.

A mapping is **not a general IDS rule**. It must fit the project and model context. It maps names, but does not override values or data types.

## Selecting a mapping (Web workbench, CLI & MCP)

### 1. In the 3D browser workbench (`@checkmate/web`)
Open tab **"4. Mapping, Rules & Export"** to view or edit the active JSON mapping live, switch between `BIM.Hamburg Brücken` and `Keine Aliase (strikt IDS)`, or upload a custom `.json` file. Clicking **"Mapping anwenden & neu prüfen"** immediately re-runs the check and updates the 3D view.

### 2. Via the command line (`@checkmate/cli`)

```bash
# Use the bundled BIM.Hamburg mapping
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default

# Run the same check without aliases (strict IDS)
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping none

# Use a custom mapping file
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping /path/to/mapping.json
```

### 3. Structure of a mapping JSON file

```json
{
  "name": "Project Example",
  "aliases": [
    {
      "from": "Klassifizierung/Objekttyp",
      "to": ["Pset_Objektinformation/_IDEbene3"]
    }
  ]
}
```

`from` specifies the requested IDS address and `to` lists candidate model addresses in lookup order (`PropertySet/Name`). The MCP tool `load_mapping` can also inspect or switch the active mapping.

[Walk through a complete check example](/CheckMate_BIMPortal/en/guides/example/)
