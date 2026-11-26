---
title: Schnellstart (0.4.1-beta)
description: Installation, CLI-Einrichtung, 3D-Browser-Workbench und erste Prüfungen mit CheckMate 0.4.1-beta.
---

Diese Anleitung beschreibt **CheckMate `0.4.1-beta`** (TypeScript & IFClite). Für das aktuelle stabile Haupt-Release der Java-25-Edition (**`0.3.3-stable`**, `Latest`) siehe den [Schnellstart (0.3.3-stable)](/CheckMate_BIMPortal/guides/quickstart/).

## Voraussetzungen

- **Node.js 22+** (auch Node 24/26 wird unterstützt) und **pnpm** (ab v10/v12).
- Ein moderner Browser mit WebGL/WebAssembly für die 3D-Browser-Workbench (`@checkmate/web`).
- Für Live-Abfragen gegen das [BIM-Portal des Bundes](https://via.bund.de/bim): Netzwerkzugriff. Öffentlich sichtbare LOINs und AIA-Projekte benötigen keine Anmeldung; zusätzlich liegen exportierte Portal-IDS-Archive sowie die Hochbau-, Infrastruktur- und Hamburger Brückenmodelle für den Offline-Betrieb im Repository bereit.

## 1. Repository einrichten & testen

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.4.1-beta

# Abhängigkeiten installieren
pnpm install

# Alle automatisierten Tests & Typprüfungen ausführen
pnpm test
pnpm build
```

## 2. Kommandozeile installieren & nutzen (`@checkmate/cli`)

Die CLI kann direkt über `pnpm cli -- ...` im Projektordner aufgerufen oder systemweit als Befehl `checkmate` verlinkt werden:

```bash
# Optional: Befehl `checkmate` global im Terminal verfügbar machen
pnpm --filter @checkmate/cli link --global
checkmate --help
```

### Erste Prüfungen ausführen

```bash
# 1) Kompaktes Hochbau-Beispiel (5 BIM-Portal-LOINs „AIA Bundesbauten“ -> Exit-Code 0)
pnpm cli -- --model resources/models/mini-hochbau-bundesbau.ifc \
  --ids resources/fixtures/ids/portal/mini-hochbau-bundesbau.zip --mapping none

# 2) Kompaktes Infrastruktur-Beispiel (6 BIM-Portal-LOINs „Ingenieurbau“ -> Exit-Code 0)
pnpm cli -- --model resources/models/mini-infrastruktur-bruecke.ifc \
  --ids resources/fixtures/ids/portal/mini-infrastruktur-bruecke.zip --mapping none

# 3) Live gegen eine veröffentlichte LOIN-GUID des BIM-Portals prüfen
pnpm cli -- --model resources/models/mini-infrastruktur-bruecke.ifc \
  --loin be959b9d-daef-5315-9aba-f60b6ad54ccb --mapping none

# 4) Hamburger Brückenmodell mit Property Mapping, JSON-Bericht & BCF-Export
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids \
  --mapping default --json build/report.json

# 5) Passende LOINs im BIM-Portal suchen und nach ModelFit sortieren
pnpm cli -- --suggest "Kappe" --discipline "Ingenieurbau"
```

Ohne `--model` (oder `CHECKMATE_MODEL`) lädt CheckMate das mitgelieferte Brückenmodell `resources/models/bruecke-tbw1-mit-merkmalen.ifc`.

| Exit-Code | Bedeutung |
|---|---|
| `0` | Alle ausgewerteten Spezifikationen erfüllt |
| `1` | Modell oder Anforderungen konnten nicht gelesen werden |
| `2` | Ungültige Kommandozeilenargumente |
| `3` | Mindestens eine Spezifikation nicht erfüllt |
| `4` | Keine Verletzung, aber `0` Elemente anwendbar (`nicht prüfbar`) oder nichts geprüft |

## 3. 3D-Browser-Workbench starten (`@checkmate/web`)

```bash
# Entwicklungsserver starten (http://localhost:5173/)
pnpm web:dev

# Optional: statisches Produktions-Bundle nach packages/web/dist/ bauen
pnpm web:build
```

Über die Kopfleiste der 3D-Workbench lassen sich sofort testen:
- **`🏢 Mini-Hochbau (5 LOINs)`**: Lädt den eingeschossigen IFC4-Pavillon in 3D und prüft alle 5 Bundesbauten-LOINs (alle Bauteile grün, `Exit-Code 0`).
- **`🌉 Mini-Brücke (6 LOINs)`**: Lädt die IFC4-Einfeldbrücke in 3D und prüft 6 Ingenieurbau-LOINs (alle Bauteile grün, `Exit-Code 0`).
- **`⚡ TBW1: Kappen-Fehler`** & **`🏛️ TBW1: Portal Kappe`**: Zeigen auf dem Hamburger Brückenmodell rot markierte Abweichungen bzw. die gelbe Fast-Treffer-Diagnose.

:::tip[Lokalen Entwicklungsserver sauber beenden]
Im Terminal, in dem `pnpm web:dev` läuft, **`Strg + C`** (bzw. **`Ctrl + C`**) oder **`q`** + **`Enter`** drücken.
:::

Die Einrichtung des MCP-Servers für **Antigravity** und **Claude Desktop** steht unter [MCP-Client einrichten](/CheckMate_BIMPortal/v0.4.1-beta/mcp/clients/).
