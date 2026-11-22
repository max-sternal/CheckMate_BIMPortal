# CheckMate 0.4 Architecture & Implementation Plan (TypeScript + IFClite)

Status: 2026-11-22
Base branch: `0.3.2` (`7a2b79e`+) → target branch: `0.4`

---

## 1. Guiding Principles & Branch Synchronisation

- **`0.3.2` / `0.3.x` (Java 25 LTS + Gradle 9.1)** stays the active, pure-Java edition (JavaFX GUI, CLI, Java MCP server, schema-driven STEP reader). It evolves towards `0.3.3` (direct LOIN-XML support, BCF export, extended BIM Portal endpoints).
- **`0.4` (TypeScript + `@ifc-lite/*`)** starts from the latest `0.3.2` commit and replaces the Java runtime on `0.4` with a pure **TypeScript / Node.js / Browser (WASM + WebGPU/WebGL)** monorepo built on **IFClite** (`@ifc-lite/*`).
- **Shared Assets & Feature Parity Contract:**
  - `mapping/bim-hamburg.json` (and custom JSON property mappings), IDS fixtures (`ids/portal/*.ids`, `ids/local/*.ids`), sample IFC models (`samples.properties`), and the Astro Starlight documentation (`website/`) share the exact same format across `0.3.x` and `0.4`.
  - Both editions expose the **identical MCP tool names, parameters, German report semantics, and CLI flags/exit codes** (`0` passed, `1` not read, `2` usage/error, `3` failed, `4` not applicable / `nicht prüfbar`), so features and test cases can be synchronised between `0.3.x` and `0.4`.

---

## 2. Why IFClite (`@ifc-lite/*`) + CheckMate?

IFClite provides a Rust/WASM + TypeScript BIM stack (~260 KB gzipped) verified against `IfcOpenShell`:

| IFClite Package | Role in CheckMate 0.4 |
|---|---|
| `@ifc-lite/parser` & `@ifc-lite/data` | `parseAuto` / `parseColumnar` for IFC2X3, IFC4, IFC4X3, `.ifczip`, and IFC5 (`IFCX`) into a columnar `IfcDataStore`; on-demand property/quantity/attribute extraction. |
| `@ifc-lite/ids` | Native IDS 1.0 parser, validator, auditor, and XML writer (`parseIDS`, `validateIDS`, `createDataAccessor` from `@ifc-lite/ids/bridge`, `createTranslationService('de')`, `auditIDSDocument`, `writeIdsXml`). |
| `@ifc-lite/rules` | `.rules.json` information-validation engine + bidirectional converter (`idsToRuleSet`, `ruleSetToIds`) for checks IDS 1.0 cannot express (case-insensitive options, negations, aggregations, uniqueness, value comparisons, SI unit conversion). |
| `@ifc-lite/geometry` & `@ifc-lite/renderer` / `@ifc-lite/viewer-core` | Exact-arithmetic CSG geometry in WASM + WebGPU / WebGL 3D viewer with GPU picking, section planes, and red/green validation highlighting. |
| `@ifc-lite/bcf` | Read/write BCF 2.1 and 3.0 topics with 3D camera viewpoints for failed elements. |
| `@ifc-lite/sdk` & `@ifc-lite/mcp` | Composable with CheckMate's BIM Portal & LOIN checking tools. |

### What CheckMate Adds on Top of IFClite

Standard IDS validators (including raw `ifctester` and raw `@ifc-lite/ids`) only tell you *that* a specification matched 0 elements or failed a property. CheckMate adds the German BIM Portal workflow and diagnostic intelligence:

1. **BIM Portal Integration (`https://via.bund.de/bim`)**: Search and filter LOINs and AIA projects by metadata (`Fachmodell`, `Leistungsphase`, `Anwendungsfall`), rank by `ModelFit` without downloading the whole catalogue, and fetch IDS / AIA-ZIP requirements on demand (deduplicating combined IDS files in AIA project ZIPs by LOIN GUID + schema).
2. **Configurable JSON Property Mapping (`PropertyMapping`)**: Wraps IFClite's `IFCDataAccessor` so project conventions like `bim-hamburg.json` (`Klassifizierung/Objekttyp` → `Pset_Objektinformation/_IDEbene3`) are checked after the primary IDS address and recorded in the report as `"als Pset_Objektinformation/_IDEbene3"`.
3. **Diagnostic Layer (Why did it fail or match 0 elements?)**:
   - **Zero-Applicability Diagnosis (`nicht prüfbar`)**: Per-facet match counts and near-miss elements with their actual values when 0 elements match.
   - **Strict Type & Spelling Hints**: Exact IDS comparison with actionable hints when a property differs only by case (`CASE_ONLY`), punctuation/umlauts (`SIMILAR`), or a related IFC data type (`IfcText` vs. `IFCLABEL`).
