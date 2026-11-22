---
title: IFC und IDS
description: IFC-Einlesen, unterstützte IDS-Anforderungen und Grenzen der Prüfung in CheckMate 0.3.1.
---

## Modell und Anforderungen

CheckMate `0.3.1` liest IFC2X3-, IFC4- und IFC4X3-Modelle mit der APSTEX IFC Toolbox. Unterstützt werden lokale `.ifc`- und `.ifczip`-Dateien sowie HTTP(S)-Links. Ein Beispielmodell ist enthalten. Modellobjekte und ihre Merkmale werden für den Prüfkern als `ModelElement` aufbereitet.

Anforderungen stammen aus LOINs oder AIA-Projekten des BIM-Portals oder aus lokalen IDS-Dateien beziehungsweise ZIP-Archiven. Der Portal-Katalog kann nach Metadaten durchsucht werden; eine vorgeschlagene LOIN ist nicht automatisch für das Projekt verbindlich.

- [buildingSMART: IFC-Schemaspezifikationen](https://technical.buildingsmart.org/standards/ifc/ifc-schema-specifications/) und [ISO 16739-1:2024](https://www.iso.org/standard/84123.html)
- [buildingSMART: Information Delivery Specification](https://technical.buildingsmart.org/projects/information-delivery-specification-ids/)
- [BIM-Portal REST-API](https://bimdeutschland.github.io/BIM-Portal-REST-API-Dokumentation/)

## Was geprüft wird

Der IDS-Parser und `IdsValidator` prüfen in `0.3.1` insbesondere Entity- und Property-Facetten, einschließlich ausgewählter `predefinedType`-Bedingungen, Datentypen, Werten und Kardinalitäten. Merkmalnamen werden grundsätzlich exakt verglichen; Schreibweisenabweichungen können als Hinweise erscheinen.

Für drei Klassifizierungsmerkmale gibt es ein fest hinterlegtes Mapping auf die Konvention des Beispiel-Brückenmodells. Seine Verwendung wird im Bericht ausgewiesen. Dieses Mapping ist keine allgemeine IDS-Regel; sein weiterer Umgang ist in [`docs/STATUS.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/STATUS.md) offen.

## Grenzen und Ergebnisse

Nicht implementiert sind unter anderem die Facetten `attribute`, `classification`, `material` und `partOf`, Einschränkungen wie Muster oder Grenzwerte für Property-Werte, `maxOccurs="0"` auf Spezifikationsebene und die Umrechnung von SI-Einheiten. Geometrie, QuantitySets sowie bestimmte komplexe Property-Typen werden nicht ausgewertet.

Nicht unterstützte IDS-Bestandteile werden im Bericht benannt. Sind sie für die Auswahl der Elemente relevant, wird die Spezifikation als **„nicht prüfbar“** ausgewiesen. Bei nicht ausgewerteten Anforderungen kann das Ergebnis **„erfüllt (soweit ausgewertet)“** sein. Auch wenn keine Elemente zutreffen, ist das kein vollständiger Nachweis.

Ein positives Ergebnis bestätigt nur die ausgewerteten Anforderungen für das geladene Modell. Es bescheinigt weder die vollständige Einhaltung von IDS, AIA oder einer Norm noch die fachliche Richtigkeit des Modells.

## Tests und Code

Tests enthalten eigene IFC-Fixtures und exportierte Portal-Anforderungen; ausgewählte Ergebnisse werden mit [IfcOpenShell / ifctester](https://docs.ifcopenshell.org/ifctester.html) verglichen. IFC-Lesetests benötigen APSTEX, weitere Modelle können mit `:hackathon-template:downloadTestModels` geladen werden.

Einstiegspunkte sind `IdsParser`, `IdsValidator`, `IfcModelElements` und `CheckService`. Details und Testvoraussetzungen stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/docs/WORKFLOW.md) und im [Schnellstart](/CheckMate_BIMPortal/guides/quickstart/).
