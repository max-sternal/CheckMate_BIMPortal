---
title: System architecture
description: Workspace packages, IFClite integration and data flow of CheckMate 0.4.1-beta.
---

## Data flow

CheckMate `0.4.1-beta` is structured as a TypeScript monorepo (`pnpm` workspace) that combines **IFClite (`@ifc-lite/*`)** WebAssembly/TypeScript packages with CheckMate's domain engine (`ModelFit`, `LoinDescription`, `MappedDataAccessor`, near-miss diagnosis):

```text
IFC file / URL --- @ifc-lite/parser + @ifc-lite/geometry --- LoadedModel (incl. 3D meshes) -+
                                                                                             +-- CheckService -- CheckReport / BCF
BIM Portal / IDS / .rules.json --- @checkmate/bimportal + @ifc-lite/ids + @ifc-lite/rules --+        |
                                                                                      +---------------+---------------+
                                                                                      |               |               |
                                                                               @checkmate/web  @checkmate/cli  @checkmate/mcp
```

Because `@checkmate/core` and `@checkmate/bimportal` run identically in modern browsers and in Node.js, the 3D browser workbench (`@checkmate/web`) executes the exact same checking engine as the CLI and MCP server.

## Workspace packages (`0.4.1-beta`)

| Package | Responsibility |
|---|---|
| `packages/bimportal` (`@checkmate/bimportal`) | TypeScript REST client for the German Federal BIM Portal (`via.bund.de/bim`), catalogue search, `CatalogueFilter`, UUID extraction and ZIP/IDS unpacking (`fflate`). |
| `packages/core` (`@checkmate/core`) | Shared checking engine: `LoadedModel` (`@ifc-lite/parser` + `@ifc-lite/geometry`), `PropertyMapping`, `MappedDataAccessor`, `ModelFit`, `LoinDescription`, `CheckService` (`@ifc-lite/ids` + `@ifc-lite/rules`), zero-applicability & near-miss diagnosis, BCF 2.1 export (`@ifc-lite/bcf`), `ReportText` and `ReportJson`. |
| `packages/web` (`@checkmate/web`) | Standalone 3D browser workbench (Vite + WebGL) with 4 tabs: Check Report & Diagnosis, BIM Portal Catalogue & `LoinDescription`, Model & Element Explorer, and Mapping, `.rules.json` & BCF Export. |
| `packages/mcp` (`@checkmate/mcp`) | MCP server over `stdio` providing `catalog.ts`, `bin/checkmate-mcp.mjs`, 10 model tools, 5 app-only tools, 3 interactive 3D MCP App views (`ui://checkmate/*`), 2 JSON resources and 2 guided prompts. |
| `packages/cli` (`@checkmate/cli`) | Command-line interface with `bin/checkmate.mjs`, `--help`, `--version` and options (`--model`, `--ids`, `--loin`, `--aia`, `--rules`, `--mapping`, `--suggest`, `--json`, `--bcf`, `--full`) with German reports and exit codes `0..4`. |

For the architecture of the pure Java edition (`0.3.3-stable`) under `bim-portal/Java_client/`, see the [System architecture (0.3.3-stable)](/CheckMate_BIMPortal/en/architecture/overview/).
