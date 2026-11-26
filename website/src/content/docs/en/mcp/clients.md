---
title: Set up an MCP client
description: Set up CheckMate as a local MCP server for compatible clients (0.3.3-stable).
---

## Install the server

The [0.3.3-stable prerequisites](/CheckMate_BIMPortal/en/guides/quickstart/) also apply to the MCP server: **JDK 25** is required.

```bash
cd bim-portal/Java_client
./gradlew :hackathon-template:installMcp
```

On Windows, use `.\gradlew.bat`. The launcher is created at `hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp`, or `checkmate-mcp.bat` on Windows. Run `installMcp` again after code changes.

## Supported Clients

### 1. Antigravity

Antigravity natively supports CheckMate via a dedicated plugin:
```bash
./gradlew :hackathon-template:installAntigravityPlugin
agy plugin validate --strict
agy plugin install checkmate
```

### 2. MCP Inspector

For development and testing, you can launch the MCP Inspector:
```bash
npx @modelcontextprotocol/inspector bim-portal/Java_client/hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp
```

### 3. Claude Desktop and Claude Code

In Claude Desktop, open **Settings → Developer → Edit Config** and add an entry to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "cmd",
      "args": ["/c", "C:\\path\\to\\checkmate-mcp.bat"],
      "env": {
        "JAVA_HOME": "C:\\path\\to\\jdk-25"
      }
    }
  }
}
```

Claude Code can register the server with `claude mcp add checkmate -- cmd /c "C:\path\to\checkmate-mcp.bat"`; `claude mcp list` shows the configuration. See [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/WORKFLOW.md) for more details.

Other clients may be used if they support local MCP servers over `stdio`.
