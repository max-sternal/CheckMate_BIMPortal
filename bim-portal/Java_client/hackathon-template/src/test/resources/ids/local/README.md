# Hand-written IDS files used as test files

Unlike the portal exports in [`../portal/`](../portal/README.md), these files are written by hand,
the way other tools write IDS. They test what only matters for local IDS files.

`mehrere-spezifikationen.ids` (title "CheckMate Testanforderungen", version 1.2):

| Specification | `ifcVersion` | What it tests |
|---|---|---|
| Platten | `IFC2X3 IFC4` | several schemas in one specification |
| Waende | `IFC4` | entity as `xs:enumeration`; an attribute facet and a value given as `xs:enumeration` in the requirements, which CheckMate does not evaluate |
| Klassifiziert | `IFC4` | applicability by a classification facet, not evaluated |
| Keine Proxies | `IFC4` | `maxOccurs="0"` (no element may apply), not evaluated |
| Bruecken | `IFC4X3_ADD2` | the IFC4X3 case files are covered by an IFC4X3_ADD2 specification |

Valid against the IDS 1.0 XSD: checked on 2026-11-21 with `ifctester.ids.open(path,
validate=True)`, ifctester 0.9.

ifctester's verdicts on the same day, the reference for `LocalIdsCheckTest`:

| Model | Platten | Waende | Klassifiziert | Keine Proxies | Bruecken |
|---|---|---|---|---|---|
| `ifc/checkmate-cases-ifc2x3.ifc` | 5 applicable, 5 failed | - | - | - | - |
| `ifc/checkmate-cases-ifc4.ifc` | 5 applicable, 5 failed | 0 applicable | 0 applicable | 0 applicable | - |
| `ifc/checkmate-cases-ifc4x3.ifc` | - | - | - | - | 0 applicable |
| `test-models/ifc4/Infra-Bridge.ifc` | 3 applicable, 3 failed | 4 applicable, 4 failed | 0 applicable | 9 applicable, failed | - |

"-" means the specification is not written for that schema. CheckMate gives the same numbers for
Platten and Waende. Klassifiziert and Keine Proxies are "nicht prüfbar" in CheckMate, which
says why. Had the parser dropped those parts silently, as it did before 2026-11-21, CheckMate
would have checked all 77 bridge elements against "Klassifiziert", and would have reported
"Keine Proxies" as passed although the model holds 9 proxies.

Edit freely, but keep the file valid IDS and update the tables when the verdicts change.
