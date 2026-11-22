---
title: Versions & Branches
description: Complete overview of all CheckMate versions (0.1, 0.2, 0.3.1, 0.3.2, and 0.4 CheckMate Lite), branches, IFC parsers, and test references.
---

CheckMate documents its evolution from hackathon prototype to a fully open-source architecture through clearly separated branches. Branches **`0.1`** and **`0.2`** remain permanently **frozen** for complete historical traceability.

## Version & Branch Matrix

| Version | Git Branch | Period / Status | Tech Stack | IFC Parsing | Test Reference & Scope |
|---|---|---|---|---|---|
| **`0.1`** | `0.1` *(from `main`)* | Sept – Nov 2025 (**frozen**) | Java 21, Gradle, JavaFX prototype, BIM Portal starter kit | **APSTEX IFC Toolbox** *(provided for hackathon)* | Starter kit examples (`BasicExampleLoinIdsTest`) |
| **`0.2`** | `0.2` *(commit `bc237bb`)* | Oct 2026 (**frozen**) | Java 21, Gradle, JavaFX GUI + initial CLI (`LoinCheckCli`) | **APSTEX IFC Toolbox** | First IDS/LOIN validation, fixed BIM Portal client, bundled bridge model |
| **`0.3.1`** | `0.3.1` *(working branch)* | Nov 2026 (**current**) | Java 21, Gradle 8.14, JavaFX GUI, CLI, **MCP server (stdio)**, Astro Starlight docs | **APSTEX IFC Toolbox** *(available on request; checking core 100% decoupled)* | **217 Java tests** (148 core tests run without APSTEX in CI) — verified against **`IfcOpenShell` / `ifctester 0.9`** |
| **`0.3.2`** | `0.3.2` *(planned)* | End of Nov 2026 (**Nov target**) | Java 21, Gradle 8.14, decoupled MCP server (Streamable HTTP, `structuredContent`, tool annotations) | **Without APSTEX** (open-source IFC reader behind `IfcModelReader` interface) | Full `0.3` Java test suite against **`IfcOpenShell`** — 100% CI-capable without proprietary JARs |
| **`0.4`** (*CheckMate Lite*) | `0.4` *(parallel, WIP)* | From late Nov 2026 (**Work in Progress**) | **Without Java** — TypeScript + Rust/WASM built on [**IFClite**](https://ifclite.dev/), MCP App (`ui://`) | **`@ifc-lite/parser`** (MPL-2.0, full IFC2X3/4/4X3/IFC5 relations + geometry) | **New TypeScript test suite** validated against the same **`IfcOpenShell` / `ifctester`** reference corpus |

---

## Release Stages Explained

### `0.1` — Direct Hackathon Snapshot (frozen)
Preserves the exact state of the **BIM Portal Hackathon Berlin (22–23 Sept 2025)** built on the official starter kit (`pb40development/bim-portal`, provided by *planen-bauen 4.0* / BIM Deutschland), including pitch decks, BIM Deutschland reference docs, and the initial JavaFX/APSTEX prototype.

### `0.2` — First Refinements (frozen)
First working post-hackathon milestone (late October 2026):
- Fixed BIM Portal API serialization/deserialization bugs and enabled anonymous public read access.
- Made the JavaFX GUI runnable with a bundled bridge IFC model (`bruecke-tbw1-mit-merkmalen.ifc`).
- Introduced the initial `ids/` validation package and headless `LoinCheckCli`.

### `0.3.1` — Current Working Release with APSTEX
Clean, deterministic checking core with unified `CheckService` shared across **GUI, CLI, and MCP server**:
- **Transparency on APSTEX:** The two **APSTEX IFC Toolbox** JARs (`com.apstex.ifctoolbox*.jar`, `com.apstex.stepparser*.jar`) were provided for the hackathon by *planen-bauen 4.0* / APSTEX, are **not freely redistributable**, and are therefore **excluded from Git**.
- **How to obtain the JARs for `0.3.1`:** To run `0.3.1` locally with full IFC loading (GUI, CLI, MCP server), you can request the APSTEX JARs from the project team (or directly from [APSTEX](https://www.apstex.com/)) and place them into `bim-portal/Java_client/hackathon-template/lib/`.
- **What works in `0.3.1` without APSTEX:** The entire checking core (`ids`, `catalogue`, `check`, `report`) is 100% free of APSTEX. Gradle automatically detects missing JARs and runs all **148 library-free core & IDS conformance tests** in any environment and on GitHub Actions.

### `0.3.2` — End-of-November Java Release (Without APSTEX)
Completes the `0.3` Java line as a self-contained open-source release:
- **Replaces APSTEX** with an open-source IFC reader behind a clean `IfcModelReader` interface, verified against the existing Java test suite and **`IfcOpenShell`**.
- **Decouples the MCP server from JavaFX** (moving `LoadedModel` out of `test.fx`) and upgrades MCP support (tool annotations, `structuredContent` from `ReportJson`, Streamable HTTP).
- Gradle remains the build system for `0.3.x`, and all Java tests are preserved.

### `0.4` — *CheckMate Lite* (Work in Progress, built on `IFClite`)
Parallel next-generation architecture **without Java and without APSTEX**:
- Built on [**IFClite**](https://ifclite.dev/) (`LTplus-AG/ifc-lite`, **MPL-2.0**): Rust core + WASM + modular TypeScript packages for **Node.js (headless CLI & MCP server)**, **browser (WebGPU 3D viewer)**, and optional **Tauri desktop**.
- Provides full schema & relation parsing for **IFC2X3, IFC4, IFC4X3, and IFC5 (IFCX)**, exact geometry, BCF export (`@ifc-lite/bcf`), and rule-based 3D colorization (`@ifc-lite/lens`).
- Enables a native **Antigravity MCP App** (`ui://` per Nov 2026 spec) with an interactive compliance table and 3D model inspection right inside AI clients.
- Introduces a **new TypeScript test suite**, keeping **`IfcOpenShell` / `ifctester`** as the shared gold-standard reference.
