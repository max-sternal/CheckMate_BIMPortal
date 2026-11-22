---
title: Versionen & Branches
description: Vom Hackathon-Prototyp über die Java-Linie 0.3.x bis zur TypeScript/IFClite-Edition 0.4.
---

| Version / Branch | Stand und Schwerpunkt |
|---|---|
| `0.1` | Ergebnis des 24-Stunden-Hackathons mit Prototyp und Präsentation. |
| `0.2` | Erste Korrekturen am Portal-Client und ein lauffähiger Prüfablauf. |
| `0.3` | Größere Überarbeitung von Prüfkern, Berichten und Schnittstellen als Grundlage der `0.3`-Linie. |
| `0.3.1` | Java-Stand mit GUI, CLI und MCP-Server; IFC-Einlesen mit APSTEX wie beim Hackathon. |
| `0.3.2` | Reine Java-Edition (Java 21 / Java 25 LTS, Gradle 9.1): eigener Java-IFC-Reader ohne APSTEX, austauschbare JSON-Mappings (`bim-hamburg.json`) und erste MCP-App-Berichtansicht. ([Zur 0.3.2-Doku](/CheckMate_BIMPortal/v0.3.2/)) |
| `0.3.3` | Geplante Weiterentwicklung der Java-Linie: Untersuchung und Unterstützung für aktuelles LOIN-XML (ISO/DIS 7817-3). |
| `0.4` (`0.4-alpha`) | TypeScript- & IFClite-Edition (`@ifc-lite/*`): 3D-Browser-Workbench (`@checkmate/web`), MCP-Server mit 15 Werkzeugen und interaktiven 3D MCP Apps (`@checkmate/mcp`), CLI (`@checkmate/cli`), `.rules.json`-Regelwerke und BCF-2.1-Export (`.bcfzip`). |

## Zwei aktive Linien: `0.3.x` (Java) und `0.4` (TypeScript + IFClite)

CheckMate wird parallel in zwei sich ergänzenden Linien gepflegt:

- **Java-Linie (`0.3.1`, `0.3.2`, `0.3.3`)**: Setzt auf das Java-Ökosystem (Gradle, JavaFX-Desktop-Oberfläche, MCP Java SDK) und bildet die Referenz für den reinen Java-Prüfkern sowie künftige LOIN-XML-Untersuchungen in `0.3.3`.
- **TypeScript / IFClite-Linie (`0.4`)**: Baut auf demselben fachlichen Prüfablauf (`ModelFit`, `LoinDescription`, `PropertyMapping`, Fast-Treffer-Diagnose, deutsche Berichte und Exit-Codes) auf, nutzt aber die WebAssembly-/TypeScript-Pakete von **IFClite (`@ifc-lite/*`)** für schnelle 3D-Visualisierung im Browser und in MCP-fähigen KI-Clients.

Über das Versions-Auswahlmenü oben in der Navigation lässt sich jederzeit zwischen der aktuellen Dokumentation (`0.4`) und den archivierten Ständen (`v0.3.2`, `v0.3.1`, `v0.2.0`, `v0.1.0`) umschalten.

[Branches im Repository ansehen](https://github.com/max-sternal/CheckMate_BIMPortal/branches) · [Releases & Pre-Releases ansehen](https://github.com/max-sternal/CheckMate_BIMPortal/releases)
