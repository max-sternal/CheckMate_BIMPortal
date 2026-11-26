# IFC-Beispiel- und Testmodelle für CheckMate & das BIM-Portal

Dieses Repository enthält **zwei Gruppen von IFC-Testmodellen**, die gezielt auf veröffentlichte LOINs des [BIM-Portals des Bundes](https://via.bund.de/bim) abgestimmt sind:

1. **Kompakte, leicht lesbare IFC4-Referenzmodelle** (~130–150 STEP-Zeilen mit 3D-SweptSolid-Geometrie, reproduzierbar erzeugt über `scripts/generate-mini-models.ts`):
   - **Hochbau**: `mini-hochbau-bundesbau.ifc` (5 LOINs aus *„Beispiel AIA Bundesbauten“*, LPH 3 — 5/5 erfüllt, `Exit-Code 0`)
   - **Infrastruktur**: `mini-infrastruktur-bruecke.ifc` (6 LOINs aus *„Pflegestelle Straße / Ingenieurbau“* — 6/6 erfüllt, `Exit-Code 0`)
2. **Erweiterte modifizierte Praxis- und buildingSMART-Testmodelle** in `docs/` (jeweils in einer vollständig erfüllten `_mod_full`-Variante und einer `_mod_partial`-Variante mit gezielten Abweichungen und Fast-Treffern):
   - `Infra-Bridge_mod_full.ifc` / `Infra-Bridge_mod_partial.ifc` (IFC4, 13 LOINs Ingenieurbau)
   - `Building-Architecture_mod_full.ifc` / `Building-Architecture_mod_partial.ifc` (IFC4, 7 LOINs Bundesbauten)
   - `bruecke-tbw1-mit-merkmalen_mod_full.ifc` / `bruecke-tbw1-mit-merkmalen_mod_partial.ifc` (IFC2X3, Hamburger Brücke TBW1 mit direkten BIM-Portal-Merkmalen)

---

## 1. Kompakte IFC4-Referenzmodelle (Hochbau & Infrastruktur)

Auf `0.4.1-beta` liegen diese beiden Modelle unter `resources/models/` (sowie in `docs/` auf `0.3.3-stable`). Sie sind bewusst so klein gehalten (~7–9 KB, keine unleserlichen Dreiecksnetze, sondern klare `IFCRECTANGLEPROFILEDEF`- + `IFCEXTRUDEDAREASOLID`-Körper), dass man jede Zeile im Texteditor lesen und das Modell gleichzeitig in 3D betrachten kann. Beide kommen **ohne Property Mapping (`--mapping none`)** aus.

### 1.1 Hochbau: `mini-hochbau-bundesbau.ifc` (151 Zeilen, Schema `IFC4`)

Ein eingeschossiger Pavillon (`1 IfcBuilding`, `1 IfcBuildingStorey`, `1 IfcSlab`, `2 IfcWall`, `1 IfcRoof`), der 5 LOINs aus **„Beispiel AIA Bundesbauten“ (LPH 3)** erfüllt:

| LOIN | BIM-Portal-GUID | Bauteile im Modell | Geforderte Merkmale (Pset = LOIN-Name, alle `IFCLABEL`) |
|---|---|---|---|
| `Gebäude` | `a3ce341f-d275-4553-911b-00cce2e489c6` | `1 × IfcBuilding` (*Pavillon Haus A*) | `Gebäude-Bezeichnung`, `Gebäude-ID`, `Bauwerkstyp`, `Gebäudeteil-ID`, `Gebäude-Nummer` |
| `Geschosse` | `c5403b95-3517-4412-b860-66de2d7bafdd` | `1 × IfcBuildingStorey` (*00 Erdgeschoss*) | `Geschoss-Codierung`, `Geschoss-ID` |
| `Geschossdecken` | `c9e1d502-6d9a-4da4-9bd1-1cd251cae3ca` | `1 × IfcSlab` (`USERDEFINED`, 6,0 × 5,0 × 0,25 m) | `Name`, `Bauteiltyp`, `Material`, `Funktion, Lage`, `Tragend`, `Gebäude-ID`, `Geschoss-ID`, `Länge (m)`, `Breite (m)`, `Höhe (m)`, `Fläche`, `Volumen` |
| `Wand, massiv` | `e84ce0d9-0ca7-4d11-a5be-eefbfcf8102b` | `2 × IfcWall` (`USERDEFINED`, je 6,0 × 0,30 × 3,0 m) | `Name`, `Bauteiltyp`, `Material`, `Gebäude-ID`, `Geschoss-ID`, `Länge (m)`, `Tiefe (m)`, `Höhe (m)`, `Fläche`, `Volumen` |
| `Dach` | `608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e` | `1 × IfcRoof` (`USERDEFINED`, 6,4 × 5,4 × 0,25 m) | `Name`, `Bauteiltyp`, `Material`, `Gebäude-ID`, `Geschoss-ID`, `Länge (m)`, `Breite (m)`, `Tiefe (m)`, `Fläche`, `Volumen` |

### 1.2 Infrastruktur: `mini-infrastruktur-bruecke.ifc` (128 Zeilen, Schema `IFC4`)

Eine Einfeldbrücke (`2 IfcFooting`, `2 IfcColumn`, `1 IfcSlab`, `2 IfcBeam`), die 6 LOINs der **Pflegestelle Straße (Ingenieurbau)** erfüllt:

| LOIN | BIM-Portal-GUID | Bauteile im Modell | Geforderte PropertySets & Datentypen |
|---|---|---|---|
| `Gruendung > Einzelfundament: LPH 1-9` | `5d35c1cf-4c24-560f-ad17-506851e78011` | `2 × IfcFooting` (*West / Ost*) | `Klassifizierung`: `Objektgruppe='Gruendung'`, `Objektklasse='Fundament'`, `Objekttyp='Einzelfundament'` (`IFCLABEL`) |
| `Pfeiler > Pfeilerschaft: LPH 1-9` | `0785d1cb-857b-553d-9b1b-a28a2636e063` | `2 × IfcColumn` (*West / Ost*) | `Klassifizierung`: `Objektgruppe='Pfeiler'`, `Objektklasse='Pfeiler'`, `Objekttyp='Pfeilerschaft'` (`IFCLABEL`) |
| `Ueberbau > Fahrbahnplatte: LPH 1-9` | `f5884e9e-f149-57cb-aa08-3b1462626e30` | `1 × IfcSlab` (*Fahrbahnplatte Feld 1*) | `Klassifizierung`: `Objektgruppe='Ueberbau'`, `Objektklasse='Platte'`, `Objekttyp='Fahrbahnplatte'` (`IFCLABEL`) |
| `Ueberbau > Fahrbahnplatte: LPH 3-9` | `81586d48-c9eb-5ec7-88b6-b2259a6352f9` | dieselbe `IfcSlab` | zusätzlich `Bauteilinformation/Fertigteil = 'nein'` (`IFCLABEL`) |
| `Ueberbau > Fahrbahnplatte: LPH 8-9` | `d4705d1e-d5b2-5728-8f15-8de87b5d626e` | dieselbe `IfcSlab` | zusätzlich `Bauteilinformation/Feldnummer = 1` (`IFCINTEGER`) |
| `Ueberbau > Kappe: LPH 8-9` | `be959b9d-daef-5315-9aba-f60b6ad54ccb` | `2 × IfcBeam` (*Kappe Nord / Süd*) | `Klassifizierung` (`Ueberbau` / `Kappe` / `Kappe`, `IFCLABEL`) + `Bauteilinformation/Feldnummer = 1` (`IFCINTEGER`) |

---

## 2. Erweiterte modifizierte IFC-Testmodelle (`_mod_full` & `_mod_partial`)

Die größeren IFC-Dateien in `docs/` wurden aus `Building-Architecture.ifc`, `Infra-Bridge.ifc` sowie dem Hamburger Brückenmodell `bruecke-tbw1-mit-merkmalen.ifc` abgeleitet, sodass jeweils eine Variante alle ausgewählten LOINs erfüllt (`_mod_full`) und eine zweite gezielte Abweichungen, Typfehler und Fast-Treffer demonstriert (`_mod_partial`).

| Datei | Schema | Erwartetes Ergebnis |
|---|---|---|
| `Infra-Bridge_mod_full.ifc` | `IFC4` | 13 von 13 LOINs erfüllt (`Exit-Code 0`) |
| `Infra-Bridge_mod_partial.ifc` | `IFC4` | 7 erfüllt, 5 nicht erfüllt, 1 nicht prüfbar (`Exit-Code 3`) |
| `Building-Architecture_mod_full.ifc` | `IFC4` | 7 von 7 LOINs erfüllt (`Exit-Code 0`) |
| `Building-Architecture_mod_partial.ifc` | `IFC4` | 4 erfüllt, 2 nicht erfüllt, 1 nicht prüfbar (`Exit-Code 3`) |
| `bruecke-tbw1-mit-merkmalen_mod_full.ifc` | `IFC2X3` | Hamburger Brücke TBW1 mit ergänztem Pset `Klassifizierung` (`IFCLABEL`) und angepassten IFC-Entities (`IfcFooting`, `IfcBeam`) |
| `bruecke-tbw1-mit-merkmalen_mod_partial.ifc` | `IFC2X3` | Teilweise angepasste Variante von TBW1 zur Demonstration gemischter Befunde (`IfcText` vs. `IfcLabel`, `IfcBuildingElementProxy` vs. `IfcBeam`) |

Geprüft mit CheckMate (`0.3.3-stable` & `0.4.1-beta`) und mit `ifctester` (IfcOpenShell 0.9). `ifctester` wertet „nicht prüfbar“ (`0` anwendbare Elemente) als bestanden, während CheckMate diesen Fall bewusst als **`nicht prüfbar` (`Exit-Code 4`)** ausweist.

### 2.1 `Infra-Bridge_mod_*.ifc` (13 LOINs Ingenieurbau)

| LOIN | ID | Elemente im Modell | in `_mod_partial` |
|---|---|---|---|
| Pfeiler > Pfeilerschaft (Ingenieurbau): LPH 1-9 | `0785d1cb-857b-553d-9b1b-a28a2636e063` | 7 `IfcColumn` (pierstem) | nicht erfüllt: zusätzlich 2 `IfcBeam` als Pfeilerschaft klassifiziert |
| Gruendung > Einzelfundament (Ingenieurbau): LPH 1-9 | `5d35c1cf-4c24-560f-ad17-506851e78011` | 7 `IfcFooting` | erfüllt |
| Ausstattung > Gelaender (Ingenieurbau): LPH 1-9 | `c22f668f-910a-5638-9e0e-041223649173` | 2 `IfcRailing` | erfüllt |
| Ueberbau > Laengstraeger (Ingenieurbau): LPH 1-9 | `7377fc52-24f9-5bd1-a6dc-a0b58b1b0069` | 3 `IfcBeam` (main girder) | erfüllt |
| Ueberbau > Laengstraeger (Ingenieurbau): LPH 8-9 | `94b90e87-6f83-531f-aceb-cfda395fda25` | dieselben 3 | nicht erfüllt: `Feldnummer` fehlt |
| Ueberbau > Quertraeger (Ingenieurbau): LPH 1-9 | `1a26646f-2d64-584c-901e-4dff1865a58a` | 3 `IfcBeam` (cross girder) | erfüllt |
| Ueberbau > Quertraeger (Ingenieurbau): LPH 8-9 | `4d889a5d-167d-546c-a8e9-f838a637fb1a` | dieselben 3 | nicht erfüllt: `Feldnummer` als `IfcLabel` statt `IfcInteger` |
| Ueberbau > Fahrbahnplatte (Ingenieurbau): LPH 1-9 | `f5884e9e-f149-57cb-aa08-3b1462626e30` | 1 `IfcSlab` (bridge deck) | erfüllt |
| Ueberbau > Fahrbahnplatte (Ingenieurbau): LPH 3-9 | `81586d48-c9eb-5ec7-88b6-b2259a6352f9` | dieselbe | nicht erfüllt: `Fertigteil` fehlt |
| Ueberbau > Fahrbahnplatte (Ingenieurbau): LPH 8-9 | `d4705d1e-d5b2-5728-8f15-8de87b5d626e` | dieselbe | erfüllt |
| Widerlager > Schleppplatte (Ingenieurbau): LPH 1-9 | `8a4c3cd9-bcac-55d6-ba47-89491ba62e35` | 2 `IfcSlab` (approach slab) | nicht prüfbar: Klassifizierung als `IfcText` statt `IfcLabel` |
| Ueberbau > Bogen (Ingenieurbau): LPH 1-9 | `7f4ae2f8-c8b0-5e19-b68d-78077bcf61b6` | 8 `IfcMember` (arch segment) | erfüllt |
| Ueberbau > Bogen (Ingenieurbau): LPH 8-9 | `713fd47a-af17-567c-b284-8bd763c43145` | dieselben 8 | nicht erfüllt: `Feldnummer` fehlt bei 4 Segmenten |

### 2.2 `Building-Architecture_mod_*.ifc` (7 LOINs aus „Beispiel AIA Bundesbauten“)

| LOIN | ID | Elemente im Modell | in `_mod_partial` |
|---|---|---|---|
| Wand, massiv | `e84ce0d9-0ca7-4d11-a5be-eefbfcf8102b` | 3 `IfcWall` (outer wall) | nicht erfüllt: Maße als Zahl statt `IfcLabel`, Material fehlt |
| Geschossdecken | `c9e1d502-6d9a-4da4-9bd1-1cd251cae3ca` | 1 `IfcSlab` (floor) | erfüllt |
| Dach | `608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e` | 1 `IfcRoof` | nicht prüfbar: Typ bleibt `GABLE_ROOF` statt `USERDEFINED` |
| Ausstattung - Möbel | `1697f890-fcc7-4fab-b086-8fb91b116c6a` | 1 `IfcFurniture` (kitchen) | erfüllt |
| Gebäude | `a3ce341f-d275-4553-911b-00cce2e489c6` | 1 `IfcBuilding` | erfüllt |
| Geschosse | `c5403b95-3517-4412-b860-66de2d7bafdd` | 1 `IfcBuildingStorey` | erfüllt |
| Räume | `23857b17-9d68-417a-a5bf-5a6f42888c41` | 2 `IfcSpace` | nicht erfüllt: „entry hall“ ohne `Raum-Codierung` |
