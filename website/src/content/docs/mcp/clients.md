---
title: MCP-Client einrichten (Antigravity & Claude Desktop)
description: Schritt-für-Schritt-Einrichtung des CheckMate-MCP-Servers und der 3D MCP Apps in Antigravity, Claude Desktop und Claude Code.
---

Nach einmaligem `pnpm install` im geklonten Repository lässt sich der MCP-Server von `0.4.1-beta` über das eigenständige Startskript **`packages/mcp/bin/checkmate-mcp.mjs`** mit einem einzigen Argument starten – ganz ohne `--import tsx`-Flags oder komplizierte Wrapper. Eine Vorlage liegt unter [`mcp_config.json`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/mcp_config.json).

---

## 1. Antigravity (inkl. 3D MCP Apps)

Antigravity unterstützt sowohl direkte `mcp_config.json`-Einträge als auch gepackte lokale Plugins und rendert die interaktiven 3D-Ansichten (`ui://checkmate/report`, `ui://checkmate/model`, `ui://checkmate/loins`) direkt im Chat.

### Variante A: Direkt über MCP-Konfiguration (`mcp_config.json`)

In Antigravity unter **Customizations / MCP Servers** folgenden Eintrag mit absolutem Pfad ergänzen:

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "node",
      "args": ["/pfad/zu/CheckMate_BIMPortal/packages/mcp/bin/checkmate-mcp.mjs"],
      "env": {
        "CHECKMATE_MAPPING": "default"
      }
    }
  }
}
```

*(Unter Windows z. B. `"C:\\Users\\Name\\CheckMate_BIMPortal\\packages\\mcp\\bin\\checkmate-mcp.mjs"`.)*

### Variante B: Als lokales Antigravity-Plugin installieren

Ein fertiges Plugin-Verzeichnis mit `plugin.json`, Logo und `mcp_config.json` lässt sich per Skript erzeugen und mit der Antigravity-CLI (`agy`) validieren und installieren:

```bash
pnpm mcp:antigravity-plugin
agy plugin validate build/checkmate-antigravity-plugin --strict
agy plugin install build/checkmate-antigravity-plugin
```

---

## 2. Claude Desktop (Windows & macOS, inkl. 3D MCP Apps)

1. In Claude Desktop **Settings → Developer → Edit Config** öffnen. Dadurch öffnet sich:
   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
2. Den `checkmate`-Eintrag mit absolutem Pfad zu `checkmate-mcp.mjs` einfügen:

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

3. **Claude Desktop vollständig beenden** (auch über das Tray-/Menüleisten-Symbol) und neu starten. Unter **Settings → Developer** erscheint `checkmate` anschließend als verbunden.

### Optionale Umgebungsvariablen in `env`

| Variable | Wirkung |
|---|---|
| `CHECKMATE_MODEL` | Pfad oder URL einer IFC-Datei, die beim Start standardmäßig geladen wird (sonst `bruecke-tbw1-mit-merkmalen.ifc`). |
| `CHECKMATE_MAPPING` | `default` (`bim-hamburg.json`), `none` (strikt IDS) oder Pfad zu einer eigenen `.json`-Datei. |
| `BIM_PORTAL_USERNAME` / `BIM_PORTAL_PASSWORD` | Optional für nicht-öffentliche Inhalte im BIM-Portal. |

### Beispiel-Fragen im Chat zum Ausprobieren

- *„Lade `resources/models/mini-infrastruktur-bruecke.ifc` und prüfe das Modell ohne Mapping gegen die LOIN `be959b9d-daef-5315-9aba-f60b6ad54ccb`.“* → Öffnet die 3D-Prüfansicht mit grün markierten Brückenkappen.
- *„Welche LOINs im BIM-Portal passen für Ingenieurbau in LPH 8 zum geladenen Brückenmodell?“* → Öffnet die interaktive LOIN-Auswahl (`ui://checkmate/loins`).
- *„Erkläre mir den Elementbericht für `Kappe_Mitte_BW`.“*

---

## 3. Claude Code & MCP Inspector

```bash
# In Claude Code registrieren und prüfen
claude mcp add checkmate -- node /pfad/zu/CheckMate_BIMPortal/packages/mcp/bin/checkmate-mcp.mjs
claude mcp list

# Interaktiv im Browser mit dem offiziellen MCP Inspector testen
npx @modelcontextprotocol/inspector node packages/mcp/bin/checkmate-mcp.mjs
```

---

## Alternative: Java-25-Edition (`0.3.3-stable`, `Latest`)

Für den reinen Java-25-MCP-Server (`installMcp` / `installAntigravityPlugin` über Gradle) siehe die Anleitung im aktuellen Haupt-Release: [MCP-Client einrichten (0.3.3-stable)](/CheckMate_BIMPortal/mcp/clients/).
