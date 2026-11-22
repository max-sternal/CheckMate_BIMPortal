package com.bimportal.hackathon.check;

import com.bimportal.hackathon.catalogue.CatalogueEntry;
import com.bimportal.hackathon.check.Source.AiaProject;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.PortalRequirements;
import com.bimportal.hackathon.ids.RequirementsException;
import com.bimportal.hackathon.report.CheckReport;
import com.bimportal.hackathon.report.CheckReport.ModelInfo;
import com.bimportal.hackathon.report.CheckReport.Run;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Checking a model, the same way for every front end: GUI, CLI and the MCP server only collect
 * their input and present the {@link CheckReport} this returns.
 */
public final class CheckService {

  /** How the service reaches the BIM Portal: a client per call, closed afterwards. */
  @FunctionalInterface
  public interface Portal {

    EnhancedBimPortalClient open();

    default void close(EnhancedBimPortalClient client) {
      client.logout();
    }

    /** One client for every call; whoever made it logs it out. */
    static Portal shared(EnhancedBimPortalClient client) {
      return new Portal() {
        @Override
        public EnhancedBimPortalClient open() {
          return client;
        }

        @Override
        public void close(EnhancedBimPortalClient unused) {}
      };
    }

    /** No portal: for checks against local files only. */
    static Portal none() {
      return () -> {
        throw new RequirementsException(
            "Für diese Prüfung ist kein Zugang zum BIM Portal vorgesehen.");
      };
    }
  }

  /** What a search finds: AIA projects and LOINs, projects first. */
  public record Found(List<AiaProject> projects, List<CatalogueEntry> loins) {}

  private final Portal portal;

  /** Requirements downloaded by this service, so each is downloaded once. */
  private final Map<String, IdsDocument> downloaded = new ConcurrentHashMap<>();

  public CheckService(Portal portal) {
    this.portal = portal;
  }

  /**
   * The requirements of a source, downloaded once per service. Two calls for the same source at
   * once share one download.
   *
   * @throws RequirementsException when they cannot be had
   */
  public IdsDocument requirements(Source source) {
    return switch (source) {
      case Source.Read read -> read.document();
      case Source.IdsFile file -> IdsParser.read(file.file());
      case Source.Loin loin ->
          downloaded.computeIfAbsent(
              key(source),
              k -> withPortal(c -> PortalRequirements.loin(c, loin.guid(), loin.name())));
      case Source.AiaProject project ->
          downloaded.computeIfAbsent(
              key(source),
              k -> withPortal(c -> PortalRequirements.project(c, project.guid(), project.name())));
      case Source.PortalId id ->
          downloaded.computeIfAbsent(
              key(source), k -> withPortal(c -> PortalRequirements.requirements(c, guid(id))));
    };
  }

  /** The requirements of a source if they are at hand without a download. */
  public Optional<IdsDocument> known(Source source) {
    try {
      return switch (source) {
        case Source.Read read -> Optional.of(read.document());
        case Source.IdsFile file -> Optional.empty();
        default -> Optional.ofNullable(downloaded.get(key(source)));
      };
    } catch (RequirementsException noId) {
      return Optional.empty();
    }
  }

  /**
   * Check a model against one source. Requirements that cannot be read or checked become a failed
   * run with the reason, not an exception: the other sources of a check still count.
   */
  public Run run(Source source, ModelInfo model, Collection<ModelElement> elements) {
    try {
      IdsDocument requirements = requirements(source);
      return Run.checked(requirements, requirements.check(model.schema(), elements));
    } catch (RequirementsException e) {
      return Run.failed(source.name(), e.getMessage());
    } catch (RuntimeException e) {
      return Run.failed(source.name(), source.name() + ": " + e);
    }
  }

  /** Check a model against every source, in order. */
  public CheckReport check(
      List<Source> sources,
      ModelInfo model,
      Collection<ModelElement> elements,
      OffsetDateTime checkedAt) {
    return check(sources, model, elements, checkedAt, run -> {});
  }

  /**
   * Check a model against every source, in order.
   *
   * @param onRun called with each run as soon as it is done, e.g. to print it
   */
  public CheckReport check(
      List<Source> sources,
      ModelInfo model,
      Collection<ModelElement> elements,
      OffsetDateTime checkedAt,
      Consumer<Run> onRun) {
    List<Run> runs = new ArrayList<>();
    for (Source source : sources) {
      Run run = run(source, model, elements);
      runs.add(run);
      onRun.accept(run);
    }
    return new CheckReport(checkedAt, model, runs);
  }

  /** AIA projects and LOINs the portal finds for a term; an empty term finds all. */
  public Found search(String term) {
    return withPortal(
        client ->
            new Found(
                PortalRequirements.searchProjects(client, term).stream()
                    .map(project -> new AiaProject(project.getGuid(), project.getName()))
                    .toList(),
                PortalRequirements.search(client, term).stream().map(CatalogueEntry::of).toList()));
  }

  private <T> T withPortal(Function<EnhancedBimPortalClient, T> work) {
    EnhancedBimPortalClient client = portal.open();
    try {
      return work.apply(client);
    } finally {
      portal.close(client);
    }
  }

  private static UUID guid(Source.PortalId id) {
    return PortalRequirements.idIn(id.text())
        .orElseThrow(() -> new RequirementsException("Keine ID in '" + id.text().trim() + "'."));
  }

  /** The cache key: one per LOIN, project or ID. */
  private static String key(Source source) {
    return switch (source) {
      case Source.Loin loin -> "loin " + loin.guid();
      case Source.AiaProject project -> "aia " + project.guid();
      case Source.PortalId id -> "id " + guid(id);
      default -> throw new IllegalArgumentException("not downloaded: " + source);
    };
  }
}
