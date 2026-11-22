---
title: Eine Prüfung nachvollziehen
description: Eine Beispielprüfung des Hamburger Brückenmodells mit einer gespeicherten Portal-IDS und 3D-Fast-Treffer-Diagnose.
---

## Ausgangspunkt

Was passiert, wenn ein Brückenmodell andere Merkmalsnamen oder Datentypen als eine Portal-Anforderung verwendet? Das lässt sich mit dem mitgelieferten IFC2X3-Modell **BIM.Hamburg TBW1** (`resources/models/bruecke-tbw1-mit-merkmalen.ifc`) und der gespeicherten Portal-IDS **„Ueberbau > Kappe“** (`resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids`) zeigen.

## 1. Modell und Anforderungen vergleichen

Entweder in der **3D-Browser-Workbench** (`pnpm web:dev` → Button **„🏛️ BIM-Portal LOIN: Kappe LPH 8-9“**) oder auf der Kommandozeile:

```bash
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default
```

Ohne `--model` lädt CheckMate das mitgelieferte Brückenmodell (303 prüfbare Bauteile). Es liest die IDS-Datei, wählt die Spezifikation für `IFC2X3` und vergleicht deren Anwendbarkeitsbedingungen (`applicability`) mit den Modellobjekten. `default` aktiviert das mitgelieferte [Hamburg-Mapping](/CheckMate_BIMPortal/guides/mapping/).

## 2. Ergebnis und Fast-Treffer-Diagnose lesen

```text
Modell: bruecke-tbw1-mit-merkmalen.ifc - Schema IFC2X3, 303 Objekte
Anforderungen: Ueberbau > Kappe
  [nicht prüfbar] Ueberbau > Kappe (IFC2X3, LPH 8: ...): 0 von 303 Elementen anwendbar
   Hinweis: 6 Element(e) mit verwandtem Datentyp (IFCTEXT statt IFCLABEL)
   Diagnose der Anwendbarkeit (Einzelfacetten):
   Facette Klassifizierung/Objekttyp = 'Kappe' (IFCLABEL): 0 von 303 Element(en) [Hinweis: 6 Element(e) mit verwandtem Datentyp (IFCTEXT statt IFCLABEL)]
   Elemente mit den meisten passenden Facetten:
   Fast-Treffer (2/3 Facetten): IfcBuildingElementProxy 'Kappe_Mitte_BW:632563' (1cxNZQHk18xe_eYvhIpXRg) -> Klassifizierung/Objekttyp (als Pset_Objektinformation/_IDEbene3) = 'Kappe' (IFCTEXT)
Ergebnis: 1 Spezifikation(en) für IFC2X3: 1 nicht prüfbar (Exit-Code 4)
```

Das Mapping findet die entsprechenden Modelladressen (`Pset_Objektinformation/_IDEbene3`). Die Portal-IDS verlangt für `Objekttyp = 'Kappe'` jedoch den IFC-Datentyp `IFCLABEL`, während die 6 Kappen im Brückenmodell als `IFCTEXT` exportiert wurden.

- Deshalb trifft formal kein Element auf alle Facetten zu (`0 von 303 Elementen anwendbar - nicht prüfbar`).
- CheckMate lässt es aber nicht bei „0 Treffer“ bewenden: Die **Fast-Treffer-Diagnose** nennt die 6 Kappen (`2/3 Facetten`) samt Ist-Werten und Datentyp-Abweichung. In der 3D-Workbench hebt ein Klick auf einen Fast-Treffer das jeweilige Bauteil sofort gelb im 3D-Brückenmodell hervor.

## 3. Mit und ohne Mapping vergleichen

Wird derselbe Aufruf mit `--mapping none` (bzw. in der Web-Workbench mit **„Keine Aliase (strikt IDS)“**) ausgeführt, meldet die Diagnose, dass `Klassifizierung/Objekttyp: fehlt`. Erst durch das Mapping wird sichtbar, dass die Information im Modell unter `Pset_Objektinformation/_IDEbene3` vorliegt und lediglich der IFC-Datentyp (`IFCTEXT` statt `IFCLABEL`) abweicht.
