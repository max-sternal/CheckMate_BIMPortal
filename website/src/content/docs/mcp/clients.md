---
title: Clients anbinden (Claude, OpenCode, Ollama & Strata)
description: Konkrete Einrichtung des CheckMate-MCP-Servers für Claude Desktop, Claude Code, OpenCode, Ollama (Mistral Cloud / lokales Qwen) und Strata.
---

CheckMate setzt bewusst auf **offene Standards**, damit Behörden, Büros und Entwickelnde frei wählen können, welches KI-Werkzeug und welches Sprachmodell (Cloud oder vollständig lokal) eingesetzt wird.

## Vorbereitung: MCP-Server bauen (`0.3.1`)

Im Verzeichnis `bim-portal/Java_client/` (benötigt in `0.3.1` noch die APSTEX-JARs in `hackathon-template/lib/` sowie Java 21):

```bash
./gradlew :hackathon-template:installMcp
```

Erzeugt das eigenständige Startskript unter:
- **Linux / macOS:** `bim-portal/Java_client/hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp`
- **Windows:** `bim-portal\Java_client\hackathon-template\build\install\checkmate-mcp\bin\checkmate-mcp.bat`

---

## 1. Claude Desktop & Claude Code *(getestet in `0.3.1`)*

### Claude Code
Im Projekt-Root ausführen:

```bash
# Linux / macOS
claude mcp add checkmate -- "/absoluter/pfad/zu/checkmate-mcp"

# Windows
claude mcp add checkmate -- cmd /c "C:\pfad\zu\checkmate-mcp.bat"
```

Prüfen mit `claude mcp list`.

### Claude Desktop
Über *Settings → Developer → Edit Config* (`claude_desktop_config.json`) ergänzen (unter Windows `JAVA_HOME` explizit in `env` setzen, da Claude Desktop diese Variable nicht immer weiterreicht):

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "cmd",
      "args": ["/c", "C:\\pfad\\zu\\checkmate-mcp.bat"],
      "env": {
        "JAVA_HOME": "C:\\pfad\\zu\\jdk-21",
        "CHECKMATE_IFC": "C:\\pfad\\zu\\modell.ifc"
      }
    }
  }
}
```

---

## 2. OpenCode + Ollama (Offene Cloud-Modelle & Lokal)

Mit [**OpenCode**](https://opencode.ai/) lässt sich CheckMate vollständig Open Source sowohl mit einem **offenen Cloud-Modell** (z. B. **Mistral über Ollama Cloud**) als auch mit einem **lokalen Qwen-Modell über Ollama** verbinden.

### CheckMate als MCP-Server in `~/.config/opencode/opencode.json` eintragen

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "checkmate": {
      "type": "local",
      "command": ["/absoluter/pfad/zu/checkmate-mcp"],
      "environment": {
        "JAVA_HOME": "/pfad/zu/jdk-21"
      },
      "enabled": true
    }
  }
}
```

### Variante A: Offenes Cloud-Modell (z. B. Mistral über Ollama Cloud)
Ideal auf Arbeitsplatz-Laptops ohne dedizierte GPU:
1. Ollama-Cloud-API-Key anlegen und in OpenCode als Provider **Ollama Cloud** auswählen (oder per `ollama launch opencode`).
2. Ein Tool-Calling-fähiges offenes Modell wie **Mistral** auswählen und direkt fragen:
   *„Beschreibe das geladene IFC-Modell und suche passende LOINs für LPH 5 im Ingenieurbau.“*

### Variante B: Vollständig lokal mit Ollama + Qwen
Für maximale Datensouveränität (keine Daten verlassen den Rechner):
1. Tool-Calling-fähiges Qwen-Modell lokal laden (z. B. `qwen3.6` / `qwen3.8` je nach VRAM/RAM) und Kontextfenster ausreichend hoch setzen (`num_ctx` ≥ 32K–64K).
2. Mit `ollama launch opencode` starten — oder alternativ über einen leichten MCP-Host wie `ollmcp` (`pip install mcp-client-for-ollama`).

:::tip[Praxis-Tipp für kleinere lokale Modelle]
Bei kleineren lokalen Kontextfenstern den Parameter `examples` bei `check_model` klein halten (Standard ist `3`) und für Details gezielt `element_report` nutzen. Mit **`0.3.2`** (`structuredContent`) wird die Tool-Genauigkeit kleiner Modelle noch weiter verbessert.
:::

---

## 3. Lokal auf Consumer-Hardware mit Strata + Qwen

Für große Mixture-of-Experts-Modelle (wie **Qwen 3.8-Flash-Next 125B MoE**) auf regulärer Workstation- oder Gaming-Hardware (z. B. 12–16 GB VRAM + 32–64 GB RAM + NVMe-SSD) bietet sich [**Strata**](https://github.com/Niko1221/Strata) als spezialisierte Open-Source-Inference-Engine an:
- Strata verteilt aktive Experten intelligent über GPU-VRAM, RAM und SSD und stellt eine **lokale OpenAI- und Anthropic-kompatible API** inklusive Tool-Calling bereit.
- OpenCode (oder andere MCP-Clients) können direkt auf den lokalen Strata-Endpunkt zeigen und CheckMate über MCP vollständig offline ansteuern.

---

## 4. Antigravity (Ziel: Native MCP App)

Sobald der Ausbau auf **Streamable HTTP** und die **MCP Apps Extension (`ui://`)** abgeschlossen ist (`0.3.2` / `0.4` *CheckMate Lite*), lässt sich CheckMate direkt in **Antigravity** einbinden — inklusive interaktiver Prüftabelle und (in `0.4`) eingebettetem 3D-IFC-Viewer direkt in der Konversation.
