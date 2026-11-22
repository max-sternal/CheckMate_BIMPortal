package com.bimportal.hackathon.report;

import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.CheckResult.Verdict;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.report.CheckReport.Run;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A report as JSON, for scripts and pipelines.
 *
 * <p>Keys are English and stable; texts meant for people (labels, details, diagnosis) are German,
 * as in the other reports. Each specification carries its verdict both as a constant ({@code
 * FAILED}) and as words ({@code nicht erfüllt}), each finding its status likewise.
 *
 * <pre>
 * {
 *   "checkedAt": "2026-11-21T22:10:05+01:00",
 *   "model": {"name": ..., "schema": "IFC2X3", "elements": 303},
 *   "exitCode": 3,
 *   "verdict": "2 Spezifikation(en) für IFC2X3: 1 erfüllt, 1 nicht erfüllt",
 *   "counts": {"specifications": 2, "PASSED": 1, "FAILED": 1, ..., "unread": 0},
 *   "requirements": [
 *     {"name": ..., "title": ..., "version": ..., "source": ..., "error": null,
 *      "specifications": [
 *        {"name": ..., "ifcVersion": ..., "identifier": ..., "phases": [8, 9], "verdict": "FAILED",
 *         "verdictLabel": "nicht erfüllt", "examinedElements": 303, "applicableElements": 6,
 *         "failingElements": 2, "summary": ..., "notes": [...], "diagnosis": [...],
 *         "findings": [{"elementId": ..., "element": ..., "requirement": ...,
 *                       "status": "MISSING", "statusLabel": "Merkmal fehlt", "detail": ...}]}]}]
 * }
 * </pre>
 */
public final class ReportJson {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  private ReportJson() {}

  public static String write(CheckReport report) {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("checkedAt", report.checkedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    ObjectNode model = root.putObject("model");
    model.put("name", report.model().name());
    model.put("schema", report.model().schema());
    model.put("elements", report.model().elements());
    root.put("exitCode", report.exitCode());
    root.put("verdict", report.verdict());
    counts(root.putObject("counts"), report);

    ArrayNode requirements = root.putArray("requirements");
    for (Run run : report.runs()) {
      run(requirements.addObject(), run);
    }
    try {
      return MAPPER.writeValueAsString(root);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("A JSON tree that cannot be written", e);
    }
  }

  private static void counts(ObjectNode counts, CheckReport report) {
    List<CheckResult> results = report.results();
    counts.put("specifications", results.size());
    for (Verdict verdict : Verdict.values()) {
      counts.put(verdict.name(), results.stream().filter(r -> r.verdict() == verdict).count());
    }
    counts.put("unread", report.runs().stream().filter(Run::failed).count());
  }

  private static void run(ObjectNode node, Run run) {
    node.put("name", run.name());
    IdsDocument requirements = run.requirements();
    node.put("title", requirements == null ? null : requirements.title());
    node.put("version", requirements == null ? null : requirements.version());
    node.put("source", requirements == null ? null : requirements.source());
    node.put("error", run.error());
    ArrayNode specifications = node.putArray("specifications");
    run.results().forEach(result -> specification(specifications.addObject(), result));
  }

  private static void specification(ObjectNode node, CheckResult result) {
    node.put("name", result.specification().name());
    node.put("ifcVersion", result.specification().ifcVersion());
    node.put("identifier", result.specification().identifier());
    ArrayNode phases = node.putArray("phases");
    result.specification().phases().forEach(phases::add);
    node.put("verdict", result.verdict().name());
    node.put("verdictLabel", result.verdict().label());
    node.put("examinedElements", result.examinedElements());
    node.put("applicableElements", result.applicableElements());
    node.put("failingElements", result.failingElements());
    node.put("summary", result.summary());
    strings(node.putArray("notes"), result.notes());
    List<String> diagnosis = result.diagnosis().lines().stream().map(String::strip).toList();
    strings(node.putArray("diagnosis"), diagnosis);

    ArrayNode findings = node.putArray("findings");
    for (Finding finding : result.findings()) {
      ObjectNode f = findings.addObject();
      f.put("elementId", finding.elementId());
      f.put("element", finding.elementLabel());
      f.put("requirement", finding.requirement());
      Status status = finding.status();
      f.put("status", status.name());
      f.put("statusLabel", status.label());
      f.put("detail", finding.detail());
    }
  }

  private static void strings(ArrayNode array, List<String> values) {
    values.forEach(array::add);
  }
}
