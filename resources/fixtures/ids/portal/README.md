# IDS files downloaded from the BIM Portal, used as test files

Downloaded unchanged from the public BIM Portal (`https://via.bund.de/bim`) through
`GET /aia/api/v1/public/loin/{guid}/IDS`.

| File | LOIN | GUID | Version |
|---|---|---|---|
| `ueberbau-kappe-lph8-9.ids` | Ueberbau > Kappe (Ingenieurbau): LPH 8-9 | `be959b9d-daef-5315-9aba-f60b6ad54ccb` | 4.1 |
| `ueberbau-schaft-lph1-9.ids` | Ueberbau > Schaft (Ingenieurbau): LPH 1-9 | `faae463a-6938-54cf-b1d6-e8944ed983a5` | 4.1 |
| `fenster.ids` | Fenster | `c0f5ba14-6f36-4b10-a328-bec7f0bd2e4a` | 1.1 |

Compact multi-LOIN reference bundles (exported unchanged from `https://via.bund.de/bim` on 2026-11-26
for offline testing of `resources/models/mini-hochbau-bundesbau.ifc` and
`resources/models/mini-infrastruktur-bruecke.ifc`):

| File | Scope | Included BIM Portal LOINs |
|---|---|---|
| `mini-hochbau-bundesbau.zip` | Hochbau (`Beispiel AIA Bundesbauten`, LPH 3, IFC4) | `Gebäude` (`a3ce341f-...`), `Geschosse` (`c5403b95-...`), `Geschossdecken` (`c9e1d502-...`), `Wand, massiv` (`e84ce0d9-...`), `Dach` (`608e3ec2-...`) |
| `mini-infrastruktur-bruecke.zip` | Infrastruktur (`Pflegestelle Straße / Ingenieurbau`, IFC4) | `Einzelfundament LPH 1-9` (`5d35c1cf-...`), `Pfeilerschaft LPH 1-9` (`0785d1cb-...`), `Fahrbahnplatte LPH 1-9 / 3-9 / 8-9` (`f5884e9e-...`, `81586d48-...`, `d4705d1e-...`), `Kappe LPH 8-9` (`be959b9d-...`) |

AIA project ZIPs, downloaded unchanged on 2026-11-21 through
`GET /aia/api/v1/public/aiaProject/{guid}/IDS`:

| File | AIA project | GUID | Version |
|---|---|---|---|
| `aia-neubau-eisenbahnbruecke.zip` | Beispiel-AIA Neubau Eisenbahnbrücke | `e6f69257-dfda-441b-97bb-e9df44c5cf8b` | 1.1 |
| `aia-strasse-demonstrator.zip` | AIA FB Straße Demonstrator FM Verkehrsanlage | `d2dc4924-5842-42ad-9dac-2bb17c3674d3` | 1.1 |

Don't edit the `.ids` files inside these archives by hand: they stand for what the portal really writes.
