---
title: MCP-Client einrichten
description: CheckMate als lokalen MCP-Server für kompatible Clients einrichten (0.3.3-stable).
---

## Server installieren

Die [Voraussetzungen für 0.3.3-stable](/CheckMate_BIMPortal/guides/quickstart/) gelten auch für den MCP-Server: **JDK 25** wird benötigt.

```bash
cd bim-portal/Java_client
./gradlew :hackathon-template:installMcp
```

Unter Windows `.\gradlew.bat` verwenden. Das Startskript liegt anschließend unter `hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp` beziehungsweise `checkmate-mcp.bat` auf Windows. Nach Codeänderungen `installMcp` erneut ausführen.

## Unterstützte Clients

### 1. Antigravity

Antigravity unterstützt CheckMate nativ über ein dediziertes Plugin:
```bash
./gradlew :hackathon-template:installAntigravityPlugin
agy plugin validate --strict
agy plugin install checkmate
```

### 2. MCP Inspector

Für Entwicklung und Tests kann der MCP Inspector gestartet werden:
```bash
npx @modelcontextprotocol/inspector bim-portal/Java_client/hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp
```

### 3. Claude Desktop und Claude Code

In Claude Desktop unter **Settings → Developer → Edit Config** einen Eintrag in `claude_desktop_config.json` ergänzen:

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "cmd",
      "args": ["/c", "C:\\pfad\\zu\\checkmate-mcp.bat"],
      "env": {
        "JAVA_HOME": "C:\\pfad\\zu\\jdk-25"
      }
    }
  }
}
```

Claude Code kann den Server mit `claude mcp add checkmate -- cmd /c "C:\pfad\zu\checkmate-mcp.bat"` registrieren; `claude mcp list` zeigt die Konfiguration. Weitere Hinweise stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/WORKFLOW.md).

Andere Clients können grundsätzlich verwendet werden, wenn sie lokale MCP-Server über `stdio` unterstützen.
