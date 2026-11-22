package com.bimportal.hackathon.mcp;

import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.check.CheckService;
import com.bimportal.hackathon.ifc.LoadedModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
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
          + " LOIN asks for; check_model; element_report for single elements. Reports and messages"
          + " are in German. 'nicht prüfbar' means the specification selected no element - that"
          + " is not a pass. Models name properties differently than the portal: load_mapping shows"
          + " or changes the JSON mapping that translates them (reports say 'als ...' when it was used).";

  /**
   * Longest tool answer in characters, about 15,000 tokens. The full report of an AIA project runs
   * to half a million characters, more than any AI app takes in one answer.
   */
  static final int MAX_ANSWER = 50_000;

  /** The MCP Apps extension (SEP-1865): servers that ship interactive views declare it. */
  static final String UI_EXTENSION = "io.modelcontextprotocol/ui";

  /** MIME type of an MCP App view. */
  static final String UI_MIME = "text/html;profile=mcp-app";

  /** The view that shows a check; {@code check_model} points to it. */
  static final String REPORT_VIEW = "ui://checkmate/report";

  /** Findings per specification that the view receives. */
  static final int VIEW_EXAMPLES = 10;

  private static final String VERSION = "0.3.2";

  /** Tools that change nothing, neither the session nor anything outside it. */
  private static final Set<String> READ_ONLY =
      Set.of(
          "describe_model",
          "list_elements",
          "find_loins",
          "describe_requirements",
          "element_report",
          "server_status");

  /** Tools that reach the BIM Portal or the network. */
  private static final Set<String> OPEN_WORLD =
      Set.of("load_model", "find_loins", "describe_requirements", "check_model");

  private static final Map<String, String> TITLES =
      Map.of(
          "load_model", "Modell laden",
          "describe_model", "Modell beschreiben",
          "list_elements", "Elemente auflisten",
          "find_loins", "LOINs finden",
          "describe_requirements", "Anforderungen beschreiben",
          "check_model", "Modell prüfen",
          "element_report", "Elementbericht",
          "load_mapping", "Mapping laden",
          "server_status", "Status");

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
            LoadedModel::configured);
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
                .extensions(Map.of(UI_EXTENSION, Map.of()))
                .build())
        .resources(reportView())
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
            handler(args -> tools.describeModel()))
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
                        bool(args, "include_unfitting"))))
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
                () -> tools.reportData(VIEW_EXAMPLES)))
        .toolCall(
            tool(
                "element_report",
                "What the last check_model said about an element, found by GlobalId or a part of"
                    + " its name, with the properties it carries.",
                Map.of("element", text("GlobalId or part of the element name")),
                List.of("element")),
            handler(args -> tools.elementReport(string(args, "element"))))
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
    if (name.equals("check_model")) {
      // Hosts that render MCP Apps show the view; the others ignore it and use the text.
      builder.meta(Map.of("ui", Map.of("resourceUri", REPORT_VIEW), "ui/resourceUri", REPORT_VIEW));
    }
    return builder.build();
  }

  /** The check view, an MCP App: one HTML file, no network, talks to the host by postMessage. */
  private static SyncResourceSpecification reportView() {
    Resource resource =
        Resource.builder()
            .uri(REPORT_VIEW)
            .name("checkmate_report")
            .title("CheckMate Prüfbericht")
            .description("Interaktive Ansicht des Prüfberichts von check_model")
            .mimeType(UI_MIME)
            .meta(Map.of("ui", Map.of("prefersBorder", true)))
            .build();
    return new SyncResourceSpecification(
        resource,
        (exchange, request) ->
            new ReadResourceResult(
                List.of(
                    new TextResourceContents(
                        REPORT_VIEW,
                        UI_MIME,
                        html("/mcpapp/report.html"),
                        Map.of("ui", Map.of("prefersBorder", true))))));
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
      Function<Map<String, Object>, String> work, Supplier<Map<String, Object>> data) {
    return (exchange, request) -> {
      Map<String, Object> args = request.arguments() == null ? Map.of() : request.arguments();
      try {
        String answer = shorten(work.apply(args), MAX_ANSWER);
        CallToolResult.Builder result =
            CallToolResult.builder().addTextContent(answer).isError(false);
        if (data != null) {
          Map<String, Object> structured = data.get();
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
