package com.bimportal.hackathon.cli;

import com.bimportal.hackathon.catalogue.CatalogueEntry;
import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.catalogue.ModelFit;
import com.bimportal.hackathon.catalogue.ModelFit.Ranked;
import com.bimportal.hackathon.check.CheckService;
import com.bimportal.hackathon.check.CheckService.Found;
import com.bimportal.hackathon.check.CheckService.Portal;
import com.bimportal.hackathon.check.Source;
import com.bimportal.hackathon.ids.PortalRequirements;
import com.bimportal.hackathon.ids.RequirementsException;
import com.bimportal.hackathon.ifc.LoadedModel;
import com.bimportal.hackathon.ifc.ModelLoadException;
import com.bimportal.hackathon.report.CheckReport;
import com.bimportal.hackathon.report.ReportJson;
import com.bimportal.hackathon.report.ReportText;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CheckMate without a window: reads the command line, loads the model, and prints what {@link
 * CheckService} reports. Options, exit codes and examples: {@code docs/WORKFLOW.md}, section 2.5.
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
          + " [--json <Datei>] [--full] [Suchbegriff ...]\"\n"
          + "--loin, --aia und --ids dürfen mehrfach stehen.";

  private LoinCheckCli() {}

  /**
   * The command line.
   *
   * @param model a path or a link; null for the configured model
   * @param portalIds IDs of LOINs or AIA projects, or texts holding one
   * @param idsFiles paths of local IDS files or ZIPs of them
   * @param search the search term; null when the requirements are given by ID or file only, empty
   *     for the whole catalogue
   * @param suggest list the fitting LOINs instead of checking
   * @param filter the context the LOINs of a search or suggestion have to fit
   * @param json where to write the report as JSON; null for no JSON
   * @param full print the whole report, as the GUI shows it, instead of a few examples
   */
  record Arguments(
      String model,
      List<String> portalIds,
      List<String> idsFiles,
      String search,
      boolean suggest,
      CatalogueFilter filter,
      String json,
      boolean full) {

    /** Arguments that check, without filters, JSON or full report. */
    Arguments(String model, List<String> portalIds, List<String> idsFiles, String search) {
      this(model, portalIds, idsFiles, search, false, CatalogueFilter.ANY, null, false);
    }

    static Arguments parse(String... args) {
      String model = null;
      List<String> portalIds = new ArrayList<>();
      List<String> idsFiles = new ArrayList<>();
      List<String> words = new ArrayList<>();
      boolean suggest = false;
      boolean full = false;
      String discipline = null;
      String phase = null;
      String purpose = null;
      String json = null;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--suggest" -> suggest = true;
          case "--full" -> full = true;
          case "--json" -> json = value(args, ++i, "--json braucht eine Datei.");
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
      if (suggest && json != null) {
        throw new IllegalArgumentException("--json gilt für Prüfungen, --suggest prüft nichts.");
      }
      return new Arguments(
          model,
          List.copyOf(portalIds),
          List.copyOf(idsFiles),
          search,
          suggest,
          new CatalogueFilter(discipline, phase, purpose),
          json,
          full);
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

    OffsetDateTime checkedAt = OffsetDateTime.now();
    LoadedModel model;
    try {
      model =
          arguments.model() == null
              ? LoadedModel.configured()
              : LoadedModel.read(arguments.model());
    } catch (ModelLoadException e) {
      System.err.println(e.getMessage());
      System.exit(CheckReport.NOT_READ);
      return;
    }

    // One client for the whole run, and only when the portal is needed at all.
    EnhancedBimPortalClient client =
        arguments.needsPortal() ? BimPortalClientBuilder.buildAuthenticatedOrPublic() : null;
    int exitCode;
    try {
      CheckService service =
          new CheckService(client == null ? Portal.none() : Portal.shared(client));
      exitCode =
          arguments.suggest()
              ? suggest(service, arguments, model)
              : check(service, arguments, model, checkedAt);
    } finally {
      if (client != null) {
        client.logout();
      }
    }
    System.exit(exitCode);
  }

  /**
   * Check everything the arguments name. Briefly, each set of requirements is printed as soon as
   * it is checked; in full, the whole report at the end.
   *
   * @return the exit code
   */
  private static int check(
      CheckService service, Arguments arguments, LoadedModel model, OffsetDateTime checkedAt) {
    String schema = model.schema();
    if (!arguments.full()) {
      new CheckReport(checkedAt, model.info(), List.of()).header().forEach(System.out::println);
      System.out.println();
    }

    List<Source> sources = new ArrayList<>();
    arguments.idsFiles().forEach(file -> sources.add(new Source.IdsFile(Path.of(file))));
    arguments.portalIds().forEach(id -> sources.add(new Source.PortalId(id)));
    if (arguments.search() != null) {
      sources.addAll(search(service, arguments.search(), arguments.filter(), !arguments.full()));
    }

    CheckReport report =
        service.check(
            sources,
            model.info(),
            model.elements(),
            checkedAt,
            run -> {
              if (!arguments.full()) {
                ReportText.brief(run, schema, EXAMPLES).forEach(System.out::println);
              }
            });
    if (arguments.full()) {
      ReportText.full(report).forEach(System.out::println);
      System.out.println();
    }
    System.out.println(ReportText.verdictLine(report));

    int exitCode = report.exitCode();
    if (arguments.json() != null) {
      exitCode = writeJson(report, Path.of(arguments.json()), exitCode);
    }
    return exitCode;
  }

  /** The AIA projects and filtered LOINs a search finds, projects first. */
  private static List<Source> search(
      CheckService service, String term, CatalogueFilter filter, boolean tell) {
    Found found = service.search(term);
    List<CatalogueEntry> loins = filter.apply(found.loins());
    if (tell) {
      System.out.println(
          "Suche '"
              + term
              + "': "
              + found.loins().size()
              + " LOIN(s)"
              + (filter.isAny() ? "" : ", " + loins.size() + " nach Filter")
              + ", "
              + found.projects().size()
              + " AIA-Projekt(e)\n");
    }
    List<Source> sources = new ArrayList<>(found.projects());
    loins.forEach(loin -> sources.add(new Source.Loin(loin.guid(), loin.name())));
    return sources;
  }

  /**
   * List the catalogue's LOINs that fit the model, best first, without downloading any. Each line
   * starts with the ID, ready for {@code --loin}.
   *
   * @return the exit code: 0, nothing is checked
   */
  private static int suggest(CheckService service, Arguments arguments, LoadedModel model) {
    System.out.println("Modell: " + model.summary() + "\n");
    String term = arguments.search();
    CatalogueFilter filter = arguments.filter();
    List<CatalogueEntry> found = service.search(term).loins();
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
    return CheckReport.PASSED;
  }

  /**
   * Write the report as JSON.
   *
   * @return the exit code to end with: 1 when the file cannot be written, else the given one
   */
  private static int writeJson(CheckReport report, Path file, int exitCode) {
    try {
      Files.writeString(file, ReportJson.write(report), StandardCharsets.UTF_8);
      System.out.println("JSON: " + file.toAbsolutePath());
      return exitCode;
    } catch (IOException e) {
      System.err.println("Der Bericht lässt sich nicht nach " + file + " schreiben: " + e);
      return CheckReport.NOT_READ;
    }
  }
}
