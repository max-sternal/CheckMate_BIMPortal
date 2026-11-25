---
title: MCP interface
description: The CheckMate MCP server and its tools in version 0.3.2.
---

The [Model Context Protocol](https://modelcontextprotocol.io/) lets a compatible application use CheckMate as a tool. The `0.3.2` server runs locally over `stdio`. The CheckMate core performs the check; a connected language model can explain results but does not decide the rules.

| Tool | Purpose |
|---|---|
| `load_model` | Load an IFC file or URL, replacing the current model |
| `describe_model` | Describe the model and its IFC types |
| `list_elements` | List elements by type or name |
| `find_loins` | Search the portal for LOINs and AIA projects |
| `describe_requirements` | Describe the requirements of a LOIN or AIA project |
| `check_model` | Check the model against portal or local IDS requirements |
| `element_report` | Show findings and properties for an element |
| `load_mapping` | Show or switch the mapping; switching discards the last check report |
| `server_status` | Show the current model, mapping and last check result |

An initial MCP App report view provides structured output for `check_model` and a tool for loading additional findings. The view has been tested at protocol level, but not yet visually in Claude Desktop or Antigravity. Clients without the necessary support still receive a text response.

Tools return model information, properties and reports to the calling client. Depending on the client and model provider, this information may be further processed or sent externally. The server does not prevent such disclosure; assess confidentiality and data protection before use. Portal searches require network access.

On the MCP path the client provides the language model; CheckMate itself talks to no model. On branch `0.3.3` the [chat in the interface](/CheckMate_BIMPortal/en/guides/chat/) can instead start a check through a model that CheckMate calls with its own key. Both paths use the same checking core; they differ in who provides and pays for the model, and in that the chat so far knows only `check_model`.

Large responses may be truncated. See [IFC and IDS](/CheckMate_BIMPortal/en/architecture/ifc-ids-checking/) for unevaluated requirements and checking limitations. The [setup guide](/CheckMate_BIMPortal/en/mcp/clients/) explains how to start the server and provides a client example.
