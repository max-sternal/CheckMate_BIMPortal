package com.bimportal.hackathon.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import com.bimportal.hackathon.report.CheckReport.ModelInfo;
import com.bimportal.hackathon.report.CheckReport.Run;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The report as text: in full for the GUI, brief for the CLI, from the same report. */
class ReportTextTest {

  private static final OffsetDateTime NOON =
      OffsetDateTime.of(2026, 11, 22, 12, 0, 0, 0, ZoneOffset.ofHours(1));

  private static final ModelInfo MODEL = new ModelInfo("Platten.ifc", "IFC2X3", 3);

  @Test
  void theFullReportHasHeaderOverviewAndEveryFinding() throws Exception {
    CheckReport report = report(3);

    List<String> lines = ReportText.full(report);

    assertThat(lines.subList(0, 3))
        .containsExactly(
            "CheckMate-Prüfung vom 22.11.2026 12:00",
            "Modell: Platten.ifc - Schema IFC2X3, 3 Objekte",
            "2 Einträge geprüft - 1 Spezifikation(en) für IFC2X3: 1 nicht erfüllt; 1 nicht lesbar");
    assertThat(lines)
        .contains(
            "Anforderungen: CheckMate Testanforderungen, Version 1.2 (lokal.ids)",
            "Platten (IFC2X3 IFC4): 3 von 3 Elementen anwendbar, 1 erfüllt, 2 Merkmal fehlt",
            "fehlt.ids wurde nicht gefunden.")
        .filteredOn(line -> line.contains("[Merkmal fehlt]"))
        .hasSize(2);
  }

  @Test
  void theBriefReportShowsAFewExamples() throws Exception {
    Run platten = report(3).runs().get(0);

    List<String> lines = ReportText.brief(platten, "IFC2X3", 1);

    assertThat(lines.get(0))
        .isEqualTo("Anforderungen: CheckMate Testanforderungen, Version 1.2 (lokal.ids)");
    assertThat(lines).filteredOn(line -> line.contains("[Merkmal fehlt]")).hasSize(1);
  }

  @Test
  void oneSpecificationOnItsOwn() throws Exception {
    CheckReport report = report(1);

    List<String> lines = ReportText.single(report, "lokal.ids", report.results().get(0));

    assertThat(lines)
        .startsWith(
            "CheckMate-Prüfung vom 22.11.2026 12:00",
            "Modell: Platten.ifc - Schema IFC2X3, 3 Objekte",
            "Anforderungen: lokal.ids");
    assertThat(ReportText.verdictLine(report)).endsWith("(Exit-Code 1)");
  }

  /**
   * The local IDS's "Platten" against three slabs, one of which carries LoadBearing, plus an IDS
   * file that is missing.
   *
   * @param slabs how many slabs; the first carries the property
   */
  private static CheckReport report(int slabs) throws IOException {
    IdsDocument ids = localIds();
    List<ModelElement> elements =
        IntStream.range(0, slabs)
            .mapToObj(
                i ->
                    new ModelElement(
                        "p" + i,
                        "Platte " + i,
                        "IfcSlab",
                        i == 0
                            ? List.of(
                                new ModelProperty(
                                    new PropertyKey("Pset_SlabCommon", "LoadBearing"),
                                    "true",
                                    "IfcBoolean"))
                            : List.of()))
            .toList();
    return new CheckReport(
        NOON,
        MODEL,
        List.of(
            Run.checked(ids, ids.check("IFC2X3", elements)),
            Run.failed("fehlt.ids", "fehlt.ids wurde nicht gefunden.")));
  }

  private static IdsDocument localIds() throws IOException {
    try (InputStream in =
        ReportTextTest.class.getResourceAsStream("/ids/local/mehrere-spezifikationen.ids")) {
      if (in == null) {
        throw new IOException("the local IDS test file is missing");
      }
      return IdsParser.read(in, "lokal.ids");
    }
  }
}
