---
title: MCP-Client einrichten
description: CheckMate als lokalen MCP-Server für kompatible Clients einrichten.
---

## Server installieren

Die [Voraussetzungen für 0.3.1](/CheckMate_BIMPortal/guides/quickstart/) gelten auch für den MCP-Server: JDK 21 mit JavaFX und die APSTEX-JARs werden zum Bauen benötigt.

```bash
cd bim-portal/Java_client
./gradlew :hackathon-template:installMcp
```

Unter Windows `.\gradlew.bat` verwenden. Das Startskript liegt anschließend unter `hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp` beziehungsweise `checkmate-mcp.bat` auf Windows. Nach Codeänderungen `installMcp` erneut ausführen.

## Beispiel: Claude Desktop unter Windows

In Claude Desktop unter **Settings → Developer → Edit Config** einen Eintrag in `claude_desktop_config.json` ergänzen. Die Pfade durch lokale absolute Pfade ersetzen:

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "cmd",
      "args": ["/c", "C:\\pfad\\zu\\checkmate-mcp.bat"],
      "env": {
        "JAVA_HOME": "C:\\pfad\\zu\\jdk-21-fx"
      }
    }
  }
}
```

Sind bereits Server eingetragen, nur den `checkmate`-Eintrag in das vorhandene `mcpServers`-Objekt aufnehmen. Claude Desktop vollständig beenden und neu starten. Optional kann `CHECKMATE_IFC` in `env` ein Modell vorgeben.

Claude Code kann den Server mit `claude mcp add checkmate -- cmd /c "C:\pfad\zu\checkmate-mcp.bat"` registrieren; `claude mcp list` zeigt die Konfiguration. Weitere Hinweise stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/WORKFLOW.md).

Andere Clients können grundsätzlich verwendet werden, wenn sie lokale MCP-Server über `stdio` unterstützen. Einrichtung, Berechtigungen und Datenverarbeitung hängen vom jeweiligen Client und Anbieter ab; die Kompatibilität ist im Einzelfall zu prüfen.
