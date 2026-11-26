---
title: Versionen & Branches
description: Vom Hackathon-Prototyp über das aktuelle Haupt-Release 0.3.3-stable (Latest) bis zur TypeScript/IFClite-Beta 0.4.1-beta.
---

| Version / Release | Branch | Stand und Schwerpunkt |
|---|---|---|
| `v0.1.0` | `0.1` / `main` | Ergebnis des 24-Stunden-Hackathons (September 2025) mit Erstprototyp und Präsentation. ([Doku](/CheckMate_BIMPortal/v0.1.0/)) |
| `v0.2.0` | `0.2` | Erste Korrekturen am Portal-Client und ein durchgängig lauffähiger Prüfablauf. ([Doku](/CheckMate_BIMPortal/v0.2.0/)) |
| `v0.3.1` | `0.3.1` | Überarbeiteter Java-Prüfkern mit JavaFX-GUI, CLI und MCP-Server (noch mit APSTEX). ([Doku](/CheckMate_BIMPortal/v0.3.1/)) |
| `v0.3.2` | `0.3.2` | Reine Java-25-Edition ohne APSTEX: eigener STEP-IFC-Reader, JSON-Mapping (`bim-hamburg.json`), Merkmals-Client, IDS-Wertrestriktionen, 3 MCP-App-Ansichten und Antigravity-Plugin. ([Doku](/CheckMate_BIMPortal/v0.3.2/)) |
| **`v0.3.3-stable` (`Latest`)** | `0.3.3-stable` / `0.3.3` | **Aktuelles stabiles Haupt-Release (Java 25 LTS):** erweitert `0.3.2` um den konfigurierbaren KI-Chat direkt in der JavaFX-Oberfläche (`CheckMateChat`), gehärtetes Build-/Dependency-Setup sowie aufbereitete BIM-Portal-Testmodelle. ([Zur Latest-Doku](/CheckMate_BIMPortal/)) |
| `v0.4-alpha` (`Alpha`) | `0.4` (Tag `v0.4-alpha`) | Erster TypeScript- & IFClite-Durchstich (`@ifc-lite/*`): 3D-Browser-Workbench, 3D MCP Apps, CLI, `.rules.json` und BCF-2.1-Export. ([Doku](/CheckMate_BIMPortal/v0.4-alpha/)) |
| **`v0.4.1-beta` (`Beta`)** | `0.4.1-beta` / `0.4` | **Hier dokumentierte Beta-Version (TypeScript + IFClite):** vereinfachter MCP-Server (`checkmate-mcp.mjs`, JSON-Ressourcen, Prompts, Antigravity-Plugin), direkte CLI-Installation (`checkmate`), Schema- & Live-Portal-Fixes sowie zwei kompakte 3D-IFC4-Referenzmodelle für Hochbau und Infrastruktur (`Exit-Code 0`). |

## Zwei sich ergänzende Linien: `0.3.3-stable` (Java 25) und `0.4.1-beta` (TypeScript + IFClite)

CheckMate wird in zwei klar abgegrenzten, fachlich konsistenten Editionen bereitgestellt:

- **`0.3.3-stable` (`Latest`, Java 25 LTS):** Die stabile Referenzimplementierung mit klassischer JavaFX-Desktop-Oberfläche (inklusive optionalem KI-Chat über API-Key), Kommandozeile und Java-MCP-Server – komplett ohne proprietäre Drittbibliotheken.
- **`0.4.1-beta` (`Beta`, TypeScript + IFClite):** Überträgt denselben fachlichen Prüfkern (`ModelFit`, `LoinDescription`, `PropertyMapping`, Fast-Treffer-Diagnose, deutsche Berichte und Exit-Codes `0..4`) auf Node.js 22+ und WebAssembly (`@ifc-lite/*`) für interaktive 3D-Prüfung im Browser sowie eingebettete 3D MCP Apps in Antigravity und Claude Desktop.

Über das Versions-Auswahlmenü oben in der Navigation lässt sich jederzeit zwischen **`Latest (0.3.3-stable)`**, **`v0.4.1-beta`**, **`v0.4-alpha`** und den älteren Ständen (`v0.3.2`, `v0.3.1`, `v0.2.0`, `v0.1.0`) wechseln.

[Branches im Repository ansehen](https://github.com/max-sternal/CheckMate_BIMPortal/branches) · [Releases & Pre-Releases ansehen](https://github.com/max-sternal/CheckMate_BIMPortal/releases)
