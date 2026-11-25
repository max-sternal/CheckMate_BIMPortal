---
title: Quick start (0.3.2)
description: Prerequisites, setup and execution of CheckMate.
---

This guide describes the development state on branch `0.3.2`. The Gradle Wrapper downloads Gradle and required dependencies when needed.

## Prerequisites

- JDK 21 for the build, CLI and MCP server. The graphical interface requires a JDK with JavaFX, such as Azul Zulu 21 JDK FX.
- The IFC reader is implemented in Java; `0.3.2` does not require APSTEX JARs.
- For portal requirements: network access to the [BIM Portal](https://via.bund.de/bim). Publicly visible content can be retrieved without signing in.

Without JavaFX, the build excludes only the GUI; the CLI, MCP server and IFC reader remain available. The build uses Gradle 8.14 and targets Java 21. Java 25 is not supported by this wrapper and would require a Gradle upgrade.

## Setup and execution

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.3.2
cd bim-portal/Java_client

# Tests
./gradlew :hackathon-template:test

# Desktop interface (requires a JDK with JavaFX)
./gradlew :hackathon-template:runGui

# Check an IFC model against a local IDS file
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /path/to/model.ifc --ids /path/to/requirements.ids"

# Check a LOIN from the portal
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /path/to/model.ifc --loin <LOIN-ID> --json build/report.json"
```

On Windows, use `.\gradlew.bat` instead of `./gradlew` and adjust file paths. Without `--model` or the `CHECKMATE_IFC` environment variable, the bundled sample model is used. Relative file paths are resolved from `hackathon-template/`. The CLI also supports `--aia` for a portal project and `--suggest` for catalogue selection; see [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/docs/WORKFLOW.md).

For non-public portal content, copy `bim-portal/Java_client/.env_example` to `.env` and add credentials. Never commit credentials. The same file configures the [chat in the interface](/CheckMate_BIMPortal/en/guides/chat/) on branch `0.3.3`; without a key it stays inactive and everything else works unchanged.

The [MCP guide](/CheckMate_BIMPortal/en/mcp/clients/) explains how to set up the server.

## CLI results

| Code | Meaning |
|---|---|
| `0` | All evaluated specifications passed |
| `1` | Model or requirements could not be read or checked |
| `2` | Invalid command-line arguments |
| `3` | At least one specification failed |
| `4` | No violation, but not everything was confirmed or nothing was checked |

When invoked through Gradle, a nonzero exit code is reported externally as `1`. The CheckMate code appears in the text report and in the JSON `exitCode` field. Take any unevaluated requirements in the report into account.
