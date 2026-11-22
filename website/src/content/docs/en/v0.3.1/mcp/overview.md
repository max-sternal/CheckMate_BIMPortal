---
editUrl: false
title: MCP interface
description: The CheckMate MCP server and its tools in version 0.3.1.
slug: en/v0.3.1/mcp/overview
---

The [Model Context Protocol](https://modelcontextprotocol.io/) lets a compatible application use CheckMate as a tool. The `0.3.1` server runs locally over `stdio`. The CheckMate core performs the check; a connected language model can explain results but does not decide the rules.

| Tool | Purpose |
|---|---|
| `load_model` | Load an IFC file or URL, replacing the current model |
| `describe_model` | Describe the model and its IFC types |
| `list_elements` | List elements by type or name |
| `find_loins` | Search the portal for LOINs and AIA projects |
| `describe_requirements` | Describe the requirements of a LOIN or AIA project |
| `check_model` | Check the model against portal or local IDS requirements |
| `element_report` | Show findings and properties for an element |

Tools return model information, properties and reports to the calling client. Depending on the client and model provider, this information may be further processed or sent externally. The server does not prevent such disclosure; assess confidentiality and data protection before use. Portal searches require network access.

Large responses may be truncated. See [IFC and IDS](/CheckMate_BIMPortal/en/v0.3.1/architecture/ifc-ids-checking/) for unevaluated requirements and checking limitations. The [setup guide](/CheckMate_BIMPortal/en/v0.3.1/mcp/clients/) explains how to start the server and provides a client example.
