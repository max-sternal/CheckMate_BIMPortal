---
title: MCP-Server & 3D MCP Apps
description: Der CheckMate-MCP-Server, seine 15 Werkzeuge und die interaktiven 3D MCP-App-Ansichten in Version 0.4.
---

Über das [Model Context Protocol](https://modelcontextprotocol.io/) kann eine kompatible KI-Anwendung (z. B. Claude Desktop, Claude Code oder Antigravity) CheckMate als Werkzeug nutzen. Der Server (`@checkmate/mcp`) läuft lokal über `stdio`. Die regelbasierte Prüfung erfolgt im CheckMate-Kern (`@checkmate/core`); das Sprachmodell ruft die Werkzeuge auf und erläutert die Ergebnisse.

## 10 Modell-Werkzeuge (für KI & Nutzer)

| Werkzeug | Funktion |
|---|---|
| `load_model` | IFC-Datei (`.ifc`, `.ifczip`) oder HTTP(S)-/GitHub-Link laden (`ui://checkmate/model`) |
| `describe_model` | Aktuelles Modell, IFC-Schema, Bauteilanzahl und häufigste IFC-Entities beschreiben (`ui://checkmate/model`) |
| `list_elements` | Bauteile als `GlobalId \| Entity \| Name \| PredefinedType` nach Entity oder Namensteil filtern |
| `find_loins` | LOINs und AIA-Projekte im BIM-Portal suchen, filtern und nach `ModelFit` sortieren (`ui://checkmate/loins`) |
| `describe_requirements` | Über `LoinDescription` detailliert zeigen, was eine LOIN auswählt (`Wählt aus`), verlangt (`Verlangt`) und wie viele Elemente im Modell zutreffen (`Im Modell`) |
| `check_model` | Modell gegen Portal-IDs, lokale IDS-Dateien oder `.rules.json` prüfen (`ui://checkmate/report`) |
| `element_report` | Prüfergebnisse und alle IFC-Merkmale eines bestimmten Bauteils (per GlobalId oder Namensteil) ausgeben |
| `load_mapping` | Aktives JSON-Mapping anzeigen oder wechseln (`default`, `none` oder Dateipfad) |
| `export_bcf` | Abweichungen der letzten Prüfung als BCF-2.1-Archiv (`.bcfzip`) speichern |
| `server_status` | Aktuelles Modell, aktives Mapping und Ergebnis der letzten Prüfung anzeigen |

## 3 Interaktive 3D MCP-App-Ansichten & 5 App-only-Werkzeuge

Hosts, die die MCP-Apps-Erweiterung (`io.modelcontextprotocol/ui`) unterstützen, rendern direkt im Chat interaktive HTML/WebGL-Ansichten:

- `ui://checkmate/report`: 3D-Prüfbericht mit rot/grün gefärbten Bauteilen, Fast-Treffer-Diagnose und Bauteil-Fokus.
- `ui://checkmate/model`: 3D-Modellübersicht mit klickbarem Entity-Histogramm und Elementtabelle.
- `ui://checkmate/loins`: Interaktiver BIM-Portal-Katalog mit Filtern für Fachmodell, Leistungsphase und Anwendungsfall.

Für das Nachladen von Daten und 3D-Geometrie innerhalb der Ansichten stellt der Server 5 zusätzliche App-only-Werkzeuge (`visibility: ["app"]`) bereit: `app_report_page`, `app_element_page`, `app_set_filter`, `app_check_selected` und `app_element_mesh`. Clients ohne MCP-Apps-Unterstützung erhalten weiterhin die vollständige deutsche Textantwort.
