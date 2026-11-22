---
title: Projektstatus
description: Stand und offene Punkte von CheckMate 0.4 und der Java-Linie 0.3.2 / 0.3.3.
---

## Stand von `0.4` (`0.4-alpha`)

CheckMate `0.4` verbindet den gesamten fachlichen Prüfkern aus `0.3.2` mit **TypeScript und IFClite (`@ifc-lite/*`)**:

- **3D-Browser-Workbench (`@checkmate/web`)**: 4-Reiter-Oberfläche mit WebGL-3D-Viewer, Bauteil-Inspektor (`element_report`), Live-BIM-Portal-Suche mit `ModelFit`-Sortierung, `LoinDescription`-Vorschau, Übersichtstabelle, Fast-Treffer-Diagnose (`nicht prüfbar`), Live-JSON-Mapping-Editor, `.rules.json`-Konvertierung und BCF-2.1-Download (`.bcfzip`).
- **MCP-Server & 3D MCP Apps (`@checkmate/mcp`)**: 10 Modell-Werkzeuge, 5 App-only-Werkzeuge und 3 interaktive 3D-HTML-Ansichten (`ui://checkmate/report`, `ui://checkmate/model`, `ui://checkmate/loins`).
- **CLI (`@checkmate/cli`)**: Kompatibel mit den Parametern, deutschen Berichten und Exit-Codes (`0..4`) von `0.3.2`, ergänzt um `--rules` und `--bcf`.

## Stand der Java-Linie (`0.3.2` & `0.3.3`)

- **`0.3.2`**: Läuft mit Java 21 bis Java 25 LTS und Gradle 9.1.0, liest IFC2X3, IFC4 und IFC4X3 ohne APSTEX und unterstützt austauschbare JSON-Mappings (`bim-hamburg.json`) sowie die erste MCP-App-Berichtansicht. ([Zur archivierten 0.3.2-Dokumentation](/CheckMate_BIMPortal/v0.3.2/))
- **`0.3.3` (Geplant)**: Untersuchung und Unterstützung von aktuellem LOIN-XML (siehe [BIM-Portal-Importschnittstelle](https://github.com/bimdeutschland/BIM-Portal-Importschnittstellendokumentation) und [ISO/DIS 7817-3](https://www.iso.org/standard/89077.html)).

## Offene Punkte

- Praktische Erprobung der 3D-Browser-Workbench (`0.4-alpha`) und der 3D MCP Apps mit verschiedenen Fachmodellen und AIA-Projekten.
- Untersuchung von LOIN-XML in `0.3.3` und Rückführung neuer Erkenntnisse zwischen `0.3.x` und `0.4`.
