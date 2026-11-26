---
title: Contributors & Transparency
description: CheckMate's origin, responsibilities, acknowledgments, licensing, and transparent use of AI tools.
---

## Project & Origin

**CheckMate** is a **private open-source project** licensed under the **Apache License 2.0**. It originated from *Team CheckMate*'s submission at the [BIM Portal Hackathon Berlin](https://www.bimdeutschland.de/veranstaltungen/hackathon-2025) on 22 and 23 September 2025 and is developed collaboratively.

:::note[Institutional Distinction]
- CheckMate is **not an official product of the German Federal Government or TU Berlin**.
- The API client and starter kit originate from the [public hackathon starter kit](https://github.com/pb40development/bim-portal) by *planen-bauen 4.0* / BIM Deutschland.
:::

## Team & Responsibilities

| Contributor | Responsibility | Contributions |
|---|---|---|
| [**Maximilian Sternal**](https://github.com/max-sternal) · [LinkedIn](https://www.linkedin.com/in/maximilian-sternal/) | Project lead, organisation, system architecture | Project concept and coordination, repository setup, technical decisions, releases, documentation |
| [**Sophie Lipskaya**](https://github.com/SOPHIEfree) · [LinkedIn](https://www.linkedin.com/in/sophie-lipskaya/) | IFC and BIM Portal backend | Portal client, IFC processing, runnable interface, initial IDS validation, headless CLI, in-GUI AI chat (`0.3.3`) |
| [**Robert Jürgensen**](https://github.com/Robert4J) · [LinkedIn](https://www.linkedin.com/in/robert-j%C3%BCrgensen-563b74411/) | Frontend, checking development, deployment | JavaFX, checking core and workflows, IFC/IDS hardening, tests, reports, delivery |
| **Julian Keller** · [LinkedIn](https://www.linkedin.com/in/julian-wladyslaw-keller/) | MCP interface, test models, and documentation | MCP development, curated IFC test models, and technical and user-facing documentation |

**Project contact:** [Maximilian.Sternal@tu-berlin.de](mailto:Maximilian.Sternal@tu-berlin.de)

### Collaboration Across Modules

- IFC processing and checking are closely linked and are developed jointly by Sophie Lipskaya and Robert Jürgensen.
- Maximilian Sternal is responsible for system architecture, the `0.4` TypeScript/IFClite track, and the technical direction, coordinated with the team.
- Julian Keller develops the MCP interface, test models, and documentation and integrates them with the backend, frontend, and checking logic.

## Acknowledgments & Support

The entire team extends heartfelt thanks to everyone who made the **BIM Portal Hackathon 2025** possible and supported CheckMate from the very beginning:

- **Hackathon Organisation & Technical Support:** Sincere thanks to *planen-bauen 4.0* and *BIM Deutschland – National Centre for the Digitalisation of Construction* — in particular **Dr.-Ing. Eike Tauscher** and **Michael Theiler**, along with the entire organising and mentoring team — for hosting and supporting the hackathon, providing the BIM Portal API and starter kit, and offering guidance throughout.
- **Provision of Real-World Models:** Special thanks for providing practical BIM models and requirements, above all to **BIM.Hamburg** and especially **Cornelius Lüdtke** (LSBG Hamburg) as well as **Daniel Dombeck** (HPA), whose Hamburg infrastructure and bridge models served as a cornerstone for developing and testing CheckMate.
- **Final Award Presentation, Jury & Hosting:** Warm thanks to Federal Minister for Housing, Urban Development and Building **Verena Hubertz** (*BMWSB*) for the final award presentation, to the **Jury** for their trust, and to the **Federal Ministry for Transport (BMV)** for hosting this outstanding event in Berlin — an inspiring format bringing together public administration, research, and industry that **should definitely take place more often and on a regular basis**!

This support provided an important foundation for the project, while not implying official institutional responsibility for CheckMate.

## Work Packages & Evidence

| Work package | Scope | Responsibility | Evidence in the repository |
|---|---|---|---|
| Hackathon & initial prototype | Concept, starter kit, initial JavaFX/IFC implementation | Maximilian Sternal, Sophie Lipskaya, team | Branch `0.1`, pitch, starter kit |
| BIM Portal & IFC | API integration, IFC processing, and in-GUI chat | Sophie Lipskaya | Branches `0.2`–`0.3.3`, backend client, checking workflows |
| Checking & frontend | IDS/LOIN validation, interface, reporting, deployment | Robert Jürgensen | Checking core, GUI, tests, application |
| Architecture, 3D workbench & releases | Module boundaries, TypeScript/IFClite architecture (`0.4`), integration | Maximilian Sternal | Repository, architecture documentation, `0.4.x`, releases |
| MCP, test models & documentation | AI integration, BIM Portal test models, and user documentation | Julian Keller | MCP server, `docs/`, project pages |

## Licensing & AI Tools

### Licensing

- **CheckMate source code:** [Apache License 2.0](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/LICENSE).
- **IFClite (`@ifc-lite/*`):** Open-source TypeScript/WebAssembly packages (MPL-2.0 / MIT) powering IFC parsing, 3D geometry, IDS validation, and BCF export in the `0.4` track.
- **APSTEX IFC Toolbox:** Proprietary third-party library used solely during the hackathon and in `0.3.1`. It is not distributed in the repository; neither `0.3.2` / `0.3.3-stable` nor `0.4.x` require it.
- **IfcOpenShell / ifctester:** Open-source tools licensed under LGPL-3.0-or-later and used as external testing and reference tools.
- **Starter kit and reference material:** Originating from *planen-bauen 4.0* / BIM Deutschland.

### Transparent Use of AI Tools

AI coding assistants support development, including traceable `Co-Authored-By` notices. Architecture, technical decisions, specification checks, and approvals remain with the human contributors.
