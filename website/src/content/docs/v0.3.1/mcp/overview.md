---
title: MCP-Schnittstelle
description: Der CheckMate-MCP-Server und seine Werkzeuge in Version 0.3.1.
slug: v0.3.1/mcp/overview
---

Über das [Model Context Protocol](https://modelcontextprotocol.io/) kann eine kompatible Anwendung CheckMate als Werkzeug nutzen. Der Server in `0.3.1` läuft lokal über `stdio`. Die Prüfung erfolgt im CheckMate-Kern; ein angeschlossenes Sprachmodell kann Ergebnisse aufbereiten, entscheidet aber nicht über die Regeln.

| Werkzeug | Funktion |
|---|---|
| `load_model` | IFC-Datei oder URL laden und das aktuelle Modell ersetzen |
| `describe_model` | Modell und enthaltene IFC-Typen beschreiben |
| `list_elements` | Elemente nach Typ oder Name auflisten |
| `find_loins` | LOINs und AIA-Projekte im Portal suchen |
| `describe_requirements` | Anforderungen einer LOIN oder eines AIA-Projekts beschreiben |
| `check_model` | Modell gegen Portal- oder lokale IDS-Anforderungen prüfen |
| `element_report` | Ergebnisse und Merkmale eines Elements anzeigen |

Die Werkzeuge liefern Modellinformationen, Merkmale und Prüfberichte an den aufrufenden Client. Je nach Client und Modellanbieter können diese Informationen weiterverarbeitet oder extern übertragen werden. Der Server selbst schützt nicht vor einer solchen Weitergabe; vor der Nutzung sind Datenschutz und Vertraulichkeit zu prüfen. Für Portalabfragen ist Netzwerkzugriff erforderlich.

Die Antworten können bei großer Ausgabe gekürzt werden. Nicht ausgewertete Anforderungen und die Grenzen der Prüfung stehen unter [IFC und IDS](/CheckMate_BIMPortal/v0.3.1/architecture/ifc-ids-checking/). Die [Einrichtung](/CheckMate_BIMPortal/v0.3.1/mcp/clients/) beschreibt den Start des Servers und ein Client-Beispiel.