4. **Dual Validation Engine (`@ifc-lite/ids` + `@ifc-lite/rules`)**:
   - **Strict IDS 1.0 mode** via `@ifc-lite/ids` + CheckMate `MappedDataAccessor` & diagnostic post-processor.
   - **Extended Rules mode** via `@ifc-lite/rules` (`.rules.json`), allowing users or the AI to convert a portal IDS into an editable rule set (`idsToRuleSet`), relax case sensitivity or add value/uniqueness rules where a project model deviates from strict IDS, and re-export valid rules to IDS (`ruleSetToIds`).

---

## 3. Monorepo Structure on Branch `0.4` (`pnpm` workspaces)

```text
CheckMate_BIMPortal/
├── packages/
│   ├── bimportal/          # @checkmate/bimportal: TypeScript REST client for via.bund.de/bim (OpenAPI-typed)
│   ├── core/               # @checkmate/core: CheckService, MappedDataAccessor, @ifc-lite/ids + @ifc-lite/rules bridge,
│   │                       #                  PropertyMapping, ModelFit, Near-Miss & Spelling Diagnostics, BCF export, ReportText/Json
│   ├── cli/                # @checkmate/cli: `checkmate` CLI (Node 22+, identical flags & exit codes 0/1/2/3/4 to LoinCheckCli)
│   ├── mcp/                # @checkmate/mcp: MCP server (stdio + Streamable HTTP) & bundled single-file MCP App views
│   └── web/                # @checkmate/web: Standalone browser Web App (IFClite 3D WebGPU/WebGL viewer + BIM Portal LOIN check)
├── resources/
│   ├── mapping/            # bim-hamburg.json (shared with 0.3.x)
│   ├── models/             # bruecke-tbw1-mit-merkmalen.ifc & samples.properties
│   └── fixtures/ids/       # Portal & local IDS test fixtures (from 0.3.2)
├── website/                # Bilingual Astro Starlight docs (DE/EN) with version selector
├── docs/                   # STATUS.md, WORKFLOW.md, MCP-APPS-PLAN.md, PLAN-0.4.md
├── pnpm-workspace.yaml
├── package.json
└── tsconfig.base.json
```

---

## 4. Detailed Component Design

### 4.1 `@checkmate/bimportal` — Zero-Legacy TypeScript BIM Portal Client
- Typed against `openapi.yaml` (`via.bund.de/bim`).
- Public access out of the box, plus optional Bearer JWT authentication (`BIM_PORTAL_USERNAME` / `BIM_PORTAL_PASSWORD` via `/infrastruktur/api/v1/public/auth/login` with token refresh).
- Endpoints:
  - **LOINs & AIA Projects**: `searchLoins`, `exportLoinIds` (IDS XML), `exportLoinXml` (LOIN-XML / ISO 7817-3 for 0.3.3/0.4 parity), `searchProjects`, `exportProjectIds` (ZIP of IDS files).
  - **Extended Portal Endpoints (synchronised with `0.3.3` goals)**: `searchDomainModels` (Fachmodelle), `searchTemplates` (AIA-Vorlagen), `searchContextInfos` (Kontextinformationen), `searchPropertyGroups` & `searchProperties` (Merkmalsgruppen / Merkmale).
- Works in Node.js (CLI & MCP server) and in the browser (Web App, with configurable base URL / optional CORS proxy).

### 4.2 `@checkmate/core` — IFClite Bridge, Hybrid IDS + Rules & Diagnostics
- **Model Loading (`LoadedModel`)**:
  - Uses `parseAuto` / `parseColumnar` from `@ifc-lite/parser` (handles `.ifc`, `.ifczip`, `.ifcx`, plus GitHub blob URLs via `ModelLinks` translation).
- **`MappedDataAccessor` (implements `IFCDataAccessor` from `@ifc-lite/ids`)**:
  - Wraps `createDataAccessor(dataStore)` from `@ifc-lite/ids/bridge`.
  - Intercepts `getPropertyValue(expressId, psetName, propName)` and `getPropertySets(expressId)`:
    1. Queries the exact requested `(psetName, propName)` first.
    2. If absent, iterates `PropertyMapping.candidates({ propertySet: psetName, baseName: propName })`.
    3. Records every alias hit in an `AliasTrace` per `(expressId, psetName, propName)` so the report states `"als Pset_Objektinformation/_IDEbene3"`.
