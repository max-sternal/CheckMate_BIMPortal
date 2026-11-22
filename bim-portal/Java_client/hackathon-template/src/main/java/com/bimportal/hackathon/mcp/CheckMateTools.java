package com.bimportal.hackathon.mcp;

import com.bimportal.hackathon.catalogue.CatalogueEntry;
import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.catalogue.LoinDescription;
import com.bimportal.hackathon.catalogue.ModelFit;
import com.bimportal.hackathon.catalogue.ModelFit.Ranked;
import com.bimportal.hackathon.check.CheckService;
import com.bimportal.hackathon.check.CheckService.Found;
import com.bimportal.hackathon.check.Source;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.PortalRequirements;
import com.bimportal.hackathon.ifc.LoadedModel;
import com.bimportal.hackathon.report.CheckReport;
import com.bimportal.hackathon.report.CheckReport.Run;
import com.bimportal.hackathon.report.ReportText;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What the MCP server's tools do, for one session: the model in use, the last check, and the
 * requirements downloaded so far. Plain Java, so it is tested without the protocol.
 */
public final class CheckMateTools {

  private final CheckService service;
  private final Function<String, LoadedModel> modelReader;
  private final Supplier<LoadedModel> defaultModel;

  private LoadedModel model;
  private CheckReport lastReport;
  private List<CatalogueEntry> lastFound = List.of();

  /**
   * @param modelReader reads a model from a path or a link
   * @param defaultModel the model to use when none was loaded: the configured or bundled one
   */
  public CheckMateTools(
      CheckService service,
      Function<String, LoadedModel> modelReader,
      Supplier<LoadedModel> defaultModel) {
    this.service = service;
    this.modelReader = modelReader;
    this.defaultModel = defaultModel;
  }

  /** Load a model from a path or a link; it replaces the one before. */
  public synchronized String loadModel(String source) {
    model = modelReader.apply(source);
    lastReport = null;
    return describeModel();
  }

  /** The model in use, with its most frequent entities. */
  public synchronized String describeModel() {
    LoadedModel current = model();
    Map<String, Integer> entities = new TreeMap<>();
    current.elements().forEach(e -> entities.merge(e.entityName(), 1, Integer::sum));
    List<String> lines = new ArrayList<>();
    lines.add("Modell: " + current.summary());
    lines.add("Entities:");
    entities.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(15)
        .forEach(entry -> lines.add("   " + entry.getKey() + ": " + entry.getValue()));
    return String.join("\n", lines);
  }

  /** Elements of the model, filtered by entity and part of the name. */
  public synchronized String listElements(String entity, String name, int limit) {
    List<ModelElement> found =
        model().elements().stream()
            .filter(e -> blank(entity) || e.entityName().equalsIgnoreCase(entity.trim()))
            .filter(e -> blank(name) || contains(e.name(), name))
            .toList();
    List<String> lines = new ArrayList<>();
    lines.add(found.size() + " Element(e)" + (found.size() > limit ? ", die ersten " + limit : ""));
    found.stream()
        .limit(limit)
        .forEach(
            e ->
                lines.add(
                    e.id()
                        + " | "
                        + e.entityName()
                        + " | "
                        + e.name()
                        + (e.predefinedType() == null ? "" : " | " + e.predefinedType())));
    return String.join("\n", lines);
  }

  /**
   * AIA projects and LOINs from the portal, the LOINs that fit the model first. Nothing is
   * downloaded but the catalogue.
   */
  public synchronized String findLoins(
      String search, CatalogueFilter filter, int limit, boolean includeUnfitting) {
    Found found = service.search(search == null ? "" : search);
    List<CatalogueEntry> filtered = filter.apply(found.loins());
    List<Ranked> ranked = ModelFit.of(model().elements()).rank(filtered);
    long fitting = ranked.stream().filter(r -> r.fit().fits()).count();
    lastFound = filtered;

    List<String> lines = new ArrayList<>();
    lines.add(
        found.loins().size()
            + " LOIN(s), "
            + filtered.size()
            + " nach Filter, "
            + fitting
            + " passend zum Modell; "
            + found.projects().size()
            + " AIA-Projekt(e)");
    found.projects()
        .forEach(p -> lines.add("AIA-Projekt | " + p.guid() + " | " + p.name()));
    ranked.stream()
        .filter(r -> includeUnfitting || fitting == 0 || r.fit().fits())
        .limit(limit)
        .forEach(
            r ->
                lines.add(
                    "LOIN | "
                        + r.entry().guid()
                        + " | "
                        + r.entry().name()
                        + " | "
                        + (r.fit().fits() ? r.fit().describe() : "passt nicht erkennbar")));
    return String.join("\n", lines);
  }

