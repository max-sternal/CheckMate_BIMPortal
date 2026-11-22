---
title: CheckMate 0.1
editUrl: https://github.com/max-sternal/CheckMate_BIMPortal/edit/0.3.2/website/src/content/docs/en/v0.1.0/index.md
description: The CheckMate hackathon prototype.
slug: en/v0.1.0
---

## The hackathon result

CheckMate `0.1` is the state of the 24-hour [2025 BIM Portal Hackathon](https://www.bimdeutschland.de/veranstaltungen/hackathon-2025), including the [project presentation](https://github.com/max-sternal/CheckMate_BIMPortal/blob/v0.1.0/CheckMate_Hackathon_BIMPortal_2025.pdf). The idea: help planners and reviewers work with BIM information requirements.

The prototype connects a JavaFX interface to the BIM Portal client and reads an IFC model using APSTEX. The interface displays model elements and a LOIN list. The “Validate LOIN” button is still a placeholder in this version; it should not be taken as a working IDS check or MCP integration.

## Context and prerequisites

This is a historical prototype, not a current installation guide. It contains a machine-specific model path and requires JavaFX and the APSTEX IFC Toolbox, which is not included in the repository. A fresh checkout therefore needs adjustments before it can run.

- [Source code for v0.1.0](https://github.com/max-sternal/CheckMate_BIMPortal/tree/v0.1.0)
- [Hackathon starter kit and API client](https://github.com/pb40development/bim-portal)

This page was written retrospectively. The release tag points to the unchanged historical branch state; the page is not part of that commit.
