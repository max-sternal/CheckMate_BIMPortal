package test.fx;

import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.PortalLoins;
import com.bimportal.hackathon.ids.RequirementsException;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Runs the LOIN check without a GUI: checks an IFC model against LOINs named by ID, local IDS
 * files, or the LOINs a portal search finds, and reports each.
 *
 * <p>Headless on purpose - this is the path that works in a container, and the one to reach for
 * when a report has to be reproducible rather than clicked together.
 *
 * <pre>
 *   ./gradlew :hackathon-template:runLoinCheck --args="Kappe"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--loin be959b9d-daef-5315-9aba-f60b6ad54ccb"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--ids anforderungen.ids"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--model test-models/ifc4/Bridge.ifc Kappe"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--model https://github.com/o/r/blob/a.ifc"
 *   CHECKMATE_IFC=/models/bruecke.ifc ./gradlew :hackathon-template:runLoinCheck --args="Kappe"
 * </pre>
 *
 * <p>Without {@code --model}, the model {@code CHECKMATE_IFC} names is checked, or the bundled one.
 * Without {@code --loin}, {@code --ids} and a search term, the search term is "Kappe". A search
 * term holding a LOIN ID counts as {@code --loin}, as in the GUI. A relative path is relative to
 * {@code hackathon-template/}, where Gradle runs the task.
 *
 * <p>Exit code 1 when the model or any of the requirements cannot be read or checked (no such
 * LOIN, no IDS export, a file that is not IDS, no specification for the model's schema); the other
 * requirements are still checked. Exit code 2 for arguments it does not understand. Requirements
 * that are not met do not change the exit code yet.
 */
public final class LoinCheckCli {

  /** Enough example findings to see the pattern, few enough to read. */
  private static final int EXAMPLES = 8;

  /** What is searched for when nothing else is asked for: the demo term. */
  private static final String DEFAULT_SEARCH = "Kappe";

  private static final String USAGE =
      "Aufruf: runLoinCheck --args=\"[--model <IFC-Datei oder Link>] [--loin <LOIN-ID oder Link>]"
          + " [--ids <IDS-Datei>] [Suchbegriff ...]\"\n"
          + "--loin und --ids dürfen mehrfach stehen.";

  private LoinCheckCli() {}

  /**
   * The command line: an optional model, the requirements, and a search term of any number of
   * words.
   *
   * @param model a path or a link; null for the configured model
   * @param loins LOIN IDs, or texts holding one, such as links copied from the portal
   * @param idsFiles paths of local IDS files
   * @param search the search term; null when the requirements are given by ID or file only
   */
  record Arguments(String model, List<String> loins, List<String> idsFiles, String search) {

    static Arguments parse(String... args) {
      String model = null;
      List<String> loins = new ArrayList<>();
      List<String> idsFiles = new ArrayList<>();
      List<String> words = new ArrayList<>();
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--model" ->
              model = value(args, ++i, "--model braucht eine IFC-Datei oder einen Link.");
          case "--loin" ->
              loins.add(value(args, ++i, "--loin braucht eine LOIN-ID oder einen Link."));
          case "--ids" -> idsFiles.add(value(args, ++i, "--ids braucht eine IDS-Datei."));
          default -> {
            if (args[i].startsWith("--")) {
              throw new IllegalArgumentException("Unbekannte Option " + args[i]);
            }
            words.add(args[i]);
          }
        }
      }

      // Words holding a LOIN ID name that LOIN, as in the GUI's search field: searching the
      // catalogue for an ID finds nothing.
      String search = null;
      if (!words.isEmpty()) {
        String text = String.join(" ", words);
        if (containsLoinId(text)) {
          loins.add(text);
        } else {
          search = text;
        }
      } else if (loins.isEmpty() && idsFiles.isEmpty()) {
        search = DEFAULT_SEARCH;
      }
      return new Arguments(model, List.copyOf(loins), List.copyOf(idsFiles), search);
    }

    private static boolean containsLoinId(String text) {
      try {
        return PortalLoins.idIn(text).isPresent();
      } catch (RequirementsException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }

    private static String value(String[] args, int index, String missing) {
      if (index >= args.length || args[index].startsWith("--")) {
        throw new IllegalArgumentException(missing);
      }
      return args[index];
    }

    boolean needsPortal() {
      return !loins.isEmpty() || search != null;
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
    System.out.println("Modell: " + model.summary() + "\n");

    boolean allRead = true;
    for (String file : arguments.idsFiles()) {
      allRead &= report(file, () -> IdsParser.read(Path.of(file)), model);
    }

    if (arguments.needsPortal()) {
      EnhancedBimPortalClient client = BimPortalClientBuilder.buildAuthenticatedOrPublic();
      try {
        for (String loin : arguments.loins()) {
          allRead &= report(loin, () -> PortalLoins.requirements(client, loin), model);
        }
        if (arguments.search() != null) {
          List<SimpleLoinPublicDto> found = PortalLoins.search(client, arguments.search());
          System.out.println("Suche '" + arguments.search() + "': " + found.size() + " LOIN(s)\n");
          for (SimpleLoinPublicDto loin : found) {
            allRead &=
                report(
                    loin.getName(), () -> PortalLoins.requirements(client, loin.getGuid()), model);
          }
        }
      } finally {
        client.logout();
      }
    }
    System.exit(allRead ? 0 : 1);
  }

  /**
   * Read one set of requirements, check the model against it and print the outcome.
   *
   * @param name what the requirements are called before they are read, for error messages
   * @return false when the requirements could not be read or checked
   */
  private static boolean report(
      String name, Supplier<IdsDocument> requirements, LoadedModel model) {
    try {
      IdsDocument document = requirements.get();
      List<CheckResult> results = document.check(model.schema(), model.elements());
      System.out.println("Anforderungen: " + document.describe());
      results.forEach(LoinCheckCli::print);
      return true;
    } catch (RequirementsException e) {
      System.out.println(e.getMessage() + "\n");
      return false;
    } catch (RuntimeException e) {
      System.out.println(name + ": " + e + "\n");
      return false;
    }
  }

  private static void print(CheckResult result) {
    System.out.println(result.summary());
    result.notes().forEach(note -> System.out.println("   " + note));

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
      } else if (!result.complete() && result.findings().stream().allMatch(f -> f.status().ok())) {
        System.out.println("   alle ausgewerteten Anforderungen erfüllt");
      }
    }
    System.out.println();
  }
}
