package com.bimportal.hackathon.ids;

import com.bimportal.client.model.AiaProjectForPublicRequest;
import com.bimportal.client.model.LoinForPublicRequest;
import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Requirements from the BIM Portal: LOINs and AIA projects, found by ID or by name.
 *
 * <p>The portal is the source of the "Soll": rather than reassembling requirements from the
 * property and property-group endpoints, this asks for them as IDS - the buildingSMART format meant
 * for exactly this. That keeps the semantics the portal's rather than ours, and leaves the door
 * open to running the same file through an external IDS validator. A LOIN comes as one IDS file, an
 * AIA project as a ZIP of them (see {@link IdsArchive}). Nothing is written to the portal: the
 * client calls these downloads "exports" ({@code exportLoinIds}, {@code exportProjectIds}) because
 * that is the portal's name for them.
 *
 * <p>The portal keeps two IDs for each LOIN and each project: the {@code guid}, which stays the
 * same across versions and is what the downloads ask for, and the {@code id} of each version's
 * record. Only the first works for a download, so an ID no download knows is looked up among the
 * catalogues' {@code id}s before giving up.
 */
public final class PortalRequirements {

  /** Entries the portal puts on one page of search results; a shorter page is the last. */
  static final int PAGE_SIZE = 1000;

  /** Bound on paging, so a portal always answering a full page cannot spin forever. */
  private static final int MAX_PAGES = 20;

  private static final Pattern UUID_TEXT =
      Pattern.compile(
          "(?<![0-9a-fA-F])[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"
              + "-[0-9a-fA-F]{12}(?![0-9a-fA-F])");

  private static final Logger logger = LoggerFactory.getLogger(PortalRequirements.class);

  private PortalRequirements() {}

  /**
   * The ID in a text: the ID itself, or a link or note that contains one.
   *
   * @return empty when the text holds no ID
   * @throws RequirementsException when it holds several different ones, since guessing would check
   *     against the wrong requirements
   */
  public static Optional<UUID> idIn(String text) {
    if (text == null) {
      return Optional.empty();
    }
    List<String> ids = new ArrayList<>();
    Matcher matcher = UUID_TEXT.matcher(text);
    while (matcher.find()) {
      String id = matcher.group().toLowerCase(Locale.ROOT);
      if (!ids.contains(id)) {
        ids.add(id);
      }
    }
    if (ids.size() > 1) {
      throw new RequirementsException(
          "Der Text enthält mehrere IDs (" + String.join(", ", ids) + "). Bitte nur eine.");
    }
    return ids.stream().findFirst().map(UUID::fromString);
  }

  /**
   * Requirements from the ID in a text such as a link copied from the portal.
   *
   * @throws RequirementsException when the text holds no ID, or see {@link
   *     #requirements(EnhancedBimPortalClient, UUID)}
   */
  public static IdsDocument requirements(EnhancedBimPortalClient client, String idOrLink) {
    UUID id =
        idIn(idOrLink)
            .orElseThrow(() -> new RequirementsException("Keine ID in '" + idOrLink.trim() + "'."));
    return requirements(client, id);
  }

