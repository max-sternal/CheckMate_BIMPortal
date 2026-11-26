---
title: Eine Prüfung nachvollziehen
description: Kompakte 3D-IFC4-Beispiele für Hochbau und Infrastruktur (Exit-Code 0) sowie Fast-Treffer-Diagnose am Hamburger Brückenmodell (Exit-Code 4).
---

Anhand von drei mitgelieferten Modellen lässt sich in wenigen Sekunden nachvollziehen, wie das BIM-Portal Anforderungen in IDS formuliert, wann ein IFC4-Modell sie direkt erfüllt (`Exit-Code 0`) und wie CheckMate bei abweichenden Praxismodellen die Ursache diagnostiziert (`Exit-Code 4`).

---

## Teil 1: Kompakte IFC4-Referenzmodelle für das BIM-Portal (`Exit-Code 0`)

Über das Skript [`scripts/generate-mini-models.ts`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/scripts/generate-mini-models.ts) wurden zwei bewusst sehr kleine, im Texteditor vollständig überblickbare IFC4-Modelle (~130–150 STEP-Zeilen mit `IfcExtrudedAreaSolid`-3D-Geometrie) aufgebaut und mit IFClite sowie gegen das Live-BIM-Portal verifiziert. Beide kommen **ohne Property Mapping (`--mapping none`)** aus.

### Beispiel A – Hochbau: `mini-hochbau-bundesbau.ifc` (5 LOINs „Beispiel AIA Bundesbauten“)

Das Modell bildet einen eingeschossigen Pavillon (`1 IfcBuilding`, `1 IfcBuildingStorey`, `1 IfcSlab` Bodenplatte, `2 IfcWall` Außenwände, `1 IfcRoof` Flachdach) ab. Die Bundesbauten-LOINs (LPH 3) wählen Wand, Decke und Dach über `PredefinedType = USERDEFINED` aus und verlangen je Bauteilklasse ein gleichnamiges PropertySet (z. B. `Wand, massiv`, `Geschossdecken`, `Dach`) mit sämtlichen Merkmalen – einschließlich der Abmessungen – als `IFCLABEL`.

**Prüfung starten** (in `pnpm web:dev` über **`🏢 Mini-Hochbau (5 LOINs)`**, offline per `.zip` oder live gegen das BIM-Portal):

```bash
pnpm cli -- --model resources/models/mini-hochbau-bundesbau.ifc \
  --mapping none \
  --loin a3ce341f-d275-4553-911b-00cce2e489c6 \
  --loin c5403b95-3517-4412-b860-66de2d7bafdd \
  --loin c9e1d502-6d9a-4da4-9bd1-1cd251cae3ca \
  --loin e84ce0d9-0ca7-4d11-a5be-eefbfcf8102b \
  --loin 608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e
```

*(Offline-Alternative ohne Netzwerkzugriff: `--ids resources/fixtures/ids/portal/mini-hochbau-bundesbau.zip`)*

```text
Modell: mini-hochbau-bundesbau.ifc - Schema IFC4, 8 Objekte

Anforderungen: Gebäude
  [erfüllt] Gebäude (IFC4, LPH 3: Entwurfsplanung): 1 von 8 Elementen anwendbar, 1 erfüllt, 0 nicht erfüllt
    alle Anforderungen erfüllt
Anforderungen: Geschosse
  [erfüllt] Geschosse (IFC4, LPH 3: Entwurfsplanung): 1 von 8 Elementen anwendbar, 1 erfüllt, 0 nicht erfüllt
    alle Anforderungen erfüllt
Anforderungen: Geschossdecken
  [erfüllt] Geschossdecken (IFC4, LPH 3: Entwurfsplanung): 1 von 8 Elementen anwendbar, 1 erfüllt, 0 nicht erfüllt
    alle Anforderungen erfüllt
Anforderungen: Wand, massiv
  [erfüllt] Wand, massiv (IFC4, LPH 3: Entwurfsplanung): 2 von 8 Elementen anwendbar, 2 erfüllt, 0 nicht erfüllt
    alle Anforderungen erfüllt
Anforderungen: Dach
  [erfüllt] Dach (IFC4, LPH 3: Entwurfsplanung): 1 von 8 Elementen anwendbar, 1 erfüllt, 0 nicht erfüllt
    alle Anforderungen erfüllt
Ergebnis: 5 Spezifikation(en) für IFC4: 5 erfüllt (Exit-Code 0)
```

