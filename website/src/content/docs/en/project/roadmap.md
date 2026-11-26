---
title: Project status
description: Current state and open points for CheckMate 0.3.3-stable and outlook on 0.4.1-beta.
---

## State of `0.3.3-stable` (Latest)

CheckMate `0.3.3-stable` provides a JavaFX interface with built-in configurable AI chat (`CheckMateChat`), a robust CLI, and an MCP server featuring 11 tools, 3 MCP App views, 2 JSON resources, and 2 prompts for checking IFC models against supported IDS requirements. Requirements can come from the BIM Portal or local IDS files. The Java reader handles IFC2X3, IFC4 and IFC4X3 without APSTEX. Curated BIM Portal test models for building and infrastructure are documented in `docs/README.md` (`docs/mini-hochbau-bundesbau.ifc`, `docs/mini-infrastruktur-bruecke.ifc`).

## Active Development on `0.4.1-beta` (Beta)

Active development takes place on the TypeScript and IFClite track `0.4.1-beta` ([Beta Docs](/CheckMate_BIMPortal/en/v0.4.1-beta/)), offering a 3D browser viewer, BCF export, and a streamlined Node.js edition (`checkmate-mcp.mjs`) with native Antigravity support.

## Open Points & Outlook

- Support further IDS facets and IFC properties; current limits are described in [IFC and IDS](/CheckMate_BIMPortal/en/architecture/ifc-ids-checking/).
- Evaluate MCP App views in additional clients.
- Seamless advancement of the TypeScript/IFClite architecture (`0.4.1-beta`).
- Maintain interfaces and documentation as development continues.

Development branches and ideas are not commitments to particular features or dates. Code-level details are in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/WORKFLOW.md) and [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/STATUS.md).