- **Validation & Diagnostic Pipeline (`CheckService`)**:
  1. Runs `validateIDS(idsDoc, mappedAccessor, modelInfo, { translator: createTranslationService('de') })`.
  2. For specifications with `applicableElements === 0` (`Verdict.NOT_APPLICABLE` / `nicht prüfbar`), runs the **CheckMate Diagnosis pass**: evaluates each applicability facet individually on the store and collects the top `NearMiss` elements with their actual values.
  3. For failed property requirements, runs the **Spelling & Related-Type pass**: scans the element's property sets for `CASE_ONLY` or normalized umlaut/hyphen matches (`PropertyKey.spellingOf`) and related IFC types (`IfcText` vs. `IFCLABEL`) and attaches the hint to the finding.
  4. **`.rules.json` Mode (`@ifc-lite/rules`)**:
     - `convertIdsToRules(idsDoc)` via `idsToRuleSet` (reporting any `refused` facets or `droppedChecks`).
     - `checkRules(ruleSet, models)` for extended checks (case-insensitive value comparisons, regex, numeric tolerances, uniqueness, aggregations).
- **BCF Export (`@ifc-lite/bcf`)**:
  - `exportReportToBcf(report, dataStore, geometryResult?)`: turns failed specifications/elements into BCF 2.1 / 3.0 `.bcfzip` archives with element GUIDs, German failure descriptions, and optional 3D camera viewpoints.

### 4.3 `@checkmate/mcp` — TypeScript MCP Server & Interactive MCP Apps (3D + UI)
- Built with `@modelcontextprotocol/sdk` and `@modelcontextprotocol/ext-apps`.
- **Dual Transport**:
  - `stdio` (default, for Claude Desktop, Claude Code, Antigravity local plugin).
  - `Streamable HTTP` (`--http --port 3001`, for remote connectors, `basic-host` testing, and stateless MCP 2026-07-28 readiness).
- **Model-Facing Tools** (with `ToolAnnotations` and `structuredContent` + German text fallback):
  1. `load_model` (`source`) → linked to `ui://checkmate/model`
  2. `describe_model` () → linked to `ui://checkmate/model`
  3. `list_elements` (`entity`, `name`, `limit`)
  4. `find_loins` (`search`, `discipline`, `phase`, `purpose`, `limit`, `include_unfitting`) → linked to `ui://checkmate/loins`
  5. `describe_requirements` (`id`)
  6. `check_model` (`ids`, `ids_files`, `rules_files`, `examples`) → linked to `ui://checkmate/report`
  7. `element_report` (`element`)
  8. `load_mapping` (`source`: path, `default`, `none`)
  9. `export_bcf` (`output`: path for `.bcfzip` of the last check)
  10. `server_status` ()
- **App-Only Tools (`_meta.ui.visibility: ["app"]`)**:
  1. `app_report_page` (`run`, `spec`, `offset`, `limit`) — paginate unmet findings in `ui://checkmate/report`.
  2. `app_element_page` (`entity`, `name`, `offset`, `limit`) — paginate and filter elements in `ui://checkmate/model`.
  3. `app_set_filter` (`search`, `discipline`, `phase`, `purpose`, `include_unfitting`) — live filtering in `ui://checkmate/loins` without spamming the chat.
  4. `app_check_selected` (`ids`) — trigger a check directly from the LOIN picker view and update model context (`ui/update-model-context`).
  5. `app_element_mesh` (`elementIds`) — stream lightweight triangle mesh buffers (from `@ifc-lite/geometry`) to the MCP App iframe so the report/model view can render an interactive 3D preview of failed or selected elements directly inside Claude Desktop and Antigravity!
- **Bundled Single-File MCP App Views (`ui://checkmate/*`)**:
  - Built with Vite + `vite-plugin-singlefile` into self-contained HTML resources (`text/html;profile=mcp-app`) obeying strict default CSP (`connect-src 'none'`).
  - **Host Integration**:
    - **Claude Desktop**: local stdio config (`claude_desktop_config.json`), renders inline & fullscreen views.
    - **Claude Code**: uses the rich German text output and `structuredContent`.
    - **Antigravity App (v2.34+)**: packaged with `mcp_config.json` (`${PLUGIN_ROOT}`), supports inline chat cards and side-panel / full-page surfaces (`com.google.antigravity/ui`).

