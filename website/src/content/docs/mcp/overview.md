---
title: MCP-Server & 3D MCP Apps
description: Der übersichtliche CheckMate-MCP-Server (0.4.1-beta) mit 10 Werkzeugen, 3 interaktiven 3D MCP Apps, JSON-Ressourcen und Prompts.
---

Über das [Model Context Protocol](https://modelcontextprotocol.io/) kann eine KI-Anwendung wie **Antigravity**, **Claude Desktop** oder **Claude Code** CheckMate direkt als lokales Werkzeug nutzen. Der Server (`@checkmate/mcp`) kommuniziert lokal über `stdio`. Die regelbasierte Prüfung erfolgt vollständig deterministisch in `@checkmate/core`; das Sprachmodell ruft die Werkzeuge auf und erläutert die deutschen Berichte.

## Übersichtlicher Aufbau (`packages/mcp/`)

Um den Server leicht verständlich und wartbar zu halten, ist `@checkmate/mcp` in drei kleine Dateien gegliedert:
- `bin/checkmate-mcp.mjs`: Eigenständiges Node-Startskript (schützt `stdout` für JSON-RPC und startet direkt ohne zusätzliche Flags).
- `src/catalog.ts`: Deklarative Definitionen aller Werkzeuge, 3D-Ansichten, JSON-Ressourcen und Prompts.
- `src/tools.ts` & `src/index.ts`: Zustandsverwaltung der Sitzung (`CheckMateTools`) und schlanker MCP-Request-Router.

## 10 Modell-Werkzeuge (für KI & Nutzer:innen)

| Werkzeug | Funktion | MCP-App-Ansicht |
|---|---|---|
| `load_model` | IFC-Datei (`.ifc`, `.ifczip`) oder HTTP(S)-/GitHub-Link laden | `ui://checkmate/model` |
| `describe_model` | Aktuelles Modell, IFC-Schema, Bauteilanzahl und häufigste Entities zeigen | `ui://checkmate/model` |
| `list_elements` | Bauteile als `GlobalId \| Entity \| Name \| PredefinedType` filtern | — |
| `find_loins` | LOINs und AIA-Projekte im BIM-Portal suchen, filtern und nach `ModelFit` sortieren | `ui://checkmate/loins` |
| `describe_requirements` | Über `LoinDescription` zeigen, was eine LOIN auswählt (`Wählt aus`), verlangt (`Verlangt`) und wie viele Modellbauteile passen | — |
| `check_model` | Modell gegen Portal-IDs, lokale `.ids`/`.zip` oder `.rules.json` prüfen | `ui://checkmate/report` |
| `element_report` | Prüfbefunde und alle IFC-Merkmale eines Bauteils (per GlobalId oder Namensteil) ausgeben | — |
| `load_mapping` | Aktives JSON-Mapping anzeigen oder wechseln (`default`, `none` oder Dateipfad) | — |
| `export_bcf` | Abweichungen der letzten Prüfung als BCF-2.1-Archiv (`.bcfzip`) speichern | — |
| `server_status` | Aktives Modell, aktives Mapping und Ergebnis der letzten Prüfung anzeigen | — |

## 3 Interaktive 3D MCP-App-Ansichten (`io.modelcontextprotocol/ui`)

Clients mit MCP-Apps-Unterstützung (wie **Antigravity** und **Claude Desktop**) rendern direkt im Chatverlauf interaktive HTML/WebGL-Ansichten:

- `ui://checkmate/report`: Interaktiver 3D-Prüfbericht mit rot/grün markierten Bauteilen, Fast-Treffer-Diagnose und Bauteil-Fokus.
- `ui://checkmate/model`: 3D-Modellübersicht mit klickbarem Entity-Histogramm und Elementtabelle.
- `ui://checkmate/loins`: Interaktiver BIM-Portal-Katalog mit Filtern für Fachmodell, Leistungsphase und Anwendungsfall.

Für das Nachladen von Seiten und 3D-Dreiecksnetzen (`@ifc-lite/geometry`) innerhalb dieser Ansichten stellt der Server 5 interne App-only-Werkzeuge (`visibility: ["app"]`) bereit: `app_report_page`, `app_element_page`, `app_set_filter`, `app_check_selected` und `app_element_mesh`. Clients ohne UI-Erweiterung erhalten automatisch den vollständigen deutschen Textbericht.

## JSON-Ressourcen & geführte Prompts

- **JSON-Ressourcen**: `checkmate://model/current` (Metadaten des aktiven IFC-Modells) und `checkmate://report/last.json` (vollständiger letzter Prüfbericht als JSON).
- **Geführte Prompts**: `check-loin` (optional `discipline`, `phase`) für einen geführten Such- und Prüfdurchlauf sowie `explain-element` (`element`) zur verständlichen Einzelerklärung eines Bauteils.
