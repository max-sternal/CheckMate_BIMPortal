package com.bimportal.hackathon.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import com.bimportal.hackathon.report.CheckReport.ModelInfo;
import com.bimportal.hackathon.report.CheckReport.Run;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What a run of checks comes to: the verdict, the exit code, the header and the JSON. The
 * requirements are the hand-written local IDS, the elements are built by hand.
 */
class CheckReportTest {

  private static final OffsetDateTime NOON =
      OffsetDateTime.of(2026, 11, 21, 12, 0, 0, 0, ZoneOffset.ofHours(1));

  private static final ModelInfo MODEL = new ModelInfo("Platten.ifc", "IFC2X3", 2);

  /** Carries what "Platten" asks for: Pset_SlabCommon/LoadBearing as IFCBOOLEAN. */
  private static final ModelElement GOOD_SLAB =
      new ModelElement(
          "gut",
          "Platte gut",
          "IfcSlab",
          List.of(
              new ModelProperty(
                  new PropertyKey("Pset_SlabCommon", "LoadBearing"), "TRUE", "IfcBoolean")));

  private static final ModelElement BARE_SLAB =
      new ModelElement("nackt", "Platte nackt", "IfcSlab", List.of());

  @Test
  void everySpecificationMetIsExitCode0() throws Exception {
    CheckReport report = report(List.of(GOOD_SLAB));

    assertThat(report.exitCode()).isEqualTo(CheckReport.PASSED).isZero();
    assertThat(report.verdict()).isEqualTo("1 Spezifikation(en) für IFC2X3: 1 erfüllt");
  }

  @Test
  void aSpecificationNotMetIsExitCode3() throws Exception {
    CheckReport report = report(List.of(GOOD_SLAB, BARE_SLAB));

    assertThat(report.exitCode()).isEqualTo(3);
    assertThat(report.results().get(0).failingElements()).isEqualTo(1);
    assertThat(report.results().get(0).passingElements()).isEqualTo(1);
  }

  @Test
  void nothingViolatedButNothingConfirmedIsExitCode4() throws Exception {
    ModelElement wall = new ModelElement("w", "Wand", "IfcWall", List.of());

    assertThat(report(List.of(wall)).exitCode()).isEqualTo(4);
    assertThat(new CheckReport(NOON, MODEL, List.of()).exitCode()).isEqualTo(4);
    assertThat(new CheckReport(NOON, MODEL, List.of()).verdict()).isEqualTo("nichts geprüft");
  }

  @Test
  void requirementsPartlyEvaluatedAreNotConfirmedEither() throws Exception {
    // "Waende" lists two requirements CheckMate does not evaluate; the rest is met.
    ModelElement wall =
        new ModelElement(
            "w",
            "Wand",
            "IfcWall",
            List.of(
                new ModelProperty(
                    new PropertyKey("Pset_WallCommon", "FireRating"), "REI30", "IfcLabel")));
    IdsDocument ids = localIds();
    CheckResult waende =
        ids.check("IFC4", List.of(wall)).stream()
            .filter(result -> result.specification().name().equals("Waende"))
            .findFirst()
            .orElseThrow();

    CheckReport report =
        new CheckReport(
            NOON, new ModelInfo("w.ifc", "IFC4", 1), List.of(Run.checked(ids, List.of(waende))));

    assertThat(report.exitCode()).isEqualTo(4);
  }

  @Test
  void requirementsThatCouldNotBeReadAreExitCode1WhateverElseHappened() throws Exception {
    CheckReport checked = report(List.of(BARE_SLAB));
    List<Run> runs = List.of(checked.runs().get(0), Run.failed("fehlt.ids", "nicht gefunden"));

    CheckReport report = new CheckReport(NOON, MODEL, runs);

    assertThat(report.exitCode()).isEqualTo(1);
    assertThat(report.verdict())
        .isEqualTo("1 Spezifikation(en) für IFC2X3: 1 nicht erfüllt; 1 nicht lesbar");
  }

  @Test
  void theHeaderSaysWhenAndWhat() throws Exception {
    assertThat(report(List.of(GOOD_SLAB)).header())
        .containsExactly(
            "CheckMate-Prüfung vom 21.11.2026 12:00",
            "Modell: Platten.ifc - Schema IFC2X3, 2 Objekte");
  }

  @Test
  void writesTheReportAsJson() throws Exception {
    CheckReport checked = report(List.of(GOOD_SLAB, BARE_SLAB));
    CheckReport report =
        new CheckReport(
            NOON,
            MODEL,
            List.of(checked.runs().get(0), Run.failed("fehlt.ids", "nicht gefunden")));

    JsonNode json = new ObjectMapper().readTree(ReportJson.write(report));

    assertThat(json.get("checkedAt").asText()).isEqualTo("2026-11-21T12:00:00+01:00");
    assertThat(json.get("model").get("elements").asInt()).isEqualTo(2);
    assertThat(json.get("exitCode").asInt()).isEqualTo(1);
    assertThat(json.get("counts").get("FAILED").asInt()).isEqualTo(1);
    assertThat(json.get("counts").get("unread").asInt()).isEqualTo(1);

    JsonNode platten = json.get("requirements").get(0).get("specifications").get(0);
    assertThat(platten.get("verdict").asText()).isEqualTo("FAILED");
    assertThat(platten.get("verdictLabel").asText()).isEqualTo("nicht erfüllt");
    assertThat(platten.get("failingElements").asInt()).isEqualTo(1);
    assertThat(platten.get("findings")).hasSize(2);
    JsonNode missing = platten.get("findings").get(1);
    assertThat(missing.get("elementId").asText()).isEqualTo("nackt");
    assertThat(missing.get("status").asText()).isEqualTo("MISSING");
    assertThat(missing.get("statusLabel").asText()).isEqualTo("Merkmal fehlt");

    JsonNode unread = json.get("requirements").get(1);
    assertThat(unread.get("error").asText()).isEqualTo("nicht gefunden");
    assertThat(unread.get("specifications")).isEmpty();
  }

  /** The local IDS's IFC2X3 part - only "Platten" - against the given elements. */
  private static CheckReport report(List<ModelElement> elements) throws IOException {
    IdsDocument ids = localIds();
    return new CheckReport(NOON, MODEL, List.of(Run.checked(ids, ids.check("IFC2X3", elements))));
  }

  private static IdsDocument localIds() throws IOException {
    String resource = "/ids/local/mehrere-spezifikationen.ids";
    try (InputStream in = CheckReportTest.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException(resource + " is missing from the test classpath");
      }
      return IdsParser.read(in, "mehrere-spezifikationen.ids");
    }
  }
}
