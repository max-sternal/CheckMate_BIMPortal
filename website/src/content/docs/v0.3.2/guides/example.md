---
editUrl: false
title: Eine Prüfung nachvollziehen
description: Eine Beispielprüfung des Hamburger Brückenmodells mit einer gespeicherten Portal-IDS.
slug: v0.3.2/guides/example
---

## Ausgangspunkt

Was passiert, wenn ein Brückenmodell andere Merkmalsnamen als eine Anforderung verwendet? Das lässt sich mit dem mitgelieferten IFC2X3-Modell **BIM.Hamburg TBW1** und der gespeicherten Portal-IDS **„Ueberbau > Kappe“** zeigen.

Die IDS-Datei liegt unter `hackathon-template/src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids`. Sie wurde aus dem BIM-Portal exportiert; Herkunft und Version stehen im [Fixture-Verzeichnis](https://github.com/max-sternal/CheckMate_BIMPortal/v0.3.2/blob/0.3.2/bim-portal/Java_client/hackathon-template/src/test/resources/ids/portal/README.md). Die Prüfung verwendet diese lokale Datei und benötigt keinen Portalzugriff.

## 1. Modell und Anforderungen vergleichen

Im Verzeichnis `bim-portal/Java_client/`:

```bash
./gradlew :hackathon-template:runLoinCheck --args="--ids src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default"
```

Ohne `--model` und ohne gesetztes `CHECKMATE_IFC` lädt CheckMate das mitgelieferte Brückenmodell. Es liest die IDS-Datei, wählt die Spezifikation für IFC2X3 und vergleicht deren Anwendbarkeitsbedingungen mit den Modellobjekten. `default` aktiviert das mitgelieferte [Hamburg-Mapping](/CheckMate_BIMPortal/v0.3.2/guides/mapping/).

## 2. Ergebnis lesen

Ein gekürzter Auszug eines Prüflaufs:

```text
Modell: bruecke-tbw1-mit-merkmalen.ifc - Schema IFC2X3, 303 Objekte
Ueberbau > Kappe ...: 0 von 303 Elementen anwendbar - nicht prüfbar
...
Klassifizierung/Objektgruppe: 'Kappe' statt 'Ueberbau',
  Datentyp IFCTEXT statt IFCLABEL (als Pset_Objektinformation/_IDEbene1)
...
Ergebnis: 1 Spezifikation(en) für IFC2X3: 1 nicht prüfbar (Exit-Code 4)
```

Das Mapping findet die entsprechenden Modelladressen. Die Anforderungen unterscheiden sich hier aber weiterhin in Wert und Datentyp: `Kappe` statt `Ueberbau`, `IFCTEXT` statt `IFCLABEL`. Deshalb wird kein Element als anwendbar bestätigt. **„Nicht prüfbar“ bedeutet hier weder bestanden noch ein Gesamturteil über das Modell.**

## 3. Ohne Mapping vergleichen

Wird derselbe Aufruf mit `--mapping none` ausgeführt, meldet der Bericht für die drei Klassifizierungsmerkmale, dass sie unter den angeforderten Namen im Modell nicht vorhanden sind. Mit Mapping liefert er zusätzliche Hinweise auf die anders benannten Merkmale. Ein Ergebnis hängt somit von Modell, Anforderungen und gewähltem Mapping ab.

Gradle meldet bei Exit-Code `4` die Aufgabe als fehlgeschlagen; das ist hier das Prüfungsergebnis und kein Hinweis auf einen fehlgeschlagenen Compile-Schritt. Für ein anderes Projekt müssen Modell, Anforderungen und Mapping fachlich zusammenpassen.
