---
title: Project Status
description: Current status of CheckMate 0.4.1-beta (TypeScript + IFClite) and 0.3.3-stable (Latest, Java 25).
---

## State of `0.4.1-beta` (TypeScript + IFClite)

CheckMate `0.4.1-beta` polishes the TypeScript and IFClite (`@ifc-lite/*`) stack introduced in `0.4-alpha` into a clean, ready-to-use beta edition:

- **3D browser workbench (`@checkmate/web`)**: 4-tab workbench with WebGL 3D viewer, 1-click presets for building (`🏢 Mini-Hochbau`), infrastructure (`🌉 Mini-Brücke`), and BIM.Hamburg (`TBW1`), element inspector (`element_report`), live BIM Portal catalogue search with `ModelFit` ranking, `LoinDescription` preview, near-miss diagnosis, live JSON mapping editor, `.rules.json` conversion, and BCF 2.1 download (`.bcfzip`).
- **Streamlined MCP server & 3D MCP Apps (`@checkmate/mcp`)**: Clean declarative server structure (`catalog.ts` / `tools.ts` / `index.ts`) with a standalone launcher (`checkmate-mcp.mjs`), 10 model tools, 5 app-only tools, 3 interactive 3D HTML views (`ui://checkmate/*`), 2 JSON resources (`checkmate://model/current`, `checkmate://report/last.json`), 2 guided prompts (`check-loin`, `explain-element`), and an Antigravity plugin builder (`pnpm mcp:antigravity-plugin`).
- **Command-line interface (`@checkmate/cli`)**: Runnable via `pnpm cli` or globally linked via `pnpm --filter @checkmate/cli link --global` as `checkmate`, with `--help`, `--version`, `--rules`, `--bcf`, and exit codes `0..4`.
- **BIM Portal IFC4 reference models**: Two compact, human-readable 3D IFC4 models for building construction (`mini-hochbau-bundesbau.ifc`, 5 LOINs) and infrastructure (`mini-infrastruktur-bruecke.ifc`, 6 LOINs), alongside 6 extended `_mod_full`/`_mod_partial` test models documented in [`docs/README.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/docs/README.md).

## State of `0.3.3-stable` (`Latest`, Java 25 LTS)

- **`0.3.3-stable`** is the current stable main release of the Java edition ([view Latest documentation](/CheckMate_BIMPortal/en/)): running on JDK 25 LTS and Gradle 9.1.0 without APSTEX, it provides the JavaFX GUI, CLI, Java MCP server (11 tools, 3 MCP App views, Antigravity plugin), and a configurable in-GUI AI chat (`CheckMateChat`).

## Outlook

- Practical evaluation of both editions (`0.3.3-stable` and `0.4.1-beta`) across additional discipline models, object catalogues, and AIA projects.
- Tracking ongoing LOIN-XML standardisation ([BIM Portal import interface](https://github.com/bimdeutschland/BIM-Portal-Importschnittstellendokumentation), [ISO/DIS 7817-3](https://www.iso.org/standard/89077.html)).
