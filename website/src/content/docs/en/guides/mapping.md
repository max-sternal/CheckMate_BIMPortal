---
title: Examples & mapping
description: Properties of the Hamburg bridge model and project-specific mappings in CheckMate 0.3.2.
---

## Why use a mapping?

An IDS requirement identifies a property by its property set and name. A model may use different names for similar information. A mapping allows CheckMate to look for additional agreed addresses in the model; it does not change the model or the IDS file.

The bundled BIM.Hamburg bridge model uses this mapping:

| Requirement | Address in the model |
|---|---|
| `Klassifizierung/Objektgruppe` | `Pset_Objektinformation/_IDEbene1` |
| `Klassifizierung/Objektklasse` | `Pset_Objektinformation/_IDEbene2` |
| `Klassifizierung/Objekttyp` | `Pset_Objektinformation/_IDEbene3` |

[`bim-hamburg.json`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/bim-portal/Java_client/hackathon-template/src/main/resources/mapping/bim-hamburg.json) is active by default in `0.3.3-stable`. The requested name is always checked first, followed by mapping addresses. The report identifies aliases used as `als Pset_Objektinformation/...`.

A mapping is **not a general IDS rule**. It must be appropriate for the project and model. It maps names but does not correct values or data types. Finding an address therefore does not automatically mean a requirement is satisfied.

## Choose a mapping

From `bim-portal/Java_client/`:

```bash
# Use the bundled mapping
./gradlew :hackathon-template:runLoinCheck --args="--ids src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default"

# Run the same check without aliases
./gradlew :hackathon-template:runLoinCheck --args="--ids src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids --mapping none"

# Use a custom mapping: provide its absolute path
./gradlew :hackathon-template:runLoinCheck --args="--ids src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids --mapping /path/to/mapping.json"
```

On Windows, use `.\gradlew.bat` and adjust the paths. The check can return a nonzero exit code, which Gradle reports as a failed task. The report gives the precise checking status.

For other models, supply a JSON file:

```json
{
  "name": "Project example",
  "aliases": [
    {
      "from": "Klassifizierung/Objekttyp",
      "to": ["Pset_Objektinformation/_IDEbene3"]
    }
  ]
}
```

`from` is the requested address, and `to` is a list of model addresses in search order. Each address has the form `PropertySet/Name`. The MCP tool `load_mapping` can also display or switch mappings; switching discards the last check report.

[See the worked example](/CheckMate_BIMPortal/en/guides/example/)
