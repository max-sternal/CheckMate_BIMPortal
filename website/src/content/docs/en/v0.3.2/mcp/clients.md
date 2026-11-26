---
editUrl: false
title: Set up an MCP client
description: Set up CheckMate as a local MCP server for compatible clients.
slug: en/v0.3.2/mcp/clients
---

## Install the server

The [0.3.2 prerequisites](/CheckMate_BIMPortal/en/v0.3.2/guides/quickstart/) also apply to the MCP server: JDK 21 is sufficient; JavaFX and APSTEX are not needed.

```bash
cd bim-portal/Java_client
./gradlew :hackathon-template:installMcp
```

On Windows, use `.\gradlew.bat`. The launcher is created at `hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp`, or `checkmate-mcp.bat` on Windows. Run `installMcp` again after code changes.

## Example: Claude Desktop on Windows

In Claude Desktop, open **Settings → Developer → Edit Config** and add an entry to `claude_desktop_config.json`. Replace the paths with local absolute paths:

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "cmd",
      "args": ["/c", "C:\\path\\to\\checkmate-mcp.bat"],
      "env": {
        "JAVA_HOME": "C:\\path\\to\\jdk-21"
      }
    }
  }
}
```

If servers are already configured, add only the `checkmate` entry to the existing `mcpServers` object. Quit Claude Desktop completely and restart it. Optionally, use `CHECKMATE_IFC` in `env` to select a model.

Claude Code can register the server with `claude mcp add checkmate -- cmd /c "C:\path\to\checkmate-mcp.bat"`; `claude mcp list` shows the configuration. See [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/docs/WORKFLOW.md) for more details.

Other clients may be used if they support local MCP servers over `stdio`. Setup, permissions and data handling depend on the client and provider; compatibility must be checked for each setup.
