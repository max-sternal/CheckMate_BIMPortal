package com.bimportal.hackathon.mcp;

import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.check.CheckService;
import com.bimportal.hackathon.ifc.LoadedModel;
import com.bimportal.hackathon.properties.PortalProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * CheckMate as an MCP server: an AI app starts it and calls its tools over stdin/stdout. Tools and
 * setup: {@code docs/WORKFLOW.md}, section 2.7.
 */
public final class CheckMateMcpServer {

  /** Read by the AI app; tells the model how the tools fit together. */
  static final String INSTRUCTIONS =
      "CheckMate checks IFC building models against the requirements of the German BIM Portal"
          + " (LOINs and AIA projects, delivered as IDS). Typical order: describe_model or"
          + " load_model; find_loins, or IDs the user gives; describe_requirements to see what a"
          + " LOIN asks for; find_properties and describe_property to inspect BIM Portal Merkmale"
          + " and Merkmalsgruppen; check_model; element_report for single elements. Reports and"
          + " messages are in German. 'nicht prüfbar' means the specification selected no element"
          + " - that is not a pass. Models name properties differently than the portal:"
          + " load_mapping shows or changes the JSON mapping that translates them (reports say"
          + " 'als ...' when it was used).";

  /**
   * Longest tool answer in characters, about 15,000 tokens. The full report of an AIA project runs
   * to half a million characters, more than any AI app takes in one answer.
   */
  static final int MAX_ANSWER = 50_000;

  /** The MCP Apps extension (SEP-1865): servers that ship interactive views declare it. */
  static final String UI_EXTENSION = "io.modelcontextprotocol/ui";

  /** MIME type of an MCP App view. */
  static final String UI_MIME = "text/html;profile=mcp-app";

  /** MIME type of JSON data resources. */
  static final String JSON_MIME = "application/json";

  /** The view that shows a check; {@code check_model} points to it. */
  static final String REPORT_VIEW = "ui://checkmate/report";

  /** The view that lets the user pick LOINs; {@code find_loins} points to it. */
  static final String LOINS_VIEW = "ui://checkmate/loins";

  /** The view that shows the loaded model; {@code describe_model} points to it. */
  static final String MODEL_VIEW = "ui://checkmate/model";

  /** Read-only JSON resource for the currently loaded model. */
  static final String MODEL_RESOURCE = "checkmate://model/current";

  /** Read-only JSON resource for the last check report. */
  static final String REPORT_RESOURCE = "checkmate://report/last.json";

  /** Findings per specification that the view receives. */
  static final int VIEW_EXAMPLES = 10;

  private static final Map<String, String> TOOL_VIEWS =
      Map.of(
          "check_model", REPORT_VIEW,
          "find_loins", LOINS_VIEW,
          "describe_model", MODEL_VIEW);

  private static final String VERSION = "0.3.2";

  /** Tools that change nothing, neither the session nor anything outside it. */
  private static final Set<String> READ_ONLY =
      Set.of(
          "describe_model",
          "list_elements",
          "find_loins",
          "describe_requirements",
          "find_properties",
          "describe_property",
          "element_report",
          "server_status");

  /** Tools that reach the BIM Portal or the network. */
  private static final Set<String> OPEN_WORLD =
      Set.of(
          "load_model",
          "find_loins",
          "describe_requirements",
          "find_properties",
          "describe_property",
          "check_model");

  private static final Map<String, String> TITLES =
      Map.ofEntries(
          Map.entry("load_model", "Modell laden"),
          Map.entry("describe_model", "Modell beschreiben"),
          Map.entry("list_elements", "Elemente auflisten"),
          Map.entry("find_loins", "LOINs finden"),
          Map.entry("describe_requirements", "Anforderungen beschreiben"),
          Map.entry("find_properties", "Merkmale finden"),
          Map.entry("describe_property", "Merkmal beschreiben"),
          Map.entry("check_model", "Modell prüfen"),
          Map.entry("element_report", "Elementbericht"),
          Map.entry("load_mapping", "Mapping laden"),
          Map.entry("server_status", "Status"),
          Map.entry("app_report_page", "Mehr Befunde laden"));

