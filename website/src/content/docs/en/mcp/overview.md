---
title: MCP Server & 3D MCP Apps
description: The clean CheckMate MCP server (0.4.1-beta) with 10 tools, 3 interactive 3D MCP Apps, JSON resources, and guided prompts.
---

Through the [Model Context Protocol](https://modelcontextprotocol.io/), AI clients such as **Antigravity**, **Claude Desktop**, and **Claude Code** can use CheckMate as a local tool. The server (`@checkmate/mcp`) runs locally over `stdio`. Rule evaluation happens deterministically inside `@checkmate/core`; the language model invokes the tools and explains the German reports.

## Clean, Simple Structure (`packages/mcp/`)

To keep the server easy to read and extend, `@checkmate/mcp` is split into three focused modules:
- `bin/checkmate-mcp.mjs`: Standalone Node.js launcher (protects `stdout` for JSON-RPC and runs without extra CLI flags).
- `src/catalog.ts`: Declarative definitions of all tools, 3D views, JSON resources, and prompts.
- `src/tools.ts` & `src/index.ts`: Session state (`CheckMateTools`) and concise MCP request handlers.

## 10 Model Tools (for AI & Users)

| Tool | Purpose | MCP App View |
|---|---|---|
| `load_model` | Load an IFC file (`.ifc`, `.ifczip`) or HTTP(S)/GitHub link | `ui://checkmate/model` |
| `describe_model` | Describe the active model, IFC schema, element count, and top entities | `ui://checkmate/model` |
| `list_elements` | Filter elements as `GlobalId \| Entity \| Name \| PredefinedType` | — |
| `find_loins` | Search, filter, and rank BIM Portal LOINs and AIA projects by `ModelFit` | `ui://checkmate/loins` |
| `describe_requirements` | Use `LoinDescription` to inspect what a LOIN selects (`Wählt aus`), requires (`Verlangt`), and matches | — |
| `check_model` | Check the model against portal IDs, local `.ids`/`.zip`, or `.rules.json` | `ui://checkmate/report` |
| `element_report` | Show check findings and all IFC properties for an element (by GlobalId or name) | — |
| `load_mapping` | Show or switch the active JSON mapping (`default`, `none`, or file path) | — |
| `export_bcf` | Export failed findings from the last run as a BCF 2.1 archive (`.bcfzip`) | — |
| `server_status` | Show active model, active mapping, and last check verdict | — |

## 3 Interactive 3D MCP App Views (`io.modelcontextprotocol/ui`)

Hosts supporting the MCP Apps extension (such as **Antigravity** and **Claude Desktop**) render interactive HTML/WebGL views directly inside the conversation:

- `ui://checkmate/report`: Interactive 3D check report with red/green element highlighting, near-miss diagnosis, and element focus.
- `ui://checkmate/model`: 3D model explorer with clickable entity histogram and element table.
- `ui://checkmate/loins`: Interactive BIM Portal catalogue with discipline, phase, and purpose filters.

To paginate findings and stream 3D triangle meshes (`@ifc-lite/geometry`) inside these views, the server registers 5 internal app-only tools (`visibility: ["app"]`): `app_report_page`, `app_element_page`, `app_set_filter`, `app_check_selected`, and `app_element_mesh`. Text-only MCP clients continue to receive the complete German text report.

## JSON Resources & Guided Prompts

- **JSON resources**: `checkmate://model/current` (metadata of the active IFC model) and `checkmate://report/last.json` (complete JSON report of the last check).
- **Guided prompts**: `check-loin` (optional `discipline`, `phase`) for a guided BIM Portal search-and-check workflow, and `explain-element` (`element`) for a clear explanation of a single building element.
