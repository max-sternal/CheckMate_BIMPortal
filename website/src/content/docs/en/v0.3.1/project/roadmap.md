---
editUrl: false
title: Project status
description: Current state and open points for CheckMate 0.3.1.
slug: en/v0.3.1/project/roadmap
---

## Version 0.3.1

CheckMate provides a JavaFX interface, CLI and MCP server for checking IFC models against supported IDS requirements. Requirements can come from the BIM Portal or local files. IFC2X3, IFC4 and IFC4X3 are read with APSTEX.

The source code is licensed under Apache 2.0; the required APSTEX IFC Toolbox is a separate proprietary dependency. Without it, the checking core and its tests can be used, but CheckMate cannot load IFC models.

## Open points

* Support further IDS facets and IFC properties; current limits are described in [IFC and IDS](/CheckMate_BIMPortal/en/v0.3.1/architecture/ifc-ids-checking/).
* Investigate an openly available alternative for reading IFC. An implementation is not part of `0.3.1`.
* Decide how to handle project-specific property mappings; there is currently a fixed mapping for the sample model.
* Maintain interfaces and documentation as development continues.

Development branches and ideas are not commitments to particular features or dates. Code-level details are in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/v0.3.1/docs/WORKFLOW.md) and [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/v0.3.1/docs/STATUS.md).
