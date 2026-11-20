# BIM Portal IDS exports used as test files

Downloaded unchanged on 2026-11-20 from the public BIM Portal (`https://via.bund.de/bim`) through
`GET /aia/api/v1/public/loin/{guid}/IDS`.

| File | LOIN | GUID | Version |
|---|---|---|---|
| `ueberbau-kappe-lph8-9.ids` | Ueberbau > Kappe (Ingenieurbau): LPH 8-9 | `be959b9d-daef-5315-9aba-f60b6ad54ccb` | 4.1 |
| `ueberbau-schaft-lph1-9.ids` | Ueberbau > Schaft (Ingenieurbau): LPH 1-9 | `faae463a-6938-54cf-b1d6-e8944ed983a5` | 4.1 |
| `fenster.ids` | Fenster | `c0f5ba14-6f36-4b10-a328-bec7f0bd2e4a` | 1.1 |

Why these three:

- Kappe: classification, `Feldnummer` (IFCINTEGER), the entity as a single name, `predefinedType`
  in the IFC4X3 block, and the blocks in the order IFC4, IFC4X3_ADD2, IFC2X3.
- Schaft: the entity as a pattern listing many entities.
- Fenster: selects by entity with `predefinedType` instead of by classification, IFC4 only.

Don't edit these files by hand: they stand for what the portal really writes. To test something the
portal doesn't write, build the specification in the test instead.
