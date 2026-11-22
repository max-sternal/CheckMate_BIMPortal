---
title: Mitwirkende & Transparenz
description: Herkunft, Verantwortlichkeiten, Unterstützung, Lizenzen und transparent dokumentierter KI-Einsatz bei CheckMate.
---

## Projekt & Herkunft

**CheckMate** ist ein **privates Open-Source-Projekt** unter der **Apache License 2.0**. Es entstand aus der Einreichung von *Team CheckMate* beim **BIM-Portal-Hackathon Berlin** am 22. und 23. September 2025 und wird gemeinschaftlich weiterentwickelt.

:::note[Abgrenzung & Trägerschaft]
- CheckMate ist **kein offizielles Produkt des Bundes oder der TU Berlin**.
- API-Client, Starterkit und Referenzmaterial stammen aus dem öffentlichen Hackathon-Starterkit von *planen-bauen 4.0* / BIM Deutschland.
:::

## Team & Verantwortlichkeiten

| Person | Verantwortung | Beiträge |
|---|---|---|
| [**Maximilian Sternal**](https://github.com/max-sternal) · [LinkedIn](https://www.linkedin.com/in/maximilian-sternal/) | Projektleitung, Organisation, Systemarchitektur | Projektidee und Koordination, Repository-Aufbau, technische Leitentscheidungen, Releases, Dokumentation |
| [**Sophie Lipskaya**](https://github.com/SOPHIEfree) · [LinkedIn](https://www.linkedin.com/in/sophie-lipskaya/) | IFC und BIM-Portal-Backend | Portal-Client, IFC-Verarbeitung, lauffähige Oberfläche, initiale IDS-Prüfung, Headless-CLI |
| [**Robert Jürgensen**](https://github.com/Robert4J) · [LinkedIn](https://www.linkedin.com/in/robert-j%C3%BCrgensen-563b74411/) | Frontend, Checking-Entwicklung, Deployment | JavaFX, Prüfkern und Prüfabläufe, IFC-/IDS-Härtung, Tests, Berichte, Bereitstellung |
| **Julian Keller** · [LinkedIn](https://www.linkedin.com/in/julian-wladyslaw-keller/) | MCP-Schnittstelle und Dokumentation | MCP-Entwicklung sowie technische und nutzungsbezogene Dokumentation |

**Projektkontakt:** [Maximilian.Sternal@tu-berlin.de](mailto:Maximilian.Sternal@tu-berlin.de)

### Zusammenarbeit über Modulgrenzen

- IFC-Verarbeitung und Checking greifen ineinander und werden von Sophie Lipskaya und Robert Jürgensen gemeinsam weiterentwickelt.
- Systemarchitektur und technische Zielrichtung werden von Maximilian Sternal verantwortet und mit dem Team abgestimmt.
- MCP-Schnittstelle und Dokumentation werden von Julian Keller bearbeitet und mit Backend, Frontend und Prüflogik verzahnt.

## Unterstützung

*planen-bauen 4.0* / BIM Deutschland unterstützte den Hackathon und die anschließende Arbeit insbesondere mit der BIM-Portal-API, dem Starterkit sowie IFC- und APSTEX-Werkzeugen.

- **Eike Tauscher**
- **Michael Theiler**

Die Unterstützung bildete eine wichtige Grundlage, begründet aber keine offizielle Projektverantwortung für CheckMate.

## Arbeitspakete & Nachweise

| Arbeitspaket | Inhalt | Verantwortungsbereich | Nachweis im Repository |
|---|---|---|---|
| Hackathon & Erstprototyp | Konzept, Starterkit, erster JavaFX-/IFC-Durchstich | Maximilian Sternal, Sophie Lipskaya, Team | Branch `0.1`, Pitch, Starterkit |
| BIM-Portal & IFC | API-Anbindung und IFC-Verarbeitung | Sophie Lipskaya | Branch `0.2`, Backend-Client, Prüfabläufe |
| Checking & Frontend | IDS-/LOIN-Prüfung, Oberfläche, Berichte, Deployment | Robert Jürgensen | Prüfkern, GUI, Tests, Anwendung |
| Architektur & Integration | Modulgrenzen, technische Entscheidungen, Integration | Maximilian Sternal | Repository, Architekturdokumentation, Releases |
| MCP & Dokumentation | KI-Anbindung und technische Benutzerdokumentation | Julian Keller | MCP-Server und Projektseiten |

## Lizenzen & KI-Werkzeuge

### Lizenztransparenz

- **CheckMate-Quellcode:** [Apache License 2.0](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.1/LICENSE).
- **APSTEX IFC Toolbox:** Proprietäre Drittbibliothek, die für den Hackathon bereitgestellt wurde. Sie ist nicht Teil der Apache-2.0-Lizenz und wird nicht im Repository verbreitet.
- **IfcOpenShell / ifctester:** Open-Source-Werkzeuge unter LGPL-3.0-or-later, die als externe Test- und Referenzwerkzeuge eingesetzt werden.
- **Starterkit und Referenzmaterial:** Herkunft aus *planen-bauen 4.0* / BIM Deutschland.

### Transparenter Einsatz von KI-Werkzeugen

KI-Coding-Assistenten unterstützen die Entwicklung; dies ist unter anderem über `Co-Authored-By`-Hinweise nachvollziehbar. Architektur, fachliche Entscheidungen, Spezifikationsprüfung und Freigaben verbleiben bei den menschlichen Mitwirkenden.
