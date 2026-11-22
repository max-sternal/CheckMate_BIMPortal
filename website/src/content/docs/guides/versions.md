---
title: Versionen & Branches
description: Vollständige Übersicht aller CheckMate-Versionen (0.1, 0.2, 0.3.1, 0.3.2 und 0.4 CheckMate Lite), ihrer Branches, IFC-Parser und Test-Referenzen.
---

CheckMate dokumentiert seine technische Entwicklung vom Hackathon-Prototyp bis zur vollständig offenen Architektur über klar getrennte Branches. Die Versionen **`0.1`** und **`0.2`** bleiben dauerhaft **eingefroren**, damit der Projektverlauf jederzeit nachvollziehbar bleibt.

## Versions- & Branch-Matrix

| Version | Git-Branch | Zeitraum / Status | Technologie-Stack | IFC-Parsing | Test-Referenz & Umfang |
|---|---|---|---|---|---|
| **`0.1`** | `0.1` *(von `main`)* | Sept. – Nov. 2025 (**eingefroren**) | Java 21, Gradle, JavaFX-Prototyp, BIM-Portal-Starterkit | **APSTEX IFC Toolbox** *(bereitgestellt im Hackathon)* | Basis-Beispiele aus Starterkit (`BasicExampleLoinIdsTest`) |
| **`0.2`** | `0.2` *(Commit `bc237bb`)* | Okt. 2026 (**eingefroren**) | Java 21, Gradle, JavaFX GUI + erste CLI (`LoinCheckCli`) | **APSTEX IFC Toolbox** | Erste IDS-/LOIN-Prüfung, gefixter BIM-Portal-Client, gebündeltes Brückenmodell |
| **`0.3.1`** | `0.3.1` *(Arbeitsbranch)* | Nov. 2026 (**aktuell**) | Java 21, Gradle 8.14, JavaFX GUI, CLI, **MCP-Server (stdio)**, Astro-Starlight-Doku | **APSTEX IFC Toolbox** *(extern anzufragen; Prüfkern 100 % entkoppelt)* | **217 Java-Tests** (148 Kern-Tests laufen ohne APSTEX in CI) — abgeglichen gegen **`IfcOpenShell` / `ifctester 0.9`** |
| **`0.3.2`** | `0.3.2` *(geplant)* | Ende Nov. 2026 (**Ziel Ende November**) | Java 21, Gradle 8.14, entkoppelter MCP-Server (Streamable HTTP, `structuredContent`, Tool Annotations) | **Ohne APSTEX** (freier Open-Source-IFC-Reader hinter `IfcModelReader`-Schnittstelle) | Alle Java-Tests aus `0.3` gegen **`IfcOpenShell`** — komplett in CI ohne proprietäre JARs |
| **`0.4`** (*CheckMate Lite*) | `0.4` *(parallel, WIP)* | Ab Ende Nov. 2026 (**Work in Progress**) | **Ohne Java** — TypeScript + Rust/WASM auf Basis von [**IFClite**](https://ifclite.dev/), MCP App (`ui://`) | **`@ifc-lite/parser`** (MPL-2.0, volle IFC2X3/4/4X3/IFC5-Relationen + Geometrie) | **Neue Testsuite** (TypeScript) gegen denselben **`IfcOpenShell` / `ifctester`**-Referenzkorpus |

---

## Einordnung der einzelnen Stufen

### `0.1` — Direkter Hackathon-Stand (eingefroren)
Repräsentiert den unveränderten Stand des **BIM-Portal-Hackathons Berlin (22.–23.09.2025)** auf Basis des offiziellen Starterkits (`pb40development/bim-portal`, bereitgestellt durch *planen-bauen 4.0* / BIM Deutschland) inklusive Pitch-Unterlagen, BIM-Deutschland-Dokumentation und erstem JavaFX-/APSTEX-Prototyp.

### `0.2` — Erste Anpassungen (eingefroren)
Erste produktive Weiterentwicklung Ende Oktober 2026:
- Behebung der API-Serialisierungs- und Deserialisierungsfehler im BIM-Portal-Client sowie anonymer öffentlicher Lesezugriff (ohne zwingenden Login).
- Lauffähige JavaFX-GUI mit gebündeltem Brückenmodell (`bruecke-tbw1-mit-merkmalen.ifc`).
- Erste Implementierung des `ids/`-Pakets und der Headless-CLI `LoinCheckCli`.

### `0.3.1` — Echter Arbeitsstand mit APSTEX (aktuell)
Vollständig überarbeiteter, deterministischer Prüfkern mit einheitlichem `CheckService` für **GUI, CLI und MCP-Server**:
- **Wichtig zu APSTEX:** Die beiden JARs der **APSTEX IFC Toolbox** (`com.apstex.ifctoolbox*.jar`, `com.apstex.stepparser*.jar`) wurden für den Hackathon durch *planen-bauen 4.0* / APSTEX zur Verfügung gestellt, sind jedoch **nicht frei redistribuierbar** und daher **nicht im Git-Repository enthalten**.
- **So erhältst du die JARs für `0.3.1`:** Wer `0.3.1` lokal mit vollem IFC-Parsing (GUI, CLI, MCP-Server) nutzen möchte, kann die benötigten APSTEX-JARs beim Projektteam (oder direkt über [APSTEX](https://www.apstex.com/)) anfragen und in `bim-portal/Java_client/hackathon-template/lib/` ablegen.
- **Was auch ohne APSTEX in `0.3.1` funktioniert:** Der gesamte Prüfkern (`ids`, `catalogue`, `check`, `report`) ist vollständig frei von APSTEX. Gradle erkennt fehlende JARs automatisch und führt alle **148 bibliotheksfreien Kern- und IDS-Konformitätstests** direkt in jeder Umgebung und in GitHub Actions aus.

### `0.3.2` — Finales Ende-November-Ergebnis (Java ohne APSTEX)
Schließt die Java-`0.3`-Linie als vollständig eigenständiges Open-Source-Release ab:
- **Ablösung von APSTEX** durch einen frei verteilbaren IFC-Reader hinter einer klaren `IfcModelReader`-Schnittstelle — verifiziert gegen die gesamte bestehende Java-Testsuite und **`IfcOpenShell`**.
- **Entkopplung des MCP-Servers von JavaFX** (`LoadedModel` aus `test.fx` herauslösen) und Upgrade auf moderne MCP-Standards (Tool Annotations, `structuredContent` aus `ReportJson`, Streamable HTTP).
- Gradle bleibt das Build-System der `0.3`-Linie; alle Java-Tests bleiben erhalten.

### `0.4` — *CheckMate Lite* (Work in Progress, auf `IFClite`-Basis)
Parallel entstehende Next-Generation-Architektur **ohne Java und ohne APSTEX**:
- Baut auf [**IFClite**](https://ifclite.dev/) (`LTplus-AG/ifc-lite`, **MPL-2.0**) auf: Rust-Kern + WASM + modulare TypeScript-Pakete für **Node.js (Headless CLI & MCP-Server)**, **Browser (WebGPU 3D-Viewer)** und optional **Tauri-Desktop**.
- Liefert vollständiges Relations-Parsing für **IFC2X3, IFC4, IFC4X3 und IFC5 (IFCX)**, exakte Geometrie, BCF-Export (`@ifc-lite/bcf`) und regelbasierte 3D-Einfärbung (`@ifc-lite/lens`).
- Ermöglicht eine native **Antigravity MCP App** (`ui://` nach Stand November 2026) mit interaktiver Prüftabelle und 3D-Modellansicht direkt im KI-Client.
- Erhält eine **neue, eigenständige Testsuite**, die weiterhin gegen **`IfcOpenShell` / `ifctester`** als gemeinsame Referenz validiert wird.
