---
title: Schnellstart (0.4)
description: Voraussetzungen, Einrichtung und Start von CheckMate 0.4 (TypeScript, IFClite 3D-Workbench, CLI und MCP-Server).
---

Diese Anleitung beschreibt den Entwicklungsstand auf Branch `0.4` (TypeScript & IFClite). Für die reine Java-Edition (`0.3.2`) siehe den [Schnellstart (0.3.2)](/CheckMate_BIMPortal/v0.3.2/guides/quickstart/).

## Voraussetzungen

- **Node.js 22+** (auch Node 24/26 wird unterstützt) und **pnpm** (ab v10/v12).
- Ein moderner Browser mit WebGL/WebAssembly für die 3D-Web-Workbench (`@checkmate/web`).
- Für Live-Abfragen gegen das [BIM-Portal des Bundes](https://via.bund.de/bim): Netzwerkzugriff. Öffentlich sichtbare LOINs und AIA-Projekte können ohne Anmeldung geladen werden; zusätzlich sind im Repository exportierte Portal-IDS-Dateien und das Hamburger Brückenmodell (`bruecke-tbw1-mit-merkmalen.ifc`) für den Offline-Betrieb enthalten.

## Einrichtung und Start

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.4

# Abhängigkeiten für alle Workspace-Pakete installieren
pnpm install

# Alle automatisierten Tests ausführen (Vitest)
pnpm test

# 3D-Browser-Workbench starten (http://localhost:5173/)
pnpm web:dev
```

:::tip[Lokalen Entwicklungsserver sauber beenden]
In dem Terminal-/PowerShell-Fenster, in dem `pnpm web:dev` (Vite) läuft, drückst du **`Strg + C`** (bzw. **`Ctrl + C`**) oder **`q`** und anschließend **`Enter`**. Falls PowerShell fragt `Batchvorgang abbrechen (J/N)?`, mit **`J`** und **`Enter`** bestätigen. Danach ist Port `5173` sofort wieder frei.
:::

## Kommandozeile (`@checkmate/cli`)

Die CLI auf Branch `0.4` unterstützt dieselben Parameter, Berichte und Exit-Codes wie `0.3.2` und erweitert sie um `.rules.json` und BCF-Export (`--bcf`):

```bash
# Mitgeliefertes Brückenmodell gegen eine gespeicherte Portal-IDS prüfen
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default

# Eigenes IFC-Modell gegen eine Portal-LOIN prüfen und JSON + BCF exportieren
pnpm cli -- \
  --model resources/models/bruecke-tbw1-mit-merkmalen.ifc \
  --loin <LOIN-GUID> \
  --json build/report.json \
  --bcf build/issues.bcfzip

# Katalogsuche mit ModelFit-Sortierung für das geladene Modell
pnpm cli -- --suggest "Kappe" --discipline "Ingenieurbau"
```

Ohne `--model` oder die Umgebungsvariable `CHECKMATE_MODEL` wird das mitgelieferte Brückenmodell `resources/models/bruecke-tbw1-mit-merkmalen.ifc` verwendet. Den MCP-Server richtet die [MCP-Anleitung](/CheckMate_BIMPortal/mcp/clients/) ein.

## CLI-Ergebnisse

| Code | Bedeutung |
|---|---|
| `0` | Alle ausgewerteten Spezifikationen erfüllt |
| `1` | Modell oder Anforderungen konnten nicht gelesen oder geprüft werden |
| `2` | Ungültige Kommandozeilenargumente |
| `3` | Mindestens eine Spezifikation nicht erfüllt |
| `4` | Keine Verletzung, aber 0 Elemente anwendbar (`nicht prüfbar`) oder nichts geprüft |