  private CheckMateMcpServer() {}

  public static void main(String[] args) {
    // stdout carries the protocol. Logging, the portal client and the IFC reader write to it
    // too, so everything else goes to stderr, where the AI app keeps it as a log.
    PrintStream protocol = System.out;
    System.setOut(System.err);

    CheckMateTools tools =
        new CheckMateTools(
            new CheckService(BimPortalClientBuilder::buildAuthenticatedOrPublic),
            LoadedModel::read,
            LoadedModel::configured,
            () -> new PortalProperties(BimPortalClientBuilder.buildAuthenticatedOrPublic()));
    start(tools, System.in, protocol);
    // The transport's threads keep the process alive until the AI app closes stdin.
  }

  /** Start the server on the given streams; tests talk to it the same way an AI app does. */
  static McpSyncServer start(CheckMateTools tools, InputStream in, OutputStream out) {
    McpJsonMapper json = new JacksonMcpJsonMapper(new ObjectMapper());
    return start(tools, new StdioServerTransportProvider(json, in, out), json);
  }

  static McpSyncServer start(
      CheckMateTools tools, McpServerTransportProvider transport, McpJsonMapper json) {
    return McpServer.sync(transport)
        .jsonMapper(json)
        .serverInfo("checkmate", VERSION)
        .instructions(INSTRUCTIONS)
        .capabilities(
            ServerCapabilities.builder()
                .tools(false)
                .resources(false, false)
                .prompts(false)
                .extensions(Map.of(UI_EXTENSION, Map.of()))
                .build())
        .resources(
            viewResource(
                REPORT_VIEW,
                "checkmate_report",
                "CheckMate Prüfbericht",
                "Interaktive Ansicht des Prüfberichts von check_model",
                "/mcpapp/report.html"),
            viewResource(
                LOINS_VIEW,
                "checkmate_loins",
                "CheckMate LOIN-Auswahl",
                "Interaktive Auswahl von LOINs und AIA-Projekten aus find_loins",
                "/mcpapp/loins.html"),
            viewResource(
                MODEL_VIEW,
                "checkmate_model",
                "CheckMate Modellübersicht",
                "Interaktive Übersicht des geladenen IFC-Modells aus describe_model",
                "/mcpapp/model.html"),
            jsonResource(
                MODEL_RESOURCE,
                "checkmate_model_current",
                "Aktuelles IFC-Modell (JSON)",
                "Metadaten und Entity-Verteilung des aktuell geladenen IFC-Modells als JSON",
                tools::currentModelJson),
            jsonResource(
                REPORT_RESOURCE,
                "checkmate_report_last",
                "Letzter Prüfbericht (JSON)",
                "Vollständiger letzter Prüfbericht als JSON",
                tools::lastReportJson))
        .prompts(checkLoinPrompt(), explainElementPrompt())
        .toolCall(
            tool(
                "load_model",
                "Load an IFC model (IFC2X3, IFC4, IFC4X3) from a local path or a link (http/https;"
                    + " a GitHub file link works as copied from the browser). It replaces the model"
                    + " in use. Without it, CheckMate uses the configured or the bundled bridge"
                    + " model.",
                Map.of("source", text("Path or link of the .ifc or .ifczip file")),
                List.of("source")),
            handler(args -> tools.loadModel(string(args, "source"))))
        .toolCall(
            tool(
                "describe_model",
                "The model in use: name, schema, number of elements and the most frequent IFC"
                    + " entities.",
                Map.of(),
                List.of()),
            handler(args -> tools.describeModel(), args -> tools.modelData()))
        .toolCall(
            tool(
                "list_elements",
                "Elements of the model as 'GlobalId | entity | name | PredefinedType', filtered by"
                    + " IFC entity and/or a part of the name.",
                properties(
                    "entity", text("IFC entity, e.g. IfcWall"),
                    "name", text("Part of the element name"),
                    "limit", number("Most elements to list, default 30")),
                List.of()),
            handler(
                args ->
                    tools.listElements(
                        string(args, "entity"), string(args, "name"), integer(args, "limit", 30))))
        .toolCall(
            tool(
                "find_loins",
                "Find requirements in the German BIM Portal: AIA projects and LOINs (Level of"
                    + " Information Need). LOINs whose object type occurs in the model come first."
                    + " Filters match a part of the catalogue value, e.g. phase 'LPH 5', discipline"
                    + " 'Ingenieurbau'. Returns IDs for describe_requirements and check_model."
                    + " Downloads no requirements.",
                properties(
                    "search", text("Search term for the names; empty for the whole catalogue"),
                    "discipline", text("Fachmodell, e.g. Ingenieurbau"),
                    "phase", text("Leistungsphase, e.g. LPH 5"),
                    "purpose", text("Anwendungsfall, e.g. Mengenermittlung"),
                    "limit", number("Most LOINs to list, default 20"),
                    "include_unfitting", flag("Also list LOINs that do not fit the model")),
                List.of()),
            handler(
                args ->
                    tools.findLoins(
                        string(args, "search"),
                        new CatalogueFilter(
                            string(args, "discipline"),
                            string(args, "phase"),
                            string(args, "purpose")),
                        integer(args, "limit", 20),
                        bool(args, "include_unfitting")),
                args -> tools.loinsData()))
        .toolCall(
            tool(
                "describe_requirements",
                "What a LOIN or AIA project from the portal requires, by its ID or a link holding"
                    + " it: what it selects elements by, the properties it requires, its phases,"
                    + " and how many elements of the model it would apply to.",
                Map.of("id", text("ID of a LOIN or AIA project, or a link holding one")),
                List.of("id")),
            handler(args -> tools.describeRequirements(string(args, "id"))))
        .toolCall(
            tool(
                "find_properties",
                "Search Merkmale (properties) and/or Merkmalsgruppen (property groups) in the"
                    + " German BIM Portal by name or keyword. Useful to look up official"
                    + " definitions, units, allowed values, or mapping candidates.",
                properties(
                    "search", text("Search term for property or group names"),
                    "kind", text("'all' (default), 'property' (Merkmale), or 'group' (Gruppen)"),
                    "limit", number("Most entries to return per kind, default 20")),
                List.of()),
            handler(
                args ->
                    tools.findProperties(
                        string(args, "search"), string(args, "kind"), integer(args, "limit", 20)),
                args -> tools.propertiesData()))
        .toolCall(
            tool(
                "describe_property",
                "Describe a single Merkmal (property) or Merkmalsgruppe (property group) from the"
                    + " German BIM Portal by its GUID or portal link: definition, data type,"
                    + " units, allowed values, and parent/member relationships.",
                properties(
                    "guid", text("GUID of a Merkmal or Merkmalsgruppe, or a portal link"),
                    "kind", text("'auto' (default), 'property', or 'group'")),
                List.of("guid")),
            handler(
                args -> tools.describeProperty(string(args, "guid"), string(args, "kind")),
                args -> tools.propertyDetailData()))
        .toolCall(
            tool(
                "check_model",
                "Check the model against LOINs or AIA projects (portal IDs or links) and/or local"
                    + " IDS files. Returns the report in German: per specification its verdict"
                    + " (erfüllt, nicht erfüllt, erfüllt (soweit ausgewertet), nicht prüfbar),"
                    + " examples of unmet requirements, or a diagnosis when nothing applied. The"
                    + " result is kept for element_report.",
                properties(
                    "ids",
                    texts("IDs of LOINs or AIA projects, or links holding them"),
                    "ids_files",
                    texts("Paths of local IDS files or ZIPs of them"),
                    "examples",
                    number(
                        "Unmet requirements to show per specification, default 3; 0 for the full"
                            + " report, which is shortened beyond 50,000 characters")),
                List.of()),
            handler(
                args ->
                    tools.check(
                        strings(args, "ids"),
                        strings(args, "ids_files"),
                        integer(args, "examples", 3)),
                args -> tools.reportData(VIEW_EXAMPLES)))
        .toolCall(
            tool(
                "element_report",
                "What the last check_model said about an element, found by GlobalId or a part of"
                    + " its name, with the properties it carries.",
                Map.of("element", text("GlobalId or part of the element name")),
                List.of("element")),
            handler(args -> tools.elementReport(string(args, "element"))))
        .toolCall(
            appTool(
                "app_report_page",
                "For the check view only: more unmet findings of one specification of the last"
                    + " check.",
                properties(
                    "run", number("Index of the run, from the check data"),
                    "spec", number("Index of the specification within the run"),
                    "offset", number("Findings to skip"),
                    "limit", number("Findings to return, default 25")),
                List.of("run", "spec")),
            handler(
                args -> "Weitere Befunde der Prüfung (nur für die Ansicht).",
                args ->
                    tools.reportPage(
                        integer(args, "run", -1),
                        integer(args, "spec", -1),
                        integer(args, "offset", 0),
                        integer(args, "limit", 25))))
        .toolCall(
            tool(
                "load_mapping",
                "Which property addresses a model uses for the ones a LOIN asks for, as a JSON"
                    + " mapping (e.g. Klassifizierung/Objekttyp -> Pset_Objektinformation/_IDEbene3)."
                    + " Without source, shows the mapping in use. 'none' switches aliases off,"
                    + " 'default' is the bundled BIM.Hamburg one, anything else is the path of a"
                    + " mapping file. The last check is discarded when the mapping changes.",
                Map.of(
                    "source",
                    text("Path of a mapping JSON file, 'none' or 'default'; empty to show it")),
                List.of()),
            handler(args -> tools.mapping(string(args, "source"))))
        .toolCall(
            tool(
                "server_status",
                "What this session holds: the model in use, the property mapping and the verdict"
                    + " of the last check.",
                Map.of(),
                List.of()),
            handler(args -> tools.status()))
        .build();
  }

