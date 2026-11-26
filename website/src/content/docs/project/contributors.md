---
title: Mitwirkende & Transparenz
description: Herkunft, Verantwortlichkeiten, Danksagung, Lizenzen und transparent dokumentierter KI-Einsatz bei CheckMate.
---

## Projekt & Herkunft

**CheckMate** ist ein **privates Open-Source-Projekt** unter der **Apache License 2.0**. Es entstand aus der Einreichung von *Team CheckMate* beim [BIM-Portal-Hackathon Berlin](https://www.bimdeutschland.de/veranstaltungen/hackathon-2025) am 22. und 23. September 2025 und wird gemeinschaftlich weiterentwickelt.

:::note[Abgrenzung & Trägerschaft]
- CheckMate ist **kein offizielles Produkt des Bundes oder der TU Berlin**.
- API-Client und Starterkit stammen aus dem [öffentlichen Hackathon-Starterkit](https://github.com/pb40development/bim-portal) von *planen-bauen 4.0* / BIM Deutschland.
:::

## Team & Verantwortlichkeiten

| Person | Verantwortung | Beiträge |
|---|---|---|
| [**Maximilian Sternal**](https://github.com/max-sternal) · [LinkedIn](https://www.linkedin.com/in/maximilian-sternal/) | Projektleitung, Organisation, Systemarchitektur | Projektidee und Koordination, Repository-Aufbau, technische Leitentscheidungen, Releases, Dokumentation |
| [**Sophie Lipskaya**](https://github.com/SOPHIEfree) · [LinkedIn](https://www.linkedin.com/in/sophie-lipskaya/) | IFC und BIM-Portal-Backend | Portal-Client, IFC-Verarbeitung, lauffähige Oberfläche, initiale IDS-Prüfung, Headless-CLI, KI-Chat (`0.3.3`) |
| [**Robert Jürgensen**](https://github.com/Robert4J) · [LinkedIn](https://www.linkedin.com/in/robert-j%C3%BCrgensen-563b74411/) | Frontend, Checking-Entwicklung, Deployment | JavaFX, Prüfkern und Prüfabläufe, IFC-/IDS-Härtung, Tests, Berichte, Bereitstellung |
| **Julian Keller** · [LinkedIn](https://www.linkedin.com/in/julian-wladyslaw-keller/) | MCP-Schnittstelle, Testmodelle und Dokumentation | MCP-Entwicklung, aufbereitete IFC-Testmodelle sowie technische und nutzungsbezogene Dokumentation |

**Projektkontakt:** [Maximilian.Sternal@tu-berlin.de](mailto:Maximilian.Sternal@tu-berlin.de)

### Zusammenarbeit über Modulgrenzen

- IFC-Verarbeitung und Checking greifen ineinander und werden von Sophie Lipskaya und Robert Jürgensen gemeinsam weiterentwickelt.
- Systemarchitektur, Weiterentwicklung der `0.4`-Linie und technische Zielrichtung werden von Maximilian Sternal verantwortet und mit dem Team abgestimmt.
- MCP-Schnittstelle, Testmodelle und Dokumentation werden von Julian Keller bearbeitet und mit Backend, Frontend und Prüflogik verzahnt.

## Danksagung & Unterstützung

Ein besonderer Dank des gesamten Teams gilt allen Beteiligten, die den **BIM-Portal-Hackathon 2025** möglich gemacht und CheckMate von der ersten Idee an unterstützt haben:

- **Ausrichtung & fachliche Unterstützung:** Herzlichen Dank an *planen-bauen 4.0* und *BIM Deutschland – Zentrum für die Digitalisierung des Bauwesens* – insbesondere an **Dr.-Ing. Eike Tauscher** und **Michael Theiler** sowie das gesamte Organisations- und Mentoring-Team – für die hervorragende Ausrichtung des Hackathons, die Bereitstellung der BIM-Portal-API und des Starterkits sowie die kontinuierliche fachliche Unterstützung.
- **Bereitstellung der Praxis- und Beispielmodelle:** Vielen Dank für die Bereitstellung realitätsnaher Modelle und Anforderungen, allen voran an **BIM.Hamburg** und insbesondere **Cornelius Lüdtke** (LSBG Hamburg) sowie **Daniel Dombeck** (HPA), deren Hamburger Infrastruktur- und Brückenmodelle das zentrale Praxisbeispiel für die Entwicklung von CheckMate bildeten.
- **Finale Übergabe, Jury & Hosting:** Ein herzliches Dankeschön gilt Bundesbauministerin **Verena Hubertz** (*Bundesministerium für Wohnen, Stadtentwicklung und Bauwesen, BMWSB*) für die feierliche finale Preisübergabe, der **Jury** für das entgegengebrachte Vertrauen sowie dem **Bundesministerium für Verkehr (BMV)** für das erstklassige Hosting dieses hervorragenden Events in Berlin – ein Format, das Verwaltung, Forschung und Praxis auf Augenhöhe zusammenbringt und unbedingt **öfter bzw. regelmäßig stattfinden sollte**!

Diese ideelle und fachliche Unterstützung bildete ein wichtiges Fundament für das Projekt, begründet jedoch keine offizielle Projektverantwortung für CheckMate.

## Arbeitspakete & Nachweise

| Arbeitspaket | Inhalt | Verantwortungsbereich | Nachweis im Repository |
|---|---|---|---|
| Hackathon & Erstprototyp | Konzept, Starterkit, erster JavaFX-/IFC-Durchstich | Maximilian Sternal, Sophie Lipskaya, Team | Branch `0.1`, Pitch, Starterkit |
| BIM-Portal & IFC | API-Anbindung, IFC-Verarbeitung und GUI-Chat | Sophie Lipskaya | Branches `0.2`–`0.3.3`, Backend-Client, Prüfabläufe |
| Checking & Frontend | IDS-/LOIN-Prüfung, Oberfläche, Berichte, Deployment | Robert Jürgensen | Prüfkern, GUI, Tests, Anwendung |
| Architektur, 3D-Workbench & Releases | Modulgrenzen, TypeScript/IFClite-Architektur (`0.4`), Integration | Maximilian Sternal | Repository, Architekturdokumentation, `0.4.x`, Releases |
| MCP, Testmodelle & Dokumentation | KI-Anbindung, BIM-Portal-Testmodelle und Benutzerdokumentation | Julian Keller | MCP-Server, `docs/`, Projektseiten |

## Lizenzen & KI-Werkzeuge

### Lizenztransparenz

- **CheckMate-Quellcode:** [Apache License 2.0](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.4.1-beta/LICENSE).
- **IFClite (`@ifc-lite/*`):** Quelloffene TypeScript-/WebAssembly-Pakete (MPL-2.0 / MIT) für IFC-Parsing, 3D-Geometrie, IDS-Prüfung und BCF-Export in der `0.4`-Linie.
- **APSTEX IFC Toolbox:** Proprietäre Drittbibliothek, die ausschließlich beim Hackathon und in `0.3.1` verwendet wurde. Sie wird nicht im Repository verbreitet; weder `0.3.2` / `0.3.3-stable` noch `0.4.x` benötigen sie.
- **IfcOpenShell / ifctester:** Open-Source-Werkzeuge unter LGPL-3.0-or-later, die als externe Test- und Referenzwerkzeuge eingesetzt werden.
- **Starterkit und Referenzmaterial:** Herkunft aus *planen-bauen 4.0* / BIM Deutschland.

### Transparenter Einsatz von KI-Werkzeuge

KI-Coding-Assistenten unterstützen die Entwicklung; dies ist unter anderem über `Co-Authored-By`-Hinweise nachvollziehbar. Architektur, fachliche Entscheidungen, Spezifikationsprüfung und Freigaben verbleiben bei den menschlichen Mitwirkenden.
