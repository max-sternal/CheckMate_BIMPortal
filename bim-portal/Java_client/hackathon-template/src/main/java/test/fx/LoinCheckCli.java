package test.fx;

import com.bimportal.client.model.LoinForPublicRequest;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.LoinIdsCheck;
import com.bimportal.hackathon.ids.ModelElement;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.List;

/**
 * Runs the LOIN check without a GUI: searches the BIM Portal for LOINs by name and reports each one
 * against the configured IFC model.
 *
 * <p>Headless on purpose - this is the path that works in a container, and the one to reach for when
 * a report has to be reproducible rather than clicked together.
 *
 * <pre>
 *   ./gradlew :hackathon-template:runLoinCheck --args="Kappe"
 *   CHECKMATE_IFC=/models/bruecke.ifc ./gradlew :hackathon-template:runLoinCheck --args="Kappe"
 * </pre>
 */
public final class LoinCheckCli {

  /** Enough example findings to see the pattern, few enough to read. */
  private static final int EXAMPLES = 8;

  private LoinCheckCli() {}

  public static void main(String[] args) {
    String search = args.length > 0 ? args[0] : "Kappe";

    var model = IfcResources.loadDefaultModel();
    String schema = IfcModelElements.schemaOf(model);
    List<ModelElement> elements = IfcModelElements.from(model);
    System.out.println("Modell: Schema " + schema + ", " + elements.size() + " Objekte");

    EnhancedBimPortalClient client = BimPortalClientBuilder.buildAuthenticatedOrPublic();
    try {
      List<SimpleLoinPublicDto> loins =
          client.searchLoins(new LoinForPublicRequest().searchString(search));
      System.out.println("Suche '" + search + "': " + loins.size() + " LOIN(s)\n");

      for (SimpleLoinPublicDto loin : loins) {
        report(client, loin, schema, elements);
      }
    } finally {
      client.logout();
    }
  }

  private static void report(
      EnhancedBimPortalClient client,
      SimpleLoinPublicDto loin,
      String schema,
      List<ModelElement> elements) {

    try {
      CheckResult result = LoinIdsCheck.check(client, loin.getGuid(), schema, elements);
      System.out.println(result.summary());

      if (result.notApplicable()) {
        // Nothing was checked, so the only thing worth printing is what stood in the way.
        result.diagnosis().lines().forEach(System.out::println);
      } else {
        result.findings().stream()
            .filter(finding -> !finding.status().ok())
            .limit(EXAMPLES)
            .forEach(finding -> System.out.println("   " + finding.line()));

        if (result.passed()) {
          System.out.println("   alle Anforderungen erfüllt");
        }
      }
    } catch (Exception e) {
      System.out.println(loin.getName() + ": " + e.getMessage());
    }
    System.out.println();
  }
}
