---
title: MCP-Server & MCP Apps
description: CheckMate über das Model Context Protocol (MCP) an KI-Clients anbinden — Stand 0.3.1 und Ausbau auf MCP-Standards November 2026 & Antigravity MCP Apps.
---

## Grundprinzip: KI erklärt, CheckMate prüft deterministisch

Über das **Model Context Protocol (MCP)** können KI-Anwendungen CheckMate als Werkzeug aufrufen — etwa mit Fragen wie *„Welche LOINs passen zu dieser Brücke?“*, *„Erfüllt das Modell die LOIN für Kappen in LPH 8?“* oder *„Warum fällt Bauteil X durch?“*.

Dabei gilt eine klare Sicherheits- und Qualitätsgrenze:
- **Die KI sieht niemals die rohe IFC-Datei**, sondern nur strukturierte Werkzeugantworten von CheckMate.
- **Die Prüfung selbst bleibt 100 % deterministisch** im CheckMate-Kern — keine Halluzinationen bei Norm- und LOIN-Urteilen.
- **Der KI-Anbieter bleibt austauschbar:** Ob Antigravity, Claude, OpenCode, ein offenes Cloud-Modell (z. B. Mistral über Ollama Cloud) oder ein rein lokales Qwen-Modell über Ollama oder Strata.

---

## Die 7 MCP-Tools in `0.3.1`

Implementiert in `CheckMateMcpServer` und `CheckMateTools` auf Basis des offiziellen **MCP Java SDK (`2.1.0`)** über **`stdio`** (`./gradlew :hackathon-template:installMcp`):

| Tool | Aufgabe | Wichtigste Parameter |
|---|---|---|
| `load_model` | Lädt ein IFC-/ifcZIP-Modell per lokalem Pfad oder HTTP(S)-/GitHub-Link | `source` *(Pflicht)* |
| `describe_model` | Liefert Modellname, IFC-Schema, Objektanzahl und die 15 häufigsten Entities | — |
| `list_elements` | Listet Bauteile als `GlobalId \| Entity \| Name \| PredefinedType` | `entity`, `name`, `limit` (30) |
| `find_loins` | Sucht AIA-Projekte & LOINs im BIM-Portal, sortiert nach Modell-Passung (ohne IDS-Download) | `search`, `discipline`, `phase`, `purpose`, `limit` (20), `include_unfitting` |
| `describe_requirements` | Erklärt Anforderungen einer LOIN / eines AIA-Projekts inkl. anwendbarer Bauteilzahl | `id` *(Pflicht, GUID oder Portal-Link)* |
| `check_model` | Führt deterministische Prüfung aus und merkt sich das Ergebnis für `element_report` | `ids`, `ids_files`, `examples` (3; `0` = Vollbericht) |
| `element_report` | Zeigt Prüfbefunde des letzten Laufs sowie Merkmale eines konkreten Bauteils | `element` *(GlobalId oder Namensteil)* |

**Schutzmechanismen in `0.3.1`:**
- Alle Logs gehen auf `stderr`; `stdout` führt ausschließlich JSON-RPC.
- Antworten werden sauber an Zeilenumbrüchen auf maximal **50.000 Zeichen** (~15.000 Tokens) gekürzt, wobei die finale Ergebniszeile stets erhalten bleibt.
- Fehler (fehlende Datei, Portal nicht erreichbar) werden als `isError: true` sauber an die KI zurückgegeben statt den Prozess abstürzen zu lassen.

---

## Ausbau auf MCP-Standards (November 2026) & Antigravity MCP App

Der aktuelle `0.3.1`-Server nutzt `stdio` und textbasierte Antworten. Für **`0.3.2`** und **`0.4` (*CheckMate Lite*)** ist der Ausbau auf die aktuellen **MCP-Spezifikationen (Stand November 2026)** geplant:

### 1. Core-Spezifikation `2026-07-28` (`0.3.2` & `0.4`)
- **Stateless Streamable HTTP (`/mcp`):** Ermöglicht Container-, Behörden-Server- und Gateway-Betrieb ohne langlebige lokale `stdio`-Prozesse; explizite Modell-/Report-Handles statt implizitem Prozess-State.
- **Tool Annotations (`readOnlyHint`, `idempotentHint`, `openWorldHint`):** Markiert rein lesende Prüf- und Such-Tools sauber, sodass Hosts wie Antigravity oder OpenCode nicht bei jedem Abruf unnötig nach Bestätigung fragen müssen.
- **Structured Outputs (`outputSchema` + `structuredContent`):** Liefert neben dem lesbaren deutschen Textbericht das maschinenlesbare JSON aus `ReportJson` — besonders wertvoll für kompakte lokale Modelle (Qwen, Mistral), die so GUIDs und Statuswerte fehlerfrei weiterverarbeiten können.
- **Progress Notifications & Prompts:** Fortschrittsmeldungen bei 7-Sekunden-Katalogabrufen oder 36-LOIN-AIA-Prüfungen sowie vordefinierte Prüf-Prompts.

### 2. MCP Apps Extension (`io.modelcontextprotocol/ui`) — Ziel: Antigravity MCP App
Mit der offiziellen **MCP-Apps-Erweiterung** (`@modelcontextprotocol/ext-apps`) liefert ein MCP-Server interaktive, sandboxed HTML-Oberflächen (`ui://`-Ressourcen, gekoppelt über `_meta.ui.resourceUri`) direkt in Hosts wie **Antigravity**:
- **`ui://checkmate/report-app`:** Interaktive Ampel-Prüftabelle (erfüllt / nicht erfüllt / nicht prüfbar) mit Filter nach Leistungsphase & Gewerk; ein Klick auf ein Bauteil ruft per `postMessage` JSON-RPC direkt `element_report` auf.
- **`ui://checkmate/catalogue-app`:** Visuelle Auswahl passender LOINs aus dem BIM-Portal-Katalog.
- **In `0.4` (*CheckMate Lite*):** Direkte Integration des **IFClite WebGPU 3D-Viewers** in die MCP App — fehlerhafte Bauteile werden im 3D-Modell rot hervorgehoben und lassen sich direkt anklicken.
