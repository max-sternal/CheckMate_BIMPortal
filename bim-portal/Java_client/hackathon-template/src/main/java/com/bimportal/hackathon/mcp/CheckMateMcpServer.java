package com.bimportal.hackathon.mcp;

import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.check.CheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import test.fx.LoadedModel;

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
          + " is not a pass.";

  /**
   * Longest tool answer in characters, about 15,000 tokens. The full report of an AIA project
   * runs to half a million characters, more than any AI app takes in one answer.
   */
  static final int MAX_ANSWER = 50_000;

  private CheckMateMcpServer() {}

  public static void main(String[] args) {
    // stdout carries the protocol. Logging, the portal client and the IFC toolbox write to it
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
        .serverInfo("checkmate", "1.0")
        .instructions(INSTRUCTIONS)
        .capabilities(ServerCapabilities.builder().tools(false).build())
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
                    "ids", texts("IDs of LOINs or AIA projects, or links holding them"),
                    "ids_files", texts("Paths of local IDS files or ZIPs of them"),
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
                        integer(args, "examples", 3))))
        .toolCall(
            tool(
                "element_report",
                "What the last check_model said about an element, found by GlobalId or a part of"
                    + " its name, with the properties it carries.",
                Map.of("element", text("GlobalId or part of the element name")),
                List.of("element")),
            handler(args -> tools.elementReport(string(args, "element"))))
        .build();
  }

  private static Tool tool(
      String name, String description, Map<String, Object> properties, List<String> required) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", required);
    return Tool.builder(name, schema).description(description).build();
  }

  /** A tool's answer, or its error as text the AI can pass on: never an exception. */
  private static BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> handler(
      Function<Map<String, Object>, String> work) {
    return (exchange, request) -> {
      Map<String, Object> args = request.arguments() == null ? Map.of() : request.arguments();
      try {
        String answer = shorten(work.apply(args), MAX_ANSWER);
        return CallToolResult.builder().addTextContent(answer).isError(false).build();
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
