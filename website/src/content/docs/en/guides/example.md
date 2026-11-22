---
title: Walk through a check
description: A step-by-step check of the Hamburg bridge model with a saved portal IDS and 3D near-miss diagnosis.
---

## Starting point

What happens when a bridge model uses different property names or data types than a portal requirement? We can see this using the bundled IFC2X3 model **BIM.Hamburg TBW1** (`resources/models/bruecke-tbw1-mit-merkmalen.ifc`) and the saved portal IDS **"Ueberbau > Kappe"** (`resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids`).

## 1. Compare model and requirements

Run the check either in the **3D browser workbench** (`pnpm web:dev` → button **"🏛️ BIM-Portal LOIN: Kappe LPH 8-9"**) or on the command line:

```bash
pnpm cli -- --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default
```

Without `--model`, CheckMate loads the bundled bridge model (303 queryable elements). It parses the IDS file, selects the specification for `IFC2X3` and evaluates its applicability facets against the model elements. `default` enables the bundled [Hamburg mapping](/CheckMate_BIMPortal/en/guides/mapping/).

## 2. Read the result and near-miss diagnosis

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

The mapping resolves the model address (`Pset_Objektinformation/_IDEbene3`). However, the portal IDS requires the IFC data type `IFCLABEL` for `Objekttyp = 'Kappe'`, whereas the 6 bridge copings in the model were exported as `IFCTEXT`.

- Formally, zero elements satisfy all applicability facets (`0 von 303 Elementen anwendbar - nicht prüfbar`).
- Instead of stopping at "0 matches", CheckMate's **near-miss diagnosis** lists the 6 copings (`2/3 Facetten`) along with their actual values and data type mismatch. In the 3D workbench, clicking any near-miss immediately highlights the element in yellow on the 3D bridge.

## 3. Compare with and without mapping

Running the same check with `--mapping none` (or **"Keine Aliase (strikt IDS)"** in the web workbench) reports `Klassifizierung/Objekttyp: fehlt`. With the mapping enabled, CheckMate reveals that the property is present under `Pset_Objektinformation/_IDEbene3` and only differs in IFC data type (`IFCTEXT` vs `IFCLABEL`).
