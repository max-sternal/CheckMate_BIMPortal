---
title: IFC-Parsing & IDS 1.0
description: Wie CheckMate IFC2X3/4/4X3-Modelle einliest, gegen buildingSMART IDS 1.0 prüft und Ergebnisse an IfcOpenShell / ifctester verifiziert.
---

## 1. IFC-Einlesen (`IfcResources`, `ModelLinks`, `IfcModelElements`)

CheckMate liest **IFC2X3-, IFC4- und IFC4X3-Modelle** (inkl. `IFC4X3_ADD2`) aus verschiedenen Quellen:
- **Lokale Dateien:** `.ifc` (STEP-Format `ISO-10303-21`) sowie `.ifczip`.
- **Links (`http`/`https`):** Automatische Umwandlung von GitHub-`blob`-Links auf `raw`, automatische Auflösung von **Git-LFS-Pointern**, Header-Erkennung (HTML-Fehlerseiten werden als solche gemeldet statt als „leeres Modell“), 200-MB-Download-Limit.
- **Gebündeltes Demo-Modell:** `bruecke-tbw1-mit-merkmalen.ifc` (IFC2X3) liegt direkt im JAR, sodass CheckMate ohne lokale Beispiel-Datei startet.

### Technische Besonderheiten in `0.3.1` (APSTEX)
- **Vorwärts-Indexierung statt inverser Relationen (`Definitions.of`):** Die APSTEX IFC Toolbox verliert beim Parsen zufällig einzelne Einträge in den inversen `IsDefinedBy`-Links (auf dem Brückenmodell fehlten pro Durchlauf zufällig 1–7 % der Relationen). `IfcModelElements` indexiert daher alle `IfcRelDefinesByProperties`- und `IfcRelDefinesByType`-Relationen vorwärts über `RelatedObjects`.
- **Deterministische Reihenfolge:** Alle Objekte werden nach ihrer STEP-Zeilennummer sortiert, sodass zwei Prüfläufe auf demselben Modell stets identische Berichte erzeugen.
- **Semantik nach `IfcOpenShell`:**
  - `SingleValue`, `EnumeratedValue` und `ListValue` aus Element- und Typ-PropertySets (Element überschreibt Typ).
  - `PredefinedType`, `ObjectType`, `ElementType` und `USERDEFINED` exakt nach `ifcopenshell.util.element.get_predefined_type` / `is_userdefined_type`.
  - QuantitySets (`IfcElementQuantity`), Bounded/Table/Complex-Properties und Geometrie werden in `0.3.1` bewusst übersprungen, da die LOINs im BIM-Portal sie derzeit nicht abfragen.

---

## 2. Deterministische IDS-1.0-Prüfung (`IdsParser`, `IdsValidator`)

CheckMate lädt LOINs und ganze AIA-Projekte als **buildingSMART IDS 1.0** (bzw. ZIP-Archive von IDS-Dateien) direkt aus dem BIM-Portal herunter — oder liest lokale `.ids`/`.zip`-Dateien.

### Was in `0.3.1` vollständig geprüft wird
- **Schema-Zuordnung:** Mehrfach-Schemas (`"IFC2X3 IFC4"`) und Addenda (`IFC4X3_ADD2` ↔ `IFC4X3`, sauber getrennt von `IFC4`).
- **Entity-Facetten:** `name` (case-insensitiv) und `predefinedType` (case-sensitiv, inkl. `simpleValue`, `xs:pattern`, `xs:enumeration` und `USERDEFINED`). Für **IFC2X3** gilt analog `ifctester` auch die Entity des Typ-Objekts (`IfcBeamType` → `IFCBEAM`).
- **Property-Facetten:** Exakter Abgleich von `propertySet` und `baseName`, Kardinalitäten `required`, `optional` und `prohibited`, strenge Datentyp-Prüfung (`IfcText` ≠ `IFCLABEL`) und typgerechter Wertevergleich (`1e-6` relative Toleranz bei Fließkommazahlen, exakter Textvergleich, XML-Schema-Booleans, leere/`UNKNOWN`-Werte als fehlend).
- **Hilfreiche Diagnosen statt stummer Fehler:**
  - Abweichende Schreibweisen (Groß-/Kleinschreibung, Umlaute, Trennzeichen, führender Unterstrich) oder verwandte Datentypen werden **niemals** fälschlich akzeptiert, aber als konkreter Korrekturhinweis ausgewiesen.
  - Greift eine Spezifikation auf `0` Elemente, liefert CheckMate stets **„nicht prüfbar“** mit Facetten-Trefferzahlen und den 5 ähnlichsten Bauteilen („Near Misses“).
