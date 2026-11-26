---
title: Projektstatus
description: Aktueller Stand von CheckMate 0.4.1-beta (TypeScript + IFClite) und 0.3.3-stable (Latest, Java 25).
---

## Stand von `0.4.1-beta` (TypeScript + IFClite)

CheckMate `0.4.1-beta` rundet den in `0.4-alpha` begonnenen TypeScript- und IFClite-Stack (`@ifc-lite/*`) zu einer aufgeräumten, direkt einsetzbaren Beta-Edition ab:

- **3D-Browser-Workbench (`@checkmate/web`)**: 4-Reiter-Oberfläche mit WebGL-3D-Viewer, 1-Klick-Vorlagen für Hochbau (`🏢 Mini-Hochbau`), Infrastruktur (`🌉 Mini-Brücke`) und BIM.Hamburg (`TBW1`), Bauteil-Inspektor (`element_report`), Live-BIM-Portal-Suche mit `ModelFit`-Sortierung, `LoinDescription`-Vorschau, Fast-Treffer-Diagnose, Live-JSON-Mapping-Editor, `.rules.json`-Konvertierung und BCF-2.1-Download (`.bcfzip`).
- **Übersichtlicher MCP-Server & 3D MCP Apps (`@checkmate/mcp`)**: Klar getrennter Serverkatalog (`catalog.ts` / `tools.ts` / `index.ts`) mit eigenständigem Starter (`checkmate-mcp.mjs`), 10 Modell-Werkzeugen, 5 App-only-Werkzeugen, 3 interaktiven 3D-HTML-Ansichten (`ui://checkmate/*`), 2 JSON-Ressourcen (`checkmate://model/current`, `checkmate://report/last.json`), 2 Prompts (`check-loin`, `explain-element`) und Antigravity-Plugin-Generator (`pnpm mcp:antigravity-plugin`).
- **Kommandozeile (`@checkmate/cli`)**: Direkt über `pnpm cli` oder global per `pnpm --filter @checkmate/cli link --global` als `checkmate` nutzbar, inklusive `--help`, `--version`, `--rules`, `--bcf` und konsistenten Exit-Codes (`0..4`).
- **BIM-Portal-IFC4-Referenzmodelle**: Zwei sehr kleine, im Texteditor nachvollziehbare 3D-IFC4-Modelle für Hochbau (`mini-hochbau-bundesbau.ifc`, 5 LOINs) und Infrastruktur (`mini-infrastruktur-bruecke.ifc`, 6 LOINs) sowie 6 erweiterte `_mod_full`/`_mod_partial`-Testmodelle in [`docs/README.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/docs/README.md).

## Stand von `0.3.3-stable` (`Latest`, Java 25 LTS)

- **`0.3.3-stable`** ist das aktuelle stabile Haupt-Release der Java-Linie ([zur Latest-Dokumentation](/CheckMate_BIMPortal/)): Es läuft auf JDK 25 LTS und Gradle 9.1.0 ohne APSTEX, bietet neben GUI, CLI und Java-MCP-Server (11 Werkzeuge, 3 MCP-App-Ansichten, Antigravity-Plugin) auch einen konfigurierbaren KI-Chat direkt im JavaFX-Fenster (`CheckMateChat`).

## Ausblick

- Praktische Erprobung beider Editionen (`0.3.3-stable` und `0.4.1-beta`) mit weiteren Fachmodellen, Objektkatalogen und AIA-Projekten.
- Beobachtung der weiteren Standardisierung von LOIN-XML ([BIM-Portal-Importschnittstelle](https://github.com/bimdeutschland/BIM-Portal-Importschnittstellendokumentation), [ISO/DIS 7817-3](https://www.iso.org/standard/89077.html)).
