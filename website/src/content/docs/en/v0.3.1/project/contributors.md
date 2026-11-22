---
editUrl: false
title: Contributors & Transparency
description: CheckMate's origin, responsibilities, support, licensing, and
  transparent use of AI tools.
slug: en/v0.3.1/project/contributors
---

## Project & Origin

**CheckMate** is a **private open-source project** licensed under the **Apache License 2.0**. It originated from *Team CheckMate*'s submission at the [BIM Portal Hackathon Berlin](https://www.bimdeutschland.de/veranstaltungen/hackathon-2025) on 22 and 23 September 2025 and is developed collaboratively.

:::note[Institutional Distinction]
* CheckMate is **not an official product of the German Federal Government or TU Berlin**.
* The API client and starter kit originate from the [public hackathon starter kit](https://github.com/pb40development/bim-portal) by *planen-bauen 4.0* / BIM Deutschland.
:::

## Team & Responsibilities

| Contributor | Responsibility | Contributions |
|---|---|---|
| [**Maximilian Sternal**](https://github.com/max-sternal) · [LinkedIn](https://www.linkedin.com/in/maximilian-sternal/) | Project lead, organisation, system architecture | Project concept and coordination, repository setup, technical decisions, releases, documentation |
| [**Sophie Lipskaya**](https://github.com/SOPHIEfree) · [LinkedIn](https://www.linkedin.com/in/sophie-lipskaya/) | IFC and BIM Portal backend | Portal client, IFC processing, runnable interface, initial IDS validation, headless CLI |
| [**Robert Jürgensen**](https://github.com/Robert4J) · [LinkedIn](https://www.linkedin.com/in/robert-j%C3%BCrgensen-563b74411/) | Frontend, checking development, deployment | JavaFX, checking core and workflows, IFC/IDS hardening, tests, reports, delivery |
| **Julian Keller** · [LinkedIn](https://www.linkedin.com/in/julian-wladyslaw-keller/) | MCP interface and documentation | MCP development and technical and user-facing documentation |

**Project contact:** <Maximilian.Sternal@tu-berlin.de>

### Collaboration Across Modules

* IFC processing and checking are closely linked and are developed jointly by Sophie Lipskaya and Robert Jürgensen.
* Maximilian Sternal is responsible for system architecture and the technical direction, coordinated with the team.
* Julian Keller develops the MCP interface and documentation and integrates them with the backend, frontend, and checking logic.

## Support

*planen-bauen 4.0* / BIM Deutschland supported the hackathon and subsequent work, particularly with the BIM Portal API, starter kit, and IFC/APSTEX tooling.

* **Eike Tauscher**
* **Michael Theiler**

This support provided an important foundation but does not imply official project responsibility for CheckMate.

## Work Packages & Evidence

| Work package | Scope | Responsibility | Evidence in the repository |
|---|---|---|---|
| Hackathon & initial prototype | Concept, starter kit, initial JavaFX/IFC implementation | Maximilian Sternal, Sophie Lipskaya, team | Branch `0.1`, pitch, starter kit |
| BIM Portal & IFC | API integration and IFC processing | Sophie Lipskaya | Branch `0.2`, backend client, checking workflows |
| Checking & frontend | IDS/LOIN validation, interface, reporting, deployment | Robert Jürgensen | Checking core, GUI, tests, application |
| Architecture & integration | Module boundaries, technical decisions, integration | Maximilian Sternal | Repository, architecture documentation, releases |
| MCP & documentation | AI integration and technical user documentation | Julian Keller | MCP server and project pages |

## Licensing & AI Tools

### Licensing

* **CheckMate source code:** [Apache License 2.0](https://github.com/max-sternal/CheckMate_BIMPortal/blob/v0.3.1/LICENSE).
* **APSTEX IFC Toolbox:** Proprietary third-party library provided for the hackathon. It is not covered by Apache 2.0 and is not distributed in the repository.
* **IfcOpenShell / ifctester:** Open-source tools licensed under LGPL-3.0-or-later and used as external testing and reference tools.
* **Starter kit and reference material:** Originating from *planen-bauen 4.0* / BIM Deutschland.

### Transparent Use of AI Tools

AI coding assistants support development, including traceable `Co-Authored-By` notices. Architecture, technical decisions, specification checks, and approvals remain with the human contributors.