- **Katalog-Auswahl (`ModelFit`):** Bewertet alle 1.568 öffentlichen Portal-LOINs anhand ihrer Metadaten gegen das geladene Modell und sortiert passende LOINs nach vorn, ohne vorab alle IDS-Dateien herunterzuladen.

### Was bewusst als „Nicht ausgewertet“ gemeldet wird
Teile von IDS 1.0, die CheckMate `0.3.1` noch nicht implementiert hat, werden **niemals still verworfen**:
- Facetten `attribute`, `classification`, `material`, `partOf`.
- Muster/Enumerationen/Grenzwerte (`xs:pattern`, `xs:enumeration`, `minInclusive` …) auf Property-Werten sowie Muster auf `propertySet`/`baseName`.
- Spezifikations-Kardinalität `maxOccurs="0"`.
- SI-Einheiten-Umrechnung bei Maßwerten (z. B. Millimeter-Modell gegen Meter-IDS).

Tritt ein nicht ausgewerteter Teil in der **Applicability** auf, wird die Spezifikation als **„nicht prüfbar“** markiert; in den **Requirements** ist das beste mögliche Urteil **„erfüllt (soweit ausgewertet)“**.

### Offene Entscheidung: `PropertyAliases` (STATUS #14)
Aktuell enthält `PropertyAliases.java` ein fest verdrahtetes Mapping für die Konvention des mitgelieferten BIM.Hamburg-Brückenmodells (`Klassifizierung/Objektgruppe|Objektklasse|Objekttyp` → `Pset_Objektinformation/_IDEbene1..3`). Jede Nutzung eines Alias wird im Bericht explizit gekennzeichnet (`als Pset_Objektinformation/_IDEbene3`). Geplant ist die Umstellung auf konfigurierbare, externe Mapping-Dateien.

---

## 3. Qualitätssicherung mit `IfcOpenShell` / `ifctester` als Referenz

**`IfcOpenShell` / `ifctester` bleibt dauerhaft die gemeinsame Test-Referenz** — sowohl für die Java-Tests der `0.3.x`-Linie als auch für die neue Testsuite von **`0.4` (*CheckMate Lite*)**:

- **Eigene IFC-Testfälle (`src/test/python/make_ifc_fixtures.py`):** Generiert deterministische Testmodelle für IFC2X3, IFC4 und IFC4X3_ADD2 (Typ-Vererbung, Überschreibungen, Enumerationen, Listen, `USERDEFINED`, ignorierte Quantities).
- **Echte Portal-Fixtures (`src/test/resources/ids/portal/`):** Reale LOIN-Exporte (`Kappe`, `Schaft`, `Fenster`) und reale AIA-Projekt-ZIPs (`Eisenbahnbrücke` mit 36 LOINs, `Straße Demonstrator`).
- **Öffentliche Referenzmodelle (`downloadTestModels`):** 5 buildingSMART-/Open-Source-Modelle (IFC2X3, IFC4, IFC4X3), gegen deren `IfcOpenShell 0.9`-Zählungen und `ifctester 0.9`-Urteile CheckMate automatisiert abgeglichen wird.
- **Test-Umfang in `0.3.1`:** **148 Kern-Tests** laufen ohne APSTEX direkt in GitHub Actions; mit APSTEX + Referenzmodellen umfasst die Suite **217 Tests**.
