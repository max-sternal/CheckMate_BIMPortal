package com.bimportal.hackathon.mcp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.bimportal.hackathon.check.CheckService;
import com.bimportal.hackathon.check.CheckService.Portal;
import com.bimportal.hackathon.ifc.LoadedModel;
import com.bimportal.hackathon.ifc.ModelLoadException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.server.McpSyncServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** The server over its stdio transport, spoken to as an AI app does: JSON-RPC, one per line. */
@Timeout(60)
class CheckMateMcpServerTest {

  private final ObjectMapper json = new ObjectMapper();

  @TempDir Path folder;

  private PipedOutputStream requests;
  private BufferedReader replies;
  private McpSyncServer server;
  private int lastId;

  @AfterEach
  void stop() throws IOException {
    if (server != null) {
      server.closeGracefully();
    }
    if (requests != null) {
      requests.close();
    }
  }

  @Test
  void anAiAppListsTheToolsAndCallsThem() throws IOException {
    Path ids =
        Files.write(
            folder.resolve("kappe.ids"),
            CheckMateToolsTest.resource("/ids/portal/ueberbau-kappe-lph8-9.ids"));
    start(CheckMateToolsTest.model("Kappen.ifc", CheckMateToolsTest.kappe("k1", "Kappe", "1")));

    JsonNode init =
        request(
            "initialize",
            Map.of(
                "protocolVersion", "2025-06-18",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "test", "version", "0")));
    notify("notifications/initialized");
    JsonNode tools = request("tools/list", Map.of()).path("result").path("tools");

    assertThat(init.path("result").path("serverInfo").path("name").asText()).isEqualTo("checkmate");
    assertThat(init.path("result").path("instructions").asText()).contains("check_model");
    assertThat(tools)
        .extracting(tool -> tool.path("name").asText())
        .containsExactly(
            "load_model",
            "describe_model",
            "list_elements",
            "find_loins",
            "describe_requirements",
            "check_model",
            "element_report",
            "app_report_page",
            "load_mapping",
            "server_status");
    JsonNode check = tools.get(5).path("inputSchema");
    assertThat(check.path("properties").path("ids_files").path("type").asText()).isEqualTo("array");

    JsonNode checked = call("check_model", Map.of("ids_files", List.of(ids.toString())));
    assertThat(checked.path("isError").asBoolean()).isFalse();
    assertThat(text(checked)).contains("Modell: Kappen.ifc", "1 erfüllt (Exit-Code 0)");
    assertThat(text(call("element_report", Map.of("element", "k1")))).contains("[erfüllt]");

