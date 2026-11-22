---
title: Schnellstart (0.3.1)
description: Voraussetzungen, Einrichtung und Aufruf von CheckMate.
slug: v0.3.1/guides/quickstart
---

Diese Anleitung beschreibt den Branch `0.3.1`. Der Gradle Wrapper lädt Gradle und benötigte Abhängigkeiten bei Bedarf herunter.

## Voraussetzungen

* JDK 21. Zum Bauen und Starten von GUI, CLI und MCP-Server mit APSTEX wird ein JDK mit JavaFX benötigt, zum Beispiel Azul Zulu 21 JDK FX. Ohne APSTEX reicht für die Kern-Tests ein JDK 21.
* Für das IFC-Einlesen: `com.apstex.ifctoolbox*.jar` und `com.apstex.stepparser*.jar` unter `bim-portal/Java_client/hackathon-template/lib/`. Die proprietären Bibliotheken sind nicht im Repository enthalten; die Verfügbarkeit kann beim Projektteam oder bei [APSTEX](https://www.apstex.com/) erfragt werden.
* Für Portal-Anforderungen: Netzwerkzugriff auf das [BIM-Portal](https://via.bund.de/bim). Öffentlich sichtbare Inhalte können ohne Anmeldung abgerufen werden.

Ohne APSTEX werden GUI, CLI, MCP-Server und IFC-Lesetests beim Build ausgelassen. Der Prüfkern und seine Tests können dennoch gebaut und ausgeführt werden.

## Einrichtung und Start

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch --detach v0.3.1
cd bim-portal/Java_client

# Tests
./gradlew :hackathon-template:test

# Desktop-Oberfläche (JDK mit JavaFX und APSTEX erforderlich)
./gradlew :hackathon-template:runGui

# Ein IFC-Modell gegen eine lokale IDS-Datei prüfen
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /pfad/zum/modell.ifc --ids /pfad/zu/anforderungen.ids"

# Eine LOIN aus dem Portal prüfen
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /pfad/zum/modell.ifc --loin <LOIN-ID> --json build/report.json"
```

Unter Windows statt `./gradlew` den Wrapper `.\gradlew.bat` verwenden und Dateipfade entsprechend anpassen. Ohne `--model` oder die Umgebungsvariable `CHECKMATE_IFC` wird das mitgelieferte Beispielmodell verwendet. Die CLI unterstützt auch `--aia` für ein Portalprojekt und `--suggest` zur Katalogauswahl; Details stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/v0.3.1/docs/WORKFLOW.md).

Für nicht öffentliche Portal-Inhalte kann `bim-portal/Java_client/.env_example` nach `.env` kopiert und mit Zugangsdaten ergänzt werden. Zugangsdaten niemals ins Repository übernehmen.

Den MCP-Server richtet die [MCP-Anleitung](/CheckMate_BIMPortal/v0.3.1/mcp/clients/) ein.

## CLI-Ergebnisse

| Code | Bedeutung |
|---|---|
| `0` | Alle ausgewerteten Spezifikationen erfüllt |
| `1` | Modell oder Anforderungen konnten nicht gelesen oder geprüft werden |
| `2` | Ungültige Kommandozeilenargumente |
| `3` | Mindestens eine Spezifikation nicht erfüllt |
| `4` | Keine Verletzung, aber nicht alles bestätigt oder nichts geprüft |

Beim Aufruf über Gradle wird ein Exit-Code ungleich null nach außen als `1` gemeldet. Der CheckMate-Code steht im Textbericht und im JSON-Feld `exitCode`. Nicht ausgewertete Anforderungen sind im Bericht zu beachten.