  private static Tool tool(
      String name, String description, Map<String, Object> properties, List<String> required) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", required);
    Tool.Builder builder =
        Tool.builder(name, schema)
            .title(TITLES.get(name))
            .description(description)
            .annotations(
                new ToolAnnotations(
                    TITLES.get(name),
                    READ_ONLY.contains(name),
                    false,
                    READ_ONLY.contains(name),
                    OPEN_WORLD.contains(name),
                    null));
    String view = TOOL_VIEWS.get(name);
    if (view != null) {
      // Hosts that render MCP Apps show the view; the others ignore it and use the text.
      builder.meta(Map.of("ui", Map.of("resourceUri", view), "ui/resourceUri", view));
    }
    return builder.build();
  }

  /**
   * A tool only a view may call ({@code visibility: ["app"]}): hosts keep it out of the AI's tool
   * list. It still answers with text, as every tool does.
   */
  private static Tool appTool(
      String name, String description, Map<String, Object> properties, List<String> required) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", required);
    return Tool.builder(name, schema)
        .title(TITLES.get(name))
        .description(description)
        .annotations(new ToolAnnotations(TITLES.get(name), true, false, true, false, null))
        .meta(Map.of("ui", Map.of("resourceUri", REPORT_VIEW, "visibility", List.of("app"))))
        .build();
  }

  /** An MCP App view: one HTML file, no network, talks to the host by postMessage. */
  private static SyncResourceSpecification viewResource(
      String uri, String name, String title, String description, String classpathHtml) {
    Map<String, Object> uiMeta = Map.of("ui", Map.of("prefersBorder", true));
    Resource resource =
        Resource.builder(uri, name)
            .title(title)
            .description(description)
            .mimeType(UI_MIME)
            .meta(uiMeta)
            .build();
    return new SyncResourceSpecification(
        resource,
        (exchange, request) ->
            ReadResourceResult.builder(
                    List.of(new TextResourceContents(uri, UI_MIME, html(classpathHtml), uiMeta)))
                .build());
  }

  /** A read-only JSON resource backed by the current session state. */
  private static SyncResourceSpecification jsonResource(
      String uri, String name, String title, String description, Supplier<String> jsonSupplier) {
    Resource resource =
        Resource.builder(uri, name)
            .title(title)
            .description(description)
            .mimeType(JSON_MIME)
            .build();
    return new SyncResourceSpecification(
        resource,
        (exchange, request) ->
            ReadResourceResult.builder(
                    List.of(new TextResourceContents(uri, JSON_MIME, jsonSupplier.get(), null)))
                .build());
  }

  private static SyncPromptSpecification checkLoinPrompt() {
    Prompt prompt =
        Prompt.builder("check-loin")
            .title("LOIN-Prüfung durchführen")
            .description(
                "Inspiziert das aktuelle IFC-Modell, findet passende LOINs im BIM-Portal, führt"
                    + " die Prüfung durch und erklärt die Ergebnisse auf Deutsch.")
            .arguments(
                List.of(
                    PromptArgument.builder("phase")
                        .description("Optionale Leistungsphase, z. B. LPH 5 oder LPH 8")
                        .required(false)
                        .build(),
                    PromptArgument.builder("discipline")
                        .description("Optionales Fachmodell, z. B. Ingenieurbau")
                        .required(false)
                        .build()))
            .build();
    return new SyncPromptSpecification(
        prompt,
        (exchange, request) -> {
          Map<String, Object> args = request.arguments() == null ? Map.of() : request.arguments();
          String phase = string(args, "phase");
          String discipline = string(args, "discipline");
          StringBuilder filterHint = new StringBuilder();
          if (phase != null && !phase.isBlank()) {
            filterHint.append(" Filter phase='").append(phase.trim()).append("'.");
          }
          if (discipline != null && !discipline.isBlank()) {
            filterHint.append(" Filter discipline='").append(discipline.trim()).append("'.");
          }
          String text =
              "Prüfe das aktuelle IFC-Modell gegen passende LOINs aus dem deutschen BIM-Portal:\n"
                  + "1. Rufe describe_model auf, um Schema, Objektanzahl und häufigste Entities zu"
                  + " erfassen.\n"
                  + "2. Suche mit find_loins nach passenden LOINs für das Modell."
                  + filterHint
                  + "\n"
                  + "3. Prüfe das Modell mit check_model gegen die am besten passenden LOINs.\n"
                  + "4. Fasse das Gesamtergebnis verständlich auf Deutsch zusammen, erkläre"
                  + " fehlende oder fehlerhafte Merkmale und weise ausdrücklich darauf hin, dass"
                  + " 'nicht prüfbar' kein Bestehen bedeutet.";
          return GetPromptResult.builder(
                  List.of(
                      PromptMessage.builder(Role.USER, TextContent.builder(text).build()).build()))
              .description("Anleitung zur LOIN-Prüfung des aktuellen IFC-Modells")
              .build();
        });
  }

  private static SyncPromptSpecification explainElementPrompt() {
    Prompt prompt =
        Prompt.builder("explain-element")
            .title("Elementbefunde erklären")
            .description(
                "Ruft element_report für ein Element auf und erklärt unerfüllte Merkmale"
                    + " verständlich auf Deutsch.")
            .arguments(
                List.of(
                    PromptArgument.builder("element")
                        .description("GlobalId oder Namensteil des zu prüfenden Elements")
                        .required(true)
                        .build()))
            .build();
    return new SyncPromptSpecification(
        prompt,
        (exchange, request) -> {
          Map<String, Object> args = request.arguments() == null ? Map.of() : request.arguments();
          String element = string(args, "element");
          String target =
              element == null || element.isBlank() ? "<Element>" : "'" + element.trim() + "'";
          String text =
              "Untersuche die Prüfbefunde für das IFC-Element "
                  + target
                  + ":\n"
                  + "1. Rufe element_report mit element="
                  + target
                  + " auf.\n"
                  + "2. Erkläre auf Deutsch, welche Anforderungen erfüllt sind und welche Merkmale"
                  + " fehlen oder falsche Werte tragen.\n"
                  + "3. Gib konkrete Hinweise, welche PropertySets und Merkmale im Modell ergänzt"
                  + " oder korrigiert werden müssen.";
          return GetPromptResult.builder(
                  List.of(
                      PromptMessage.builder(Role.USER, TextContent.builder(text).build()).build()))
              .description("Anleitung zur Erklärung von Prüfbefunden eines Elements")
              .build();
        });
  }

  private static String html(String resource) {
    try (InputStream in = CheckMateMcpServer.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("View fehlt: " + resource);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("View nicht lesbar: " + resource, e);
    }
  }

  /** A tool's answer, or its error as text the AI can pass on: never an exception. */
  private static BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> handler(
      Function<Map<String, Object>, String> work) {
    return handler(work, null);
  }

  /**
   * As {@link #handler(Function)}, with the data a view needs next to the text. Hosts without
   * views, and the AI, read the text; the view reads {@code structuredContent}.
   */
  private static BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> handler(
      Function<Map<String, Object>, String> work,
      Function<Map<String, Object>, Map<String, Object>> data) {
    return (exchange, request) -> {
      Map<String, Object> args = request.arguments() == null ? Map.of() : request.arguments();
      try {
        String answer = shorten(work.apply(args), MAX_ANSWER);
        CallToolResult.Builder result =
            CallToolResult.builder().addTextContent(answer).isError(false);
        if (data != null) {
          Map<String, Object> structured = data.apply(args);
          if (!structured.isEmpty()) {
            result.structuredContent(structured);
          }
        }
        return result.build();
      } catch (RuntimeException e) {
        String message = e.getMessage() == null ? e.toString() : e.getMessage();
        return CallToolResult.builder().addTextContent(message).isError(true).build();
      }
    };
  }

  /** Cut an answer at a line end, keeping its last line: a report ends with its verdict. */
  static String shorten(String answer, int max) {
    if (answer.length() <= max) {
      return answer;
    }
    int lastBreak = answer.lastIndexOf('\n');
    String last =
        lastBreak < 0 || answer.length() - lastBreak > max / 10 ? "" : answer.substring(lastBreak);
    String note =
        "\n... gekürzt, "
            + answer.length()
            + " Zeichen insgesamt. Weniger 'examples' wählen oder element_report für einzelne"
            + " Elemente nutzen.";
    int budget = Math.max(0, max - last.length() - note.length());
    int end = answer.lastIndexOf('\n', budget);
    return answer.substring(0, end > 0 ? end : budget) + note + last;
  }

  private static Map<String, Object> properties(Object... nameAndSchema) {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (int i = 0; i < nameAndSchema.length; i += 2) {
      properties.put((String) nameAndSchema[i], nameAndSchema[i + 1]);
    }
    return properties;
  }

  private static Map<String, Object> text(String description) {
    return Map.of("type", "string", "description", description);
  }

  private static Map<String, Object> texts(String description) {
    return Map.of("type", "array", "items", Map.of("type", "string"), "description", description);
  }

  private static Map<String, Object> number(String description) {
    return Map.of("type", "integer", "description", description);
  }

  private static Map<String, Object> flag(String description) {
    return Map.of("type", "boolean", "description", description);
  }

  private static String string(Map<String, Object> args, String name) {
    Object value = args.get(name);
    return value == null ? null : value.toString();
  }

  private static List<String> strings(Map<String, Object> args, String name) {
    Object value = args.get(name);
    if (value instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    return value == null || value.toString().isBlank() ? List.of() : List.of(value.toString());
  }

  private static int integer(Map<String, Object> args, String name, int fallback) {
    Object value = args.get(name);
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return value == null ? fallback : Integer.parseInt(value.toString().trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static boolean bool(Map<String, Object> args, String name) {
    Object value = args.get(name);
    return value instanceof Boolean b ? b : value != null && Boolean.parseBoolean(value.toString());
  }
}
