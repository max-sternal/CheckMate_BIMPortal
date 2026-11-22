---
editUrl: false
title: Walk through a check
description: An example check of the Hamburg bridge model against a saved portal IDS.
slug: en/v0.3.2/guides/example
---

## Starting point

What happens when a bridge model uses different property names from a requirement? The bundled IFC2X3 model **BIM.Hamburg TBW1** and the saved portal IDS **“Ueberbau > Kappe”** provide an example.

The IDS file is at `hackathon-template/src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids`. It was exported from the BIM Portal; its origin and version are documented in the [fixture directory](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/bim-portal/Java_client/hackathon-template/src/test/resources/ids/portal/README.md). This check uses the local file and does not require portal access.

## 1. Compare model and requirements

From `bim-portal/Java_client/`:

```bash
./gradlew :hackathon-template:runLoinCheck --args="--ids src/test/resources/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default"
```

Without `--model` and with `CHECKMATE_IFC` unset, CheckMate loads the bundled bridge model. It reads the IDS file, selects the specification for IFC2X3 and compares its applicability conditions with the model elements. `default` enables the bundled [Hamburg mapping](/CheckMate_BIMPortal/en/v0.3.2/guides/mapping/).

## 2. Read the result

An abbreviated excerpt from a run (reports are currently in German):

```text
Modell: bruecke-tbw1-mit-merkmalen.ifc - Schema IFC2X3, 303 Objekte
Ueberbau > Kappe ...: 0 von 303 Elementen anwendbar - nicht prüfbar
...
Klassifizierung/Objektgruppe: 'Kappe' statt 'Ueberbau',
  Datentyp IFCTEXT statt IFCLABEL (als Pset_Objektinformation/_IDEbene1)
...
Ergebnis: 1 Spezifikation(en) für IFC2X3: 1 nicht prüfbar (Exit-Code 4)
```

The mapping finds the corresponding model addresses. The requirement still differs in value and data type: `Kappe` instead of `Ueberbau`, `IFCTEXT` instead of `IFCLABEL`. No elements are therefore confirmed as applicable. **“Not checkable” here is neither a pass nor an overall judgement of the model.**

## 3. Compare without mapping

Running the same command with `--mapping none` reports that the three classification properties are absent under the requested names. With mapping, the report also points to the differently named properties. The result therefore depends on the model, requirements and chosen mapping.

For exit code `4`, Gradle reports a failed task; this is the checking result, not a compilation failure. For another project, the model, requirements and mapping must be appropriate for each other.
