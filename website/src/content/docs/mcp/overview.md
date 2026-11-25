---
title: MCP-Schnittstelle
description: Der CheckMate-MCP-Server und seine Werkzeuge in Version 0.3.2.
---

Über das [Model Context Protocol](https://modelcontextprotocol.io/) kann eine kompatible Anwendung CheckMate als Werkzeug nutzen. Der Server in `0.3.2` läuft lokal über `stdio`. Die Prüfung erfolgt im CheckMate-Kern; ein angeschlossenes Sprachmodell kann Ergebnisse aufbereiten, entscheidet aber nicht über die Regeln.

| Werkzeug | Funktion |
|---|---|
| `load_model` | IFC-Datei oder URL laden und das aktuelle Modell ersetzen |
| `describe_model` | Modell und enthaltene IFC-Typen beschreiben |
| `list_elements` | Elemente nach Typ oder Name auflisten |
| `find_loins` | LOINs und AIA-Projekte im Portal suchen |
| `describe_requirements` | Anforderungen einer LOIN oder eines AIA-Projekts beschreiben |
| `check_model` | Modell gegen Portal- oder lokale IDS-Anforderungen prüfen |
| `element_report` | Ergebnisse und Merkmale eines Elements anzeigen |
| `load_mapping` | Aktives Mapping anzeigen oder wechseln; ein Wechsel verwirft den letzten Prüfbericht |
| `server_status` | Aktuelles Modell, Mapping und letztes Prüfergebnis anzeigen |

Für `check_model` gibt es eine erste MCP-App-Berichtansicht mit strukturierter Ausgabe und einem Werkzeug zum Nachladen weiterer Befunde. Die Ansicht ist auf Protokollebene getestet, aber noch nicht in Claude Desktop oder Antigravity visuell geprüft. Clients ohne passende Unterstützung erhalten weiterhin eine Textantwort.

Die Werkzeuge liefern Modellinformationen, Merkmale und Prüfberichte an den aufrufenden Client. Je nach Client und Modellanbieter können diese Informationen weiterverarbeitet oder extern übertragen werden. Der Server selbst schützt nicht vor einer solchen Weitergabe; vor der Nutzung sind Datenschutz und Vertraulichkeit zu prüfen. Für Portalabfragen ist Netzwerkzugriff erforderlich.

Beim Weg über MCP stellt der Client das Sprachmodell; CheckMate selbst spricht mit keinem Modell. Auf Branch `0.3.3` kann alternativ der [Chat in der Oberfläche](/CheckMate_BIMPortal/guides/chat/) eine Prüfung über ein Modell auslösen, das CheckMate mit einem eigenen Schlüssel aufruft. Beide Wege nutzen denselben Prüfkern; sie unterscheiden sich darin, wer das Modell stellt und bezahlt, und darin, dass der Chat bislang nur `check_model` kennt.

Die Antworten können bei großer Ausgabe gekürzt werden. Nicht ausgewertete Anforderungen und die Grenzen der Prüfung stehen unter [IFC und IDS](/CheckMate_BIMPortal/architecture/ifc-ids-checking/). Die [Einrichtung](/CheckMate_BIMPortal/mcp/clients/) beschreibt den Start des Servers und ein Client-Beispiel.
