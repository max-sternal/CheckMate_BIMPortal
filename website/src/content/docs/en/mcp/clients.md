---
title: Set Up a Client (Antigravity & Claude Desktop)
description: Step-by-step setup of the CheckMate MCP server and 3D MCP Apps in Antigravity, Claude Desktop, and Claude Code.
---

After running `pnpm install` once in the cloned repository, the `0.4.1-beta` MCP server starts directly through the standalone launcher **`packages/mcp/bin/checkmate-mcp.mjs`** with a single argument — no `--import tsx` flags or shell wrappers needed. A template is included in [`mcp_config.json`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/mcp_config.json).

---

## 1. Antigravity (including 3D MCP Apps)

Antigravity supports both direct `mcp_config.json` entries and packaged local plugins, rendering the interactive 3D views (`ui://checkmate/report`, `ui://checkmate/model`, `ui://checkmate/loins`) inline in the chat.

### Option A: Direct MCP configuration (`mcp_config.json`)

In Antigravity under **Customizations / MCP Servers**, add the `checkmate` entry using an absolute path:

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "node",
      "args": ["/path/to/CheckMate_BIMPortal/packages/mcp/bin/checkmate-mcp.mjs"],
      "env": {
        "CHECKMATE_MAPPING": "default"
      }
    }
  }
}
```

*(On Windows, use e.g. `"C:\\Users\\Name\\CheckMate_BIMPortal\\packages\\mcp\\bin\\checkmate-mcp.mjs"`.)*

### Option B: Install as a local Antigravity plugin

Generate a ready-to-install plugin bundle (`plugin.json`, logo, and `mcp_config.json`) and validate/install it with the Antigravity CLI (`agy`):

```bash
pnpm mcp:antigravity-plugin
agy plugin validate build/checkmate-antigravity-plugin --strict
agy plugin install build/checkmate-antigravity-plugin
```

---

## 2. Claude Desktop (Windows & macOS, including 3D MCP Apps)

1. Open **Settings → Developer → Edit Config** in Claude Desktop, which opens:
   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
2. Add the `checkmate` entry with the absolute path to `checkmate-mcp.mjs`:

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "node",
      "args": ["C:\\Users\\Name\\CheckMate_BIMPortal\\packages\\mcp\\bin\\checkmate-mcp.mjs"],
      "env": {
        "CHECKMATE_MAPPING": "default"
      }
    }
  }
}
```

3. **Quit Claude Desktop completely** (including from the system tray / menu bar) and relaunch it. `checkmate` will appear as connected under **Settings → Developer**.

### Optional environment variables in `env`

| Variable | Effect |
|---|---|
| `CHECKMATE_MODEL` | Path or URL of an IFC file loaded by default on startup (defaults to `bruecke-tbw1-mit-merkmalen.ifc`). |
| `CHECKMATE_MAPPING` | `default` (`bim-hamburg.json`), `none` (strict IDS), or path to a custom `.json` mapping file. |
| `BIM_PORTAL_USERNAME` / `BIM_PORTAL_PASSWORD` | Optional credentials for non-public BIM Portal content. |

### Sample prompts to try

- *"Load `resources/models/mini-infrastruktur-bruecke.ifc` and check it without mapping against LOIN `be959b9d-daef-5315-9aba-f60b6ad54ccb`."* → Opens the interactive 3D report view with green bridge copings.
- *"Which BIM Portal LOINs fit the loaded bridge model for Ingenieurbau in LPH 8?"* → Opens the interactive LOIN selector (`ui://checkmate/loins`).
- *"Show and explain the element report for `Kappe_Mitte_BW`."*

---

## 3. Claude Code & MCP Inspector

```bash
# Register and verify in Claude Code
claude mcp add checkmate -- node /path/to/CheckMate_BIMPortal/packages/mcp/bin/checkmate-mcp.mjs
claude mcp list

# Test interactively in the browser with the official MCP Inspector
npx @modelcontextprotocol/inspector node packages/mcp/bin/checkmate-mcp.mjs
```

---

## Alternative: Java 25 Edition (`0.3.3-stable`, `Latest`)

For the pure Java 25 MCP server (`installMcp` / `installAntigravityPlugin` via Gradle), see the guide in the current stable release: [Set Up a Client (0.3.3-stable)](/CheckMate_BIMPortal/en/mcp/clients/).
