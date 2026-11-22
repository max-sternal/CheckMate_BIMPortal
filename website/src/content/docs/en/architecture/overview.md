---
title: System Architecture
description: Structure of CheckMate 0.3.x (Java core + 3 front ends) and target architecture for 0.3.2 & 0.4 (CheckMate Lite on IFClite).
---

## Guiding Principle: One Core for Every Front End

In CheckMate `0.3.x`, **IFC reading**, **validation logic**, and **presentation** are strictly separated. The GUI, CLI, and MCP server contain no checking logic of their own — they gather input, invoke `CheckService`, and render the returned `CheckReport`.

```text
                    ┌──────────────────────────────────────────┐
                    │    German Federal BIM Portal (REST API)  │
                    │  LOIN Catalogue · AIA Projects · IDS 1.0 │
                    └────────────────────┬─────────────────────┘
                                         │ (anonymous / optional JWT)
                                         ▼
┌─────────────────────┐      ┌───────────────────────┐      ┌──────────────────────┐
│  IFC / ifcZIP / URL │ ───► │   IFC Reader Layer    │ ───► │  ModelElement[]      │
│  (IFC2X3 / 4 / 4X3) │      │  0.3.1: APSTEX        │      │  (library-agnostic)  │
└─────────────────────┘      │  0.3.2: Open-Source   │      └──────────┬───────────┘
                             └───────────────────────┘                 │
                                                                       ▼
                             ┌─────────────────────────────────────────────────────┐
                             │             Pure-Java Checking Core                 │
                             │  ids/ · catalogue/ · check/ (CheckService) · report/│
                             └──────┬──────────────────┬──────────────────┬────────┘
                                    │                  │                  │
                                    ▼                  ▼                  ▼
                             ┌────────────┐    ┌─────────────┐    ┌───────────────┐
                             │ JavaFX GUI │    │ Headless CLI│    │  MCP Server   │
                             │ (ElementUI)│    │ (Text/JSON) │    │ (7 Tools)     │
                             └────────────┘    └─────────────┘    └───────────────┘
```

## Module & Package Layout (`0.3.1`)

All CheckMate code resides in the Gradle module `bim-portal/Java_client/hackathon-template/`:

| Package | Responsibility | Dependencies |
|---|---|---|
| `com.bimportal.hackathon.ids` | IDS 1.0 parser (`IdsParser`), ZIP archives (`IdsArchive`), validator (`IdsValidator`), `DataTypes`, `Values`, `PropertyKey`, `PropertyAliases`, `PortalRequirements`, `ModelElement` | No IFC library, no JavaFX |
| `com.bimportal.hackathon.catalogue` | Metadata filtering (`CatalogueFilter`), model-fit ranking (`ModelFit`) across 1,568 LOINs, LOIN preview (`LoinDescription`) | No IFC library, no JavaFX |
| `com.bimportal.hackathon.check` | Unified orchestration (`CheckService`), requirement sources (`Source`), per-session download cache | No IFC library, no JavaFX |
| `com.bimportal.hackathon.report` | Report model (`CheckReport`), exit codes, text formatter (`ReportText`), stable JSON (`ReportJson`) | No IFC library, no JavaFX |
| `com.bimportal.hackathon.mcp` | Official MCP Java SDK server (`CheckMateMcpServer`, `CheckMateTools`) over `stdio` | Currently imports `test.fx.LoadedModel` |
| `test.fx` *(rename planned)* | JavaFX GUI (`App`, `ElementUI`), CLI (`LoinCheckCli`), IFC loading (`IfcResources`, `ModelLinks`, `IfcModelElements`, `LoadedModel`) | APSTEX IFC Toolbox (+ JDK FX for GUI) |

Also in the repository:
- `bim-portal/Java_client/bim-portal-enhanced/`: Enhanced BIM Portal REST client (anonymous access, paging, exports).
- `bim-portal/Java_client/openapi-generated/`: OpenAPI-generated Feign client (patched with `NON_NULL` serialization & `LenientStringDeserializer`).
- `bim-portal/Python_client/`: Unmodified Python starter client from the hackathon template.

---

## Evolution Path: `0.3.2` → `0.4` (*CheckMate Lite*)

### Step 1: `0.3.2` (Java Architecture Without APSTEX)
- Move `LoadedModel` and a clean `IfcModelReader` interface into `check/`, removing the compile-time JavaFX requirement for the MCP server.
- Replace APSTEX with an open-source IFC reader verified against the existing Java tests and `IfcOpenShell`.
- Upgrade the MCP server with **Streamable HTTP**, **tool annotations**, and **`structuredContent`** (backed by `ReportJson`).

### Step 2: `0.4` — *CheckMate Lite* (No Java, Built on `IFClite`)
The long-term target unifies CLI, MCP server, web app, and Antigravity MCP App in one open-source stack:

- **IFC Foundation:** [`IFClite`](https://ifclite.dev/) (`MPL-2.0`, Rust + WASM + TypeScript) — full schema coverage for **IFC2X3, IFC4, IFC4X3, and IFC5 (IFCX)** including relationships, spatial hierarchy, exact geometry, `@ifc-lite/bcf`, and `@ifc-lite/lens` (3D colorizing by check verdict).
- **BIM Portal Client:** Lightweight TypeScript `fetch` client for the required REST endpoints.
- **Checking & Catalogue Core:** Porting CheckMate's battle-tested domain logic (`ModelFit`, AIA ZIP merging, strict German/English diagnostics & near-misses), validated by a **new TypeScript test suite** against the same **`IfcOpenShell` / `ifctester` reference**.
- **Delivery Modes:** Headless Node.js (CLI & MCP server), browser/WebGPU (Antigravity MCP App `ui://` — sensitive models stay inside the browser tab!), and optional Tauri desktop builds for offline public-sector workstations.