### 4.4 `@checkmate/web` — Standalone Browser Web App (Online 3D Viewer & Checker)
- Vite + TypeScript Web App running 100% client-side in the browser (deployable to GitHub Pages alongside `website/` or as a standalone preview):
  - **3D Viewport**: `@ifc-lite/renderer` (WebGPU) with `@ifc-lite/viewer-core` (WebGL2 fallback), section planes, orbit/pan/zoom, and GPU picking.
  - **BIM Portal & IDS Panel**: Search LOINs/AIA projects live, upload local `.ids` or `.rules.json` files, switch JSON property mappings (`bim-hamburg.json` / `none` / custom upload).
  - **Interactive 3D Validation**: Failed elements highlight in red, passed in green, non-applicable in grey; clicking a finding in the report zooms the 3D camera to the element and shows its `element_report` + diagnosis.
  - **One-Click Exports**: Download CheckMate JSON/Text report, export failed elements as BCF (`.bcfzip`), or export `.rules.json` / `.ids`.

### 4.5 `@checkmate/cli` — Node.js CLI
- `checkmate` binary (`npx @checkmate/cli` / `pnpm checkmate`):
  - Supports `--model`, `--loin`, `--aia`, `--ids`, `--rules`, `--mapping <file|none|default>`, `--suggest`, `--discipline`, `--phase`, `--purpose`, `--json <file>`, `--bcf <file>`, `--full`.
  - Produces the exact same German summary, brief/full report, and exit codes (`0`, `1`, `2`, `3`, `4`) as Java `LoinCheckCli`.

---

## 5. Step-by-Step Milestones (Small Commits, Continuous Push)

To ensure nothing is ever lost and every milestone is verifiable on its own:

- **Milestone 0 — Finish & Sync MCP App Views on `0.3.2` (or directly on `0.4` base)**
  - Keep `0.3.2` clean at `7a2b79e`.
  - Reset/branch `0.4` from `0.3.2` (`7a2b79e`) and commit this plan (`docs/PLAN-0.4.md`).
- **Milestone 1 — TypeScript Workspace & `@checkmate/bimportal` on `0.4`**
  - Set up `pnpm-workspace.yaml`, `package.json`, `tsconfig.base.json`, Vitest, and shared `resources/` (moving/linking `bim-hamburg.json`, test IDS fixtures, and `bruecke-tbw1-mit-merkmalen.ifc`).
  - Implement `@checkmate/bimportal` (LOINs, AIA projects, ZIP/IDS extraction, extended portal endpoints) with unit tests.
- **Milestone 2 — `@checkmate/core` (`@ifc-lite/parser` + `@ifc-lite/ids` + `@ifc-lite/rules` + `PropertyMapping`)**
  - Implement `LoadedModel` on `@ifc-lite/parser`, `PropertyMapping` + `MappedDataAccessor`, `ModelFit`, CheckMate Near-Miss Diagnosis & Spelling/Type hints, `@ifc-lite/rules` integration, and `@ifc-lite/bcf` export.
  - Verify parity against the existing 0.3.2 test cases (TBW1 bridge model + `ueberbau-kappe-lph8-9.ids` + Hamburg mapping).
- **Milestone 3 — `@checkmate/cli`**
  - Implement the CLI with all `LoinCheckCli` flags plus `--rules` and `--bcf`, with CLI integration tests.
- **Milestone 4 — `@checkmate/mcp` & All 4 App-Only Tools + 3 MCP App Views**
  - Implement the TypeScript MCP server (stdio + Streamable HTTP) with all tools (`load_model`, `describe_model`, `list_elements`, `find_loins`, `describe_requirements`, `check_model`, `element_report`, `load_mapping`, `export_bcf`, `server_status`).
  - Implement all app-only tools (`app_report_page`, `app_element_page`, `app_set_filter`, `app_check_selected`, `app_element_mesh`).
  - Bundle the single-file MCP App views (`ui://checkmate/report`, `ui://checkmate/model`, `ui://checkmate/loins` with 3D element preview) and Antigravity plugin config (`mcp_config.json`).
- **Milestone 5 — `@checkmate/web` (Browser 3D Viewer + LOIN Checker)**
  - Build the browser Web App combining IFClite's 3D viewer, BIM Portal search, IDS/Rules check, 3D red/green highlighting, and BCF export.
- **Milestone 6 — CI, Documentation & `0.3.x` ↔ `0.4` Sync Guide**
  - Update GitHub Actions CI on `0.4` for Node 22 + `pnpm test` + `pnpm build`, and document `0.4` in `website/`.