  /** What a LOIN or AIA project asks for, and how many elements of the model it would apply to. */
  public synchronized String describeRequirements(String id) {
    IdsDocument requirements = service.requirements(new Source.PortalId(id));
    LoadedModel current = model();
    Optional<UUID> guid = PortalRequirements.idIn(id);
    Optional<CatalogueEntry> entry =
        lastFound.stream().filter(e -> guid.isPresent() && guid.get().equals(e.guid())).findFirst();
    List<String> lines =
        entry.isPresent()
            ? LoinDescription.describe(
                entry.get(),
                ModelFit.of(current.elements()).fit(entry.get()),
                requirements,
                current.schema(),
                current.elements())
            : LoinDescription.describe(requirements, current.schema(), current.elements());
    return String.join("\n", lines);
  }

  /**
   * Check the model against LOINs or AIA projects by ID, and IDS files. The report is kept for
   * {@link #elementReport}.
   *
   * @param examples findings per specification in the brief report; 0 for the full report
   */
  public synchronized String check(List<String> ids, List<String> idsFiles, int examples) {
    List<Source> sources = new ArrayList<>();
    idsFiles.forEach(file -> sources.add(new Source.IdsFile(Path.of(file))));
    ids.forEach(id -> sources.add(new Source.PortalId(id)));
    if (sources.isEmpty()) {
      throw new IllegalArgumentException("Keine Anforderungen angegeben: ids oder ids_files.");
    }
    LoadedModel current = model();
    lastReport =
        service.check(sources, current.info(), current.elements(), OffsetDateTime.now());

    List<String> lines = new ArrayList<>();
    if (examples <= 0) {
      lines.addAll(ReportText.full(lastReport));
    } else {
      lines.addAll(lastReport.header());
      lines.add("");
      for (Run run : lastReport.runs()) {
        lines.addAll(ReportText.brief(run, current.schema(), examples));
      }
    }
    lines.add(ReportText.verdictLine(lastReport));
    return String.join("\n", lines);
  }

  /** What the last check said about an element, found by GlobalId or a part of its name. */
  public synchronized String elementReport(String element) {
    if (blank(element)) {
      throw new IllegalArgumentException("Kein Element angegeben (GlobalId oder Namensteil).");
    }
    if (lastReport == null) {
      throw new IllegalStateException("Noch keine Prüfung - zuerst check_model aufrufen.");
    }
    List<ModelElement> matches =
        model().elements().stream()
            .filter(e -> e.id().equals(element.trim()) || contains(e.name(), element))
            .limit(5)
            .toList();
    if (matches.isEmpty()) {
      return "Kein Element mit der GlobalId oder dem Namensteil '" + element + "'.";
    }
    List<String> lines = new ArrayList<>();
    for (ModelElement match : matches) {
      lines.add(match.entityName() + " '" + match.name() + "' (" + match.id() + ")");
      boolean any = false;
      for (CheckResult result : lastReport.results()) {
        List<Finding> findings =
            result.findings().stream().filter(f -> f.elementId().equals(match.id())).toList();
        if (!findings.isEmpty()) {
          any = true;
          lines.add("   " + result.specification().label() + ":");
          findings.forEach(f -> lines.add("      " + f.requirementLine()));
        }
      }
      if (!any) {
        lines.add("   von keiner geprüften Spezifikation erfasst");
      }
      lines.add("   Merkmale: " + match.properties().size());
      match.properties().stream().limit(40).forEach(p -> lines.add("      " + p));
    }
    return String.join("\n", lines);
  }

  private LoadedModel model() {
    if (model == null) {
      model = defaultModel.get();
    }
    return model;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static boolean contains(String text, String part) {
    return text != null
        && text.toLowerCase(Locale.ROOT).contains(part.trim().toLowerCase(Locale.ROOT));
  }
}
