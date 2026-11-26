---
title: MCP interface
description: The CheckMate MCP server and its tools in version 0.3.3-stable.
---

CheckMate can be connected to a language model in two ways. Through the [Model Context Protocol](https://modelcontextprotocol.io/) an AI application uses CheckMate as a tool. The [chat in the interface](/CheckMate_BIMPortal/en/guides/chat/) goes the other way round: CheckMate itself calls a model with its own key. Both paths use the same checking core.

This page describes the MCP path. The `0.3.3-stable` server runs locally over `stdio`.

## Model-Facing Tools (11 Tools)

| Tool | Purpose |
|---|---|
| `load_model` | Load an IFC file or URL, replacing the current model |
| `describe_model` | Describe the model and its IFC types |
| `list_elements` | List elements by type or name |
| `find_loins` | Search the portal for LOINs and AIA projects |
| `describe_requirements` | Describe the requirements of a LOIN or AIA project |
| `find_properties` | Search BIM Portal properties and property groups |
| `describe_property` | Describe details and value ranges of a BIM Portal property |
| `check_model` | Check the model against portal or local IDS requirements |
| `element_report` | Show findings and properties for an element |
| `load_mapping` | Show or switch the mapping |
| `server_status` | Show the current model, mapping and last check result |

## App-Only Tools

- `app_report_page`: Provides structured report data for interactive interfaces.

## MCP App Views (3 Views)

1. `ui://checkmate/report`: Interactive check report with filter and detail views.
2. `ui://checkmate/loins`: Filtered LOIN and AIA catalogue overview.
3. `ui://checkmate/model`: IFC model structure and element browser.

## JSON Resources (2 Resources)

- `checkmate://model/current`: Current model metadata and IFC types as JSON.
- `checkmate://report/last.json`: Complete last check report in JSON format.

## MCP Prompts (2 Prompts)

- `check-loin`: Guided workflow for checking a model against a portal LOIN.
- `explain-element`: Assistance in interpreting element properties and findings.

Tools and views assist developers and AI clients in automated BIM checking. See [IFC and IDS](/CheckMate_BIMPortal/en/architecture/ifc-ids-checking/) and the [clients guide](/CheckMate_BIMPortal/en/mcp/clients/).
