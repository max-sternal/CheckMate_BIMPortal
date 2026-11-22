---
title: MCP Server & MCP Apps
description: Connect CheckMate to AI clients via the Model Context Protocol (MCP) — current 0.3.1 state and roadmap to November 2026 MCP Apps standards.
---

## Core Principle: AI Explains, CheckMate Checks Deterministically

Through the **Model Context Protocol (MCP)**, AI applications can call CheckMate as a tool — answering questions like *"Which LOINs fit this bridge?"*, *"Does the model satisfy the Kappe LOIN for LPH 8?"*, or *"Why did element X fail?"*.

This enforces a strict trust and quality boundary:
- **The AI never sees the raw IFC file**, only structured tool responses from CheckMate.
- **Validation itself remains 100% deterministic** inside CheckMate's core — no hallucinated compliance verdicts.
- **The AI provider stays swappable:** Antigravity, Claude, OpenCode, open cloud models (such as Mistral via Ollama Cloud), or fully local Qwen models via Ollama or Strata.

---

## The 7 MCP Tools in `0.3.1`

Implemented in `CheckMateMcpServer` and `CheckMateTools` using the official **MCP Java SDK (`2.1.0`)** over **`stdio`** (`./gradlew :hackathon-template:installMcp`):

| Tool | Purpose | Key Arguments |
|---|---|---|
| `load_model` | Loads an IFC/ifcZIP model from a local path or HTTP(S)/GitHub link | `source` *(required)* |
| `describe_model` | Returns model name, IFC schema, element count, and top 15 entities | — |
| `list_elements` | Lists elements as `GlobalId \| Entity \| Name \| PredefinedType` | `entity`, `name`, `limit` (30) |
| `find_loins` | Searches BIM Portal AIA projects & LOINs, ranked by model fit (metadata only) | `search`, `discipline`, `phase`, `purpose`, `limit` (20), `include_unfitting` |
| `describe_requirements` | Explains what a LOIN or AIA project selects & requires, with applicable count | `id` *(required, GUID or portal URL)* |
| `check_model` | Runs deterministic check and stores results for `element_report` | `ids`, `ids_files`, `examples` (3; `0` = full report) |
| `element_report` | Shows findings from the last check and properties for one element | `element` *(GlobalId or name substring)* |

**Safeguards in `0.3.1`:**
- All logs are routed to `stderr`; `stdout` carries JSON-RPC exclusively.
- Responses are cleanly truncated at line boundaries to **50,000 characters** (~15,000 tokens) while always preserving the final verdict line.
- Errors (missing file, unreachable portal) are returned with `isError: true` so the AI can report them cleanly without crashing the server.

---

## Upgrading to November 2026 MCP Standards & Antigravity MCP Apps

The `0.3.1` server uses `stdio` and German plain-text responses. For **`0.3.2`** and **`0.4` (*CheckMate Lite*)**, CheckMate targets the latest **November 2026 MCP specifications**:

### 1. Core Specification `2026-07-28` (`0.3.2` & `0.4`)
- **Stateless Streamable HTTP (`/mcp`):** Enables containerized, public-sector server and gateway deployments without long-lived local `stdio` processes; explicit model/report handles instead of implicit process globals.
- **Tool Annotations (`readOnlyHint`, `idempotentHint`, `openWorldHint`):** Marks read-only inspection and checking tools so hosts like Antigravity or OpenCode avoid redundant approval prompts.
- **Structured Outputs (`outputSchema` + `structuredContent`):** Returns machine-readable JSON from `ReportJson` alongside human-readable text — dramatically improving reliability for smaller local models (Qwen, Mistral).
- **Progress Notifications & Prompts:** Live progress updates during 7-second catalogue fetches or 36-LOIN AIA project checks, plus guided checking prompts.

### 2. MCP Apps Extension (`io.modelcontextprotocol/ui`) — Antigravity MCP App
With the official **MCP Apps** extension (`@modelcontextprotocol/ext-apps`), an MCP server delivers interactive, sandboxed HTML UIs (`ui://` resources linked via `_meta.ui.resourceUri`) right inside hosts such as **Antigravity**:
- **`ui://checkmate/report-app`:** Interactive compliance table (passed / failed / not applicable) with phase & discipline filters; clicking an element invokes `element_report` via `postMessage` JSON-RPC.
- **`ui://checkmate/catalogue-app`:** Visual picker for fitting BIM Portal LOINs.
- **In `0.4` (*CheckMate Lite*):** Embeds the **IFClite WebGPU 3D viewer** directly inside the MCP App — failing elements highlight in red in 3D and can be inspected with a click.
