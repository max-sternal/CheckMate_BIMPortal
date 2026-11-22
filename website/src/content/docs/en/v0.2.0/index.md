---
title: CheckMate 0.2
description: Initial fixes and IDS checking after the hackathon.
slug: en/v0.2.0
---

## First developments

`0.2` builds on the hackathon prototype: portal search and interface startup were revised, and a bridge model was bundled. An initial IDS check compares IFC models with LOIN requirements from the BIM Portal. It is available through the JavaFX interface and a command line.

This is an early check, not the full feature set of later versions. In particular, today's MCP functionality and the extended reporting in `0.3.1` should not be assumed.

## Setup and limitations

The [original guide](https://github.com/max-sternal/CheckMate_BIMPortal/blob/v0.2.0/bim-portal/Java_client/hackathon-template/README.md) describes JDK 21, JavaFX and APSTEX JARs, which must be obtained separately. Public portal LOINs can be accessed without credentials. The guide and code reflect the historical version; external services and dependencies may have changed.

- [Source code for v0.2.0](https://github.com/max-sternal/CheckMate_BIMPortal/tree/v0.2.0)
- [Changes since v0.1.0](https://github.com/max-sternal/CheckMate_BIMPortal/compare/v0.1.0...v0.2.0)

This page was written retrospectively. The release tag points to the unchanged historical branch state; the page is not part of that commit.
