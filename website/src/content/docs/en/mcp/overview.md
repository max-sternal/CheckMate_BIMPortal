---
title: MCP server & 3D MCP Apps
description: The CheckMate MCP server, its 15 tools and interactive 3D MCP App views in version 0.4.
---

The [Model Context Protocol](https://modelcontextprotocol.io/) allows compatible AI applications (such as Claude Desktop, Claude Code or Antigravity) to use CheckMate as a tool. The server (`@checkmate/mcp`) runs locally over `stdio`. Deterministic rule evaluation takes place inside `@checkmate/core`; the language model invokes the tools and explains the findings.

## 10 model tools (for AI & user)

| Tool | Purpose |
|---|---|
| `load_model` | Load an IFC file (`.ifc`, `.ifczip`) or HTTP(S)/GitHub link (`ui://checkmate/model`) |
| `describe_model` | Describe the current model, IFC schema, element count and top IFC entities (`ui://checkmate/model`) |
| `list_elements` | Filter elements as `GlobalId \| Entity \| Name \| PredefinedType` by entity or name |
| `find_loins` | Search, filter and rank BIM Portal LOINs and AIA projects by `ModelFit` (`ui://checkmate/loins`) |
| `describe_requirements` | Use `LoinDescription` to show what a LOIN selects (`Wählt aus`), requires (`Verlangt`) and how many model elements match (`Im Modell`) |
| `check_model` | Check the model against portal IDs, local IDS files or `.rules.json` (`ui://checkmate/report`) |
| `element_report` | Output check findings and all IFC properties for a specific element (by GlobalId or name) |
| `load_mapping` | Show or switch the active JSON mapping (`default`, `none` or file path) |
| `export_bcf` | Export failed checks from the last run as a BCF 2.1 archive (`.bcfzip`) |
| `server_status` | Show the current model, active mapping and last check verdict |

## 3 interactive 3D MCP App views & 5 app-only tools

Hosts supporting the MCP Apps extension (`io.modelcontextprotocol/ui`) render interactive HTML/WebGL views directly inside the chat:

- `ui://checkmate/report`: 3D check report with red/green element coloring, near-miss diagnosis and element focus.
- `ui://checkmate/model`: 3D model explorer with clickable entity histogram and element table.
- `ui://checkmate/loins`: Interactive BIM Portal catalogue with discipline, phase and purpose filters.

To paginate data and stream 3D meshes inside these views, the server registers 5 app-only tools (`visibility: ["app"]`): `app_report_page`, `app_element_page`, `app_set_filter`, `app_check_selected` and `app_element_mesh`. Clients without MCP App support continue to receive the full German text response.
