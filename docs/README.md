# Modifizierte IFC-Testmodelle für CheckMate

Die IFCs in diesem Ordner wurden so modifiziert, dass einige LOINs aus dem BIM Portal vollständig
erfüllt sind und andere nur teilweise. Unten steht das Mapping von Modell zu LOIN, jeweils mit Name
und ID der LOIN.

**Herkunft:** Kopien von `test-models/ifc4/Building-Architecture.ifc` und
`test-models/ifc4/Infra-Bridge.ifc` aus dem Repo CheckMate_BIMPortal
(`bim-portal/Java_client/hackathon-template/`). Geometrie und vorhandene Daten sind unverändert;
hinzugefügt wurden nur Property Sets. Im Gebäude ist außerdem bei einigen Typobjekten der
PredefinedType auf `USERDEFINED` gesetzt (siehe unten).

| Datei | Erwartetes Ergebnis |
|---|---|
| `Infra-Bridge_mod_full.ifc` | 13 von 13 LOINs erfüllt |
| `Infra-Bridge_mod_partial.ifc` | 7 erfüllt, 5 nicht erfüllt, 1 nicht prüfbar |
| `Building-Architecture_mod_full.ifc` | 7 von 7 LOINs erfüllt |
| `Building-Architecture_mod_partial.ifc` | 4 erfüllt, 2 nicht erfüllt, 1 nicht prüfbar |

Geprüft am 24.11.2026 mit CheckMate und mit ifctester (IfcOpenShell 0.9), beide mit demselben
Ergebnis. ifctester wertet „nicht prüfbar“ (0 anwendbare Elemente) als bestanden.

**Wichtig:** Jede LOIN gehört zu genau einem der beiden Modelle. Die Brücken-LOINs gelten nur für
`Infra-Bridge_mod_*.ifc`, die Gebäude-LOINs nur für `Building-Architecture_mod_*.ifc`. Gegen das
jeweils andere Modell ist eine LOIN „nicht prüfbar“, weil dort die Bauteile und Merkmale fehlen. Das
ist dann kein Fehler der Datei.

## Infra-Bridge (LOINs Ingenieurbau): nur für `Infra-Bridge_mod_full.ifc` / `Infra-Bridge_mod_partial.ifc`

| LOIN | ID | Elemente im Modell | in `_mod_partial` |
|---|---|---|---|
| Pfeiler > Pfeilerschaft (Ingenieurbau): LPH 1-9 | `0785d1cb-857b-553d-9b1b-a28a2636e063` | 7 IfcColumn (pierstem) | nicht erfüllt: zusätzlich 2 IfcBeam (abutment support beam) als Pfeilerschaft klassifiziert, IFC-Entity nicht zulässig |
| Gruendung > Einzelfundament (Ingenieurbau): LPH 1-9 | `5d35c1cf-4c24-560f-ad17-506851e78011` | 7 IfcFooting | erfüllt |
| Ausstattung > Gelaender (Ingenieurbau): LPH 1-9 | `c22f668f-910a-5638-9e0e-041223649173` | 2 IfcRailing | erfüllt |
| Ueberbau > Laengstraeger (Ingenieurbau): LPH 1-9 | `7377fc52-24f9-5bd1-a6dc-a0b58b1b0069` | 3 IfcBeam (main girder) | erfüllt |
| Ueberbau > Laengstraeger (Ingenieurbau): LPH 8-9 | `94b90e87-6f83-531f-aceb-cfda395fda25` | dieselben 3 | nicht erfüllt: Feldnummer fehlt |
| Ueberbau > Quertraeger (Ingenieurbau): LPH 1-9 | `1a26646f-2d64-584c-901e-4dff1865a58a` | 3 IfcBeam (cross girder) | erfüllt |
| Ueberbau > Quertraeger (Ingenieurbau): LPH 8-9 | `4d889a5d-167d-546c-a8e9-f838a637fb1a` | dieselben 3 | nicht erfüllt: Feldnummer als IfcLabel statt IfcInteger |
| Ueberbau > Fahrbahnplatte (Ingenieurbau): LPH 1-9 | `f5884e9e-f149-57cb-aa08-3b1462626e30` | 1 IfcSlab (bridge deck) | erfüllt |
| Ueberbau > Fahrbahnplatte (Ingenieurbau): LPH 3-9 | `81586d48-c9eb-5ec7-88b6-b2259a6352f9` | dieselbe | nicht erfüllt: Fertigteil fehlt |
| Ueberbau > Fahrbahnplatte (Ingenieurbau): LPH 8-9 | `d4705d1e-d5b2-5728-8f15-8de87b5d626e` | dieselbe | erfüllt |
| Widerlager > Schleppplatte (Ingenieurbau): LPH 1-9 | `8a4c3cd9-bcac-55d6-ba47-89491ba62e35` | 2 IfcSlab (approach slab) | nicht prüfbar: Klassifizierung als IfcText statt IfcLabel, die LOIN wählt nichts aus |
| Ueberbau > Bogen (Ingenieurbau): LPH 1-9 | `7f4ae2f8-c8b0-5e19-b68d-78077bcf61b6` | 8 IfcMember (arch segment) | erfüllt |
| Ueberbau > Bogen (Ingenieurbau): LPH 8-9 | `713fd47a-af17-567c-b284-8bd763c43145` | dieselben 8 | nicht erfüllt: Feldnummer fehlt bei den 4 Segmenten der „road rail bridge“ |

