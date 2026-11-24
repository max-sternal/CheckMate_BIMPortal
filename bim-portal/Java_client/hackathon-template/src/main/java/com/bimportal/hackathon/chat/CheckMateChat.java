package com.bimportal.hackathon.chat;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.pb40.bimportal.config.ConfigLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The chat in the CheckMate window: an LLM that drives CheckMate through the same tools the MCP
 * server offers, so the app can be shown without a separate MCP host such as Claude Desktop.
 *
 * <p>It is an alternative to that host, not a replacement for the MCP server: both front ends go
 * through {@link com.bimportal.hackathon.check.CheckService}, so there is one place that knows how
 * to check a model. What differs is only who calls it.
 *
 * <p>Checking happens through {@link ModelChecker}, which the caller supplies. The GUI passes its
 * own check there, so a chat answer fills the report panel, the summary table and the element tabs
 * exactly as the buttons do - that is the point of the demonstration.
 *
 * <p>No key, no chat: {@link #configured()} is false when {@code ANTHROPIC_API_KEY} is set neither
 * in the environment nor in {@code .env}, and the rest of the GUI keeps working, as it does without
 * portal credentials.
 *
 * <p>Which model answers is configuration, not code: {@link #MODEL_KEY} names it and {@link
 * #BASE_URL_KEY} an endpoint other than Anthropic's own, so a gateway such as OpenRouter opens the
 * models of other vendors to the same key, the same tool and the same loop.
 *
 * <p>Every call blocks for as long as the model thinks and a check runs, which is seconds to
 * minutes. Call {@link #ask} from a background thread, never from the JavaFX application thread.
 */
public final class CheckMateChat {

  /**
   * Runs {@code check_model}: what the "Validate LOIN" button does, for the ids the model names.
   */
  @FunctionalInterface
  public interface ModelChecker {

    /**
     * @param ids LOIN or AIA project IDs, or links holding them
     * @param idsFiles paths of local IDS files or ZIPs of them
     * @param examples unmet requirements to show per specification, 0 for the full report
     * @return the report, in German, as the report panel shows it
     */
    String check(List<String> ids, List<String> idsFiles, int examples);
  }

  /** Asked unless {@link #MODEL_KEY} names another. */
  public static final String DEFAULT_MODEL = "claude-opus-5-5";

  /** Read from the environment or from {@code .env}; never from the repository. */
  public static final String API_KEY = "ANTHROPIC_API_KEY";

  /**
   * The model to ask: a plain name such as {@code claude-haiku-5-5} on Anthropic itself, or a
   * qualified one such as {@code anthropic/claude-sonnet-4} or {@code tencent/hy4-flash} on a
   * gateway, which needs the vendor as a prefix.
   */
  public static final String MODEL_KEY = "CHECKMATE_LLM_MODEL";

  /**
   * An endpoint other than Anthropic's own that speaks the Messages format, e.g. {@code
   * https://openrouter.ai/api/v1} for OpenRouter, which serves models of many vendors behind one
   * key. Set, the key travels as a bearer token, as such gateways expect; unset, CheckMate talks to
   * Anthropic with {@code x-api-key}.
   *
   * <p>Only the endpoint and the model change - the tool, the schema and the loop stay as they are,
   * so the same conversation can be run against several models to see which ones call {@code
   * check_model} correctly.
   */
  public static final String BASE_URL_KEY = "CHECKMATE_LLM_BASE_URL";

  /**
   * Tool calls per question. A question needs one check, so this only stops a model that keeps
   * calling, and keeps a runaway loop from spending money.
   */
  private static final int MAX_STEPS = 6;

  /**
   * Longest tool answer in characters, as in the MCP server: the full report of an AIA project runs
   * to half a million characters, far more than belongs in a prompt.
   */
  private static final int MAX_ANSWER = 50_000;

  private static final Tool CHECK_MODEL =
      Tool.builder()
          .name("check_model")
          .description(
              "Prüft das geladene IFC-Modell gegen LOINs oder AIA-Projekte des BIM Portals"
                  + " (IDs oder Links) und/oder lokale IDS-Dateien. Liefert den Bericht auf"
                  + " Deutsch: je Spezifikation das Ergebnis (erfüllt, nicht erfüllt, erfüllt"
                  + " (soweit ausgewertet), nicht prüfbar), Beispiele nicht erfüllter"
                  + " Anforderungen, oder eine Diagnose, wenn nichts anwendbar war. Der Bericht"
                  + " erscheint zugleich im Prüfbereich des Fensters.")
          .inputSchema(
              Tool.InputSchema.builder()
                  .properties(
                      Tool.InputSchema.Properties.builder()
                          .putAdditionalProperty(
                              "ids",
                              JsonValue.from(
                                  Map.of(
                                      "type",
                                      "array",
                                      "items",
                                      Map.of("type", "string"),
                                      "description",
                                      "IDs von LOINs oder AIA-Projekten, oder Links mit einer ID")))
                          .putAdditionalProperty(
                              "ids_files",
                              JsonValue.from(
                                  Map.of(
                                      "type",
                                      "array",
                                      "items",
                                      Map.of("type", "string"),
                                      "description",
                                      "Pfade lokaler IDS-Dateien oder ZIPs davon")))
                          .putAdditionalProperty(
                              "examples",
                              JsonValue.from(
                                  Map.of(
                                      "type",
                                      "integer",
                                      "description",
                                      "Nicht erfüllte Anforderungen je Spezifikation,"
                                          + " Standard 3; 0 für den vollen Bericht")))
                          .build())
                  .required(List.of())
                  .build())
          .build();

  private final ModelChecker checker;
  private final Supplier<String> modelInfo;
  private final List<MessageParam> conversation = new ArrayList<>();

  private AnthropicClient client;

  /**
   * @param checker runs a check and shows it, so the window follows the conversation
   * @param modelInfo one line naming the loaded model, read again for every question because "Load
   *     IFC" can replace it mid-conversation
   */
  public CheckMateChat(ModelChecker checker, Supplier<String> modelInfo) {
    this.checker = Objects.requireNonNull(checker, "checker");
    this.modelInfo = Objects.requireNonNull(modelInfo, "modelInfo");
  }

  /** Whether a key is configured. Without one the chat stays inactive and says so. */
  public static boolean configured() {
    return key() != null;
  }

  private static String key() {
    return setting(API_KEY);
  }

  /**
   * The model this chat asks, for the window to name: {@link #MODEL_KEY} or {@link #DEFAULT_MODEL}.
   */
  public static String model() {
    String value = setting(MODEL_KEY);
    return value == null ? DEFAULT_MODEL : value;
  }

  /** The endpoint to ask, or null for Anthropic's own. */
  private static String baseUrl() {
    return setting(BASE_URL_KEY);
  }

  private static String setting(String name) {
    String value = ConfigLoader.getProperty(name);
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * Ask a question and get the answer, after any checks the model decided to run. Blocks; not for
   * the JavaFX application thread.
   *
   * @throws IllegalStateException without a key, naming what to set
   */
  public String ask(String question) {
    conversation.add(MessageParam.ofUser(question));

    for (int step = 0; step < MAX_STEPS; step++) {
      Message answer =
          client()
              .messages()
              .create(
                  MessageCreateParams.builder()
                      .model(model())
                      .maxTokens(8_000L)
                      .system(systemPrompt())
                      .addTool(CHECK_MODEL)
                      .messages(List.copyOf(conversation))
                      .build());
      conversation.add(answer.toParam());

      if (answer.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
        return "Die Anfrage wurde abgelehnt. Formuliere sie bitte anders.";
      }

      List<ContentBlockParam> results = new ArrayList<>();
      for (ContentBlock block : answer.content()) {
        block.toolUse().ifPresent(use -> results.add(run(use)));
      }
      if (results.isEmpty()) {
        return text(answer);
      }
      conversation.add(MessageParam.ofUserOfContentBlockParams(results));
    }
    return "Abgebrochen nach " + MAX_STEPS + " Werkzeugaufrufen ohne Antwort.";
  }

  /** Forget the conversation; the next question starts over. */
  public void reset() {
    conversation.clear();
  }

  /** Run one tool call. A failure goes back as an error result, so the model can say what broke. */
  private ContentBlockParam run(ToolUseBlock use) {
    ToolResultBlockParam.Builder result = ToolResultBlockParam.builder().toolUseId(use.id());
    try {
      if (!"check_model".equals(use.name())) {
        throw new IllegalArgumentException("Unbekanntes Werkzeug: " + use.name());
      }
      Map<?, ?> input = use._input().convert(Map.class);
      String report =
          checker.check(
              strings(input, "ids"), strings(input, "ids_files"), integer(input, "examples", 3));
      return ContentBlockParam.ofToolResult(result.content(shorten(report)).build());
    } catch (RuntimeException failed) {
      String reason = failed.getMessage() == null ? failed.toString() : failed.getMessage();
      return ContentBlockParam.ofToolResult(result.content(reason).isError(true).build());
    }
  }

  /** The visible text of an answer, with the thinking blocks left out. */
  private static String text(Message answer) {
    String text =
        answer.content().stream()
            .map(ContentBlock::text)
            .filter(java.util.Optional::isPresent)
            .map(block -> block.get().text())
            .reduce("", (all, part) -> all.isEmpty() ? part : all + "\n" + part);
    return text.isBlank() ? "(keine Antwort)" : text;
  }

  private String systemPrompt() {
    return """
        Du bist CheckMate, eine Anwendung, die IFC-Modelle gegen die LOINs des BIM Portals \
        (via.bund.de/bim) prüft. Du sprichst Deutsch und antwortest kurz.

        Das Modell ist bereits geladen: %s

        Mit check_model prüfst du es gegen LOINs oder AIA-Projekte, die der Nutzer per ID oder \
        Link nennt, oder gegen lokale IDS-Dateien. Nennt der Nutzer keine ID, frage danach oder \
        bitte ihn, die LOIN links in der Liste zu suchen und auszuwählen - du kannst den \
        Katalog selbst nicht durchsuchen.

        'nicht prüfbar' heißt, dass die Spezifikation kein einziges Element ausgewählt hat; das \
        ist kein Bestehen, sondern meist ein Hinweis darauf, dass das Modell seine Merkmale \
        anders benennt oder andere Werte trägt als die LOIN verlangt. Steht im Bericht 'als \
        ...', wurde dafür das hinterlegte Mapping benutzt.

        Der Bericht erscheint nach jedem Aufruf im Prüfbereich des Fensters. Wiederhole ihn \
        deshalb nicht vollständig, sondern fasse das Ergebnis in wenigen Sätzen zusammen.\
        """
        .formatted(modelInfo.get());
  }

  private AnthropicClient client() {
    if (client == null) {
      String key = key();
      if (key == null) {
        throw new IllegalStateException(
            "Kein " + API_KEY + " gesetzt - in der Umgebung oder in .env hinterlegen.");
      }
      AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder();
      String baseUrl = baseUrl();
      // A gateway authenticates with a bearer token, Anthropic itself with x-api-key.
      client =
          (baseUrl == null ? builder.apiKey(key) : builder.baseUrl(baseUrl).authToken(key)).build();
    }
    return client;
  }

  private static String shorten(String answer) {
    if (answer.length() <= MAX_ANSWER) {
      return answer;
    }
    return answer.substring(0, MAX_ANSWER) + "\n[gekürzt - vollständig im Prüfbereich]";
  }

  private static List<String> strings(Map<?, ?> input, String key) {
    Object value = input.get(key);
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    List<String> texts = new ArrayList<>();
    for (Object item : list) {
      if (item != null && !item.toString().isBlank()) {
        texts.add(item.toString().trim());
      }
    }
    return texts;
  }

  private static int integer(Map<?, ?> input, String key, int fallback) {
    Object value = input.get(key);
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException notANumber) {
        return fallback;
      }
    }
    return fallback;
  }
}
