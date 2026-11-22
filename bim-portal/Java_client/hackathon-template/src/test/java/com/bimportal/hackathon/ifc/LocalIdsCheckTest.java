package com.bimportal.hackathon.ifc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Our IFC case files against the hand-written local IDS, from file to verdict.
 *
 * <p>The expected numbers are ifctester 0.9's on the same files (2026-11-21). Where CheckMate
 * deliberately differs, the test says so: a specification with no applicable element is "nicht
 * prüfbar" here and passes in ifctester, and specifications using parts CheckMate does not evaluate
 * are not checked at all.
 */
class LocalIdsCheckTest {

  private static final String LOCAL_IDS = "/ids/local/mehrere-spezifikationen.ids";

  @Test
  void ifc2x3GetsOnlyTheSpecificationWrittenForIt() throws Exception {
    List<CheckResult> results = check("ifc2x3");

    // ifctester: Platten 5 applicable, 5 failed; nothing else is for IFC2X3.
    assertThat(results)
        .extracting(result -> result.specification().name())
        .containsExactly("Platten");
    assertThat(results.get(0).applicableElements()).isEqualTo(5);
    assertThat(results.get(0).count(Status.MISSING)).isEqualTo(5);
  }

  @Test
  void ifc4GetsEverySpecificationWrittenForIt() throws Exception {
    List<CheckResult> results = check("ifc4");

    // ifctester: Platten 5 applicable, 5 failed; the other three 0 applicable.
    assertThat(results)
        .extracting(result -> result.specification().name())
        .containsExactly("Platten", "Waende", "Klassifiziert", "Keine Proxies");
    assertThat(results.get(0).count(Status.MISSING)).isEqualTo(5);
    assertThat(results.subList(1, 4))
        .allSatisfy(result -> assertThat(result.notApplicable()).isTrue());
  }

  @Test
  void ifc4x3GetsTheSpecificationWrittenForIfc4x3Add2() throws Exception {
    List<CheckResult> results = check("ifc4x3");

    // ifctester counts the IFC4X3_ADD2 specification as written for the IFC4X3 file, too.
    assertThat(results)
        .extracting(result -> result.specification().name())
        .containsExactly("Bruecken");
    assertThat(results.get(0).notApplicable()).isTrue();
  }

  private static List<CheckResult> check(String schema) throws IOException {
    LoadedModel model =
        LoadedModel.of("checkmate-cases-" + schema + ".ifc", TestModels.cases(schema));
    return localIds().check(model.schema(), model.elements());
  }

  private static IdsDocument localIds() throws IOException {
    try (InputStream in = LocalIdsCheckTest.class.getResourceAsStream(LOCAL_IDS)) {
      if (in == null) {
        throw new IOException(LOCAL_IDS + " is missing from the test classpath");
      }
      return IdsParser.read(in, LOCAL_IDS);
    }
  }
}
