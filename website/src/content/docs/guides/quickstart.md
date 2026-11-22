---
title: Schnellstart (Version 0.3.1)
description: CheckMate 0.3.1 bauen, testen, per CLI, GUI oder MCP-Server starten — mit und ohne APSTEX-JARs.
---

Diese Anleitung gilt für den aktuellen Arbeitsstand auf Branch **`0.3.1`** (Java 21 + Gradle 8.14).

## Voraussetzungen

### Immer erforderlich
- **JDK 21** (für die JavaFX-GUI ein JDK-FX-Build wie **Azul Zulu 21 JDK FX** als `JAVA_HOME`; für reine Kern-Tests reicht jedes JDK 21).
- **Internetzugriff** auf das öffentliche BIM-Portal des Bundes (`https://via.bund.de/bim`). **Kein Benutzerkonto erforderlich** — CheckMate liest alle veröffentlichten LOINs und AIA-Projekte anonym.
- **Gradle ist bereits enthalten** (Wrapper `gradlew` / `gradlew.bat` in `bim-portal/Java_client/`).

### Für volles IFC-Laden in `0.3.1` (GUI, CLI, MCP-Server)
- **APSTEX IFC Toolbox JARs** (`com.apstex.ifctoolbox*.jar` und `com.apstex.stepparser*.jar`) im Verzeichnis:
  ```text
  bim-portal/Java_client/hackathon-template/lib/
  ```
  :::note[Hinweis zur Verfügbarkeit von APSTEX]
  Die APSTEX-Bibliothek wurde im Rahmen des BIM-Portal-Hackathons bereitgestellt und darf aus lizenzrechtlichen Gründen nicht im Git-Repository mitverteilt werden. Sie kann beim Team oder direkt bei APSTEX angefragt werden.
  **Ohne diese JARs** baut und testet Gradle dennoch automatisch den kompletten Prüfkern (148 Tests). Mit Version **`0.3.2`** bzw. **`0.4` (*CheckMate Lite*)** entfällt diese Abhängigkeit vollständig.
  :::

### Optional
- **Zugangsdaten für nicht-öffentliche Portal-Inhalte:** `bim-portal/Java_client/.env_example` nach `.env` kopieren und `BIM_PORTAL_USERNAME` / `BIM_PORTAL_PASSWORD` setzen (`.env` niemals committen!).
- **Zusätzliche IFC-Testmodelle (~5 MB):** `./gradlew :hackathon-template:downloadTestModels`
- **Python 3 + `ifcopenshell` + `ifctester`:** Nur nötig, wenn du die IFC-Testfixtures neu generieren (`src/test/python/make_ifc_fixtures.py`) oder Ergebnisse direkt mit `ifctester` abgleichen möchtest.

---

## Schnellstart-Befehle

Alle Befehle werden im Verzeichnis `bim-portal/Java_client/` ausgeführt (unter Windows `.\gradlew.bat` statt `./gradlew`):

```bash
cd bim-portal/Java_client

# 1. Tests ausführen (läuft auch ohne APSTEX mit 148 Kern-Tests; mit APSTEX ~217 Tests)
./gradlew :hackathon-template:test

# 2. JavaFX-Desktop-GUI starten (benötigt JDK 21 FX + APSTEX)
./gradlew :hackathon-template:runGui

# 3. Headless CLI: gegen eine konkrete Portal-LOIN prüfen
./gradlew :hackathon-template:runLoinCheck \
  --args="--loin be959b9d-daef-5315-9aba-f60b6ad54ccb"

# 4. Headless CLI: passende LOINs für LPH 5 vorschlagen (ohne Download aller IDS)
./gradlew :hackathon-template:runLoinCheck \
  --args="--suggest --phase 'LPH 5'"

# 5. Eigenes Modell (lokale Datei oder GitHub-Link / .ifczip) + JSON-Report
./gradlew :hackathon-template:runLoinCheck \
  --args="--model /pfad/zum/modell.ifc --aia <AIA-GUID> --json build/report.json"

# 6. MCP-Server für KI-Clients installieren
./gradlew :hackathon-template:installMcp
```

---

## CLI-Exit-Codes

Die Headless-CLI `LoinCheckCli` liefert klare, skriptfähige Exit-Codes (auch im JSON-Feld `exitCode` enthalten):

| Exit-Code | Bedeutung |
|---|---|
| `0` | Alle geprüften Spezifikationen **erfüllt** |
| `1` | Modell oder Anforderungen konnten nicht gelesen/geprüft werden |
| `2` | Unbekanntes CLI-Argument |
| `3` | Mindestens eine Spezifikation **nicht erfüllt** |
| `4` | Keine Verletzung, aber nicht alles bestätigt (**nicht prüfbar**, nur teilweise ausgewertet oder nichts geprüft) |

:::tip[Hinweis zu Gradle-Exit-Codes]
Bei Aufruf über `./gradlew` übersetzt Gradle jeden Exit-Code ungleich `0` nach außen in `1`. Der exakte CheckMate-Exit-Code steht in der Gradle-Meldung, in der letzten Zeile des Textberichts und im JSON-Feld `exitCode`.
:::
