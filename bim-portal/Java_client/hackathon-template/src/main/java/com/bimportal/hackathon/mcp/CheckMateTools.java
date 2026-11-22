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
import com.bimportal.hackathon.ids.PropertyAliases;
import com.bimportal.hackathon.ifc.LoadedModel;
import com.bimportal.hackathon.properties.PortalProperties;
import com.bimportal.hackathon.properties.PortalProperties.PropertyDetail;
import com.bimportal.hackathon.properties.PortalProperties.PropertyGroupDetail;
import com.bimportal.hackathon.properties.PortalProperties.PropertySummary;
import com.bimportal.hackathon.report.CheckReport;
import com.bimportal.hackathon.report.CheckReport.Run;
import com.bimportal.hackathon.report.ReportJson;
import com.bimportal.hackathon.report.ReportText;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

  private static final ObjectMapper JSON =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  private final CheckService service;
  private final Function<String, LoadedModel> modelReader;
  private final Supplier<LoadedModel> defaultModel;
  private final Supplier<PortalProperties> propertiesSupplier;

  private LoadedModel model;
  private PortalProperties propertiesClient;
  private CheckReport lastReport;
  private List<CatalogueEntry> lastFound = List.of();
  private Map<String, Object> lastLoinsData = Map.of();
  private Map<String, Object> lastPropertiesData = Map.of();
  private Map<String, Object> lastPropertyDetailData = Map.of();

  /**
   * @param modelReader reads a model from a path or a link
   * @param defaultModel the model to use when none was loaded: the configured or bundled one
   */
  public CheckMateTools(
      CheckService service,
      Function<String, LoadedModel> modelReader,
      Supplier<LoadedModel> defaultModel) {
    this(service, modelReader, defaultModel, null);
  }

  /**
   * @param modelReader reads a model from a path or a link
   * @param defaultModel the model to use when none was loaded: the configured or bundled one
   * @param propertiesSupplier lazy supplier for the BIM Portal Merkmale &amp; Merkmalsgruppen API
   */
  public CheckMateTools(
      CheckService service,
      Function<String, LoadedModel> modelReader,
      Supplier<LoadedModel> defaultModel,
      Supplier<PortalProperties> propertiesSupplier) {
    this.service = service;
    this.modelReader = modelReader;
    this.defaultModel = defaultModel;
    this.propertiesSupplier = propertiesSupplier;
  }

  /** Load a model from a path or a link; it replaces the one before. */
  public synchronized String loadModel(String source) {
    model = modelReader.apply(source);
    lastReport = null;
    lastLoinsData = Map.of();
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

  /** The model in use as structured data for the model overview view and JSON resource. */
  public synchronized Map<String, Object> modelData() {
    LoadedModel current = model();
    Map<String, Integer> counts = new TreeMap<>();
    current.elements().forEach(e -> counts.merge(e.entityName(), 1, Integer::sum));
    Map<String, Integer> sortedCounts = new LinkedHashMap<>();
    counts.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .forEach(entry -> sortedCounts.put(entry.getKey(), entry.getValue()));
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("name", current.name());
    data.put("schema", current.schema());
    data.put("elementCount", current.elements().size());
    data.put("entityCounts", sortedCounts);
    data.put("mappingName", PropertyAliases.current().name());
    return data;
  }

  /** The current model overview serialized as JSON, for the {@code checkmate://model/current} resource. */
  public synchronized String currentModelJson() {
    return toJson(modelData());
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

    List<Ranked> shown =
        ranked.stream()
            .filter(r -> includeUnfitting || fitting == 0 || r.fit().fits())
            .limit(limit)
            .toList();
    lastLoinsData = buildLoinsData(found, filtered.size(), fitting, shown);

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
    found.projects().forEach(p -> lines.add("AIA-Projekt | " + p.guid() + " | " + p.name()));
    shown.forEach(
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

  /**
   * The last {@link #findLoins} result as structured data for the LOIN picker view. Empty before
   * the first search.
   */
  public synchronized Map<String, Object> loinsData() {
    return lastLoinsData;
  }

  private static Map<String, Object> buildLoinsData(
      Found found, int filteredCount, long fittingCount, List<Ranked> shown) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("totalLoins", found.loins().size());
    data.put("filteredLoins", filteredCount);
    data.put("fittingLoins", fittingCount);

    List<Map<String, Object>> projects = new ArrayList<>();
    found
        .projects()
        .forEach(
            p -> {
              Map<String, Object> proj = new LinkedHashMap<>();
              proj.put("id", p.guid() == null ? "" : p.guid().toString());
              proj.put("name", p.name());
              projects.add(proj);
            });
    data.put("projects", projects);

    List<Map<String, Object>> loins = new ArrayList<>();
    for (Ranked ranked : shown) {
      CatalogueEntry entry = ranked.entry();
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", entry.guid() == null ? "" : entry.guid().toString());
      item.put("name", entry.name());
      item.put("version", entry.version());
      item.put("organisation", entry.organisation());
      item.put("objectTypes", entry.objectTypes());
      item.put("disciplines", entry.disciplines());
      item.put("phases", entry.phases());
      item.put("purposes", entry.purposes());
      item.put("fits", ranked.fit().fits());
      item.put("fitKind", ranked.fit().kind().name());
      item.put("fitElements", ranked.fit().elements());
      item.put(
          "fitDescription",
          ranked.fit().fits() ? ranked.fit().describe() : "passt nicht erkennbar");
      loins.add(item);
    }
    data.put("loins", loins);
    return data;
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
    lastReport = service.check(sources, current.info(), current.elements(), OffsetDateTime.now());

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

  /**
   * The last check as data for a view: per run its specifications with verdict, counts and a few
   * unmet findings. Empty before the first check.
   *
   * @param examples unmet findings to carry per specification
   */
  public synchronized Map<String, Object> reportData(int examples) {
    Map<String, Object> data = new LinkedHashMap<>();
    if (lastReport == null) {
      return data;
    }
    data.put("header", lastReport.header());
    data.put("verdict", ReportText.verdictLine(lastReport));
    List<Map<String, Object>> runs = new ArrayList<>();
    for (Run run : lastReport.runs()) {
      Map<String, Object> runData = new LinkedHashMap<>();
      runData.put("name", run.name());
      runData.put("error", run.error());
      List<Map<String, Object>> specs = new ArrayList<>();
      for (CheckResult result : run.results()) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("label", result.specification().label());
        spec.put("verdict", result.verdict().name());
        spec.put("verdictLabel", result.verdict().label());
        spec.put("examined", result.examinedElements());
        spec.put("applicable", result.applicableElements());
        spec.put("failing", result.failingElements());
        spec.put("passing", result.passingElements());
        spec.put("summary", result.summary());
        spec.put("unmetTotal", unmetFindings(result).size());
        spec.put("unmet", unmetPage(result, 0, examples));
        specs.add(spec);
      }
      runData.put("specifications", specs);
      runs.add(runData);
    }
    data.put("runs", runs);
    return data;
  }

  /**
   * The last check report serialized as JSON, for the {@code checkmate://report/last.json}
   * resource.
   */
  public synchronized String lastReportJson() {
    if (lastReport == null) {
      Map<String, Object> empty = new LinkedHashMap<>();
      empty.put("status", "none");
      empty.put("message", "Noch keine Prüfung durchgeführt.");
      return toJson(empty);
    }
    return ReportJson.write(lastReport);
  }

  private static List<Finding> unmetFindings(CheckResult result) {
    return result.findings().stream().filter(f -> !f.status().ok()).toList();
  }

  private static List<Map<String, Object>> unmetPage(CheckResult result, int offset, int limit) {
    return unmetFindings(result).stream()
        .skip(Math.max(offset, 0))
        .limit(Math.max(limit, 0))
        .map(
            f -> {
              Map<String, Object> finding = new LinkedHashMap<>();
              finding.put("elementId", f.elementId());
              finding.put("element", f.elementLabel());
              finding.put("line", f.requirementLine());
              return finding;
            })
        .toList();
  }

  /**
   * More unmet findings of one specification of the last check, for a view that loads them on
   * demand instead of carrying them all.
   *
   * @param run index of the run, as in {@link #reportData}
   * @param spec index of the specification within the run
   * @param offset findings to skip
   * @param limit findings to return
   * @return {@code run}, {@code spec}, {@code offset}, {@code total} and {@code unmet}
   */
  public synchronized Map<String, Object> reportPage(int run, int spec, int offset, int limit) {
    if (lastReport == null) {
      throw new IllegalStateException("Noch keine Prüfung - zuerst check_model aufrufen.");
    }
    if (run < 0 || run >= lastReport.runs().size()) {
      throw new IllegalArgumentException("Kein Lauf " + run + " in der letzten Prüfung.");
    }
    List<CheckResult> results = lastReport.runs().get(run).results();
    if (spec < 0 || spec >= results.size()) {
      throw new IllegalArgumentException("Keine Spezifikation " + spec + " in Lauf " + run + ".");
    }
    CheckResult result = results.get(spec);
    Map<String, Object> page = new LinkedHashMap<>();
    page.put("run", run);
    page.put("spec", spec);
    page.put("offset", Math.max(offset, 0));
    page.put("total", unmetFindings(result).size());
    page.put("unmet", unmetPage(result, offset, limit));
    return page;
  }

  /**
   * Which property mapping is in use, or switch it.
   *
   * @param source a path to a mapping JSON file, {@code none}, {@code default}; blank to only
   *     describe the one in use
   */
  public synchronized String mapping(String source) {
    if (!blank(source)) {
      PropertyAliases.use(PropertyAliases.resolve(source));
      lastReport = null;
    }
    return String.join("\n", PropertyAliases.current().describe());
  }

  /** The state of the session: model, mapping, last check. */
  public synchronized String status() {
    List<String> lines = new ArrayList<>();
    lines.add(
        "Modell: "
            + (model == null
                ? "noch keines geladen (Standardmodell wird genutzt)"
                : model.summary()));
    lines.add("Mapping: " + PropertyAliases.current().name());
    lines.add(
        "Letzte Prüfung: " + (lastReport == null ? "keine" : ReportText.verdictLine(lastReport)));
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

  /**
   * Search Merkmale (properties) and/or Merkmalsgruppen (property groups) in the German BIM Portal.
   */
  public synchronized String findProperties(String search, String kind, int limit) {
    int cap = limit <= 0 ? 20 : Math.min(limit, 100);
    String mode = blank(kind) ? "all" : kind.trim().toLowerCase(Locale.ROOT);
    PortalProperties portal = properties();

    List<PropertySummary> props = List.of();
    List<PropertySummary> groups = List.of();
    switch (mode) {
      case "property", "properties", "merkmal", "merkmale" ->
          props = portal.searchProperties(search, cap);
      case "group", "groups", "gruppe", "merkmalsgruppe", "merkmalsgruppen" ->
          groups = portal.searchPropertyGroups(search, cap);
      case "all", "alle" -> {
        props = portal.searchProperties(search, cap);
        groups = portal.searchPropertyGroups(search, cap);
      }
      default ->
          throw new IllegalArgumentException(
              "Unbekannter Typ '" + kind + "' (erlaubt: all, property, group).");
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("query", blank(search) ? "" : search.trim());
    data.put("kind", mode);
    data.put("properties", props.stream().map(PropertySummary::toMap).toList());
    data.put("groups", groups.stream().map(PropertySummary::toMap).toList());
    lastPropertiesData = data;

    List<String> sections = new ArrayList<>();
    if (!props.isEmpty() || mode.equals("property") || mode.equals("properties")
        || mode.equals("merkmal") || mode.equals("merkmale")) {
      sections.add(PortalProperties.formatSearchResults(props, "Merkmale"));
    }
    if (!groups.isEmpty() || mode.equals("group") || mode.equals("groups")
        || mode.equals("gruppe") || mode.equals("merkmalsgruppe")
        || mode.equals("merkmalsgruppen")) {
      sections.add(PortalProperties.formatSearchResults(groups, "Merkmalsgruppen"));
    }
    if (sections.isEmpty()) {
      return "Keine Merkmale oder Merkmalsgruppen im BIM-Portal gefunden"
          + (blank(search) ? "." : " für '" + search.trim() + "'.");
    }
    return String.join("\n\n", sections);
  }

  /** Structured data of the last {@link #findProperties} call. */
  public synchronized Map<String, Object> propertiesData() {
    return lastPropertiesData;
  }

  /** Describe a single Merkmal or Merkmalsgruppe from the German BIM Portal by GUID or link. */
  public synchronized String describeProperty(String guidOrLink, String kind) {
    if (blank(guidOrLink)) {
      throw new IllegalArgumentException(
          "Keine GUID angegeben (Merkmal- oder Merkmalsgruppen-GUID oder BIM-Portal-Link).");
    }
    UUID guid =
        PortalRequirements.idIn(guidOrLink)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Keine gültige GUID in '" + guidOrLink + "' gefunden."));
    String mode = blank(kind) ? "auto" : kind.trim().toLowerCase(Locale.ROOT);
    PortalProperties portal = properties();

    if (mode.equals("group") || mode.equals("gruppe") || mode.equals("merkmalsgruppe")) {
      PropertyGroupDetail group =
          portal
              .getPropertyGroup(guid)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Keine Merkmalsgruppe im BIM-Portal mit GUID " + guid + "."));
      lastPropertyDetailData = group.toMap();
      return PortalProperties.formatPropertyGroupDetail(group);
    }

    if (mode.equals("property") || mode.equals("merkmal")) {
      PropertyDetail prop =
          portal
              .getProperty(guid)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Kein Merkmal im BIM-Portal mit GUID " + guid + "."));
      lastPropertyDetailData = prop.toMap();
      return PortalProperties.formatPropertyDetail(prop);
    }

    if (!mode.equals("auto")) {
      throw new IllegalArgumentException(
          "Unbekannter Typ '" + kind + "' (erlaubt: auto, property, group).");
    }

    Optional<PropertyDetail> prop = portal.getProperty(guid);
    if (prop.isPresent()) {
      lastPropertyDetailData = prop.get().toMap();
      return PortalProperties.formatPropertyDetail(prop.get());
    }
    Optional<PropertyGroupDetail> group = portal.getPropertyGroup(guid);
    if (group.isPresent()) {
      lastPropertyDetailData = group.get().toMap();
      return PortalProperties.formatPropertyGroupDetail(group.get());
    }
    throw new IllegalArgumentException(
        "Weder Merkmal noch Merkmalsgruppe im BIM-Portal mit GUID " + guid + " gefunden.");
  }

  /** Structured data of the last {@link #describeProperty} call. */
  public synchronized Map<String, Object> propertyDetailData() {
    return lastPropertyDetailData;
  }

  private PortalProperties properties() {
    if (propertiesClient == null) {
      if (propertiesSupplier == null) {
        throw new IllegalStateException("BIM-Portal-Merkmalsclient nicht konfiguriert.");
      }
      propertiesClient = propertiesSupplier.get();
    }
    return propertiesClient;
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
  private static String toJson(Object value) {
    try {
      return JSON.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("JSON serialization failed", e);
    }
  }
}
