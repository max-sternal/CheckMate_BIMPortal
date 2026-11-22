---
title: Beispiele & Mapping
description: Merkmale des Hamburger Brückenmodells und projektspezifische JSON-Zuordnungen in CheckMate 0.4 und 0.3.2.
---

## Warum ein Mapping?

Eine IDS-Anforderung benennt ein Merkmal durch PropertySet und Merkmalsname. Ein Modell kann fachlich ähnliche Informationen unter anderen Namen führen. Ein Mapping erlaubt CheckMate, dafür zusätzlich vereinbarte Adressen im Modell zu suchen – es ändert weder das Modell noch die IDS-Datei.

Das mitgelieferte BIM.Hamburg-Brückenmodell verwendet diese Zuordnung:

| Anforderung (BIM-Portal IDS) | Adresse im Modell |
|---|---|
| `Klassifizierung/Objektgruppe` | `Pset_Objektinformation/_IDEbene1` |
| `Klassifizierung/Objektklasse` | `Pset_Objektinformation/_IDEbene2` |
| `Klassifizierung/Objekttyp` | `Pset_Objektinformation/_IDEbene3` |

Die Datei [`resources/mapping/bim-hamburg.json`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4/resources/mapping/bim-hamburg.json) ist standardmäßig aktiv. Zuerst wird immer der in der Anforderung genannte Name gesucht; erst danach kommen die Mapping-Adressen über den `MappedDataAccessor` zum Einsatz. Verwendete Aliase erscheinen im Bericht transparent als `(als Pset_Objektinformation/...)`.

Ein Mapping ist **keine allgemeine IDS-Regel**. Es muss fachlich zum jeweiligen Projekt und Modell passen. Es gleicht Namen zu, korrigiert aber keine Werte oder Datentypen.

## Mapping wählen (Web-Workbench, CLI & MCP)

### 1. In der 3D-Browser-Workbench (`@checkmate/web`)
Im Reiter **„4. Mapping, Rules & Export“** kann das aktive JSON-Mapping direkt im Editor eingesehen, zwischen `BIM.Hamburg Brücken` und `Keine Aliase (strikt IDS)` umgeschaltet oder als eigene `.json`-Datei geladen werden. Ein Klick auf **„Mapping anwenden & neu prüfen“** aktualisiert sofort den Prüfbericht und die 3D-Färbung.

### 2. Über die Kommandozeile (`@checkmate/cli`)

```bash
# Mitgeliefertes BIM.Hamburg-Mapping verwenden
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default

# Dieselbe Prüfung ohne Aliase (strikt IDS)
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping none

# Eigenes Mapping verwenden
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping /pfad/zum/mapping.json
```

### 3. Aufbau einer Mapping-JSON-Datei

```json
{
  "name": "Projektbeispiel",
  "aliases": [
    {
      "from": "Klassifizierung/Objekttyp",
      "to": ["Pset_Objektinformation/_IDEbene3"]
    }
  ]
}
```

`from` bezeichnet die angeforderte Adresse, `to` eine Liste möglicher Modelladressen in Suchreihenfolge (`PropertySet/Name`). Über das MCP-Werkzeug `load_mapping` lässt sich das Mapping ebenfalls anzeigen oder wechseln.

[Zum durchgerechneten Beispiel](/CheckMate_BIMPortal/guides/example/)
