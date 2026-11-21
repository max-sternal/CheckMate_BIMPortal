package test.fx;

import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.catalogue.CatalogueEntry;
import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.catalogue.ModelFit;
import com.bimportal.hackathon.catalogue.ModelFit.Ranked;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.PortalRequirements;
import com.bimportal.hackathon.ids.RequirementsException;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Runs the LOIN check without a GUI: checks an IFC model against LOINs and AIA projects named by
 * ID, local IDS files and AIA project ZIPs, or what a portal search finds, and reports each.
 *
 * <p>Headless on purpose - this is the path that works in a container, and the one to reach for
 * when a report has to be reproducible rather than clicked together.
 *
 * <pre>
 *   ./gradlew :hackathon-template:runLoinCheck --args="Kappe"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--loin be959b9d-daef-5315-9aba-f60b6ad54ccb"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--aia e6f69257-dfda-441b-97bb-e9df44c5cf8b"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--ids anforderungen.ids"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--suggest --phase LPH 5"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--model test-models/ifc4/Bridge.ifc Kappe"
 *   ./gradlew :hackathon-template:runLoinCheck --args="--model https://github.com/o/r/blob/a.ifc"
 *   CHECKMATE_IFC=/models/bruecke.ifc ./gradlew :hackathon-template:runLoinCheck --args="Kappe"
 * </pre>
 *
 * <p>Without {@code --model}, the model {@code CHECKMATE_IFC} names is checked, or the bundled one.
 * {@code --loin} and {@code --aia} both take any ID from the portal, a LOIN's or an AIA project's;
 * the two names are only for reading. {@code --ids} takes an IDS file or a ZIP of them, such as
 * an AIA project's ZIP downloaded from the portal. Without any of these and without a search term,
 * the search term is "Kappe". A search term holding an ID counts as {@code --loin}, as in the GUI;
 * any other finds AIA projects and LOINs. A relative path is relative to {@code
 * hackathon-template/}, where Gradle runs the task.
 *
 * <p>{@code --suggest} checks nothing and downloads no requirements: it lists the catalogue's
 * LOINs whose object type occurs in the model, best first, with their IDs to pass to {@code
 * --loin}. {@code --discipline}, {@code --phase} and {@code --purpose} narrow that list, and the
 * LOINs a search checks, by the catalogue's metadata; a part of the value is enough ({@code LPH
 * 5}). Without a search term, {@code --suggest} goes through the whole catalogue.
 *
 * <p>Exit code 1 when the model or any of the requirements cannot be read or checked (no such
 * LOIN or project, no IDS file to download, a file that is not IDS, no specification for the
 * model's schema); the other requirements are still checked. Exit code 2 for arguments it does not
 * understand. Requirements that are not met do not change the exit code yet.
 */
public final class LoinCheckCli {

  /** Enough example findings to see the pattern, few enough to read. */
  private static final int EXAMPLES = 8;

  /** What is searched for when nothing else is asked for: the demo term. */
  private static final String DEFAULT_SEARCH = "Kappe";

  private static final String USAGE =
      "Aufruf: runLoinCheck --args=\"[--model <IFC-Datei oder Link>] [--loin <LOIN-ID oder Link>]"
          + " [--aia <AIA-Projekt-ID oder Link>] [--ids <IDS-Datei oder ZIP>] [--suggest]"
          + " [--discipline <Fachmodell>] [--phase <Leistungsphase>] [--purpose <Anwendungsfall>]"
          + " [Suchbegriff ...]\"\n"
          + "--loin, --aia und --ids dürfen mehrfach stehen.";

  private LoinCheckCli() {}

  /**
   * The command line: an optional model, the requirements, and a search term of any number of
   * words.
   *
   * @param model a path or a link; null for the configured model
   * @param portalIds IDs of LOINs or AIA projects, or texts holding one, such as links copied from
   *     the portal
   * @param idsFiles paths of local IDS files or ZIPs of them
   * @param search the search term; null when the requirements are given by ID or file only;
   *     empty for the whole catalogue
   * @param suggest list the fitting LOINs instead of checking the search's
   * @param filter the context the LOINs of a search or suggestion have to fit
   */
  record Arguments(
      String model,
      List<String> portalIds,
      List<String> idsFiles,
      String search,
      boolean suggest,
      CatalogueFilter filter) {

    /** Arguments that check, without filters. */
    Arguments(String model, List<String> portalIds, List<String> idsFiles, String search) {
      this(model, portalIds, idsFiles, search, false, CatalogueFilter.ANY);
    }

    static Arguments parse(String... args) {
      String model = null;
      List<String> portalIds = new ArrayList<>();
      List<String> idsFiles = new ArrayList<>();
      List<String> words = new ArrayList<>();
      boolean suggest = false;
      String discipline = null;
      String phase = null;
      String purpose = null;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--suggest" -> suggest = true;
          case "--discipline" ->
              discipline = value(args, ++i, "--discipline braucht ein Fachmodell.");
          case "--phase" -> phase = value(args, ++i, "--phase braucht eine Leistungsphase.");
          case "--purpose" -> purpose = value(args, ++i, "--purpose braucht einen Anwendungsfall.");
          case "--model" ->
              model = value(args, ++i, "--model braucht eine IFC-Datei oder einen Link.");
          case "--loin" ->
              portalIds.add(value(args, ++i, "--loin braucht eine LOIN-ID oder einen Link."));
          case "--aia" ->
              portalIds.add(value(args, ++i, "--aia braucht eine AIA-Projekt-ID oder einen Link."));
          case "--ids" -> idsFiles.add(value(args, ++i, "--ids braucht eine IDS-Datei."));
          default -> {
            if (args[i].startsWith("--")) {
              throw new IllegalArgumentException("Unbekannte Option " + args[i]);
            }
            words.add(args[i]);
          }
        }
      }

      // Words holding an ID name that LOIN or project, as in the GUI's search field: searching
      // the catalogue for an ID finds nothing.
      String search = null;
      if (!words.isEmpty()) {
        String text = String.join(" ", words);
        if (containsId(text)) {
          portalIds.add(text);
        } else {
          search = text;
        }
      } else if (suggest) {
        search = "";
      } else if (portalIds.isEmpty() && idsFiles.isEmpty()) {
        search = DEFAULT_SEARCH;
      }
      return new Arguments(
          model,
          List.copyOf(portalIds),
          List.copyOf(idsFiles),
          search,
          suggest,
          new CatalogueFilter(discipline, phase, purpose));
    }

    private static boolean containsId(String text) {
      try {
        return PortalRequirements.idIn(text).isPresent();
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
      return !portalIds.isEmpty() || search != null;
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
        for (String id : arguments.portalIds()) {
          allRead &= report(id, () -> PortalRequirements.requirements(client, id), model);
        }
        if (arguments.suggest()) {
          suggest(client, arguments.search(), arguments.filter(), model);
        } else if (arguments.search() != null) {
          allRead &= search(client, arguments.search(), arguments.filter(), model);
        }
      } finally {
        client.logout();
      }
    }
    System.exit(allRead ? 0 : 1);
  }

  /**
   * List the catalogue's LOINs that fit the model, best first, without downloading any.
   *
   * <p>Each line starts with the ID, so it can be passed to {@code --loin} as it is.
   */
  private static void suggest(
      EnhancedBimPortalClient client, String term, CatalogueFilter filter, LoadedModel model) {
    List<CatalogueEntry> found =
        PortalRequirements.search(client, term).stream().map(CatalogueEntry::of).toList();
    List<CatalogueEntry> filtered = filter.apply(found);
    List<Ranked> ranked = ModelFit.of(model.elements()).rank(filtered);
    long fitting = ranked.stream().filter(r -> r.fit().fits()).count();
    System.out.println(
        (term.isBlank() ? "Katalog" : "Suche '" + term + "'")
            + ": "
            + found.size()
            + " LOIN(s)"
            + (filter.isAny() ? "" : ", " + filtered.size() + " nach Filter")
            + ", "
            + fitting
            + " passend zum Modell\n");
    if (fitting == 0) {
      System.out.println("Kein Objekttyp dieser LOINs kommt im Modell vor. Die ersten 20:\n");
    }
    ranked.stream()
        .filter(r -> fitting == 0 || r.fit().fits())
        .limit(fitting == 0 ? 20 : Long.MAX_VALUE)
        .forEach(
            r ->
                System.out.println(
                    r.entry().guid()
                        + "  "
                        + r.entry().name()
                        + (r.fit().fits() ? "  -  " + r.fit().describe() : "")));
    System.out.println("\nPrüfen mit: runLoinCheck --args=\"--loin <ID> [--loin <ID> ...]\"");
  }

  /**
   * Check the model against every AIA project and LOIN a search finds, projects first. The filter
   * narrows the LOINs.
   *
   * @return false when any of them could not be read or checked
   */
  private static boolean search(
      EnhancedBimPortalClient client, String term, CatalogueFilter filter, LoadedModel model) {
    List<SimpleAiaProjectPublicDto> projects = PortalRequirements.searchProjects(client, term);
    List<SimpleLoinPublicDto> found = PortalRequirements.search(client, term);
    List<SimpleLoinPublicDto> loins =
        found.stream().filter(loin -> filter.accepts(CatalogueEntry.of(loin))).toList();
    System.out.println(
        "Suche '"
            + term
            + "': "
            + found.size()
            + " LOIN(s)"
            + (filter.isAny() ? "" : ", " + loins.size() + " nach Filter")
            + ", "
            + projects.size()
            + " AIA-Projekt(e)\n");

    boolean allRead = true;
    for (SimpleAiaProjectPublicDto project : projects) {
      allRead &=
          report(
              project.getName(),
              () -> PortalRequirements.project(client, project.getGuid(), project.getName()),
              model);
    }
    for (SimpleLoinPublicDto loin : loins) {
      allRead &=
          report(
              loin.getName(),
              () -> PortalRequirements.loin(client, loin.getGuid(), loin.getName()),
              model);
    }
    return allRead;
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
      if (results.size() > 1) {
        System.out.println(CheckResult.tally(results, model.schema()) + "\n");
      }
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
