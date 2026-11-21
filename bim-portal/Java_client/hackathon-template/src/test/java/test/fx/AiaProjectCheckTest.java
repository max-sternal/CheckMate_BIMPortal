package test.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static test.fx.TestModels.BRIDGE_IFC4;

import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.CheckResult.Verdict;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A real AIA project ZIP from the portal against a real IFC4 bridge, from file to verdict.
 *
 * <p>The expected numbers are ifctester 0.9's on the same files (2026-11-21): every one of the 36
 * specifications of "Beispiel-AIA Neubau Eisenbahnbrücke" selects any user-defined {@code
 * IfcBuildingElementProxy}, so each applies to the same 9 proxies of the bridge, and all fail.
 * Skipped, with a message, when the sample model has not been downloaded.
 */
class AiaProjectCheckTest {

  private static final String RAILWAY = "/ids/portal/aia-neubau-eisenbahnbruecke.zip";

  @Test
  void theRailwayProjectAgainstTheIfc4Bridge() throws IOException {
    LoadedModel bridge = LoadedModel.of("Infra-Bridge.ifc", TestModels.sample(BRIDGE_IFC4));

    List<CheckResult> results = railway().check(bridge.schema(), bridge.elements());

    assertThat(results).hasSize(36);
    assertThat(results).allSatisfy(result -> assertThat(result.applicableElements()).isEqualTo(9));
    assertThat(results).extracting(CheckResult::verdict).containsOnly(Verdict.FAILED);
    assertThat(CheckResult.tally(results, "IFC4"))
        .isEqualTo("36 Spezifikation(en) für IFC4: 36 nicht erfüllt");
  }

  private static IdsDocument railway() throws IOException {
    try (InputStream in = AiaProjectCheckTest.class.getResourceAsStream(RAILWAY)) {
      if (in == null) {
        throw new IOException(RAILWAY + " is missing from the test classpath");
      }
      return IdsParser.read(in, RAILWAY);
    }
  }
}
