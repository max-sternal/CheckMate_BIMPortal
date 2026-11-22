---
title: Set up an MCP client
description: Configure CheckMate 0.4 (TypeScript/Node.js) or 0.3.2 (Java) as a local MCP server.
---

## Option A: CheckMate 0.4 (TypeScript & IFClite 3D MCP Apps)

After running `pnpm install` in the repository root, the MCP server can be launched directly with Node.js (`--import tsx`). A ready-to-use template is provided in [`mcp_config.json`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4/mcp_config.json).

### Claude Desktop / Antigravity (`claude_desktop_config.json` / `mcp_config.json`)

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "node",
      "args": [
        "--import",
        "tsx",
        "C:\\Users\\Max\\Documents\\GitHub\\CheckMate_BIMPortal\\packages\\mcp\\src\\index.ts"
      ],
      "env": {
        "CHECKMATE_MAPPING": "default"
      }
    }
  }
}
```

*(On Linux/macOS, use `/path/to/CheckMate_BIMPortal/packages/mcp/src/index.ts`. Optionally set `CHECKMATE_MODEL` in `env` to specify a custom default IFC file.)*

### Claude Code

```bash
claude mcp add checkmate -- node --import tsx /path/to/CheckMate_BIMPortal/packages/mcp/src/index.ts
```

---

## Option B: CheckMate 0.3.2 (Pure Java edition)

For the Java edition (`0.3.2`) in `bim-portal/Java_client/`, build the launcher script via Gradle:

```bash
cd bim-portal/Java_client
./gradlew :hackathon-template:installMcp
```

The launcher is created at `hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp` (or `checkmate-mcp.bat` on Windows); see the [0.3.2 MCP setup guide](/CheckMate_BIMPortal/en/v0.3.2/mcp/clients/) for details.
