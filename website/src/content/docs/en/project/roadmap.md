---
title: Project status
description: Current state and open points for CheckMate 0.4 and the Java 0.3.2 / 0.3.3 track.
---

## State of `0.4` (`0.4-alpha`)

CheckMate `0.4` combines the complete domain checking workflow of `0.3.2` with **TypeScript and IFClite (`@ifc-lite/*`)**:

- **3D browser workbench (`@checkmate/web`)**: 4-tab workbench featuring a WebGL 3D viewer, element inspector (`element_report`), live BIM Portal catalogue search with `ModelFit` ranking, `LoinDescription` preview, summary table, near-miss diagnosis (`nicht prüfbar`), live JSON mapping editor, `.rules.json` conversion and BCF 2.1 download (`.bcfzip`).
- **MCP server & 3D MCP Apps (`@checkmate/mcp`)**: 10 model tools, 5 app-only tools and 3 interactive 3D HTML views (`ui://checkmate/report`, `ui://checkmate/model`, `ui://checkmate/loins`).
- **CLI (`@checkmate/cli`)**: Matches the flags, German reports and exit codes (`0..4`) of `0.3.2`, adding `--rules` and `--bcf`.

## State of the Java track (`0.3.2` & `0.3.3`)

- **`0.3.2`**: Runs on Java 21 through Java 25 LTS with Gradle 9.1.0, reads IFC2X3, IFC4 and IFC4X3 without APSTEX, and supports interchangeable JSON mappings (`bim-hamburg.json`) plus the initial MCP App report view. ([View archived 0.3.2 documentation](/CheckMate_BIMPortal/en/v0.3.2/))
- **`0.3.3` (Planned)**: Investigation and support for current LOIN-XML (see the [BIM Portal import interface](https://github.com/bimdeutschland/BIM-Portal-Importschnittstellendokumentation) and [ISO/DIS 7817-3](https://www.iso.org/standard/89077.html)).

## Open points

- Field testing of the 3D browser workbench (`0.4-alpha`) and 3D MCP Apps across diverse discipline models and AIA projects.
- LOIN-XML investigation on `0.3.3` and cross-pollination between `0.3.x` and `0.4`.
