---
title: Systemarchitektur
description: Aufbau von CheckMate 0.3.x (Java-Kern + 3 Frontends) und Zielarchitektur für 0.3.2 & 0.4 (CheckMate Lite auf IFClite).
---

## Leitprinzip: Ein Prüfkern für alle Oberflächen

In CheckMate `0.3.x` sind **IFC-Einlesen**, **Fachlogik/Prüfung** und **Präsentation** konsequent entkoppelt. GUI, CLI und MCP-Server enthalten keine eigene Prüflogik — sie sammeln lediglich Eingaben ein, rufen `CheckService` auf und stellen den zurückgegebenen `CheckReport` dar.

```text
                    ┌──────────────────────────────────────────┐
                    │     BIM-Portal des Bundes (REST API)     │
                    │  LOIN-Katalog · AIA-Projekte · IDS 1.0   │
                    └────────────────────┬─────────────────────┘
                                         │ (anonym / optional JWT)
                                         ▼
┌─────────────────────┐      ┌───────────────────────┐      ┌──────────────────────┐
│  IFC / ifcZIP / URL │ ───► │   IFC-Reader-Schicht  │ ───► │  ModelElement[]      │
│  (IFC2X3 / 4 / 4X3) │      │  0.3.1: APSTEX        │      │  (bibliotheksfrei)   │
└─────────────────────┘      │  0.3.2: Open-Source   │      └──────────┬───────────┘
                             └───────────────────────┘                 │
                                                                       ▼
                             ┌─────────────────────────────────────────────────────┐
                             │             Pure-Java-Prüfkern                      │
                             │  ids/ · catalogue/ · check/ (CheckService) · report/│
                             └──────┬──────────────────┬──────────────────┬────────┘
                                    │                  │                  │
                                    ▼                  ▼                  ▼
                             ┌────────────┐    ┌─────────────┐    ┌───────────────┐
                             │ JavaFX GUI │    │ Headless CLI│    │  MCP-Server   │
                             │ (ElementUI)│    │ (Text/JSON) │    │ (7 Tools)     │
                             └────────────┘    └─────────────┘    └───────────────┘
```

## Modul- & Paketstruktur (`0.3.1`)

Der gesamte CheckMate-Code liegt im Gradle-Modul `bim-portal/Java_client/hackathon-template/`:

| Paket | Aufgabe | Abhängigkeiten |
|---|---|---|
| `com.bimportal.hackathon.ids` | IDS-1.0-Parser (`IdsParser`), ZIP-Archive (`IdsArchive`), Validator (`IdsValidator`), `DataTypes`, `Values`, `PropertyKey`, `PropertyAliases`, `PortalRequirements`, `ModelElement` | Keine IFC-Library, kein JavaFX |
| `com.bimportal.hackathon.catalogue` | Metadaten-Filter (`CatalogueFilter`), Modell-Passungs-Ranking (`ModelFit`) über 1.568 LOINs, LOIN-Vorschau (`LoinDescription`) | Keine IFC-Library, kein JavaFX |
| `com.bimportal.hackathon.check` | Einheitliche Orchestrierung (`CheckService`), Anforderungsquellen (`Source`), Session-Download-Cache | Keine IFC-Library, kein JavaFX |
| `com.bimportal.hackathon.report` | Ergebnismodell (`CheckReport`), Exit-Codes, Text-Formatter (`ReportText`), stabiles JSON (`ReportJson`) | Keine IFC-Library, kein JavaFX |
| `com.bimportal.hackathon.mcp` | Offizieller MCP-Java-SDK-Server (`CheckMateMcpServer`, `CheckMateTools`) über `stdio` | Nutzt aktuell `test.fx.LoadedModel` |
| `test.fx` *(Umbenennung geplant)* | JavaFX-GUI (`App`, `ElementUI`), CLI (`LoinCheckCli`), IFC-Laden (`IfcResources`, `ModelLinks`, `IfcModelElements`, `LoadedModel`) | APSTEX IFC Toolbox (+ JDK FX für GUI) |

Zusätzlich im Repository:
- `bim-portal/Java_client/bim-portal-enhanced/`: Erweiterter REST-Client für das BIM-Portal (anonymer Zugang, Paging, Exporte).
- `bim-portal/Java_client/openapi-generated/`: OpenAPI-generierter Feign-Client (mit `NON_NULL`- & `LenientStringDeserializer`-Fix).
- `bim-portal/Python_client/`: Unveränderter Python-Client aus dem Hackathon-Starterkit.

---

## Entwicklungspfad: `0.3.2` → `0.4` (*CheckMate Lite*)

### Schritt 1: `0.3.2` (Java-Architektur ohne APSTEX)
- Herauslösen von `LoadedModel` und einem `IfcModelReader`-Interface in den Kern (`check/`), sodass der MCP-Server kein JavaFX mehr beim Kompilieren benötigt.
- Ersetzen der APSTEX-Abhängigkeit durch einen offenen Reader (gegen alle bestehenden Java-Tests + `IfcOpenShell` verifiziert).
- Erweiterung des MCP-Servers auf **Streamable HTTP**, **Tool Annotations** und **`structuredContent`** (direkt aus `ReportJson`).

### Schritt 2: `0.4` — *CheckMate Lite* (ohne Java, auf `IFClite`)
Die langfristige Zielarchitektur vereint CLI, MCP-Server, Web-App und Antigravity MCP App in einem einzigen Open-Source-Stack:

- **IFC-Basis:** [`IFClite`](https://ifclite.dev/) (`MPL-2.0`, Rust + WASM + TypeScript) — volle Abdeckung von **IFC2X3, IFC4, IFC4X3 und IFC5 (IFCX)** inklusive Relationen, Raumstruktur, Geometrie, `@ifc-lite/bcf` und `@ifc-lite/lens` (3D-Einfärbung nach Prüfergebnis).
- **BIM-Portal-Client:** Schlanker TypeScript-Client (`fetch`) für die benötigten REST-Endpunkte.
- **Prüf- & Katalog-Kern:** Portierung der bewährten CheckMate-Fachlogik (`ModelFit`, AIA-ZIP-Merge, strenge deutsche Diagnosen & Near-Misses), verifiziert über eine **neue TypeScript-Testsuite** gegen dieselbe **`IfcOpenShell` / `ifctester`-Referenz**.
- **Auslieferung:** Headless Node.js (CLI & MCP-Server), Browser/WebGPU (Antigravity MCP App `ui://` — lokale Modelle bleiben im Browser-Tab!) und optional Tauri-Desktop für Offline-Behördenarbeitsplätze.