  /**
   * Requirements by any ID the portal hands out: a LOIN's or an AIA project's, its GUID or the ID
   * of one of its versions.
   *
   * <p>Tried in order of cost: the two downloads answer within seconds, while looking an ID up in
   * the LOIN catalogue takes about 7 s, and only happens when nothing else knew the ID.
   *
   * @throws RequirementsException when the portal knows nothing by that ID, or holds no IDS for it
   */
  public static IdsDocument requirements(EnhancedBimPortalClient client, UUID id) {
    Optional<byte[]> loin = client.exportLoinIds(id);
    if (loin.isPresent()) {
      return IdsParser.read(loin.get(), loinSource(id));
    }
    Optional<byte[]> project = client.exportProjectIds(id);
    if (project.isPresent()) {
      return IdsParser.read(project.get(), projectSource(id));
    }

    Optional<SimpleAiaProjectPublicDto> projectRecord =
        searchProjects(client, "").stream().filter(p -> id.equals(p.getId())).findFirst();
    if (projectRecord.isPresent()) {
      logger.info("{} is the record id of AIA project {}", id, projectRecord.get().getGuid());
      return project(client, projectRecord.get().getGuid(), projectRecord.get().getName());
    }
    Optional<SimpleLoinPublicDto> loinRecord =
        search(client, "").stream().filter(l -> id.equals(l.getId())).findFirst();
    if (loinRecord.isPresent()) {
      logger.info("{} is the record id of LOIN {}", id, loinRecord.get().getGuid());
      return loin(client, loinRecord.get().getGuid(), loinRecord.get().getName());
    }
    throw new RequirementsException(
        "Das BIM Portal kennt keine LOIN und kein AIA-Projekt mit der ID "
            + id
            + " (oder ist nicht erreichbar).");
  }

  /**
   * A LOIN's requirements by its GUID: its IDS file, downloaded.
   *
   * @param name what the LOIN is called, for the error message
   * @throws RequirementsException when the portal has no IDS file for it
   */
  public static IdsDocument loin(EnhancedBimPortalClient client, UUID guid, String name) {
    byte[] ids =
        client.exportLoinIds(guid).orElseThrow(() -> noIdsFile("die LOIN '" + name + "'", guid));
    return IdsParser.read(ids, loinSource(guid));
  }

  /**
   * An AIA project's requirements by its GUID: every LOIN of the project, from its IDS files,
   * downloaded as one ZIP.
   *
   * @param name what the project is called, for the error message
   * @throws RequirementsException when the portal has no IDS files for it
   */
  public static IdsDocument project(EnhancedBimPortalClient client, UUID guid, String name) {
    byte[] archive =
        client
            .exportProjectIds(guid)
            .orElseThrow(() -> noIdsFile("das AIA-Projekt '" + name + "'", guid));
    return IdsParser.read(archive, projectSource(guid));
  }

  /**
   * Every LOIN the portal finds for a term, paged; an empty term lists the whole catalogue.
   *
   * <p>A single request stops at {@value #PAGE_SIZE} results, filtered or not - a term like "LPH"
   * hits that limit just as an empty one does, and the list would silently end mid-catalogue. So
   * pages are read until one comes back short. The term goes to the server rather than being
   * filtered here, because the matches for something like "Schaft" sit outside the first page.
   */
  public static List<SimpleLoinPublicDto> search(EnhancedBimPortalClient client, String term) {
    return paged(
        page -> {
          LoinForPublicRequest request = new LoinForPublicRequest().pageNumber(page);
          if (term != null && !term.isBlank()) {
            request.searchString(term.trim());
          }
          return client.searchLoins(request);
        });
  }

  /** Every AIA project the portal finds for a term, paged; an empty term lists all of them. */
  public static List<SimpleAiaProjectPublicDto> searchProjects(
      EnhancedBimPortalClient client, String term) {
    return paged(
        page -> {
          AiaProjectForPublicRequest request = new AiaProjectForPublicRequest().pageNumber(page);
          if (term != null && !term.isBlank()) {
            request.searchString(term.trim());
          }
          return client.searchProjects(request);
        });
  }

  private static <T> List<T> paged(IntFunction<List<T>> page) {
    List<T> all = new ArrayList<>();
    for (int number = 0; number < MAX_PAGES; number++) {
      List<T> found = page.apply(number);
      all.addAll(found);
      if (found.size() < PAGE_SIZE) {
        break;
      }
    }
    return all;
  }

  private static String loinSource(UUID guid) {
    return "BIM Portal, LOIN " + guid;
  }

  private static String projectSource(UUID guid) {
    return "BIM Portal, AIA-Projekt " + guid;
  }

  private static RequirementsException noIdsFile(String what, UUID guid) {
    return new RequirementsException(
        "Das BIM Portal bietet für " + what + " keine IDS-Datei zum Herunterladen (" + guid + ").");
  }
}
