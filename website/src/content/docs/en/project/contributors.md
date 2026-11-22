---
title: Contributors & Transparency
description: Transparent record of CheckMate's origin, module ownership, commit history, licensing, AI tooling disclosure, and public-sector compliance reporting.
---

## 1. Project Status & Origin

**CheckMate** is a **private open-source project** licensed under **Apache License 2.0**. It originated from *Team CheckMate*'s submission at the **BIM Portal Hackathon Berlin (22–23 September 2025)** and is developed privately by **employees and alumni of TU Berlin**.

:::note[Institutional Distinction]
- CheckMate is **not an official product of the German Federal Government or TU Berlin**, but an independent private open-source tool supporting public authorities, clients, and planners working with the German Federal BIM Portal (`via.bund.de/bim`).
- The original API starter clients (`bim-portal/Java_client/openapi-generated`, `bim-portal-enhanced`, `Python_client`) and reference material (`DocsBIMDeutschland/`) originate from the public hackathon starter kit (`pb40development/bim-portal`, *planen-bauen 4.0* / BIM Deutschland).
:::

---

## 2. Responsibilities & Contributions by Person

The table below is derived directly from Git history across all branches (`0.1`, `0.2`, `0.3.1` / `webView`, as of **22 Nov 2026**, 23 commits total). For fair and auditable metrics, **handwritten code & documentation are separated from generated files, upstream starter code, and IFC model data**.

| Contributor (GitHub / Affiliation) | Active Period | Commits | Handwritten Code & Config (`+`/`-`) | Docs & Tooling (`+`/`-`) | Data / Vendor / Generated | Areas of Responsibility & Work Packages |
|---|---|---|---|---|---|---|
| **Maximilian Sternal** (`@max-sternal`, TU Berlin) | 22 Sept 2025 – 11 Nov 2025 *(+ project lead & 0.3.1 release)* | **3** *(branch `0.1` / `main`)* | `+15,540 / -1` *(incl. hackathon starter kit import + initial `test.fx` `+469` lines)* | `+437 / -0` | `+43,306` *(OpenAPI client, `DocsBIMDeutschland`, pitch PDFs)* | Project initiation & vision, repository setup (Apache 2.0), archiving hackathon baseline (`0.1`), initial JavaFX/APSTEX prototype, coordination, release & documentation steering (`0.3.1` / `0.4`) |
| **SOPHIEfree** (`@SOPHIEfree`) | 29 Oct 2026 – 30 Oct 2026 | **4** *(branch `0.2`)* | `+3,339 / -1,383` | `+185 / -5` | `+189,020 / -88,074` *(bundled IFC bridge model)* | BIM Portal API client fixes (anonymous public access, `NON_NULL` serialization, `LenientStringDeserializer`), runnable JavaFX GUI, initial `ids/` engine, initial headless CLI (`LoinCheckCli`), bundling TBW1 bridge model |
| **Robert Jürgensen** (`@Robert4J`, TU Berlin) | 19 Nov 2026 – 22 Nov 2026 | **16** *(branch `0.3.1` / `webView`)* | `+11,187 / -1,794` | `+1,814 / -205` | `+101 / -59` *(IFC test fixtures, Eclipse cleanup)* | Core architecture (`CheckService`, `catalogue/`, `report/`, `mcp/`), IDS 1.0 & `ifctester` parity, APSTEX `RelatedObjects` bugfix, URL/GitHub/LFS loading, AIA project checks, 217-test suite with `IfcOpenShell` reference, Java MCP server, `README.md`, `WORKFLOW.md`, `STATUS.md`, GUI driver skill |

### Module Ownership Summary
- **Hackathon Baseline, Project Coordination & Vision:** Maximilian Sternal (with Team CheckMate, Sept 2025)
- **BIM Portal API Fixes & Initial End-to-End IDS/GUI Prototype (`0.2`):** SOPHIEfree
- **Checking Core (`ids/`, `catalogue/`, `check/`, `report/`), IFC Reader Hardening, Test Suite & MCP Server (`0.3.1`):** Robert Jürgensen
- **Co-owned Modules:** `com.bimportal.hackathon.ids/` (initiated by SOPHIEfree, expanded & hardened by Robert Jürgensen) and `test.fx/` (hackathon prototype → runnable GUI by SOPHIEfree → refactored by Robert Jürgensen)

---

## 3. Transparent Effort & Compliance Accounting (Ministry Requirements)

:::caution[Draft Pending Alignment — Version 0.3.1]
This section provides a structured template for transparent work-package accounting and compliance reporting aligned with applicable public-sector / ministry requirements and obligations. Exact effort figures, legal notices, and reporting fields will be finalized jointly with the team.
:::

| Work Package (WP) | Version | Owner | Status | Deliverable / Evidence (Git & Artifacts) | Effort / Accounting *(to be aligned)* |
|---|---|---|---|---|---|
| **WP 1:** Hackathon concept, starter kit integration & initial prototype | `0.1` | Maximilian Sternal & Hackathon Team | Completed (frozen) | Branch `0.1`, pitch deck `CheckMate_Hackathon_BIMPortal_2025.pdf` | *to be finalized* |
| **WP 2:** API client fixes, runnable GUI & initial IDS validation | `0.2` | SOPHIEfree | Completed (frozen) | Branch `0.2` (commits `eeb136e`–`bc237bb`) | *to be finalized* |
| **WP 3:** Deterministic IDS 1.0 core, AIA/catalogue, `IfcOpenShell` tests & MCP server | `0.3.1` | Robert Jürgensen | Active / Completed | Branch `0.3.1` (commits `1236655`–`c42a1b5`, 217 tests) | *to be finalized* |
| **WP 4:** Bilingual documentation (DE/EN), GitHub Pages, CI & transparency record | `0.3.1` | Maximilian Sternal / Team | In Progress | Branch `0.3.1` (`website/`, `.github/workflows/`) | *to be finalized* |
| **WP 5:** APSTEX-free Java IFC reader & MCP upgrade (Nov 2026 standards) | `0.3.2` | Team | Planned (End of Nov 2026) | Branch `0.3.2` | *to be finalized* |
| **WP 6:** *CheckMate Lite* on `IFClite` (no Java, 3D MCP App, new test suite) | `0.4` | Team | Work in Progress | Branch `0.4` | *to be finalized* |

---

## 4. License & AI Tooling Disclosure

### Licensing
- **CheckMate source code:** [Apache License 2.0](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/LICENSE).
- **APSTEX IFC Toolbox (`0.1`–`0.3.1` only):** Proprietary third-party library provided for the hackathon — **not** covered by Apache 2.0 and **not** distributed in Git. Removed in `0.3.2` and `0.4`.
- **IFClite (`0.4` *CheckMate Lite*):** Open-source under **Mozilla Public License 2.0 (MPL-2.0)** — compatible with Apache 2.0 at file level.
- **IfcOpenShell / ifctester (test reference):** Open-source (LGPL-3.0), used strictly as an external testing and reference oracle.

### AI Tooling Disclosure
AI coding assistants were used transparently as development aids (documented via `Co-Authored-By` trailers in Git history, `.github/copilot-instructions.md`, and `.claude/skills/run-checkmate/`). All architectural decisions, BIM Portal conformance analyses, and verification against `IfcOpenShell` / `ifctester` remain under human review and responsibility.
