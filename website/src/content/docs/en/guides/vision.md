---
title: Vision & Mission
description: Accelerating bureaucracy instead of lowering standards — how CheckMate supports public authorities, clients, and planners with LOIN compliance.
---

## Background: Accelerating Bureaucracy Without Compromising Standards

Requirements for climate protection, structural safety, accessibility, lifecycle documentation, and digital compliance in public construction are growing for good reasons. The answer is **not** to cut standards, safety, or environmental protection, but to **accelerate verification processes** through clear, machine-readable requirements and reliable checking tools.

The **German Federal BIM Portal** (`via.bund.de/bim`) already publishes more than **1,500 public LOINs** (Level of Information Need) and full **AIA projects** in the open buildingSMART **IDS 1.0** standard. Yet in day-to-day practice a bottleneck remains:

- **Planners** have to navigate thousands of properties and object templates to identify which requirements apply to a given asset and project phase.
- **Reviewers in public authorities and client organizations** face large IFC models with hundreds or thousands of elements and must assess reliably whether required information is complete, correctly typed, and compliant.

## The CheckMate Approach: Determinism + AI Assistance

> CheckMate prevents planners and reviewers from being put in "checkmate" by a flood of data and requirements — acting as an independent, swappable, and secure **Mate** in the checking workflow of the future.

CheckMate strictly separates two concerns:

1. **Deterministic, auditable rule checking in code:**
   - An AI model never guesses whether a requirement passes.
   - The core engine checks IFC models strictly against IDS 1.0 (matching `ifctester`), reports datatype and spelling mismatches explicitly, and honestly marks inapplicable or partially evaluated specifications as **"nicht prüfbar" (not checkable)** or **"erfüllt (soweit ausgewertet)"** — never silently reporting a pass.
2. **Human-machine integration via open standards (MCP):**
   - Over the **Model Context Protocol (MCP)**, any AI assistant — whether running locally on your own hardware via **Ollama** / **Strata** with **Qwen**, through open cloud models like **Mistral**, or inside hosts like **Antigravity**, **OpenCode**, and **Claude** — can query models, rank fitting LOINs, trigger checks, and explain deterministic findings in plain language.
   - Final decisions remain 100% with humans, and the AI model stays completely interchangeable.

## Origin & Project Status

CheckMate is a **private open-source project** licensed under **Apache License 2.0**, originating from the **BIM Portal Hackathon in Berlin (September 2025)** by employees and alumni of **TU Berlin**.

Long-term perspectives beyond checking:
- Supporting authorities and clients with **LOIN / IDS authoring and maintenance**.
- Adding **geometric and documentation LOIN checks (ISO 7817-2)** as well as **BCF issue reporting** and **3D visualization** in CheckMate Lite (`0.4`).
