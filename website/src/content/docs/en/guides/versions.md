---
title: Versions & branches
description: From the hackathon prototype through the Java 0.3.x line to the TypeScript/IFClite 0.4 edition.
---

| Version / Branch | State and focus |
|---|---|
| `0.1` | Result of the 24-hour hackathon, including the prototype and presentation. |
| `0.2` | Initial fixes to the portal client and a working checking workflow. |
| `0.3` | A larger revision of the checking core, reports and interfaces as the foundation of the `0.3` line. |
| `0.3.1` | Java version with GUI, CLI and MCP server; IFC loading uses APSTEX, as at the hackathon. |
| `0.3.2` | Pure Java edition (Java 21 / Java 25 LTS, Gradle 9.1): custom Java IFC reader without APSTEX, interchangeable JSON mappings (`bim-hamburg.json`) and initial MCP App report view. ([View 0.3.2 docs](/CheckMate_BIMPortal/en/v0.3.2/)) |
| `0.3.3` | Planned continuation of the Java line: investigate and add support for current LOIN-XML (ISO/DIS 7817-3). |
| `0.4` (`0.4-alpha`) | TypeScript & IFClite edition (`@ifc-lite/*`): 3D browser workbench (`@checkmate/web`), MCP server with 15 tools and interactive 3D MCP Apps (`@checkmate/mcp`), CLI (`@checkmate/cli`), `.rules.json` rule sets and BCF 2.1 export (`.bcfzip`). |

## Two active tracks: `0.3.x` (Java) and `0.4` (TypeScript + IFClite)

CheckMate is maintained along two complementary tracks:

- **Java track (`0.3.1`, `0.3.2`, `0.3.3`)**: Built on the Java ecosystem (Gradle, JavaFX desktop GUI, MCP Java SDK), serving as the reference for the pure-Java checking core and upcoming LOIN-XML work in `0.3.3`.
- **TypeScript / IFClite track (`0.4`)**: Preserves the exact CheckMate workflow (`ModelFit`, `LoinDescription`, `PropertyMapping`, near-miss diagnosis, German reports and exit codes) while using **IFClite (`@ifc-lite/*`)** WebAssembly/TypeScript packages for 3D visualization in the browser and inside MCP-capable AI clients.

Use the version selector in the top navigation bar to switch between the current documentation (`0.4`) and archived snapshots (`v0.3.2`, `v0.3.1`, `v0.2.0`, `v0.1.0`).

[View repository branches](https://github.com/max-sternal/CheckMate_BIMPortal/branches) · [View releases & pre-releases](https://github.com/max-sternal/CheckMate_BIMPortal/releases)
