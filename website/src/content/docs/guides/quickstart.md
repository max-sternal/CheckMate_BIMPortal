---
title: Schnellstart (0.3.3-stable)
description: Voraussetzungen, Einrichtung und Aufruf von CheckMate.
---

Diese Anleitung beschreibt den Entwicklungsstand auf Branch `0.3.3-stable`. Der Gradle Wrapper lädt Gradle und benötigte Abhängigkeiten bei Bedarf herunter.

## Voraussetzungen

- **JDK 25 LTS** für Build, CLI, MCP-Server und Tests (z. B. Azul Zulu 25 JDK FX für die Ausführung der JavaFX-Oberfläche oder jedes Standard-JDK 25 für CLI/MCP).
- Der IFC-Reader ist in Java implementiert; keine APSTEX-JARs erforderlich.
- Für Portal-Anforderungen: Netzwerkzugriff auf das [BIM-Portal](https://via.bund.de/bim). Öffentlich sichtbare Inhalte können ohne Anmeldung abgerufen werden.

Die GUI kompiliert auf jedem JDK 25 über `org.openjfx` `compileOnly` und `runGui` prüft zur Laufzeit auf JavaFX. Der Build verwendet **Gradle 9.1.0** (Wrapper im Repository enthalten).

## Einrichtung und Start

```bash
git clone https://github.com/max-sternal/CheckMate_BIMPortal.git
cd CheckMate_BIMPortal
git switch 0.3.3-stable
cd bim-portal/Java_client

# Tests
./gradlew :hackathon-template:test

# Desktop-Oberfläche (JDK mit JavaFX erforderlich)
./gradlew :hackathon-template:runGui

# Ein kompaktes Testmodell gegen eine LOIN aus dem Portal prüfen (z. B. Hochbau oder Infrastruktur)
./gradlew :hackathon-template:runLoinCheck \
  --args="--model docs/mini-hochbau-bundesbau.ifc --mapping none --loin be959b9d-daef-5315-9aba-f60b6ad54ccb"

./gradlew :hackathon-template:runLoinCheck \
  --args="--model docs/mini-infrastruktur-bruecke.ifc --mapping none --loin be959b9d-daef-5315-9aba-f60b6ad54ccb"
```

Unter Windows statt `./gradlew` den Wrapper `.\gradlew.bat` verwenden und Dateipfade entsprechend anpassen. Ohne `--model` oder die Umgebungsvariable `CHECKMATE_IFC` wird das mitgelieferte Beispielmodell verwendet. Relative Dateipfade beziehen sich auf `hackathon-template/`. Die CLI unterstützt auch `--aia` für ein Portalprojekt und `--suggest` zur Katalogauswahl; Details stehen in [`docs/WORKFLOW.md`](https://github.com/max-sternal/CheckMate_BIMPortal/blob/0.3.3-stable/docs/WORKFLOW.md).

Für nicht öffentliche Portal-Inhalte kann `bim-portal/Java_client/.env_example` nach `.env` kopiert und mit Zugangsdaten ergänzt werden. Zugangsdaten niemals ins Repository übernehmen. In derselben Datei wird der [Chat in der Oberfläche](/CheckMate_BIMPortal/guides/chat/) eingerichtet; ohne Schlüssel bleibt er inaktiv und alles andere unverändert nutzbar.

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
