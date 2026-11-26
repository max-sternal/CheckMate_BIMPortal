---
title: System architecture
description: Components and interfaces of CheckMate 0.3.3-stable.
---

## Data flow

CheckMate processes an IFC model and requirements from the BIM Portal or a local IDS file. The shared checking core produces a report for the graphical interface, command line or MCP server.

```text
IFC file / URL -- Java IFC reader -- ModelElement --+
                                                    +-- CheckService -- CheckReport
BIM Portal / local IDS file ----- IDS parser -------+       |             |
                                                        GUI / CLI / MCP
```

No AI model is required for checking or reporting. The MCP server is an optional interface to the same functions; the [chat in the interface](/CheckMate_BIMPortal/en/guides/chat/) is a further caller of the same checking core.

## Components

CheckMate code lives in the Gradle module `bim-portal/Java_client/hackathon-template/`.

| Package | Responsibility |
|---|---|
| `com.bimportal.hackathon.ids` | Read IDS and check supported requirements; internal model representation. |
| `com.bimportal.hackathon.ifc` | Read IFC files with the Java reader and prepare model elements. |
| `com.bimportal.hackathon.catalogue` | Search portal LOINs by metadata and rank them for a model. |
| `com.bimportal.hackathon.properties` | `PortalProperties` — BIM Portal Merkmale & Merkmalsgruppen client. |
| `com.bimportal.hackathon.check` | Load requirements and coordinate checks through `CheckService`. |
| `com.bimportal.hackathon.report` | Results and text/JSON reports. |
| `com.bimportal.hackathon.cli` | Command line for checks and catalogue searches. |
| `com.bimportal.hackathon.mcp` | MCP tools and server over `stdio`. |
| `com.bimportal.hackathon.chat` | Conversation, tool definition and loop for the chat in the interface. |
| `test.fx` | JavaFX interface. |

The IFC reader requires neither APSTEX nor JavaFX. The CLI and MCP server can be used without the graphical interface. The module structure makes reader changes easier, but is not a ready-made plug-in interface.

The `bim-portal-enhanced` and `openapi-generated` modules provide the Java connection to the portal API. The Python client also originates from the [hackathon starter kit](https://github.com/pb40development/bim-portal) and is not part of CheckMate's checking core.

## Interfaces and limits

IFC and IDS are open exchange formats; the portal is one requirements source alongside local IDS files. These formats allow different tools to be used but do not guarantee identical results or full interoperability. Supported facets and limitations are described in [IFC and IDS](/CheckMate_BIMPortal/en/architecture/ifc-ids-checking/).

For current classes and open decisions, see the [repository](https://github.com/max-sternal/CheckMate_BIMPortal/tree/0.3.3-stable) and [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/STATUS.md).
