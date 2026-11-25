---
title: Project status
description: Current state and open points for CheckMate 0.3.2.
---

## Version 0.3.2

CheckMate provides a JavaFX interface, CLI and MCP server for checking IFC models against supported IDS requirements. Requirements can come from the BIM Portal or local IDS files. The Java reader handles IFC2X3, IFC4 and IFC4X3 without APSTEX.

The MCP App report view is implemented and tested at protocol level; visual testing in Claude Desktop or Antigravity remains open. JSON mappings can be loaded and switched.

## In progress on 0.3.3

The [chat in the interface](/CheckMate_BIMPortal/en/guides/chat/) starts a check through a language model that CheckMate calls with its own API key. The model and endpoint are configurable, so models from other vendors can be used through a gateway. A question was answered end to end on 24 November 2026; a paid model has not been compared, and `check_model` is so far the chat's only tool.

## Open points

- Support further IDS facets and IFC properties; current limits are described in [IFC and IDS](/CheckMate_BIMPortal/en/architecture/ifc-ids-checking/).
- Test the MCP App report view in compatible clients and investigate further views.
- Investigate a catalogue search as a second chat tool, so a LOIN need not be named by its ID.
- Investigate LOIN-XML support for `0.3.3`. LOIN-XML is distinct from the currently supported IDS format. Starting points are the [BIM Portal import interface](https://github.com/bimdeutschland/BIM-Portal-Importschnittstellendokumentation) and [ISO/DIS 7817-3](https://www.iso.org/standard/89077.html); the standard is still under development.
- Investigate better online use with a viewer and MCP App integration for `0.4`.
- Maintain interfaces and documentation as development continues.

Development branches and ideas are not commitments to particular features or dates. Code-level details are in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/docs/WORKFLOW.md) and [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/docs/STATUS.md).
