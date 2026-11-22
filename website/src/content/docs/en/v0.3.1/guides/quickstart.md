---
editUrl: false
title: Quick start (0.3.1)
description: Prerequisites, setup and execution of CheckMate.
slug: en/v0.3.1/guides/quickstart
---

This guide describes branch `0.3.1`. The Gradle Wrapper downloads Gradle and required dependencies when needed.

## Prerequisites

* JDK 21. To build and run the GUI, CLI and MCP server with APSTEX, use a JDK with JavaFX, such as Azul Zulu 21 JDK FX. A JDK 21 without JavaFX is sufficient for core tests without APSTEX.
* To read IFC: place `com.apstex.ifctoolbox*.jar` and `com.apstex.stepparser*.jar` in `bim-portal/Java_client/hackathon-template/lib/`. These proprietary libraries are not included in the repository; ask the project team or [APSTEX](https://www.apstex.com/) about availability.
* For portal requirements: network access to the [BIM Portal](https://via.bund.de/bim). Publicly visible content can be retrieved without signing in.

Without APSTEX, the build excludes the GUI, CLI, MCP server and IFC-reading tests. The checking core and its tests can still be built and run.

## Setup and execution

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch --detach v0.3.1
cd bim-portal/Java_client

# Tests
./gradlew :hackathon-template:test

# Desktop interface (requires a JDK with JavaFX and APSTEX)
./gradlew :hackathon-template:runGui

# Check an IFC model against a local IDS file
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /path/to/model.ifc --ids /path/to/requirements.ids"

# Check a LOIN from the portal
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /path/to/model.ifc --loin <LOIN-ID> --json build/report.json"
```

On Windows, use `.\gradlew.bat` instead of `./gradlew` and adjust file paths. Without `--model` or the `CHECKMATE_IFC` environment variable, the bundled sample model is used. The CLI also supports `--aia` for a portal project and `--suggest` for catalogue selection; see [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/v0.3.1/docs/WORKFLOW.md).

For non-public portal content, copy `bim-portal/Java_client/.env_example` to `.env` and add credentials. Never commit credentials.

The [MCP guide](/CheckMate_BIMPortal/en/v0.3.1/mcp/clients/) explains how to set up the server.

## CLI results

| Code | Meaning |
|---|---|
| `0` | All evaluated specifications passed |
| `1` | Model or requirements could not be read or checked |
| `2` | Invalid command-line arguments |
| `3` | At least one specification failed |
| `4` | No violation, but not everything was confirmed or nothing was checked |

When invoked through Gradle, a nonzero exit code is reported externally as `1`. The CheckMate code appears in the text report and in the JSON `exitCode` field. Take any unevaluated requirements in the report into account.
