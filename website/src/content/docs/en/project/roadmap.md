---
title: Status & Roadmap
description: Current status, open architectural decisions, and milestones from CheckMate 0.3.1 to 0.4 (CheckMate Lite).
---

A detailed, code-level checklist is maintained directly in the repository in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/WORKFLOW.md) and [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/STATUS.md).

## What Works Today (`0.3.1`)

- **IFC Loading:** IFC2X3, IFC4, IFC4X3; local files, `.ifczip`, HTTP(S) & GitHub links (with Git LFS resolution).
- **Requirements:** Individual LOINs (GUID/link), full AIA project ZIPs, local IDS files/ZIPs, anonymous access to all 1,568 public portal LOINs.
- **Catalogue Selection:** Discipline, phase, and use-case filtering plus metadata-based ranking (`ModelFit`).
- **Deterministic Validation:** Strict IDS 1.0 rules (entity, `PredefinedType`/`USERDEFINED`, property, data type, values, `prohibited`/`optional`), honest diagnostics & near-misses, `IfcOpenShell`/`ifctester` parity.
- **Interfaces:** JavaFX GUI with summary table, headless CLI (`--json`, exit codes `0–4`), stdio MCP server (7 tools, verified with Claude Desktop & Claude Code).
- **CI & Docs:** GitHub Actions running 148 library-free core tests + bilingual Astro Starlight documentation (DE/EN).

---

## Open Decisions & Known Limits

| Topic | Status Ref. | Summary |
|---|---|---|
| **APSTEX-free IFC reader** | `STATUS #15` | Replace non-redistributable APSTEX JARs in `0.3.2` (Java) and migrate fully to `IFClite` in `0.4`. |
| **Hard-coded `PropertyAliases`** | `STATUS #14` | Externalize BIM.Hamburg mapping (`_IDEbene1..3`) into project-level mapping files instead of code. |
| **Package name `test.fx`** | `STATUS #13` | Rename to `com.bimportal.hackathon.gui` and decouple `LoadedModel` for the MCP server. |
| **Ollama / OpenCode / Strata testing** | `STATUS #16` | End-to-end verification with open local & cloud models. |
| **Jackson version in MCP SDK** | `STATUS #17` | Project pins Jackson `2.15.2` while MCP Java SDK targets `2.21`. |
| **Additional IDS 1.0 facets** | `WORKFLOW 4` | `attribute`, `classification`, `material`, `partOf`, regex/bounds on values, SI unit conversion. |
| **Catalogue cache & BCF export** | `WORKFLOW 4` | Local disk cache for 3 MB LOIN catalogue (~7 s load time); BCF export (built-in via `@ifc-lite/bcf` in `0.4`). |

---

## Milestones

### `0.3.1` — End of This Week
- Frozen historical branches **`0.1`** and **`0.2`**, active release branch **`0.3.1`**.
- Public bilingual documentation (DE/EN) on GitHub Pages including the transparent Contributors & Compliance page.
- Automated CI tests for the APSTEX-free checking core and documentation build.

### `0.3.2` — End of November 2026
- **Fully APSTEX-free Java release** keeping Gradle and the full Java test suite verified against `IfcOpenShell`.
- **MCP upgrade:** Decoupled from JavaFX, tool annotations, `structuredContent` (from `ReportJson`), Streamable HTTP (`/mcp`), verified OpenCode/Ollama/Strata setups.

### `0.4` (*CheckMate Lite*) — Parallel / Work in Progress
- Ground-up architecture **without Java and without APSTEX**, built on [**IFClite**](https://ifclite.dev/) (TypeScript + Rust/WASM, MPL-2.0).
- **Native Antigravity MCP App** (`ui://`) with interactive compliance table and embedded 3D WebGPU viewer (`@ifc-lite/lens`), plus BCF export (`@ifc-lite/bcf`).
- **New TypeScript test suite** validated against the same `IfcOpenShell`/`ifctester` reference.
- Foundation for **LOIN authoring** and **geometric checks per ISO 7817-2**.
