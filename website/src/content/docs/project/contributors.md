---
title: Mitwirkende & Transparenz
description: Klare Übersicht über Herkunft, Verantwortlichkeiten, Beiträge, Lizenzen, KI-Einsatz und Nachweisführung bei CheckMate.
---

## 1. Projektstatus & Herkunft

**CheckMate** ist ein **privates, offenes Open-Source-Projekt** unter der **Apache License 2.0**. Es basiert auf der Einreichung von *Team CheckMate* beim **BIM-Portal-Hackathon Berlin (22.–23. September 2025)** und wird von **Mitarbeitenden und Alumni der TU Berlin** privat und gemeinschaftlich weiterentwickelt.

:::note[Abgrenzung & Trägerschaft]
- CheckMate ist **kein offizielles Produkt des Bundes oder der TU Berlin**, sondern ein eigenständiges, privates Open-Source-Werkzeug zur Unterstützung von Behörden, Auftraggebern und Planenden im Umgang mit dem öffentlichen BIM-Portal des Bundes (`via.bund.de/bim`).
- Die ursprünglichen API-Starter-Clients (`bim-portal/Java_client/openapi-generated`, `bim-portal-enhanced`, `Python_client`) sowie Referenzdokumente (`DocsBIMDeutschland/`) stammen aus dem öffentlichen Hackathon-Starterkit (`pb40development/bim-portal`, *planen-bauen 4.0* / BIM Deutschland).
:::

---

## 2. Verantwortlichkeiten & Beiträge nach Personen

Die folgende Aufstellung basiert direkt auf der Git-Historie über alle Branches (`0.1`, `0.2`, `0.3.1` / `webView`, Stand **22.11.2026**, insgesamt 23 Commits). Für eine faire und belastbare Auswertung werden **handgeschriebener Code & Dokumentation strikt von generierten Dateien, Upstream-Starterkits und IFC-Modelldaten getrennt**.

| Mitwirkende:r (GitHub / Zugehörigkeit) | Zeitraum | Commits | Handgeschriebener Code & Config (`+`/`-`) | Dokumentation & Tooling (`+`/`-`) | Daten / Vendor / Generiert | Verantwortungsbereiche & Arbeitspakete |
|---|---|---|---|---|---|---|
| **Maximilian Sternal** (`@max-sternal`, TU Berlin) | 22.09.2025 – 11.11.2025 *(+ Projektleitung & Release 0.3.1)* | **3** *(Branch `0.1` / `main`)* | `+15.540 / -1` *(inkl. Übernahme des Hackathon-Starterkits + initiales `test.fx` `+469` Z.)* | `+437 / -0` | `+43.306` *(OpenAPI-Client, `DocsBIMDeutschland`, Pitch-PDFs)* | Projekt-Initiierung & Vision, Repository-Setup (Apache 2.0), Archivierung des Hackathon-Standes (`0.1`), erster JavaFX-/APSTEX-Prototyp, Koordination, Release- & Doku-Steuerung (`0.3.1` / `0.4`) |
| **SOPHIEfree** (`@SOPHIEfree`) | 29.10.2026 – 30.10.2026 | **4** *(Branch `0.2`)* | `+3.339 / -1.383` | `+185 / -5` | `+189.020 / -88.074` *(IFC-Brückenmodell-Bundling)* | Rettung & Korrektur des BIM-Portal-Clients (anonymer öffentlicher Zugriff, `NON_NULL`-Serialisierung, `LenientStringDeserializer`), lauffähige JavaFX-GUI, erstes `ids/`-Prüfpaket, erste Headless-CLI (`LoinCheckCli`), Bundling des TBW1-Modells |
| **Robert Jürgensen** (`@Robert4J`, TU Berlin) | 19.11.2026 – 22.11.2026 | **16** *(Branch `0.3.1` / `webView`)* | `+11.187 / -1.794` | `+1.814 / -205` | `+101 / -59` *(IFC-Testfixtures, Eclipse-Cleanup)* | Kern-Architektur (`CheckService`, `catalogue/`, `report/`, `mcp/`), IDS-1.0- & `ifctester`-Parität, APSTEX-`RelatedObjects`-Bugfix, URL-/GitHub-/LFS-Laden, AIA-Projekt-Prüfung, 217-Test-Suite mit `IfcOpenShell`-Referenz, Java-MCP-Server, `README.md`, `WORKFLOW.md`, `STATUS.md`, GUI-Driver-Skill |

