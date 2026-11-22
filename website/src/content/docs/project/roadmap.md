---
title: Projektstatus
description: Stand und offene Punkte von CheckMate 0.3.1.
---

## Stand von 0.3.1

CheckMate bietet eine JavaFX-Oberfläche, eine CLI und einen MCP-Server für die Prüfung von IFC-Modellen gegen unterstützte IDS-Anforderungen. Anforderungen können aus dem BIM-Portal oder lokalen Dateien stammen. IFC2X3, IFC4 und IFC4X3 werden mit APSTEX eingelesen.

Der Quellcode steht unter Apache 2.0; die benötigte APSTEX IFC Toolbox ist eine separate proprietäre Abhängigkeit. Ohne sie lassen sich der Prüfkern und seine Tests nutzen, aber keine IFC-Modelle mit CheckMate laden.

## Offene Punkte

- Weitere IDS-Facetten und IFC-Eigenschaften unterstützen; bestehende Grenzen stehen unter [IFC und IDS](/CheckMate_BIMPortal/architecture/ifc-ids-checking/).
- Eine offen verfügbare Alternative für das IFC-Einlesen untersuchen. Die konkrete Umsetzung ist noch nicht Bestandteil von `0.3.1`.
- Den Umgang mit projektspezifischen Merkmalszuordnungen klären; aktuell gibt es ein festes Mapping für das Beispielmodell.
- Schnittstellen und Dokumentation mit der weiteren Entwicklung pflegen.

Entwicklungszweige und Ideen sind keine Zusage eines bestimmten Funktionsumfangs oder Termins. Code-nahe Einzelheiten stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/WORKFLOW.md) und [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/STATUS.md).
