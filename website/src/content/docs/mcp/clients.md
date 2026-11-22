---
title: MCP-Client einrichten
description: CheckMate 0.4 (TypeScript/Node.js) oder 0.3.2 (Java) als lokalen MCP-Server einrichten.
---

## Option A: CheckMate 0.4 (TypeScript & IFClite 3D MCP Apps)

Nach `pnpm install` im Wurzelverzeichnis des Repositories kann der MCP-Server direkt über Node.js (`--import tsx`) gestartet werden. Eine fertige Vorlage liegt in [`mcp_config.json`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4/mcp_config.json).

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

*(Unter Linux/macOS den Pfad entsprechend als `/pfad/zu/CheckMate_BIMPortal/packages/mcp/src/index.ts` eintragen. Optional kann über `CHECKMATE_MODEL` eine andere Standard-IFC-Datei gesetzt werden.)*

### Claude Code

```bash
claude mcp add checkmate -- node --import tsx /pfad/zu/CheckMate_BIMPortal/packages/mcp/src/index.ts
```

---

## Option B: CheckMate 0.3.2 (Reine Java-Edition)

Für die Java-Edition (`0.3.2`) im Verzeichnis `bim-portal/Java_client/` wird das Startskript per Gradle erzeugt:

```bash
cd bim-portal/Java_client
./gradlew :hackathon-template:installMcp
```

Das Skript liegt danach unter `hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp` (bzw. `checkmate-mcp.bat` unter Windows); Details stehen in der [0.3.2-MCP-Anleitung](/CheckMate_BIMPortal/v0.3.2/mcp/clients/).
