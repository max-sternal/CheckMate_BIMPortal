---
title: Chat in der Oberfläche
description: Eine Prüfung per Sprachmodell aus CheckMate heraus auslösen, mit Anthropic oder über ein Gateway.
---

Das Chatfeld rechts im Fenster lässt eine Prüfung über ein Sprachmodell auslösen, ohne dass daneben ein MCP-Client läuft. Es ist eine **Ergänzung zur [MCP-Schnittstelle](/CheckMate_BIMPortal/mcp/overview/), kein Ersatz**: beide Wege rufen denselben `CheckService` auf, nur der Aufrufer unterscheidet sich.

Diese Funktion entsteht auf Branch `0.3.3` und ist in `0.3.2` nicht enthalten.

## Einrichtung

Der Schlüssel wird aus der Umgebung oder aus `bim-portal/Java_client/.env` gelesen:

```text
ANTHROPIC_API_KEY=...
```

Danach CheckMate neu starten; das Eingabefeld wird einmalig beim Start freigeschaltet. Ohne Schlüssel weist das Feld darauf hin und bleibt inaktiv — die übrige Oberfläche, die CLI und der MCP-Server sind davon nicht betroffen. `*.env` ist von der Versionsverwaltung ausgenommen; Zugangsdaten niemals ins Repository übernehmen.

## Modell und Endpunkt wählen

| Variable | Bedeutung |
|---|---|
| `ANTHROPIC_API_KEY` | Schlüssel des gewählten Anbieters; ohne ihn bleibt der Chat inaktiv. |
| `CHECKMATE_LLM_MODEL` | Modellname, Standard `claude-opus-5-5`. |
| `CHECKMATE_LLM_BASE_URL` | Optionaler anderer Endpunkt, der dasselbe Messages-Format spricht. |

Ist `CHECKMATE_LLM_BASE_URL` gesetzt, wird der Schlüssel als Bearer-Token übertragen statt als `x-api-key`. So lassen sich Modelle verschiedener Anbieter über ein Gateway hinter einem Schlüssel ansprechen, zum Beispiel [OpenRouter](https://openrouter.ai/):

```text
CHECKMATE_LLM_BASE_URL=https://openrouter.ai/api
CHECKMATE_LLM_MODEL=openrouter/free
```

Die Basis-URL enthält **kein abschließendes `/v1`**: das SDK ergänzt `v1/messages` selbst, ein auf `/v1` endender Wert fragt also `.../v1/v1/messages` ab und führt zu einem Fehler 404. Über ein Gateway benötigt der Modellname den Anbieter als Präfix, etwa `anthropic/claude-sonnet-4`.

`openrouter/free` ist der kostenlose Router dieses Anbieters. Er wählt pro Anfrage ein freies Modell und filtert nach den benötigten Fähigkeiten, darunter Werkzeugaufrufe. Freie Modelle sind begrenzt — etwa 50 Anfragen pro Tag, wobei eine Frage bis zu sechs davon verbraucht — und das Angebot wechselt häufig.

Die erste Zeile im Chat nennt das konfigurierte Modell. Hat ein anderes Modell geantwortet, etwa weil ein Router es ausgewählt hat, endet die Antwort mit `[geantwortet hat: ...]`.

## Was der Chat auslöst

Dem Modell steht ein einziges Werkzeug zur Verfügung: `check_model`, verbunden mit derselben Prüfung, die die Schaltfläche „Validate LOIN“ auslöst. Eine Antwort füllt daher die Ergebnistabelle, den Berichtsbereich und die Elementreiter genauso wie ein Prüflauf über die Oberfläche.

Die LOIN wird über ihre ID oder ihren Portal-Link benannt. Eine Katalogsuche ist noch kein Werkzeug des Chats; die gewünschte LOIN lässt sich in der Liste links im Fenster nachsehen.

## Kosten

Jede Frage wird nach Token abgerechnet, da die Anfrage von CheckMate selbst ausgeht und nicht von einer bereits bezahlten KI-Anwendung. Eine Frage mit dem gekürzten Bericht kostet wenige Cent, mit dem vollständigen Bericht mehr. Ein kleineres oder über ein Gateway bezogenes günstiges Modell senkt das um eine bis drei Größenordnungen. Der Weg über [MCP](/CheckMate_BIMPortal/mcp/overview/) verursacht diese Kosten nicht, da dort der Client das Modell stellt.

## Grenzen

- Die Werkzeugantwort ist auf 50.000 Zeichen begrenzt, damit kein vollständiger Bericht in eine Anfrage gelangt; der Berichtsbereich zeigt weiterhin den ganzen Bericht.
- Höchstens sechs Werkzeugaufrufe pro Frage.
- Der Aufruf läuft im Hintergrund, das Fenster bleibt bedienbar. Während das Modell arbeitet, gibt es keine Fortschrittsmeldung.
- Frage, Modell- und LOIN-Angaben sowie der gekürzte Bericht werden an den gewählten Anbieter übertragen, bei einem Gateway zusätzlich über dessen Betreiber. Vertraulichkeit und Datenschutz sind vor der Nutzung zu prüfen.
- Ob ein Modell das Werkzeug korrekt aufruft, hängt vom Modell ab. Bei freien Modellen ist das nicht verlässlich: von drei Prüfläufen am 24.11.2026 lief einer in eine sich wiederholende Tabelle, bis die Token-Grenze erreicht war.

## Stand

Am 24.11.2026 wurde eine Frage über OpenRouter mit `openrouter/free` vollständig beantwortet: das Modell rief `check_model` auf, die Prüfung lief, Ergebnistabelle und Berichtsbereich füllten sich, und die Antwort benannte die Ursache des Ergebnisses „nicht prüfbar“ zutreffend. Ein kostenpflichtiges Modell wurde nicht vergleichend geprüft.

Code-nahe Einzelheiten stehen auf Branch `0.3.3` in `docs/WORKFLOW.md` (Abschnitt 2.8) und `docs/STATUS.md`; die Konfiguration ist in `bim-portal/Java_client/.env_example` beschrieben.
