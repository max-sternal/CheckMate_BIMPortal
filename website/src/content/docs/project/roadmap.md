---
title: Status & Roadmap
description: Aktueller Stand, offene Entscheidungen und Meilensteine von CheckMate 0.3.1 bis 0.4 (CheckMate Lite).
---

Eine tagesaktuelle, code-nahe Detail-Checkliste pflegt das Projekt direkt im Repository in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/WORKFLOW.md) und [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/STATUS.md).

## Was heute (`0.3.1`) bereits funktioniert

- **IFC-Laden:** IFC2X3, IFC4, IFC4X3, lokale Dateien, `.ifczip`, HTTP(S)- und GitHub-Links (inkl. Git-LFS).
- **Anforderungen:** Einzelne LOINs (GUID/Link), komplette AIA-Projekt-ZIPs, lokale IDS-Dateien/ZIPs, anonymer Zugriff auf alle 1.568 öffentlichen Portal-LOINs.
- **Katalog-Auswahl:** Filterung nach Fachmodell, Leistungsphase und Anwendungsfall + Metadaten-Passungsranking (`ModelFit`).
- **Deterministische Prüfung:** Strenge IDS-1.0-Regeln (Entity, `PredefinedType`/`USERDEFINED`, Property, Datentyp, Werte, `prohibited`/`optional`), ehrliche Diagnosen & Near-Misses, `IfcOpenShell`/`ifctester`-Abgleich.
- **Schnittstellen:** JavaFX-GUI mit Ergebnistabelle, Headless-CLI (`--json`, Exit-Codes `0–4`), stdio-MCP-Server (7 Tools, getestet mit Claude Desktop & Claude Code).
- **CI & Doku:** GitHub Actions für 148 bibliotheksfreie Kern-Tests + zweisprachige Astro-Starlight-Dokumentation (DE/EN).

---

## Offene Entscheidungen & bekannte Grenzen

| Thema | Status-Ref. | Kurzbeschreibung |
|---|---|---|
| **IFC-Bibliothek ohne APSTEX** | `STATUS #15` | Ablösung der nicht-redistribuierbaren APSTEX-JARs in `0.3.2` (Java) und komplettem Wechsel auf `IFClite` in `0.4`. |
| **Hardcodierte `PropertyAliases`** | `STATUS #14` | BIM.Hamburg-Mapping (`_IDEbene1..3`) aus dem Code in externe, projektbezogene Mapping-Dateien auslagern. |
| **Paketname `test.fx`** | `STATUS #13` | Umbenennung in `com.bimportal.hackathon.gui` + Entkopplung von `LoadedModel` für den MCP-Server. |
| **Ollama / OpenCode / Strata-Tests** | `STATUS #16` | Praktische End-to-End-Tests mit offenen lokalen & Cloud-Modellen. |
| **Jackson-Version im MCP-SDK** | `STATUS #17` | Projekt pinnt Jackson `2.15.2`, MCP-Java-SDK erwartet `2.21`. |
| **Weitere IDS-1.0-Facetten** | `WORKFLOW 4` | `attribute`, `classification`, `material`, `partOf`, Regex/Bounds auf Werten, SI-Einheitenumrechnung. |
| **Katalog-Cache & BCF-Export** | `WORKFLOW 4` | Lokaler Disk-Cache für 3-MB-LOIN-Katalog (~7 s Startzeit); BCF-Export (in `0.4` direkt über `@ifc-lite/bcf`). |

---

## Meilensteine

### `0.3.1` — Ende dieser Woche
- Eingefrorene Historien-Branches **`0.1`** und **`0.2`**, Arbeitsbranch **`0.3.1`**.
- Öffentliche, zweisprachige Dokumentation (DE/EN) auf GitHub Pages inklusive transparenter Contributor- & Nachweisseite.
- Automatisierte CI-Tests für den APSTEX-freien Prüfkern und Doku-Build.

### `0.3.2` — Ende November 2026
- **Vollständig APSTEX-freie Java-Version** mit Gradle und allen bestehenden Java-Tests gegen `IfcOpenShell`.
- **MCP-Upgrade:** Entkopplung von JavaFX, Tool Annotations, `structuredContent` (aus `ReportJson`), Streamable HTTP (`/mcp`), getestete OpenCode-/Ollama-/Strata-Anbindung.

### `0.4` (*CheckMate Lite*) — Parallel / Work in Progress
- Kompletter Neustart **ohne Java und ohne APSTEX** auf Basis von [**IFClite**](https://ifclite.dev/) (TypeScript + Rust/WASM, MPL-2.0).
- **Native Antigravity MCP App** (`ui://`) mit interaktiver Prüftabelle und eingebettetem 3D-WebGPU-Viewer (`@ifc-lite/lens`), plus BCF-Export (`@ifc-lite/bcf`).
- **Neue Testsuite** gegen dieselbe `IfcOpenShell`/`ifctester`-Referenz.
- Perspektive für **LOIN-Erstellung** und **geometrische Prüfungen nach ISO 7817-2**.
