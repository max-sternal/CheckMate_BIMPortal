package test.fx;

import com.bimportal.client.model.LoinForPublicRequest;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.LoinIdsCheck;
import com.bimportal.hackathon.ids.ModelElement;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the LOIN check without a GUI: searches the BIM Portal for LOINs by name and reports each one
 * against an IFC model.
 *
 * <p>Headless on purpose - this is the path that works in a container, and the one to reach for when
 * a report has to be reproducible rather than clicked together.
 *
 * <pre>
 *   ./gradlew :hackathon-template:runLoinCheck --args="Kappe"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--model test-models/ifc4/Bridge.ifc Kappe"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--model https://github.com/o/r/blob/a.ifc"
 *   CHECKMATE_IFC=/models/bruecke.ifc ./gradlew :hackathon-template:runLoinCheck --args="Kappe"
 * </pre>
 *
 * <p>Without {@code --model}, the model {@code CHECKMATE_IFC} names is checked, or the bundled one.
 * A relative path is relative to {@code hackathon-template/}, where Gradle runs the task. Exit code
 * 1 when the model cannot be read, 2 for arguments it does not understand.
 */
public final class LoinCheckCli {

  /** Enough example findings to see the pattern, few enough to read. */
  private static final int EXAMPLES = 8;

  private static final String USAGE =
      "Aufruf: runLoinCheck --args=\"[--model <IFC-Datei oder Link>] [Suchbegriff ...]\"";

  private LoinCheckCli() {}

  /**
   * The command line: an optional model, then the search term, which may have several words.
   *
   * @param model a path or a link; null for the configured model
   */
  record Arguments(String model, String search) {

    static Arguments parse(String... args) {
      String model = null;
      List<String> words = new ArrayList<>();
      for (int i = 0; i < args.length; i++) {
        if ("--model".equals(args[i])) {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException("--model braucht eine IFC-Datei oder einen Link.");
          }
          model = args[++i];
        } else if (args[i].startsWith("--")) {
          throw new IllegalArgumentException("Unbekannte Option " + args[i]);
        } else {
          words.add(args[i]);
        }
      }
      return new Arguments(model, words.isEmpty() ? "Kappe" : String.join(" ", words));
    }
  }

  public static void main(String[] args) {
    Arguments arguments;
    try {
      arguments = Arguments.parse(args);
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.err.println(USAGE);
      System.exit(2);
      return;
    }

    LoadedModel model;
    try {
      model =
          arguments.model() == null
              ? LoadedModel.configured()
              : LoadedModel.read(arguments.model());
    } catch (ModelLoadException e) {
      System.err.println(e.getMessage());
      System.exit(1);
      return;
    }
    String search = arguments.search();
    String schema = model.schema();
    List<ModelElement> elements = model.elements();
    System.out.println("Modell: " + model.summary());

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
      if (!result.relatedTypes().isEmpty()) {
        System.out.println("   " + result.relatedTypes().line());
      }

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