### Beispiel B – Infrastruktur: `mini-infrastruktur-bruecke.ifc` (6 LOINs „Ingenieurbau“)

Das Brückenmodell besteht aus `2 IfcFooting` (*Einzelfundament West/Ost*), `2 IfcColumn` (*Pfeilerschaft West/Ost*), `1 IfcSlab` (*Fahrbahnplatte Feld 1*) und `2 IfcBeam` (*Kappe Nord/Süd*). Anders als im Hochbau wählen die Ingenieurbau-LOINs der *Pflegestelle Straße* Bauteile über das PropertySet `Klassifizierung` (`Objektgruppe`, `Objektklasse`, `Objekttyp` als `IFCLABEL`) aus und prüfen daraufhin die zulässige IFC-Entity sowie phasenabhängige Merkmale in `Bauteilinformation` (`Fertigteil` als `IFCLABEL`, `Feldnummer` als `IFCINTEGER`).

**Prüfung starten** (in `pnpm web:dev` über **`🌉 Mini-Brücke (6 LOINs)`**, offline per `.zip` oder live gegen das BIM-Portal):

```bash
pnpm cli -- --model resources/models/mini-infrastruktur-bruecke.ifc \
  --mapping none \
  --loin 5d35c1cf-4c24-560f-ad17-506851e78011 \
  --loin 0785d1cb-857b-553d-9b1b-a28a2636e063 \
  --loin f5884e9e-f149-57cb-aa08-3b1462626e30 \
  --loin 81586d48-c9eb-5ec7-88b6-b2259a6352f9 \
  --loin d4705d1e-d5b2-5728-8f15-8de87b5d626e \
  --loin be959b9d-daef-5315-9aba-f60b6ad54ccb
```

*(Offline-Alternative: `--ids resources/fixtures/ids/portal/mini-infrastruktur-bruecke.zip`)*

```text
Modell: mini-infrastruktur-bruecke.ifc - Schema IFC4, 11 Objekte
...
Anforderungen: Ueberbau > Kappe (Ingenieurbau): LPH 8-9
  [erfüllt] Ueberbau > Kappe (Ingenieurbau): LPH 8-9 (IFC4, LPH 9: Objektbetreuung, LPH 8: Objektüberwachung): 2 von 11 Elementen anwendbar, 2 erfüllt, 0 nicht erfüllt
    alle Anforderungen erfüllt
Ergebnis: 6 Spezifikation(en) für IFC4: 6 erfüllt (Exit-Code 0)
```

Weitere umfangreichere Testmodelle – jeweils als vollständig erfüllte `_mod_full`- und als fehlerbehaftete `_mod_partial`-Variante – sind in [`docs/README.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/docs/README.md) dokumentiert.

---

## Teil 2: Praxismodell mit abweichenden Konventionen (`BIM.Hamburg TBW1` — `Exit-Code 4`)

Was passiert, wenn dieselbe Kappen-LOIN (`be959b9d-daef-5315-9aba-f60b6ad54ccb` bzw. `ueberbau-kappe-lph8-9.ids`) auf das unveränderte Praxismodell **BIM.Hamburg TBW1** (`bruecke-tbw1-mit-merkmalen.ifc`, `IFC2X3`) angewendet wird?

```bash
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default
```

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

- Das [Hamburg-Mapping](/CheckMate_BIMPortal/v0.4.1-beta/guides/mapping/) übersetzt die Merkmalsadresse (`Pset_Objektinformation/_IDEbene3`), doch die 6 Kappen wurden im Bestandsmodell als `IFCTEXT` statt `IFCLABEL` exportiert.
- CheckMate wertet `0` anwendbare Bauteile niemals fälschlich als „bestanden“, sondern gibt **`nicht prüfbar` (`Exit-Code 4`)** samt konkreter **Fast-Treffer-Diagnose** aus – und hebt die betroffenen Kappen in der 3D-Workbench auf Klick direkt hervor.
