---
title: Einordnung & Grundlagen
description: CheckMate im Kontext von BIM-Portal, offenen Standards und Informationsanforderungen.
---

## Ein Werkzeug für Informationsanforderungen

Anforderungen an Sicherheit, Barrierefreiheit, Umwelt und Dokumentation sind wichtig. CheckMate möchte die Arbeit mit ihnen erleichtern: **Bürokratie-Beschleunigung statt Bürokratie-Abbau** – durch nachvollziehbare digitale Prüfungen, nicht durch niedrigere Standards.

Auftraggeber definieren, welche Informationen sie zu bestimmten Zeitpunkten für ein Projekt benötigen. Das BIM-Portal stellt dafür unter anderem Merkmale, Informationsbedarfstiefen (Level of Information Need, LOIN) und Auftraggeber-Informationsanforderungen (AIA) bereit.

Planer:innen müssen die passenden Anforderungen für ihr Projekt im Blick behalten; Prüfer:innen vergleichen sie mit den gelieferten Modellen. CheckMate soll beide dabei unterstützen, damit die Menge an Informationen sie nicht „schachmatt“ setzt.

CheckMate gleicht IFC-Modelle mit maschinenlesbaren Anforderungen im IDS-Format ab. Anforderungen können aus dem BIM-Portal oder aus lokalen IDS-Dateien stammen. Das Werkzeug unterstützt die Prüfung einzelner Informationen; es ersetzt weder die fachliche Bewertung noch die vollständige Prüfung einer AIA oder die Anwendung einer Norm.

Die regelbasierte Prüfung erfolgt im Programm. Eine KI-Anwendung kann über die optionale MCP-Schnittstelle Prüfungen aufrufen und Ergebnisse erläutern. Das Prüfergebnis und eine KI-Erklärung sind getrennt zu betrachten; sensible Daten dürfen nur entsprechend den geltenden Vorgaben verarbeitet werden.

## Bundesinitiative und BIM-Portal

- [BIM Deutschland – BIM-Portal](https://www.bimdeutschland.de/bim-portal): Einordnung und Zugang zu den Modulen des Bundes.
- [BIM Deutschland – Auftraggeber-Informationsanforderungen](https://www.bimdeutschland.de/bim-wissen/auftraggeber-informationsanforderungen): AIA, LOIN und Muster-AIA.
- [BIM-Portal-Benutzerhandbuch, Version 4 (2025)](https://www.bimdeutschland.de/fileadmin/media/Bilder/Neue_Website_2024/Veranstaltungen/BIMD_VAS/25-06-20_BIM-Portal_Benutzerhandbuch_V4.pdf): Aufbau und Bedienung des Portals.
- [Stufenplan Digitales Planen und Bauen](https://www.bmv.de/SharedDocs/DE/Publikationen/G/stufenplan-digitales-bauen.pdf?__blob=publicationFile) und [Masterplan BIM Bundesfernstraßen](https://www.bimdeutschland.de/fileadmin/media/Downloads/Download-Liste/Strasse/BIM_Masterplan_Bundesfernstrassen_barrierefrei.pdf): Strategien des Bundes; weitere Unterlagen unter [BIM Deutschland – Downloads](https://www.bimdeutschland.de/bim-wissen/downloads).
- [BIM-Portal REST-API-Dokumentation](https://bimdeutschland.github.io/BIM-Portal-REST-API-Dokumentation/): Schnittstelle und Daten des Portals.
- [BIM-Portal-Hackathon 2025](https://www.bimdeutschland.de/veranstaltungen/hackathon-2025) und [Starterkit von planen-bauen 4.0](https://github.com/pb40development/bim-portal): Ursprung von CheckMate; siehe auch [planen-bauen 4.0](https://planen-bauen40.de/).

## Normen und offene Standards

Die folgenden Dokumente erläutern den fachlichen Zusammenhang. Ihre Nennung ist **keine Aussage, dass CheckMate die jeweilige Norm vollständig umsetzt oder eine Normkonformität nachweist**. Der Prüfumfang steht unter [IFC und IDS](/CheckMate_BIMPortal/architecture/ifc-ids-checking/).

| Dokument | Bezug |
|---|---|
| [DIN EN ISO 7817-1:2024-11](https://www.dinmedia.de/de/norm/din-en-iso-7817-1/382382712) | Konzepte und Grundsätze der Informationsbedarfstiefe; ersetzt DIN EN 17412-1. |
| [ISO/DIS 7817-3](https://www.iso.org/standard/89077.html) | Entwurf für Datenmodell und Schema der Informationsbedarfstiefe; noch in Entwicklung. |
| [DIN EN ISO 19650-1](https://www.dinmedia.de/en/standard/din-en-iso-19650-1/299953244) / [ISO 19650-1](https://www.iso.org/standard/68078.html) und [ISO 19650-2](https://www.iso.org/standard/68080.html) | Grundsätze und Informationsmanagement in der Bereitstellungsphase. |
| [ISO 29481-1:2025](https://www.iso.org/standard/88515.html) | Methodik zur Beschreibung von Informationsaustausch und Prozessen (Information Delivery Manual). |
| [ISO 23386](https://www.iso.org/standard/75401.html) und [ISO 23387](https://www.iso.org/standard/75403.html) | Beschreibung und Pflege von Merkmalen sowie Datenvorlagen für Bauobjekte. |
| [ISO 16739-1:2024](https://www.iso.org/standard/84123.html) und [buildingSMART IFC](https://technical.buildingsmart.org/standards/ifc/ifc-schema-specifications/) | Offenes IFC-Datenschema; IFC 4.3 ADD2 entspricht ISO 16739-1:2024. |
| [VDI 2552 Blatt 1](https://www.vdi.de/en/home/vdi-standards/details/vdi-2552-blatt-1-building-information-modeling-grundlagen) und [Blatt 10](https://www.vdi.de/en/home/vdi-standards/details/vdi-2552-blatt-10-building-information-modeling-auftraggeber-informations-anforderungen-aia-und-bim-abwicklungsplaene-bap) | BIM-Grundlagen sowie AIA und BIM-Abwicklungspläne. |
| [buildingSMART IDS](https://technical.buildingsmart.org/projects/information-delivery-specification-ids/) | Offene Spezifikation für maschinenlesbare Informationsanforderungen an IFC-Modelle. |

Die Links führen zu den offiziellen Beschreibungen; Normtexte und VDI-Richtlinien können kostenpflichtig sein. Maßgeblich sind jeweils die für das konkrete Projekt vereinbarten Ausgaben und Anforderungen.
