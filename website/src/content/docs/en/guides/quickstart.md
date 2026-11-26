---
title: Quick start (0.3.3-stable)
description: Prerequisites, setup and execution of CheckMate.
---

This guide describes the development state on branch `0.3.3-stable`. The Gradle Wrapper downloads Gradle and required dependencies when needed.

## Prerequisites

- **JDK 25 LTS** for build, CLI, MCP server, and tests (e.g. Azul Zulu 25 JDK FX for running the JavaFX GUI, or any standard JDK 25 for CLI/MCP).
- The IFC reader is implemented in Java; no APSTEX JARs required.
- For portal requirements: network access to the [BIM Portal](https://via.bund.de/bim). Publicly visible content can be retrieved without signing in.

The GUI compiles on every JDK 25 via `org.openjfx` `compileOnly` and `runGui` checks for JavaFX at runtime. The build uses **Gradle 9.1.0** (wrapper included in the repository).

## Setup and execution

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.3.3-stable
cd bim-portal/Java_client

# Tests
./gradlew :hackathon-template:test

# Desktop interface (requires a JDK with JavaFX)
./gradlew :hackathon-template:runGui

# Check a compact test model against a portal LOIN (e.g. building or infrastructure)
./gradlew :hackathon-template:runLoinCheck \
  --args="--model docs/mini-hochbau-bundesbau.ifc --mapping none --loin be959b9d-daef-5315-9aba-f60b6ad54ccb"

./gradlew :hackathon-template:runLoinCheck \
  --args="--model docs/mini-infrastruktur-bruecke.ifc --mapping none --loin be959b9d-daef-5315-9aba-f60b6ad54ccb"
```

On Windows, use `.\gradlew.bat` instead of `./gradlew` and adjust file paths. Without `--model` or the `CHECKMATE_IFC` environment variable, the bundled sample model is used. Relative file paths are resolved from `hackathon-template/`. The CLI also supports `--aia` for a portal project and `--suggest` for catalogue selection; see [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/WORKFLOW.md).

For non-public portal content, copy `bim-portal/Java_client/.env_example` to `.env` and add credentials. Never commit credentials. The same file configures the [chat in the interface](/CheckMate_BIMPortal/en/guides/chat/); without a key it stays inactive and everything else works unchanged.

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
