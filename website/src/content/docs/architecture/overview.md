---
title: Systemarchitektur
description: Workspace-Pakete, IFClite-Integration und Datenfluss von CheckMate 0.4.
---

## Datenfluss

CheckMate `0.4` ist als TypeScript-Monorepo (`pnpm` Workspace) aufgebaut und kombiniert die WebAssembly-/TypeScript-Pakete von **IFClite (`@ifc-lite/*`)** mit dem fachlichen CheckMate-Prüfkern (`ModelFit`, `LoinDescription`, `MappedDataAccessor`, Fast-Treffer-Diagnose):

```text
IFC-Datei / URL --- @ifc-lite/parser + @ifc-lite/geometry --- LoadedModel (inkl. 3D-Meshes) -+
                                                                                              +-- CheckService -- CheckReport / BCF
BIM-Portal / IDS / .rules.json --- @checkmate/bimportal + @ifc-lite/ids + @ifc-lite/rules ---+        |
                                                                                      +---------------+---------------+
                                                                                      |               |               |
                                                                               @checkmate/web  @checkmate/cli  @checkmate/mcp
```

Da `@checkmate/core` und `@checkmate/bimportal` ohne Node-spezifische Abhängigkeiten im Browser wie in Node.js laufen, führt die 3D-Browser-Workbench (`@checkmate/web`) exakt denselben Prüfkern aus wie die CLI und der MCP-Server.

## Workspace-Pakete (`0.4`)

| Paket | Aufgabe |
|---|---|
| `packages/bimportal` (`@checkmate/bimportal`) | TypeScript-REST-Client für das BIM-Portal des Bundes (`via.bund.de/bim`), Katalogsuche, `CatalogueFilter`, UUID-Extraktion und ZIP-/IDS-Entpacken (`fflate`). |
| `packages/core` (`@checkmate/core`) | Gemeinsamer Prüfkern: `LoadedModel` (`@ifc-lite/parser` + `@ifc-lite/geometry`), `PropertyMapping`, `MappedDataAccessor`, `ModelFit`, `LoinDescription`, `CheckService` (`@ifc-lite/ids` + `@ifc-lite/rules`), Null-Treffer- & Fast-Treffer-Diagnose, BCF-2.1-Export (`@ifc-lite/bcf`), `ReportText` und `ReportJson`. |
| `packages/web` (`@checkmate/web`) | Standalone 3D-Browser-Workbench (Vite + WebGL) mit 4 Reitern: Prüfbericht & Diagnose, BIM-Portal-Katalog & `LoinDescription`, Modell & Element-Browser sowie Mapping-, `.rules.json`- & BCF-Export. |
| `packages/mcp` (`@checkmate/mcp`) | MCP-Server über `stdio` mit 10 Modell-Werkzeugen, 5 App-only-Werkzeugen (`visibility: ["app"]`) und 3 interaktiven 3D MCP-App-Ansichten (`ui://checkmate/report`, `ui://checkmate/model`, `ui://checkmate/loins`). |
| `packages/cli` (`@checkmate/cli`) | Kommandozeile (`--model`, `--ids`, `--loin`, `--aia`, `--rules`, `--mapping`, `--suggest`, `--json`, `--bcf`, `--full`) mit deutschen Berichten und Exit-Codes `0..4`. |

Für die Architektur der reinen Java-Edition (`0.3.2`) im Verzeichnis `bim-portal/Java_client/` siehe die [Systemarchitektur (0.3.2)](/CheckMate_BIMPortal/v0.3.2/architecture/overview/).
