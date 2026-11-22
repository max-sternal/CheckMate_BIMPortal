---
title: IFC und IDS
description: IFC-Einlesen, IDS-1.0- und Rules-Prüfung, Fast-Treffer-Diagnose und BCF-Export in CheckMate 0.4.
---

## Modell und Anforderungen

CheckMate `0.4` liest `IFC2X3`-, `IFC4`- und `IFC4X3`-Modelle über `@ifc-lite/parser` sowie 3D-Dreiecksgeometrie über den WebAssembly-Kern `@ifc-lite/geometry`. Unterstützt werden lokale `.ifc`- und `.ifczip`-Dateien sowie HTTP(S)-Links (inklusive automatischer Übersetzung von GitHub-Blob-Links in Raw-Links).

Anforderungen stammen aus:
- **LOINs oder AIA-Projekten des BIM-Portals** (abgerufen als IDS-XML oder ZIP-Archiv über `@checkmate/bimportal`),
- **lokalen IDS-1.0-Dateien oder ZIP-Archiven** (mit automatischer Deduplizierung identischer `(LOIN-GUID, IFCVersion)`-Spezifikationen in AIA-Projekten),
- **`.rules.json`-Regelwerken** (`@ifc-lite/rules`), in die sich IDS-Dokumente auch direkt aus der Oberfläche konvertieren lassen.

## Was geprüft wird

Über `@ifc-lite/ids` (`validateIDS`, `checkFacet`) in Kombination mit CheckMates `MappedDataAccessor` werden die IDS-1.0-Facetten (`entity`, `property`, `attribute`, `classification`, `material`, `partOf`) sowie XSD-Einschränkungen (`simpleValue`, `pattern`, `enumeration`, `bounds`, `length`) ausgewertet.

Zusätzlich zum Standard-IDS-Ergebnis ergänzt CheckMate:
1. **Transparentes Property Mapping (`MappedDataAccessor`)**: Prüft zuerst die angeforderte IDS-Adresse und anschließend definierte Aliase aus `bim-hamburg.json` oder einer eigenen Mapping-Datei; jeder Treffer über einen Alias wird als `(als ...)` dokumentiert.
2. **Null-Treffer- & Fast-Treffer-Diagnose (`diagnoseZeroApplicability`)**: Trifft eine Spezifikation auf `0` Elemente zu (`nicht prüfbar`), wertet CheckMate jede Applicability-Facette einzeln aus, erkennt verwandte IFC-Datentypen (z. B. `IFCTEXT statt IFCLABEL`) sowie ähnliche Schreibweisen (Groß-/Kleinschreibung, Umlaute, Trennzeichen) und listet die besten **Fast-Treffer (`N/M Facetten`)** auf.
3. **BCF-2.1-Export (`@ifc-lite/bcf`)**: Nicht erfüllte Anforderungen können als `.bcfzip`-Archiv für BIM-Koordinationswerkzeuge exportiert werden.

## Grenzen und Einordnung

Ein positives Prüfergebnis bestätigt nur die maschinenlesbar ausgewerteten Anforderungen für das geladene Modell. Es ersetzt keine fachliche Prüfung der Konstruktion und ist keine formale Zertifizierung. Spezifikationen mit `0` anwendbaren Elementen werden bewusst als **`nicht prüfbar` (Exit-Code `4`)** und niemals als „bestanden“ gewertet.
