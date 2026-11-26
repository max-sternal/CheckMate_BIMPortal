---
title: MCP-Schnittstelle
description: Der CheckMate-MCP-Server und seine Werkzeuge in Version 0.3.3-stable.
---

CheckMate lässt sich auf zwei Wegen mit einem Sprachmodell verbinden. Über das [Model Context Protocol](https://modelcontextprotocol.io/) nutzt eine KI-Anwendung CheckMate als Werkzeug. Der [Chat in der Oberfläche](/CheckMate_BIMPortal/guides/chat/) geht den umgekehrten Weg: CheckMate ruft selbst ein Modell mit eigenem Schlüssel auf. Beide Wege nutzen denselben Prüfkern.

Diese Seite beschreibt den MCP-Weg. Der Server in `0.3.3-stable` läuft lokal über `stdio`.

## Modell-gestützte Werkzeuge (11 Tools)

| Werkzeug | Funktion |
|---|---|
| `load_model` | IFC-Datei oder URL laden und das aktuelle Modell ersetzen |
| `describe_model` | Modell und enthaltene IFC-Typen beschreiben |
| `list_elements` | Elemente nach Typ oder Name auflisten |
| `find_loins` | LOINs und AIA-Projekte im Portal suchen |
| `describe_requirements` | Anforderungen einer LOIN oder eines AIA-Projekts beschreiben |
| `find_properties` | Merkmale und Merkmalsgruppen im BIM-Portal durchsuchen |
| `describe_property` | Details und Wertebereiche eines BIM-Portal-Merkmals anzeigen |
| `check_model` | Modell gegen Portal- oder lokale IDS-Anforderungen prüfen |
| `element_report` | Ergebnisse und Merkmale eines Elements anzeigen |
| `load_mapping` | Aktives Mapping anzeigen oder wechseln |
| `server_status` | Aktuelles Modell, Mapping und letztes Prüfergebnis anzeigen |

## App-Only Werkzeuge

- `app_report_page`: Liefert strukturierte Berichtsdaten für interaktive Oberflächen.

## MCP App Views (3 Ansichten)

1. `ui://checkmate/report`: Interaktiver Prüfbericht mit Filter- und Detailansichten.
2. `ui://checkmate/loins`: Gefilterte LOIN- und AIA-Katalogübersicht.
3. `ui://checkmate/model`: IFC-Modellstruktur und Elemente-Browser.

## JSON-Ressourcen (2 Ressourcen)

- `checkmate://model/current`: Aktuelle Modellmetadaten und IFC-Typen als JSON.
- `checkmate://report/last.json`: Vollständiger letzter Prüfbericht im JSON-Format.

## MCP Prompts (2 Prompts)

- `check-loin`: Geführter Workflow zur Prüfung eines Modells gegen eine Portal-LOIN.
- `explain-element`: Hilfestellung zur Interpretation von Elementmerkmalen und Fehlern.

Die Werkzeuge und Ansichten unterstützen Entwickler und KI-Clients bei der automatisierten BIM-Prüfung. Details stehen unter [IFC und IDS](/CheckMate_BIMPortal/architecture/ifc-ids-checking/) und in der [Client-Anleitung](/CheckMate_BIMPortal/mcp/clients/).
