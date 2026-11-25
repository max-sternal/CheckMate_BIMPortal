---
title: Systemarchitektur
description: Komponenten und Schnittstellen von CheckMate 0.3.2.
---

## Datenfluss

CheckMate verarbeitet ein IFC-Modell und Anforderungen aus dem BIM-Portal oder einer lokalen IDS-Datei. Der gemeinsame Prüfkern erstellt einen Bericht für die Oberfläche, die Kommandozeile oder den MCP-Server.

```text
IFC-Datei / URL -- Java-IFC-Reader -- ModelElement -+
                                                     +-- CheckService -- CheckReport
BIM-Portal / lokale IDS-Datei ---- IDS-Parser -------+       |             |
                                                         GUI / CLI / MCP
```

Für die Prüfung und den Bericht ist kein KI-Modell erforderlich. Der MCP-Server ist ein optionaler Zugang zu denselben Funktionen; der [Chat in der Oberfläche](/CheckMate_BIMPortal/guides/chat/) auf Branch `0.3.3` ist ein weiterer Aufrufer desselben Prüfkerns.

## Komponenten

Der CheckMate-Code liegt im Gradle-Modul `bim-portal/Java_client/hackathon-template/`.

| Paket | Aufgabe |
|---|---|
| `com.bimportal.hackathon.ids` | IDS einlesen und unterstützte Anforderungen prüfen; interne Modellrepräsentation. |
| `com.bimportal.hackathon.ifc` | IFC-Dateien mit dem Java-Reader einlesen und Modellobjekte aufbereiten. |
| `com.bimportal.hackathon.catalogue` | Portal-LOINs anhand von Metadaten suchen und für ein Modell einordnen. |
| `com.bimportal.hackathon.check` | Anforderungen laden und Prüfungen über `CheckService` koordinieren. |
| `com.bimportal.hackathon.report` | Ergebnisse, Text- und JSON-Berichte. |
| `com.bimportal.hackathon.cli` | Kommandozeile für Prüfungen und Katalogsuche. |
| `com.bimportal.hackathon.mcp` | MCP-Werkzeuge und Server über `stdio`. |
| `com.bimportal.hackathon.chat` | Gespräch, Werkzeugdefinition und Schleife für den Chat in der Oberfläche (`0.3.3`). |
| `test.fx` | JavaFX-Oberfläche. |

Der IFC-Reader benötigt weder APSTEX noch JavaFX. CLI und MCP-Server können ohne grafische Oberfläche verwendet werden. Die Modulstruktur erleichtert Änderungen am Reader, ist aber keine fertige Plugin-Schnittstelle.

Die Module `bim-portal-enhanced` und `openapi-generated` bilden den Java-Zugang zur Portal-API. Der Python-Client stammt ebenfalls aus dem [Hackathon-Starterkit](https://github.com/pb40development/bim-portal) und ist kein Bestandteil des CheckMate-Prüfkerns.

## Schnittstellen und Grenzen

IFC und IDS sind offene Austauschformate; das Portal ist eine Anforderungsquelle neben lokalen IDS-Dateien. Die Formate ermöglichen die Nutzung verschiedener Werkzeuge, garantieren aber keine identischen Prüfergebnisse oder vollständige Interoperabilität. Die derzeit unterstützten Facetten und Abweichungen sind unter [IFC und IDS](/CheckMate_BIMPortal/architecture/ifc-ids-checking/) beschrieben.

Die aktuellen Klassen und offenen Entscheidungen stehen im [Repository](https://github.com/max-sternal/CheckMate_BIMPortal/tree/0.3.2) und in [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/docs/STATUS.md).
