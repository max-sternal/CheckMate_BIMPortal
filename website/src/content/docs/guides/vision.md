---
title: Vision & Idee
description: Bürokratie-Beschleunigung statt Bürokratie-Abbau — wie CheckMate Behörden, Auftraggeber und Planende bei LOIN-Prüfungen unterstützt.
---

## Ausgangslage: Warum „Bürokratie-Beschleunigung“?

Im öffentlichen Bauwesen steigen Anforderungen an Klimaschutz, Sicherheit, Barrierefreiheit, Lebenszyklusdaten und digitale Nachweispflichten — aus gutem Grund. Die Antwort auf komplexe Prüfprozesse darf daher nicht **Bürokratie-Abbau auf Kosten von Standards, Sicherheit oder Umweltschutz** sein, sondern **Bürokratie-Beschleunigung durch klare, maschinenlesbare Anforderungen und verlässliche Prüfwerkzeuge**.

Mit dem **BIM-Portal des Bundes** (`via.bund.de/bim`) stehen heute über **1.500 öffentliche LOINs** (Level of Information Need) und komplette **AIA-Projekte** im offenen buildingSMART-Standard **IDS 1.0** bereit. In der Praxis entsteht dennoch ein Engpass:

- **Planer:innen** müssen aus tausenden Merkmalen und Objektvorlagen herausfinden, welche Anforderungen für ein konkretes Bauwerk und eine Leistungsphase gelten.
- **Prüfer:innen in Behörden und bei Auftraggebern** stehen vor großen IFC-Modellen mit hunderten bis tausenden Bauteilen und müssen belastbar beurteilen, ob die geforderten Informationen vollständig, typgerecht und korrekt vorliegen.

## Der CheckMate-Ansatz: Determinismus + KI-Begleitung

> CheckMate verhindert, dass Planende und Prüfende durch eine Flut von Daten und Anforderungen „Schachmatt“ gesetzt werden — und agiert als unabhängiger, austauschbarer und sicherer **Mate (Partner)** im Prüfprozess.

Dafür trennt CheckMate zwei Verantwortlichkeiten strikt voneinander:

1. **Deterministische, nachvollziehbare Prüfung im Code:**
   - Ein KI-Modell „rät“ niemals, ob eine Anforderung erfüllt ist.
   - Der Prüfkern vergleicht das IFC-Modell streng nach IDS 1.0 (wie `ifctester`), benennt Datentyp- oder Schreibweisenabweichungen explizit und meldet nicht anwendbare oder nur teilweise auswertbare Spezifikationen ehrlich als **„nicht prüfbar“** bzw. **„erfüllt (soweit ausgewertet)“** — niemals stillschweigend als „bestanden“.
2. **Mensch-Maschine-Integration über offene Schnittstellen (MCP):**
   - Über das **Model Context Protocol (MCP)** kann eine KI (ob lokal auf eigener Hardware via **Ollama** / **Strata** mit **Qwen**, über offene Cloud-Modelle wie **Mistral** oder in Clients wie **Antigravity**, **OpenCode** und **Claude**) Fragen stellen, passende LOINs vorschlagen, Prüfungen anstoßen und die deterministischen Befunde verständlich für Menschen aufbereiten.
   - Die Entscheidung und Verantwortung bleibt dabei vollständig beim Menschen — und das KI-Modell bleibt jederzeit austauschbar.

## Herkunft & Status des Projekts

CheckMate ist ein **privates Open-Source-Projekt** unter **Apache License 2.0**, entstanden auf Basis des **BIM-Portal-Hackathons im September 2025 in Berlin** durch Mitarbeitende und Alumni der **TU Berlin**.

Langfristige Perspektiven über das reine Prüfen hinaus:
- Unterstützung bei der **Erstellung und Pflege von LOINs / IDS-Anforderungen** für Behörden und Auftraggeber.
- Erweiterung um **geometrische und dokumentationsbezogene LOIN-Aspekte (ISO 7817-2)** sowie **BCF-Prüfberichte** und **3D-Visualisierung** in CheckMate Lite (`0.4`).
