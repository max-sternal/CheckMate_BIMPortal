# CheckMate `0.4.1-beta` — TypeScript + IFClite 3D Workbench, CLI & MCP Apps

CheckMate checks IFC building and infrastructure models against the requirements of the German federal
[BIM Portal](https://via.bund.de/bim): LOINs, whole AIA projects, local IDS 1.0 files, or
`@ifc-lite/rules` `.rules.json` rule sets.

- **Latest Stable Release (`0.3.3-stable`, Pure Java 25)**: JavaFX desktop GUI with optional in-GUI AI chat (`CheckMateChat`), CLI, and Java MCP server on branch [`0.3.3-stable`](https://github.com/max-sternal/CheckMate_BIMPortal/tree/0.3.3-stable) / [`0.3.3`](https://github.com/max-sternal/CheckMate_BIMPortal/tree/0.3.3).
- **Beta Pre-Release (`0.4.1-beta`, TypeScript + IFClite)**: Browser 3D Workbench (`@checkmate/web`), streamlined MCP server with interactive 3D MCP Apps (`@checkmate/mcp`), standalone CLI (`@checkmate/cli`), BCF 2.1 export, and compact 3D BIM Portal IFC4 reference models on branch [`0.4.1-beta`](https://github.com/max-sternal/CheckMate_BIMPortal/tree/0.4.1-beta) / [`0.4`](https://github.com/max-sternal/CheckMate_BIMPortal/tree/0.4).
- **Documentation Site**: Bilingual (DE/EN) Starlight docs at **[max-sternal.github.io/CheckMate_BIMPortal](https://max-sternal.github.io/CheckMate_BIMPortal/)** with version selector (`Latest (0.3.3-stable)`, `v0.4.1-beta`, `v0.4-alpha`, `v0.3.2`, `v0.3.1`, `v0.2.0`, `v0.1.0`).

---

## Workspace packages (`0.4.1-beta`)

| Path | Purpose |
|---|---|
| `packages/bimportal/` (`@checkmate/bimportal`) | TypeScript REST client for `https://via.bund.de/bim`, catalogue search, `CatalogueFilter`, UUID extraction, and ZIP/IDS unpacking (`fflate`) |
| `packages/core/` (`@checkmate/core`) | Browser- and Node-compatible checking engine: `LoadedModel` (`@ifc-lite/parser` + `@ifc-lite/geometry`), `PropertyMapping`, `MappedDataAccessor`, `ModelFit`, `LoinDescription`, `CheckService` (`@ifc-lite/ids` + `@ifc-lite/rules`), near-miss diagnosis, BCF 2.1 export (`@ifc-lite/bcf`), `ReportText`, and `ReportJson` |
| `packages/web/` (`@checkmate/web`) | Standalone 3D browser workbench (Vite + WebGL) with 1-click Hochbau, Infrastruktur & BIM.Hamburg presets and 4 tabs (Check Report, BIM Portal Catalogue, Element Explorer, Mapping/Rules/BCF) |
| `packages/mcp/` (`@checkmate/mcp`) | Clean, modular MCP server over `stdio` (`packages/mcp/bin/checkmate-mcp.mjs`) with 10 model tools, 5 app-only tools, 3 interactive 3D MCP App views (`ui://checkmate/*`), 2 JSON resources, and 2 guided prompts |
| `packages/cli/` (`@checkmate/cli`) | Standalone CLI (`packages/cli/bin/checkmate.mjs`) supporting `--model`, `--loin`, `--aia`, `--ids`, `--rules`, `--mapping`, `--suggest`, `--json`, `--bcf`, `--full`, `--help`, `--version`, and exit codes `0..4` |
| `resources/` & `docs/` | Bundled `mapping/bim-hamburg.json`, compact 3D IFC4 BIM Portal reference models (`mini-hochbau-bundesbau.ifc`, `mini-infrastruktur-bruecke.ifc`), `bruecke-tbw1-mit-merkmalen.ifc`, and extended test models (`docs/README.md`) |

---

## Quick start (`0.4.1-beta` — Node.js 22+ & pnpm)

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.4.1-beta

pnpm install          # install all workspace dependencies
pnpm test             # run automated Vitest test suites
pnpm build            # typecheck all workspace packages
pnpm web:dev          # launch 3D browser workbench at http://localhost:5173/
```

To stop `pnpm web:dev`, press **`Ctrl + C`** (or **`q` + `Enter`**) in the terminal.

### 1. CLI installation & BIM Portal IFC4 examples (`@checkmate/cli`)

Run directly via `pnpm cli -- ...` or link `checkmate` globally for terminal-wide access:

```bash
pnpm --filter @checkmate/cli link --global
checkmate --help
```

Check the two compact 3D IFC4 BIM Portal reference models (offline via bundled `.zip` or live via `--loin`):

```bash
# Hochbau: 5 LOINs from "Beispiel AIA Bundesbauten" (LPH 3) -> 5/5 passed (Exit-Code 0)
pnpm cli -- --model resources/models/mini-hochbau-bundesbau.ifc \
  --ids resources/fixtures/ids/portal/mini-hochbau-bundesbau.zip --mapping none

# Infrastruktur: 6 LOINs from "Pflegestelle Straße" (Ingenieurbau) -> 6/6 passed (Exit-Code 0)
pnpm cli -- --model resources/models/mini-infrastruktur-bruecke.ifc \
  --ids resources/fixtures/ids/portal/mini-infrastruktur-bruecke.zip --mapping none

# BIM.Hamburg TBW1 bridge: demonstrates PropertyMapping & near-miss diagnosis (Exit-Code 4)
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default
```

### 2. MCP Server & 3D MCP Apps (`@checkmate/mcp`)

After `pnpm install`, the standalone launcher `packages/mcp/bin/checkmate-mcp.mjs` runs directly under Node.js 22+ (no `--import tsx` flags needed):

- **Claude Desktop** (`claude_desktop_config.json`) & **Antigravity** (`mcp_config.json`):
  ```json
  {
    "mcpServers": {
      "checkmate": {
        "command": "node",
        "args": ["/absolute/path/to/CheckMate_BIMPortal/packages/mcp/bin/checkmate-mcp.mjs"],
        "env": {
          "CHECKMATE_MAPPING": "default"
        }
      }
    }
  }
  ```
- **Antigravity local plugin bundle**:
  ```bash
  pnpm mcp:antigravity-plugin
  agy plugin validate build/checkmate-antigravity-plugin --strict
  agy plugin install build/checkmate-antigravity-plugin
  ```
- **Claude Code**:
  ```bash
  claude mcp add checkmate -- node /absolute/path/to/CheckMate_BIMPortal/packages/mcp/bin/checkmate-mcp.mjs
  ```

---

## Acknowledgments & License

CheckMate originated at the [BIM Portal Hackathon Berlin](https://www.bimdeutschland.de/veranstaltungen/hackathon-2025) (22–23 September 2025). Special thanks go to *planen-bauen 4.0* / *BIM Deutschland* for organizing and supporting the hackathon, to **BIM.Hamburg** (especially **Cornelius Lüdtke** and Daniel Dombeck) for providing real-world bridge models, to Federal Building Minister **Verena Hubertz** (*BMWSB*) for the award presentation, to the **Jury** for their trust, and to the **Federal Ministry for Transport (BMV)** for hosting an outstanding event. See [Contributors & Transparency](https://max-sternal.github.io/CheckMate_BIMPortal/project/contributors/) for full details.

Licensed under the [Apache License 2.0](LICENSE). CheckMate has no proprietary third-party runtime dependencies.
