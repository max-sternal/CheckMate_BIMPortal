---
title: Quick start (0.4)
description: Prerequisites, setup and execution of CheckMate 0.4 (TypeScript, IFClite 3D workbench, CLI and MCP server).
---

This guide describes the development state on branch `0.4` (TypeScript & IFClite). For the pure Java edition (`0.3.2`), see the [Quick start (0.3.2)](/CheckMate_BIMPortal/en/v0.3.2/guides/quickstart/).

## Prerequisites

- **Node.js 22+** (Node 24/26 also supported) and **pnpm** (v10/v12+).
- A modern browser with WebGL/WebAssembly for the 3D web workbench (`@checkmate/web`).
- For live queries against the [German Federal BIM Portal](https://via.bund.de/bim): network access. Publicly visible LOINs and AIA projects can be retrieved without signing in; the repository also bundles exported portal IDS fixtures and the Hamburg bridge model (`bruecke-tbw1-mit-merkmalen.ifc`) for offline use.

## Setup and execution

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.4

# Install dependencies across all workspace packages
pnpm install

# Run all automated tests (Vitest)
pnpm test

# Start the 3D browser workbench (http://localhost:5173/)
pnpm web:dev
```

:::tip[Cleanly stopping the local dev server]
In the terminal or PowerShell window where `pnpm web:dev` (Vite) is running, press **`Ctrl + C`** or **`q`** followed by **`Enter`**. If PowerShell asks `Terminate batch job (Y/N)?`, press **`Y`** and **`Enter`**. Port `5173` is released immediately.
:::

## Command line (`@checkmate/cli`)

The CLI on branch `0.4` supports the same flags, German reports and exit codes as `0.3.2`, adding `.rules.json` evaluation and BCF export (`--bcf`):

```bash
# Check the bundled bridge model against a saved portal IDS fixture
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default

# Check an IFC model against a portal LOIN and export JSON + BCF
pnpm cli -- \
  --model resources/models/bruecke-tbw1-mit-merkmalen.ifc \
  --loin <LOIN-GUID> \
  --json build/report.json \
  --bcf build/issues.bcfzip

# Search the portal catalogue ranked by ModelFit
pnpm cli -- --suggest "Kappe" --discipline "Ingenieurbau"
```

Without `--model` or the `CHECKMATE_MODEL` environment variable, the bundled sample model `resources/models/bruecke-tbw1-mit-merkmalen.ifc` is used. The [MCP guide](/CheckMate_BIMPortal/en/mcp/clients/) explains how to configure the MCP server.

## CLI results

| Code | Meaning |
|---|---|
| `0` | All evaluated specifications passed |
| `1` | Model or requirements could not be read or checked |
| `2` | Invalid command-line arguments |
| `3` | At least one specification failed |
| `4` | No violation, but 0 elements applicable (`nicht prüfbar`) or nothing was checked |
