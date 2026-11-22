---
title: Projektstatus
description: Stand und offene Punkte von CheckMate 0.3.2.
---

## Stand von 0.3.2

CheckMate bietet eine JavaFX-Oberfläche, eine CLI und einen MCP-Server für die Prüfung von IFC-Modellen gegen unterstützte IDS-Anforderungen. Anforderungen können aus dem BIM-Portal oder lokalen IDS-Dateien stammen. Der Java-Reader liest IFC2X3, IFC4 und IFC4X3 ohne APSTEX.

Die MCP-App-Berichtansicht ist implementiert und auf Protokollebene getestet; eine Sichtprüfung in Claude Desktop oder Antigravity steht noch aus. JSON-Mappings können geladen und gewechselt werden.

## Offene Punkte

- Weitere IDS-Facetten und IFC-Eigenschaften unterstützen; bestehende Grenzen stehen unter [IFC und IDS](/CheckMate_BIMPortal/architecture/ifc-ids-checking/).
- MCP-App-Berichtansicht in geeigneten Clients praktisch prüfen und weitere Ansichten untersuchen.
- Für `0.3.3` die Unterstützung von LOIN-XML untersuchen. LOIN-XML ist nicht mit dem derzeit unterstützten IDS-Format gleichzusetzen. Als Ausgangspunkte dienen die [BIM-Portal-Importschnittstelle](https://github.com/bimdeutschland/BIM-Portal-Importschnittstellendokumentation) und [ISO/DIS 7817-3](https://www.iso.org/standard/89077.html); der Standard ist noch in Entwicklung.
- Für `0.4` eine bessere Online-Nutzung mit Viewer und MCP-App-Integration untersuchen.
- Schnittstellen und Dokumentation mit der weiteren Entwicklung pflegen.

Entwicklungszweige und Ideen sind keine Zusage eines bestimmten Funktionsumfangs oder Termins. Code-nahe Einzelheiten stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/docs/WORKFLOW.md) und [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/docs/STATUS.md).
