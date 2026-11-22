---
title: Quick Start (Version 0.3.1)
description: Build, test, and run CheckMate 0.3.1 via CLI, GUI, or MCP server — with and without APSTEX JARs.
---

This guide covers the current working release on branch **`0.3.1`** (Java 21 + Gradle 8.14).

## Prerequisites

### Always required
- **JDK 21** (for the JavaFX GUI, use a JDK-FX distribution such as **Azul Zulu 21 JDK FX** as `JAVA_HOME`; any JDK 21 suffices for core tests).
- **Network access** to the German Federal BIM Portal (`https://via.bund.de/bim`). **No account required** — CheckMate reads all published LOINs and AIA projects anonymously.
- **Gradle is bundled** via the wrapper (`gradlew` / `gradlew.bat` in `bim-portal/Java_client/`).

### For full IFC loading in `0.3.1` (GUI, CLI, MCP server)
- **APSTEX IFC Toolbox JARs** (`com.apstex.ifctoolbox*.jar` and `com.apstex.stepparser*.jar`) placed in:
  ```text
  bim-portal/Java_client/hackathon-template/lib/
  ```
  :::note[Availability of APSTEX JARs]
  The APSTEX library was provided for the BIM Portal Hackathon and cannot be redistributed in Git due to licensing. You may request access from the team or directly from APSTEX.
  **Without these JARs**, Gradle still automatically compiles and runs all **148 core & IDS conformance tests**. Versions **`0.3.2`** and **`0.4` (*CheckMate Lite*)** remove this dependency completely.
  :::

### Optional
- **Private portal credentials:** Copy `bim-portal/Java_client/.env_example` to `.env` and set `BIM_PORTAL_USERNAME` and `BIM_PORTAL_PASSWORD` (never commit `.env`).
- **Additional sample IFC models (~5 MB):** `./gradlew :hackathon-template:downloadTestModels`
- **Python 3 + `ifcopenshell` + `ifctester`:** Only needed to regenerate IFC test fixtures (`src/test/python/make_ifc_fixtures.py`) or compare results directly against `ifctester`.

---

## Quick Start Commands

Run all commands from `bim-portal/Java_client/` (on Windows use `.\gradlew.bat` instead of `./gradlew`):

```bash
cd bim-portal/Java_client

# 1. Run tests (148 core tests run without APSTEX; ~217 tests with APSTEX + sample models)
./gradlew :hackathon-template:test

# 2. Launch the JavaFX desktop GUI (requires JDK 21 FX + APSTEX)
./gradlew :hackathon-template:runGui

# 3. Headless CLI: check against a specific BIM Portal LOIN
./gradlew :hackathon-template:runLoinCheck \
  --args="--loin be959b9d-daef-5315-9aba-f60b6ad54ccb"

# 4. Headless CLI: suggest fitting LOINs for phase LPH 5 (metadata only)
./gradlew :hackathon-template:runLoinCheck \
  --args="--suggest --phase 'LPH 5'"

# 5. Custom model (local file, GitHub link, or .ifczip) + JSON report
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /path/to/model.ifc --aia <AIA-GUID> --json build/report.json"

# 6. Build and install the MCP server for AI clients
./gradlew :hackathon-template:installMcp
```

---

## CLI Exit Codes

The headless CLI `LoinCheckCli` returns deterministic exit codes (also exposed as `exitCode` in JSON output):

| Exit Code | Meaning |
|---|---|
| `0` | All evaluated specifications **passed** |
| `1` | Model or requirements could not be read or checked |
| `2` | Unknown CLI argument |
| `3` | At least one specification **failed** |
| `4` | No violation found, but not everything confirmed (**not applicable**, partly evaluated, or nothing checked) |

:::tip[Note on Gradle exit codes]
When invoked via `./gradlew`, Gradle maps any non-zero process exit code to `1`. The true CheckMate exit code appears in Gradle's log line, the final line of the text report, and the JSON `exitCode` field.
:::
