---
title: Quick Start (0.4.1-beta)
description: Installation, CLI setup, 3D browser workbench, and first checks with CheckMate 0.4.1-beta.
---

This guide covers **CheckMate `0.4.1-beta`** (TypeScript & IFClite). For the current stable main release of the Java 25 edition (**`0.3.3-stable`**, `Latest`), see [Quick Start (0.3.3-stable)](/CheckMate_BIMPortal/en/guides/quickstart/).

## Requirements

- **Node.js 22+** (Node 24/26 also supported) and **pnpm** (v10/v12+).
- A modern browser with WebGL/WebAssembly for the 3D browser workbench (`@checkmate/web`).
- Network access for live queries against the [German Federal BIM Portal](https://via.bund.de/bim). Public LOINs and AIA projects require no login; exported portal IDS archives and the building, bridge, and BIM.Hamburg models are bundled for offline operation.

## 1. Clone, install & verify

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.4.1-beta

# Install workspace dependencies
pnpm install

# Run automated tests & typechecks
pnpm test
pnpm build
```

## 2. Install & use the CLI (`@checkmate/cli`)

Run the CLI directly from the repository via `pnpm cli -- ...` or link `checkmate` globally so it is available anywhere in your terminal:

```bash
# Optional: make `checkmate` available globally in your terminal
pnpm --filter @checkmate/cli link --global
checkmate --help
```

### Run your first checks

```bash
# 1) Compact building example (5 BIM Portal LOINs "AIA Bundesbauten" -> exit code 0)
pnpm cli -- --model resources/models/mini-hochbau-bundesbau.ifc \
  --ids resources/fixtures/ids/portal/mini-hochbau-bundesbau.zip --mapping none

# 2) Compact infrastructure example (6 BIM Portal LOINs "Ingenieurbau" -> exit code 0)
pnpm cli -- --model resources/models/mini-infrastruktur-bruecke.ifc \
  --ids resources/fixtures/ids/portal/mini-infrastruktur-bruecke.zip --mapping none

# 3) Live check against a published BIM Portal LOIN GUID
pnpm cli -- --model resources/models/mini-infrastruktur-bruecke.ifc \
  --loin be959b9d-daef-5315-9aba-f60b6ad54ccb --mapping none

# 4) Hamburg bridge model with PropertyMapping & JSON report export
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids \
  --mapping default --json build/report.json

# 5) Search fitting LOINs in the BIM Portal ranked by ModelFit
pnpm cli -- --suggest "Kappe" --discipline "Ingenieurbau"
```

Without `--model` (or `CHECKMATE_MODEL`), CheckMate loads the bundled bridge model `resources/models/bruecke-tbw1-mit-merkmalen.ifc`.

| Exit code | Meaning |
|---|---|
| `0` | All evaluated specifications passed |
| `1` | Model or requirements could not be read |
| `2` | Invalid command-line arguments |
| `3` | At least one specification failed |
| `4` | No failure, but `0` applicable elements (`nicht prüfbar`) or nothing checked |

## 3. Launch the 3D browser workbench (`@checkmate/web`)

```bash
# Start local dev server (http://localhost:5173/)
pnpm web:dev

# Optional: build static production bundle into packages/web/dist/
pnpm web:build
```

The top toolbar provides 1-click presets:
- **`🏢 Mini-Hochbau (5 LOINs)`**: Loads the single-storey IFC4 pavilion in 3D and checks all 5 federal building LOINs (all elements green, `exit code 0`).
- **`🌉 Mini-Brücke (6 LOINs)`**: Loads the single-span IFC4 bridge in 3D and checks 6 civil engineering LOINs (all elements green, `exit code 0`).
- **`⚡ TBW1: Kappen-Fehler`** & **`🏛️ TBW1: Portal Kappe`**: Demonstrate red failure highlighting and yellow near-miss diagnosis on the Hamburg bridge model.

:::tip[Stopping the local dev server cleanly]
Press **`Ctrl + C`** (or **`q`** + **`Enter`**) in the terminal running `pnpm web:dev`.
:::

To connect **Antigravity** or **Claude Desktop** to the MCP server, see [Set Up a Client](/CheckMate_BIMPortal/en/v0.4.1-beta/mcp/clients/).
