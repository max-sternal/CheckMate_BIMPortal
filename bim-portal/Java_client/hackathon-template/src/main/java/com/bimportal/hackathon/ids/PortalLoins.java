package com.bimportal.hackathon.ids;

import com.bimportal.client.model.LoinForPublicRequest;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LOINs in the BIM Portal: finding them by ID or by name, and getting their requirements.
 *
 * <p>The portal is the source of the "Soll": rather than reassembling a LOIN's requirements from
 * the property and property-group endpoints, this asks for the LOIN as IDS - the buildingSMART
 * format meant for exactly this. That keeps the semantics the portal's rather than ours, and leaves
 * the door open to running the same file through an external IDS validator.
 */
public final class PortalLoins {

  /** Entries the portal puts on one page of search results; a shorter page is the last. */
  static final int PAGE_SIZE = 1000;

  /** Bound on paging, so a portal always answering a full page cannot spin forever. */
  private static final int MAX_PAGES = 20;

  private static final Pattern UUID_TEXT =
      Pattern.compile(
          "(?<![0-9a-fA-F])[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"
              + "-[0-9a-fA-F]{12}(?![0-9a-fA-F])");

  private static final Logger logger = LoggerFactory.getLogger(PortalLoins.class);

  private PortalLoins() {}

  /**
   * The LOIN ID in a text: the ID itself, or a link or note that contains one.
   *
   * @return empty when the text holds no ID
   * @throws RequirementsException when it holds several different ones, since guessing would check
   *     the wrong LOIN
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
          "Der Text enthält mehrere IDs (" + String.join(", ", ids) + "). Bitte nur die LOIN-ID.");
    }
    return ids.stream().findFirst().map(UUID::fromString);
  }

  /**
   * A LOIN's requirements, from the ID in a text such as a link copied from the portal.
   *
   * @throws RequirementsException when the text holds no ID, or see {@link #requirements(
   *     EnhancedBimPortalClient, UUID)}
   */
  public static IdsDocument requirements(EnhancedBimPortalClient client, String idOrLink) {
    UUID id =
        idIn(idOrLink)
            .orElseThrow(
                () -> new RequirementsException("Keine LOIN-ID in '" + idOrLink.trim() + "'."));
    return requirements(client, id);
  }

  /**
   * A LOIN's requirements: its IDS export, read.
   *
   * <p>The portal keeps two IDs per LOIN: the {@code guid}, which stays the same across versions
   * and is what the export asks for, and the {@code id} of each version's record. Only the first
   * works for the export, so an ID the export does not know is looked up among the catalogue's
   * {@code id}s before giving up. That takes a few seconds, but only on a miss.
   *
   * @throws RequirementsException when the portal knows no such LOIN, or holds no IDS for it
   */
  public static IdsDocument requirements(EnhancedBimPortalClient client, UUID id) {
    Optional<byte[]> ids = client.exportLoinIds(id);
    UUID guid = id;
    if (ids.isEmpty()) {
      Optional<SimpleLoinPublicDto> record =
          search(client, "").stream().filter(loin -> id.equals(loin.getId())).findFirst();
      if (record.isEmpty()) {
        throw new RequirementsException(
            "Das BIM Portal kennt keine LOIN mit der ID " + id + " (oder ist nicht erreichbar).");
      }
      guid = record.get().getGuid();
      logger.info("{} is the record id of LOIN {} ({})", id, guid, record.get().getName());
      ids = client.exportLoinIds(guid);
      if (ids.isEmpty()) {
        throw new RequirementsException(
            "Das BIM Portal liefert für die LOIN '"
                + record.get().getName()
                + "' keinen IDS-Export ("
                + guid
                + ").");
      }
    }
    return IdsParser.read(ids.get(), "BIM Portal, LOIN " + guid);
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
    List<SimpleLoinPublicDto> all = new ArrayList<>();
    for (int page = 0; page < MAX_PAGES; page++) {
      LoinForPublicRequest request = new LoinForPublicRequest().pageNumber(page);
      if (term != null && !term.isBlank()) {
        request.searchString(term.trim());
      }

      List<SimpleLoinPublicDto> found = client.searchLoins(request);
      all.addAll(found);
      if (found.size() < PAGE_SIZE) {
        break;
      }
    }
    return all;
  }
}