### Modul-Verantwortung auf einen Blick
- **Hackathon-Grundlage, Projektkoordination & Vision:** Maximilian Sternal (mit Team CheckMate Sept. 2025)
- **BIM-Portal-API-Fixes & erster durchgängiger IDS-/GUI-Durchstich (`0.2`):** SOPHIEfree
- **Prüfkern (`ids/`, `catalogue/`, `check/`, `report/`), IFC-Reader-Härtung, Testsuite & MCP-Server (`0.3.1`):** Robert Jürgensen
- **Co-Entwicklung:** `com.bimportal.hackathon.ids/` (begründet durch SOPHIEfree, ausgebaut & gehärtet durch Robert Jürgensen) sowie `test.fx/` (Prototyp Hackathon → lauffähige GUI SOPHIEfree → Architektur-Umbau Robert Jürgensen)

---

## 3. Transparente Arbeits- & Nachweisdokumentation (Ministeriumsanforderungen)

:::caution[In Abstimmung — Entwurf für Version 0.3.1]
Dieser Abschnitt dient als strukturierte Vorlage für die transparente Auflistung und Nachweisführung der eingeflossenen Arbeit gemäß den jeweiligen Anforderungen und Pflichten (z. B. Fördermittel-, Nebentätigkeits-, Urheberrechts- oder Ministeriumsvorgaben). Die konkreten Kennzahlen, Zeitbudgets und rechtlichen Nachweise werden im Team gemeinsam abgestimmt und vor finaler Einreichung ergänzt.
:::

| Arbeitspaket (AP) | Version | Verantwortlich | Status | Ergebnis / Nachweis (Git / Artefakte) | Zeit- / Aufwandsangabe *(abzustimmen)* |
|---|---|---|---|---|---|
| **AP 1:** Hackathon-Konzept, Starterkit-Integration & Erstprototyp | `0.1` | Maximilian Sternal & Hackathon-Team | Abgeschlossen (eingefroren) | Branch `0.1`, Pitch-Deck `CheckMate_Hackathon_BIMPortal_2025.pdf` | *noch abzustimmen* |
| **AP 2:** API-Client-Korrekturen, lauffähige GUI & erster IDS-Check | `0.2` | SOPHIEfree | Abgeschlossen (eingefroren) | Branch `0.2` (Commits `eeb136e`–`bc237bb`) | *noch abzustimmen* |
| **AP 3:** Deterministischer IDS-1.0-Kern, AIA/Katalog, `IfcOpenShell`-Tests & MCP-Server | `0.3.1` | Robert Jürgensen | Aktiv / Abgeschlossen | Branch `0.3.1` (Commits `1236655`–`c42a1b5`, 217 Tests) | *noch abzustimmen* |
| **AP 4:** Zweisprachige Dokumentation (DE/EN), GitHub Pages, CI & Transparenzbericht | `0.3.1` | Maximilian Sternal / Team | In Arbeit | Branch `0.3.1` (`website/`, `.github/workflows/`) | *noch abzustimmen* |
| **AP 5:** APSTEX-freier IFC-Reader & MCP-Upgrade (Nov-2026-Standard) | `0.3.2` | Team | Geplant (Ende Nov. 2026) | Branch `0.3.2` | *noch abzustimmen* |
| **AP 6:** *CheckMate Lite* auf `IFClite`-Basis (ohne Java, 3D-MCP-App, neue Testsuite) | `0.4` | Team | Work in Progress | Branch `0.4` | *noch abzustimmen* |

---

## 4. Offenlegung zu Lizenzen & KI-Werkzeugen

### Lizenztransparenz
- **CheckMate-Quellcode:** [Apache License 2.0](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/LICENSE).
- **APSTEX IFC Toolbox (nur `0.1`–`0.3.1`):** Proprietäre Drittbibliothek, bereitgestellt für den Hackathon — **nicht** Teil der Apache-2.0-Lizenz und **nicht** im Repository enthalten. Entfällt ab `0.3.2` bzw. `0.4`.
- **IFClite (ab `0.4` *CheckMate Lite*):** Open Source unter **Mozilla Public License 2.0 (MPL-2.0)** — kompatibel mit Apache 2.0 auf Dateiebene.
- **IfcOpenShell / ifctester (Test-Referenz):** Open Source (LGPL-3.0), ausschließlich als Test- und Referenzwerkzeug eingesetzt.

### Transparenz zum Einsatz von KI-Werkzeugen in der Entwicklung
Bei der Entwicklung von CheckMate wurden KI-Coding-Assistenten transparent als unterstützende Werkzeuge eingesetzt (u. a. dokumentiert über `Co-Authored-By`-Trailers in der Git-Historie, `.github/copilot-instructions.md` sowie `.claude/skills/run-checkmate/`). Alle fachlichen Architekturentscheidungen, Spezifikations-Abgleiche mit dem BIM-Portal sowie die Verifikation gegen `IfcOpenShell` / `ifctester` erfolgen unter menschlicher Kontrolle und Verantwortung.