Hinzugefügt: Pset `Klassifizierung` mit `Objektgruppe`, `Objektklasse`, `Objekttyp` (IfcLabel,
Werte aus der LOIN); für die Varianten LPH 3-9 und 8-9 Pset `Bauteilinformation` mit `Fertigteil`
(IfcLabel) bzw. `Feldnummer` (IfcInteger).

## Building-Architecture (LOINs aus „Beispiel AIA Bundesbauten“): nur für `Building-Architecture_mod_full.ifc` / `Building-Architecture_mod_partial.ifc`

| LOIN | ID | Elemente im Modell | in `_mod_partial` |
|---|---|---|---|
| Wand, massiv | `e84ce0d9-0ca7-4d11-a5be-eefbfcf8102b` | 3 IfcWall (outer wall) | nicht erfüllt: „house left“ gibt die Maße als Zahl (IfcLengthMeasure usw.) statt IfcLabel, „house right back“ hat kein Material |
| Geschossdecken | `c9e1d502-6d9a-4da4-9bd1-1cd251cae3ca` | 1 IfcSlab (floor) | erfüllt |
| Dach | `608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e` | 1 IfcRoof | nicht prüfbar: Typ bleibt GABLE_ROOF, die LOIN wählt nur USERDEFINED aus |
| Ausstattung - Möbel | `1697f890-fcc7-4fab-b086-8fb91b116c6a` | 1 IfcFurniture (kitchen) | erfüllt |
| Gebäude | `a3ce341f-d275-4553-911b-00cce2e489c6` | 1 IfcBuilding | erfüllt |
| Geschosse | `c5403b95-3517-4412-b860-66de2d7bafdd` | 1 IfcBuildingStorey | erfüllt |
| Räume | `23857b17-9d68-417a-a5bf-5a6f42888c41` | 2 IfcSpace | nicht erfüllt: „entry hall“ hat keine Raum-Codierung |

Hinzugefügt: je LOIN ein Pset mit dem Namen der LOIN (z. B. `Wand, massiv`) und allen geforderten
Merkmalen, alle als IfcLabel, wie das Portal es verlangt (auch die Maße). Diese LOINs wählen Wand,
Decke und Dach nur mit PredefinedType `USERDEFINED` aus; deshalb sind die Typobjekte der 3
Außenwände und der Bodenplatte auf `USERDEFINED` gesetzt, das des Dachs nur in `_mod_full`.

## Werte

- Aus dem Modell: Name, Bauteiltyp (ElementType des Typobjekts), Material, Gebäude-, Geschoss- und
  Raum-ID (GlobalId), Geschoss-Codierung `00`, Maße aus `Qto_*BaseQuantities` bzw. der Geometrie
  (m, m², m³).
- Platzhalter: Feldnummer (Bogensegmente je Eisenbahnbrücke 1-4 entlang der Achse, sonst 1),
  Fertigteil `nein`, Gebäude-Nummer `1`, Gebäudeteil-ID `A`, Raum-Codierung `00.01`/`00.02`,
  Funktion, Lage `Bodenplatte, Erdgeschoss`, Tragend `ja`.

## Hinweise

- Ohne Merkmale geblieben: Stirnwände (spandrel wall), Proxys, Baugruppen, Widerlagerbalken (nur in
  `_mod_partial` als Fehlerfall klassifiziert), Trennwand (plumbing wall), Schornstein.
- „Wand, Trockenbau“ (`72083abf-de28-4d43-889e-7f55dd744939`) wählt ebenfalls jede
  USERDEFINED-Wand aus und schlägt deshalb an den massiven Wänden fehl; sie gehört nicht zum
  Testsatz. Die Trennwand bleibt PLUMBINGWALL und wird von keiner der beiden ausgewählt.
- Die IDs lassen sich direkt in CheckMate verwenden (CLI `--loin <ID>`, MCP `check_model`).
