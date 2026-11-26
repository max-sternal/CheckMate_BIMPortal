---
title: Versions & Branches
description: From the hackathon prototype to the current stable release 0.3.3-stable (Latest) and the TypeScript/IFClite beta 0.4.1-beta.
---

| Version / Release | Branch | Scope and Focus |
|---|---|---|
| `v0.1.0` | `0.1` / `main` | Result of the 24-hour hackathon (September 2025) with initial prototype and pitch. ([Docs](/CheckMate_BIMPortal/en/v0.1.0/)) |
| `v0.2.0` | `0.2` | First portal client fixes and an end-to-end runnable checking workflow. ([Docs](/CheckMate_BIMPortal/en/v0.2.0/)) |
| `v0.3.1` | `0.3.1` | Reworked Java checking core with JavaFX GUI, CLI, and MCP server (using APSTEX). ([Docs](/CheckMate_BIMPortal/en/v0.3.1/)) |
| `v0.3.2` | `0.3.2` | Pure Java 25 edition without APSTEX: custom STEP IFC reader, JSON mapping (`bim-hamburg.json`), property catalogue client, IDS value restrictions, 3 MCP App views, and Antigravity plugin. ([Docs](/CheckMate_BIMPortal/en/v0.3.2/)) |
| **`v0.3.3-stable` (`Latest`)** | `0.3.3-stable` / `0.3.3` | **Current stable main release documented here (Java 25 LTS):** extends `0.3.2` with configurable in-GUI AI chat (`CheckMateChat`), hardened build/dependency setup, and curated BIM Portal IFC test models. ([Latest Docs](/CheckMate_BIMPortal/en/)) |
| `v0.4-alpha` (`Alpha`) | `0.4` (tag `v0.4-alpha`) | Initial TypeScript & IFClite milestone (`@ifc-lite/*`): 3D browser workbench, 3D MCP Apps, CLI, `.rules.json`, and BCF 2.1 export. ([Docs](/CheckMate_BIMPortal/en/v0.4-alpha/)) |
| **`v0.4.1-beta` (`Beta`)** | `0.4.1-beta` / `0.4` | **Beta Version (TypeScript + IFClite):** streamlined MCP server, direct CLI installation, schema & live portal fixes, and two compact 3D IFC4 reference models. ([Beta Docs](/CheckMate_BIMPortal/en/v0.4.1-beta/)) |

## Two Complementary Editions: `0.3.3-stable` (Java 25) and `0.4.1-beta` (TypeScript + IFClite)

CheckMate is maintained in two consistent, complementary editions:

- **`0.3.3-stable` (`Latest`, Java 25 LTS):** The stable reference implementation with a classic JavaFX desktop GUI (including optional API-key AI chat), headless CLI, and Java MCP server — completely free of proprietary third-party libraries.
- **`0.4.1-beta` (`Beta`, TypeScript + IFClite):** Brings the exact same domain workflow (`ModelFit`, `LoinDescription`, `PropertyMapping`, near-miss diagnosis, German reports, and exit codes `0..4`) to Node.js 22+ and WebAssembly (`@ifc-lite/*`) for interactive 3D checking in the browser and inline 3D MCP Apps in Antigravity and Claude Desktop.

Use the version selector at the top of the navigation bar to switch between **`Latest (0.3.3-stable)`**, **`v0.4.1-beta`**, **`v0.4-alpha`**, and archived releases (`v0.3.2`, `v0.3.1`, `v0.2.0`, `v0.1.0`).

[View branches on GitHub](https://github.com/max-sternal/CheckMate_BIMPortal/branches) · [View releases & pre-releases](https://github.com/max-sternal/CheckMate_BIMPortal/releases)
