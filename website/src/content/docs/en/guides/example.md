---
title: Walk Through a Check
description: Compact 3D IFC4 reference models for building and infrastructure (exit code 0) alongside near-miss diagnosis on the Hamburg bridge model (exit code 4).
---

Using three bundled models, you can see within seconds how the German BIM Portal expresses requirements in IDS, how a clean IFC4 model satisfies them directly (`exit code 0`), and how CheckMate diagnoses naming and data-type mismatches on real-world models (`exit code 4`).

---

## Part 1: Compact IFC4 Reference Models for the BIM Portal (`exit code 0`)

Generated reproducibly by [`scripts/generate-mini-models.ts`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/scripts/generate-mini-models.ts), CheckMate ships with two intentionally tiny, human-readable IFC4 models (~130–150 STEP lines with `IfcExtrudedAreaSolid` 3D geometry) verified with IFClite and against the live BIM Portal. Neither model needs property mapping (`--mapping none`).

### Example A – Building Construction (`Hochbau`): `mini-hochbau-bundesbau.ifc` (5 LOINs)

Models a single-storey pavilion (`1 IfcBuilding`, `1 IfcBuildingStorey`, `1 IfcSlab` floor slab, `2 IfcWall` exterior walls, `1 IfcRoof` flat roof). The federal building LOINs (*"Beispiel AIA Bundesbauten"*, LPH 3) select walls, slabs, and roofs via `PredefinedType = USERDEFINED` and require a matching PropertySet (`Wand, massiv`, `Geschossdecken`, `Dach`) carrying every property — including dimensions — as `IFCLABEL`.

**Run the check** (via **`🏢 Mini-Hochbau (5 LOINs)`** in `pnpm web:dev`, offline via `.zip`, or live against the BIM Portal):

```bash
pnpm cli -- --model resources/models/mini-hochbau-bundesbau.ifc \
  --mapping none \
  --loin a3ce341f-d275-4553-911b-00cce2e489c6 \
  --loin c5403b95-3517-4412-b860-66de2d7bafdd \
  --loin c9e1d502-6d9a-4da4-9bd1-1cd251cae3ca \
  --loin e84ce0d9-0ca7-4d11-a5be-eefbfcf8102b \
  --loin 608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e
```

*(Offline alternative: `--ids resources/fixtures/ids/portal/mini-hochbau-bundesbau.zip`)*

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

### Example B – Infrastructure (`Ingenieurbau`): `mini-infrastruktur-bruecke.ifc` (6 LOINs)

Models a single-span bridge (`2 IfcFooting` foundations, `2 IfcColumn` pier shafts, `1 IfcSlab` bridge deck, `2 IfcBeam` copings / *Kappen*). Civil engineering LOINs from *Pflegestelle Straße* select elements via the `Klassifizierung` PropertySet (`Objektgruppe`, `Objektklasse`, `Objekttyp` as `IFCLABEL`) and verify both the permitted IFC entity and phase-specific properties in `Bauteilinformation` (`Fertigteil` as `IFCLABEL`, `Feldnummer` as `IFCINTEGER`).

**Run the check** (via **`🌉 Mini-Brücke (6 LOINs)`** in `pnpm web:dev`, offline via `.zip`, or live against the BIM Portal):

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

*(Offline alternative: `--ids resources/fixtures/ids/portal/mini-infrastruktur-bruecke.zip`)*

```text
Modell: mini-infrastruktur-bruecke.ifc - Schema IFC4, 11 Objekte
...
Anforderungen: Ueberbau > Kappe (Ingenieurbau): LPH 8-9
  [erfüllt] Ueberbau > Kappe (Ingenieurbau): LPH 8-9 (IFC4, LPH 9: Objektbetreuung, LPH 8: Objektüberwachung): 2 von 11 Elementen anwendbar, 2 erfüllt, 0 nicht erfüllt
    alle Anforderungen erfüllt
Ergebnis: 6 Spezifikation(en) für IFC4: 6 erfüllt (Exit-Code 0)
```

Larger modified test models — each provided in a fully passing `_mod_full` variant and a `_mod_partial` variant with intentional deviations — are documented in [`docs/README.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/docs/README.md).

---

## Part 2: Real-World Model with Differing Conventions (`BIM.Hamburg TBW1` — `exit code 4`)

What happens when the same coping LOIN (`be959b9d-daef-5315-9aba-f60b6ad54ccb` / `ueberbau-kappe-lph8-9.ids`) is checked against the unmodified real-world **BIM.Hamburg TBW1** bridge model (`bruecke-tbw1-mit-merkmalen.ifc`, `IFC2X3`)?

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

- The [Hamburg mapping](/CheckMate_BIMPortal/en/v0.4.1-beta/guides/mapping/) bridges the property names (`Pset_Objektinformation/_IDEbene3`), but the 6 copings were exported as `IFCTEXT` rather than `IFCLABEL`.
- CheckMate never treats `0` applicable elements as a silent pass: it reports **`nicht prüfbar` (`exit code 4`)** together with a detailed **near-miss diagnosis** and highlights the elements in 3D when clicked.
