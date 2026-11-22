---
title: Schnellstart (0.3.2)
description: Voraussetzungen, Einrichtung und Aufruf von CheckMate.
---

Diese Anleitung beschreibt den Entwicklungsstand auf Branch `0.3.2`. Der Gradle Wrapper lädt Gradle und benötigte Abhängigkeiten bei Bedarf herunter.

## Voraussetzungen

- JDK 21 für Build, CLI und MCP-Server. Für die grafische Oberfläche wird ein JDK mit JavaFX benötigt, zum Beispiel Azul Zulu 21 JDK FX.
- Der IFC-Reader ist in Java implementiert; APSTEX-JARs sind für `0.3.2` nicht erforderlich.
- Für Portal-Anforderungen: Netzwerkzugriff auf das [BIM-Portal](https://via.bund.de/bim). Öffentlich sichtbare Inhalte können ohne Anmeldung abgerufen werden.

Ohne JavaFX wird beim Build nur die GUI ausgelassen; CLI, MCP-Server und IFC-Reader bleiben verfügbar. Der Build verwendet Gradle 8.14 und zielt auf Java 21. Java 25 wird von diesem Wrapper nicht unterstützt; dafür wäre ein Gradle-Upgrade nötig.

## Einrichtung und Start

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.3.2
cd bim-portal/Java_client

# Tests
./gradlew :hackathon-template:test

# Desktop-Oberfläche (JDK mit JavaFX erforderlich)
./gradlew :hackathon-template:runGui

# Ein IFC-Modell gegen eine lokale IDS-Datei prüfen
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /pfad/zum/modell.ifc --ids /pfad/zu/anforderungen.ids"

# Eine LOIN aus dem Portal prüfen
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /pfad/zum/modell.ifc --loin <LOIN-ID> --json build/report.json"
```

Unter Windows statt `./gradlew` den Wrapper `.\gradlew.bat` verwenden und Dateipfade entsprechend anpassen. Ohne `--model` oder die Umgebungsvariable `CHECKMATE_IFC` wird das mitgelieferte Beispielmodell verwendet. Relative Dateipfade beziehen sich auf `hackathon-template/`. Die CLI unterstützt auch `--aia` für ein Portalprojekt und `--suggest` zur Katalogauswahl; Details stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.2/docs/WORKFLOW.md).

Für nicht öffentliche Portal-Inhalte kann `bim-portal/Java_client/.env_example` nach `.env` kopiert und mit Zugangsdaten ergänzt werden. Zugangsdaten niemals ins Repository übernehmen.

Den MCP-Server richtet die [MCP-Anleitung](/CheckMate_BIMPortal/mcp/clients/) ein.

## CLI-Ergebnisse

| Code | Bedeutung |
|---|---|
| `0` | Alle ausgewerteten Spezifikationen erfüllt |
| `1` | Modell oder Anforderungen konnten nicht gelesen oder geprüft werden |
| `2` | Ungültige Kommandozeilenargumente |
| `3` | Mindestens eine Spezifikation nicht erfüllt |
| `4` | Keine Verletzung, aber nicht alles bestätigt oder nichts geprüft |

Beim Aufruf über Gradle wird ein Exit-Code ungleich null nach außen als `1` gemeldet. Der CheckMate-Code steht im Textbericht und im JSON-Feld `exitCode`. Nicht ausgewertete Anforderungen sind im Bericht zu beachten.
