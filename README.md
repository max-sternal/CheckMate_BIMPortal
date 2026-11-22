# CheckMate 0.4 (`0.4-alpha` — TypeScript + IFClite Edition)

CheckMate checks an IFC model against the requirements of the German federal
[BIM Portal](https://via.bund.de/bim): LOINs, whole AIA projects, local IDS 1.0 files, or
`@ifc-lite/rules` `.rules.json` rule sets.

Branch **`0.4`** combines CheckMate's domain workflow (`ModelFit`, `LoinDescription`,
`PropertyMapping` / `bim-hamburg.json`, zero-applicability near-miss diagnosis, German reports and
exit codes `0..4`) with **TypeScript and IFClite (`@ifc-lite/*`)** for interactive 3D visualization
in the browser and inside MCP-capable AI clients.

The pure-Java 25 edition continues in parallel on branch **`0.3.2`** (and upcoming **`0.3.3`**).
The bilingual [documentation site](https://max-sternal.github.io/CheckMate_BIMPortal/) includes a
version selector for `0.4`, `v0.3.2`, `v0.3.1`, `v0.2.0` and `v0.1.0`.

## Workspace packages (`0.4`)

| Path | What it is |
|---|---|
| `packages/bimportal/` (`@checkmate/bimportal`) | TypeScript REST client for `https://via.bund.de/bim`, catalogue search, `CatalogueFilter`, UUID extraction and ZIP/IDS unpacking (`fflate`) |
| `packages/core/` (`@checkmate/core`) | Browser- and Node-compatible checking engine: `LoadedModel` (`@ifc-lite/parser` + `@ifc-lite/geometry`), `PropertyMapping`, `MappedDataAccessor`, `ModelFit`, `LoinDescription`, `CheckService` (`@ifc-lite/ids` + `@ifc-lite/rules`), near-miss diagnosis, BCF 2.1 export (`@ifc-lite/bcf`), `ReportText` and `ReportJson` |
| `packages/web/` (`@checkmate/web`) | Standalone 3D browser workbench (Vite + WebGL) with 4 tabs: Check Report & Diagnosis, BIM Portal Catalogue & `LoinDescription`, Model & Element Explorer, and Mapping / `.rules.json` / BCF Export |
| `packages/mcp/` (`@checkmate/mcp`) | MCP server over `stdio` with 10 model tools, 5 app-only tools (`visibility: ["app"]`) and 3 interactive 3D MCP App views (`ui://checkmate/report`, `ui://checkmate/model`, `ui://checkmate/loins`) |
| `packages/cli/` (`@checkmate/cli`) | Headless CLI (`--model`, `--ids`, `--loin`, `--aia`, `--rules`, `--mapping`, `--suggest`, `--json`, `--bcf`, `--full`) matching `0.3.2` exit codes `0..4` |
| `resources/` | Bundled `mapping/bim-hamburg.json`, sample bridge model `models/bruecke-tbw1-mit-merkmalen.ifc` and portal/local IDS fixtures |
| `bim-portal/Java_client/` | Pure-Java 25 edition from `0.3.2` (Gradle 9.1.0, JavaFX GUI, CLI, Java MCP server) |
| `website/` | Bilingual (DE/EN) Astro Starlight documentation site with version selector (`0.4`, `v0.3.2`, `v0.3.1`, `v0.2.0`, `v0.1.0`) |

## Quick start (`0.4` — TypeScript & 3D Web Workbench)

Requires **Node.js 22+** and **pnpm**:

```bash
pnpm install                  # install workspace dependencies
pnpm test                     # run all Vitest suites
pnpm web:dev                  # launch 3D browser workbench at http://localhost:5173/
```

To cleanly stop `pnpm web:dev`, press **`Ctrl + C`** (or **`q` + `Enter`**) in the terminal.

### CLI (`@checkmate/cli`)

```bash
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default
pnpm cli -- --suggest "Kappe" --discipline "Ingenieurbau"
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-schaft-lph1-9.ids --bcf build/issues.bcfzip
```

### MCP Server (`@checkmate/mcp`)

See [`mcp_config.json`](mcp_config.json) for a ready-to-use configuration for Claude Desktop,
Claude Code, or Antigravity:

```bash
claude mcp add checkmate -- node --import tsx /path/to/CheckMate_BIMPortal/packages/mcp/src/index.ts
```

## Quick start (`0.3.2` — Pure Java 25 Edition)

From `bim-portal/Java_client/` (requires JDK 21 or JDK 25 LTS):

```bash
./gradlew :hackathon-template:test
./gradlew :hackathon-template:runGui
./gradlew :hackathon-template:runLoinCheck --args="--loin be959b9d-daef-5315-9aba-f60b6ad54ccb"
./gradlew :hackathon-template:installMcp
```

## License

Apache License 2.0, see [LICENSE](LICENSE). CheckMate has no proprietary third-party runtime
dependencies.