    // MCP Apps: the extension is declared, check_model points to a view, and the answer carries
    // its data next to the text.
    assertThat(
            init.path("result")
                .path("capabilities")
                .path("extensions")
                .has("io.modelcontextprotocol/ui"))
        .isTrue();
    JsonNode checkTool = tools.get(5);
    assertThat(checkTool.path("_meta").path("ui").path("resourceUri").asText())
        .isEqualTo("ui://checkmate/report");
    assertThat(checkTool.path("annotations").path("readOnlyHint").asBoolean()).isFalse();
    assertThat(tools.get(1).path("annotations").path("readOnlyHint").asBoolean()).isTrue();
    JsonNode spec =
        checked.path("structuredContent").path("runs").path(0).path("specifications").path(0);
    assertThat(spec.path("verdict").asText()).isEqualTo("PASSED");
    JsonNode appTool = tools.get(7);
    assertThat(appTool.path("name").asText()).isEqualTo("app_report_page");
    assertThat(appTool.path("_meta").path("ui").path("visibility"))
        .extracting(JsonNode::asText)
        .containsExactly("app");
    assertThat(checkTool.path("_meta").path("ui").has("visibility")).isFalse();
    assertThat(spec.path("unmetTotal").asInt()).isZero();
    JsonNode page = call("app_report_page", Map.of("run", 0, "spec", 0)).path("structuredContent");
    assertThat(page.path("total").asInt()).isZero();
    assertThat(call("app_report_page", Map.of("run", 3, "spec", 0)).path("isError").asBoolean())
        .isTrue();
    JsonNode view =
        request("resources/read", Map.of("uri", "ui://checkmate/report"))
            .path("result")
            .path("contents")
            .path(0);
    assertThat(view.path("mimeType").asText()).isEqualTo("text/html;profile=mcp-app");
    assertThat(view.path("text").asText())
        .contains("ui/initialize", "ui/notifications/tool-result");
  }

  @Test
  void theMappingCanBeShownAndSwitched() throws IOException {
    start(CheckMateToolsTest.model("Kappen.ifc", CheckMateToolsTest.kappe("k1", "Kappe", "1")));
    request(
        "initialize",
        Map.of(
            "protocolVersion", "2025-06-18",
            "capabilities", Map.of(),
            "clientInfo", Map.of("name", "test", "version", "0")));
    notify("notifications/initialized");
    try {
      assertThat(text(call("load_mapping", Map.of())))
          .contains("Klassifizierung/Objekttyp -> Pset_Objektinformation/_IDEbene3");
      assertThat(text(call("load_mapping", Map.of("source", "none")))).contains("0 Alias");
      assertThat(call("load_mapping", Map.of("source", "fehlt.json")).path("isError").asBoolean())
          .isTrue();
    } finally {
      call("load_mapping", Map.of("source", "default"));
    }
    assertThat(text(call("server_status", Map.of()))).contains("Mapping: BIM.Hamburg");
  }

  @Test
  void anErrorIsAnAnswerTheAiCanPassOn() throws IOException {
    start(CheckMateToolsTest.model("Kappen.ifc", CheckMateToolsTest.kappe("k1", "Kappe", "1")));
    request(
        "initialize",
        Map.of(
            "protocolVersion", "2025-06-18",
            "capabilities", Map.of(),
            "clientInfo", Map.of("name", "test", "version", "0")));
    notify("notifications/initialized");

    JsonNode missing = call("load_model", Map.of("source", "fehlt.ifc"));
    JsonNode tooEarly = call("element_report", Map.of("element", "k1"));

    assertThat(missing.path("isError").asBoolean()).isTrue();
    assertThat(text(missing)).isEqualTo("IFC-Datei nicht gefunden: fehlt.ifc");
    assertThat(tooEarly.path("isError").asBoolean()).isTrue();
    assertThat(text(tooEarly)).contains("zuerst check_model");
    assertThat(text(call("describe_model", Map.of()))).contains("Kappen.ifc");
  }

  @Test
  void aLongAnswerIsCutAtALineEndAndKeepsItsVerdict() {
    String finding = "[Merkmal fehlt] Kappe - Feldnummer\n";
    String report = "Kopf\n" + finding.repeat(5_000) + "Ergebnis: nicht erfüllt";

    String cut = CheckMateMcpServer.shorten(report, 50_000);

    assertThat(cut.length()).isLessThanOrEqualTo(50_000);
    assertThat(cut)
        .startsWith("Kopf\n[Merkmal fehlt]")
        .contains("Feldnummer\n... gekürzt, " + report.length() + " Zeichen insgesamt.")
        .endsWith("Elemente nutzen.\nErgebnis: nicht erfüllt");
    assertThat(CheckMateMcpServer.shorten("x".repeat(5_000), 1_000)).hasSizeLessThan(1_001);
    assertThat(CheckMateMcpServer.shorten("kurz", 10)).isEqualTo("kurz");
  }

  private void start(LoadedModel model) throws IOException {
    requests = new PipedOutputStream();
    PipedInputStream serverIn = new PipedInputStream(requests, 1 << 16);
    PipedInputStream fromServer = new PipedInputStream(1 << 20);
    PipedOutputStream serverOut = new PipedOutputStream(fromServer);
    replies = new BufferedReader(new InputStreamReader(fromServer, UTF_8));
    CheckMateTools tools =
        new CheckMateTools(
            new CheckService(Portal.none()),
            source -> {
              throw new ModelLoadException("IFC-Datei nicht gefunden: " + source);
            },
            () -> model);
    server = CheckMateMcpServer.start(tools, serverIn, serverOut);
  }

  private JsonNode call(String tool, Map<String, Object> arguments) throws IOException {
    return request("tools/call", Map.of("name", tool, "arguments", arguments)).path("result");
  }

  private JsonNode request(String method, Object params) throws IOException {
    int id = ++lastId;
    ObjectNode message = json.createObjectNode().put("jsonrpc", "2.0").put("id", id);
    message.put("method", method).set("params", json.valueToTree(params));
    send(message);
    while (true) {
      String line = replies.readLine();
      if (line == null) {
        throw new IOException("the server closed its output");
      }
      JsonNode reply = json.readTree(line);
      if (reply.path("id").asInt() == id) {
        return reply;
      }
    }
  }

  private void notify(String method) throws IOException {
    send(json.createObjectNode().put("jsonrpc", "2.0").put("method", method));
  }

  private void send(JsonNode message) throws IOException {
    requests.write((json.writeValueAsString(message) + "\n").getBytes(UTF_8));
    requests.flush();
  }

  private static String text(JsonNode result) {
    return result.path("content").path(0).path("text").asText();
  }
}
