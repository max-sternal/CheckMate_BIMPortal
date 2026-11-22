---
title: Context & foundations
description: CheckMate in the context of the German BIM Portal, open standards and information requirements.
---

## A tool for information requirements

Clients define the information they need at particular points in a project. The German BIM Portal provides, among other things, properties, levels of information need (LOIN) and client information requirements (AIA).

CheckMate compares IFC models with machine-readable requirements in IDS format. Requirements can come from the BIM Portal or local IDS files. The tool supports checks of individual information requirements; it does not replace professional judgement, a complete AIA review or the application of a standard.

The application performs the rule-based check. An AI application can optionally use MCP to run checks and explain results. The check result and any AI explanation should be considered separately; sensitive data must be handled according to the applicable rules.

## Federal initiative and BIM Portal

- [BIM Deutschland – BIM Portal](https://www.bimdeutschland.de/bim-portal): background and access to the federal portal's modules.
- [BIM Deutschland – client information requirements](https://www.bimdeutschland.de/bim-wissen/auftraggeber-informationsanforderungen): AIA, LOIN and sample requirements.
- [BIM Portal user manual, version 4 (2025)](https://www.bimdeutschland.de/fileadmin/media/Bilder/Neue_Website_2024/Veranstaltungen/BIMD_VAS/25-06-20_BIM-Portal_Benutzerhandbuch_V4.pdf): the portal's structure and operation.
- [Digital Design and Construction Roadmap](https://www.bmv.de/SharedDocs/DE/Publikationen/G/stufenplan-digitales-bauen.pdf?__blob=publicationFile) and [BIM Master Plan for federal trunk roads](https://www.bimdeutschland.de/fileadmin/media/Downloads/Download-Liste/Strasse/BIM_Masterplan_Bundesfernstrassen_barrierefrei.pdf): federal strategies; more material is available from [BIM Deutschland – downloads](https://www.bimdeutschland.de/bim-wissen/downloads).
- [BIM Portal REST API documentation](https://bimdeutschland.github.io/BIM-Portal-REST-API-Dokumentation/): the portal's interface and data.
- [2025 BIM Portal Hackathon](https://www.bimdeutschland.de/veranstaltungen/hackathon-2025) and [starter kit by planen-bauen 4.0](https://github.com/pb40development/bim-portal): CheckMate's origins; see also [planen-bauen 4.0](https://planen-bauen40.de/).

## Standards and open specifications

These documents provide context. Their inclusion **does not claim that CheckMate fully implements or demonstrates compliance with any standard**. See [IFC and IDS](/CheckMate_BIMPortal/en/architecture/ifc-ids-checking/) for the implemented checking scope.

| Document | Relevance |
|---|---|
| [ISO 7817-1:2024](https://www.iso.org/standard/82914.html) / [DIN EN ISO 7817-1](https://www.dinmedia.de/en/standard/din-en-iso-7817-1/382382712) | Concepts and principles for level of information need; the German edition replaces DIN EN 17412-1. |
| [ISO/DIS 7817-3](https://www.iso.org/standard/89077.html) | Draft data model and schema for level of information need; still under development. |
| [DIN EN ISO 19650-1](https://www.dinmedia.de/en/standard/din-en-iso-19650-1/299953244) / [ISO 19650-1](https://www.iso.org/standard/68078.html) and [ISO 19650-2](https://www.iso.org/standard/68080.html) | Principles and information management during the delivery phase. |
| [ISO 29481-1:2025](https://www.iso.org/standard/88515.html) | Methodology for describing information exchange and processes (Information Delivery Manual). |
| [ISO 23386](https://www.iso.org/standard/75401.html) and [ISO 23387](https://www.iso.org/standard/75403.html) | Description and maintenance of properties and data templates for construction objects. |
| [ISO 16739-1:2024](https://www.iso.org/standard/84123.html) and [buildingSMART IFC](https://technical.buildingsmart.org/standards/ifc/ifc-schema-specifications/) | Open IFC data schema; IFC 4.3 ADD2 corresponds to ISO 16739-1:2024. |
| [VDI 2552 Part 1](https://www.vdi.de/en/home/vdi-standards/details/vdi-2552-blatt-1-building-information-modeling-fundamentals) and [Part 10](https://www.vdi.de/en/home/vdi-standards/details/vdi-2552-blatt-10-building-information-modeling-employers-information-requirements-eir-and-bim-execution-plan-bep) | BIM fundamentals, client information requirements and BIM execution plans. |
| [buildingSMART IDS](https://technical.buildingsmart.org/projects/information-delivery-specification-ids/) | Open specification for machine-readable information requirements for IFC models. |

Links lead to official descriptions; standards and VDI guidelines may require purchase. The editions and requirements agreed for the individual project remain authoritative.
