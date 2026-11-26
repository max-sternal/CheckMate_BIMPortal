---
title: IFC and IDS
description: IFC loading, supported IDS requirements and checking limitations in CheckMate 0.3.3-stable.
---

## Model and requirements

CheckMate `0.3.3-stable` reads IFC2X3, IFC4 and IFC4X3 models using its own Java reader. It accepts local `.ifc` and `.ifczip` files and HTTP(S) links. Compact sample models for building and infrastructure are included. Model elements and their properties are prepared as `ModelElement` records for the checking core.

Requirements come from BIM Portal LOINs or AIA projects, or from local IDS files or ZIP archives. The portal catalogue can be searched by metadata; a suggested LOIN is not automatically a binding project requirement.

The portal supplies requirements for this check as IDS.

- [buildingSMART: IFC schema specifications](https://technical.buildingsmart.org/standards/ifc/ifc-schema-specifications/) and [ISO 16739-1:2024](https://www.iso.org/standard/84123.html)
- [buildingSMART: Information Delivery Specification](https://technical.buildingsmart.org/projects/information-delivery-specification-ids/)
- [BIM Portal REST API](https://bimdeutschland.github.io/BIM-Portal-REST-API-Dokumentation/)

## What is checked

The IDS parser and `IdsValidator` check entity and property facets, including selected `predefinedType` conditions, data types, values and cardinalities. Property names are generally compared exactly; spelling differences may be reported as hints.

Additionally, `ValueConstraint` fully supports standard-compliant regular expressions (`xs:pattern`), numeric bounds (`minInclusive`/`maxInclusive`/`minExclusive`/`maxExclusive`), and length constraints (`length`/`minLength`/`maxLength`) — each guarded against ReDoS and OOM.

An interchangeable JSON mapping maps classification properties by default. The report identifies when it is used. See [Examples and mapping](/CheckMate_BIMPortal/en/guides/mapping/).

## Limits and results

Unimplemented features include the `attribute`, `classification`, `material` and `partOf` facets, specification-level `maxOccurs="0"` and SI unit conversion. Geometry, quantity sets and certain complex property types are not evaluated.

The report identifies unsupported IDS constructs. When they affect element selection, the specification is marked **not checkable**. When requirements are not evaluated, a result can be **passed as far as evaluated**. A specification with no applicable elements is not a complete demonstration of compliance either.

A positive result confirms only the requirements evaluated for the loaded model. It does not certify full compliance with IDS, an AIA or a standard, nor the professional correctness of the model.

## Tests and code

Tests include custom IFC fixtures and exported portal requirements; selected results are compared with [IfcOpenShell / ifctester](https://docs.ifcopenshell.org/ifctester.html). Additional models can be downloaded with `:hackathon-template:downloadTestModels`.

Entry points include `IdsParser`, `IdsValidator`, `IfcModelElements`, `ValueConstraint` and `CheckService`. For details and test prerequisites, see [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/WORKFLOW.md) and the [quick start](/CheckMate_BIMPortal/en/guides/quickstart/).
