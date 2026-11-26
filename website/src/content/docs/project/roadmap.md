---
title: Projektstatus
description: Stand und offene Punkte von CheckMate 0.3.3-stable und Ausblick auf 0.4.1-beta.
---

## Stand von `0.3.3-stable` (Latest)

CheckMate `0.3.3-stable` bietet eine JavaFX-Oberfläche mit integriertem konfigurierbarem KI-Chat (`CheckMateChat`), eine robuste CLI und einen MCP-Server mit 11 Werkzeugen, 3 MCP App-Ansichten, 2 JSON-Ressourcen und 2 Prompts für die Prüfung von IFC-Modellen gegen unterstützte IDS-Anforderungen. Anforderungen können aus dem BIM-Portal oder lokalen IDS-Dateien stammen. Der Java-Reader liest IFC2X3, IFC4 und IFC4X3 ohne APSTEX. Aufbereitete BIM-Portal-Testmodelle für Hochbau und Infrastruktur liegen in `docs/README.md` (`docs/mini-hochbau-bundesbau.ifc`, `docs/mini-infrastruktur-bruecke.ifc`).

## Aktive Entwicklung auf `0.4.1-beta` (Beta)

Die aktive Weiterentwicklung findet auf der TypeScript- und IFClite-Linie `0.4.1-beta` statt ([Zur Beta-Doku](/CheckMate_BIMPortal/v0.4.1-beta/)), die einen 3D-Browser-Viewer, BCF-Export und eine schlanke Node.js-Edition (`checkmate-mcp.mjs`) mit nativer Antigravity-Unterstützung bereitstellt.

## Offene Punkte & Ausblick

- Weitere IDS-Facetten und IFC-Eigenschaften unterstützen; bestehende Grenzen stehen unter [IFC und IDS](/CheckMate_BIMPortal/architecture/ifc-ids-checking/).
- MCP-App-Ansichten in weiteren Clients evaluieren.
- Nahtlose Weiterentwicklung der TypeScript-/IFClite-Architektur (`0.4.1-beta`).
- Schnittstellen und Dokumentation mit der weiteren Entwicklung pflegen.

Entwicklungszweige und Ideen sind keine Zusage eines bestimmten Funktionsumfangs oder Termins. Code-nahe Einzelheiten stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/WORKFLOW.md) und [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/STATUS.md).
