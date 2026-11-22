---
editUrl: false
title: System architecture
description: Components and interfaces of CheckMate 0.3.1.
slug: en/v0.3.1/architecture/overview
---

## Data flow

CheckMate processes an IFC model and requirements from the BIM Portal or a local IDS file. The shared checking core produces a report for the graphical interface, command line or MCP server.

```text
IFC file / URL -- APSTEX reader -- ModelElement ----+
                                                    +-- CheckService -- CheckReport
BIM Portal / local IDS file ----- IDS parser -------+       |             |
                                                        GUI / CLI / MCP
```

No AI model is required for checking or reporting. The MCP server is an optional interface to the same functions.

## Components

CheckMate code lives in the Gradle module `bim-portal/Java_client/hackathon-template/`.

| Package | Responsibility |
|---|---|
| `com.bimportal.hackathon.ids` | Read IDS and check supported requirements; internal model representation. |
| `com.bimportal.hackathon.catalogue` | Search portal LOINs by metadata and rank them for a model. |
| `com.bimportal.hackathon.check` | Load requirements and coordinate checks through `CheckService`. |
| `com.bimportal.hackathon.report` | Results and text/JSON reports. |
| `com.bimportal.hackathon.mcp` | MCP tools and server over `stdio`. |
| `test.fx` | JavaFX interface, CLI and IFC loading with APSTEX. |

The checking core uses neither JavaFX nor the APSTEX IFC library. IFC loading and the MCP server still depend on `test.fx` in `0.3.1`, however: replacing the reader is **not yet available as a ready-made plug-in**.

The `bim-portal-enhanced` and `openapi-generated` modules provide the Java connection to the portal API. The Python client also originates from the [hackathon starter kit](https://github.com/pb40development/bim-portal) and is not part of CheckMate's checking core.

## Interfaces and limits

IFC and IDS are open exchange formats; the portal is one requirements source alongside local IDS files. These formats allow different tools to be used but do not guarantee identical results or full interoperability. Supported facets and limitations are described in [IFC and IDS](/CheckMate_BIMPortal/en/v0.3.1/architecture/ifc-ids-checking/).

For current classes and open decisions, see the [repository](https://github.com/max-sternal/CheckMate_BIMPortal/tree/0.3.1) and [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/v0.3.1/docs/STATUS.md).
