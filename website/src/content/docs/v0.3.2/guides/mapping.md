---
editUrl: false
title: Beispiele & Mapping
description: Merkmale des Hamburger Brückenmodells und projektspezifische Zuordnungen in CheckMate 0.3.2.
slug: v0.3.2/guides/mapping
---

## Warum ein Mapping?

Eine IDS-Anforderung benennt ein Merkmal durch PropertySet und Merkmalsname. Ein Modell kann fachlich ähnliche Informationen unter anderen Namen führen. Ein Mapping erlaubt CheckMate, dafür zusätzlich vereinbarte Adressen im Modell zu suchen – es ändert weder das Modell noch die IDS-Datei.

Das mitgelieferte BIM.Hamburg-Brückenmodell verwendet diese Zuordnung:

| Anforderung | Adresse im Modell |
|---|---|
| `Klassifizierung/Objektgruppe` | `Pset_Objektinformation/_IDEbene1` |
| `Klassifizierung/Objektklasse` | `Pset_Objektinformation/_IDEbene2` |
| `Klassifizierung/Objekttyp` | `Pset_Objektinformation/_IDEbene3` |

Die Datei [`bim-hamburg.json`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/bim-portal/Java_client/hackathon-template/src/main/resources/mapping/bim-hamburg.json) ist in `0.3.2` standardmäßig aktiv. Zuerst wird immer der in der Anforderung genannte Name gesucht; erst danach kommen die Mapping-Adressen zum Einsatz. Verwendete Aliase erscheinen im Bericht als `als Pset_Objektinformation/...`.

Ein Mapping ist **keine allgemeine IDS-Regel**. Es muss fachlich zum jeweiligen Projekt und Modell passen. Es gleicht Namen zu, aber korrigiert keine Werte oder Datentypen. Eine gefundene Adresse bedeutet daher nicht automatisch, dass eine Anforderung erfüllt ist.

## Mapping wählen

Im Verzeichnis `bim-portal/Java_client/`:

```bash
# Mitgeliefertes Mapping verwenden
./gradlew :hackathon-template:runLoinCheck --args="--ids src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default"

# Dieselbe Prüfung ohne Aliase
./gradlew :hackathon-template:runLoinCheck --args="--ids src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids --mapping none"

# Eigenes Mapping verwenden: absoluten Dateipfad einsetzen
./gradlew :hackathon-template:runLoinCheck --args="--ids src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids --mapping /pfad/zum/mapping.json"
```

Unter Windows `.\gradlew.bat` verwenden und Pfade anpassen. Die Prüfung kann einen von null verschiedenen Exit-Code liefern; Gradle meldet dies als fehlgeschlagene Aufgabe. Der genaue Prüfstatus steht im Bericht.

Für andere Modelle kann eine eigene JSON-Datei angegeben werden:

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

`from` bezeichnet die angeforderte Adresse, `to` eine Liste möglicher Modelladressen in Suchreihenfolge. Jede Adresse hat die Form `PropertySet/Name`. Über das MCP-Werkzeug `load_mapping` lässt sich das Mapping ebenfalls anzeigen oder wechseln; ein Wechsel verwirft den letzten Prüfbericht.

[Zum durchgerechneten Beispiel](/CheckMate_BIMPortal/v0.3.2/guides/example/)
