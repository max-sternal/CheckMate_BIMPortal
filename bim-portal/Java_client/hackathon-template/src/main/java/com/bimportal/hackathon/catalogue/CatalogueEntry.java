package com.bimportal.hackathon.catalogue;

import com.bimportal.client.model.SimpleLoinPublicDto;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One LOIN of the BIM Portal's catalogue, with the metadata to choose it by.
 *
 * <p>All of this comes with the LOIN list itself, so choosing needs no download of requirements.
 * Only the LOINs chosen for a check are downloaded.
 *
 * @param objectTypes what kind of object the LOIN describes, e.g. {@code Kappe}
 * @param disciplines the portal's "Fachmodell", e.g. {@code Ingenieurbau}
 * @param constructionTypes e.g. {@code Ingenieurbauwerk}
 * @param phases e.g. {@code LPH 5: Ausführungsplanung}
 * @param purposes the use cases ("Anwendungsfälle"), e.g. {@code 060 Planungsfortschrittskontrolle
 *     und Qualitätsprüfung}
 */
public record CatalogueEntry(
    UUID guid,
    String name,
    String version,
    String organisation,
    List<String> objectTypes,
    List<String> disciplines,
    List<String> constructionTypes,
    List<String> phases,
    List<String> purposes) {

  public CatalogueEntry {
    objectTypes = clean(objectTypes);
    disciplines = clean(disciplines);
    constructionTypes = clean(constructionTypes);
    phases = clean(phases);
    purposes = clean(purposes);
  }

  /** The entry for a LOIN as the portal's list delivers it. */
  public static CatalogueEntry of(SimpleLoinPublicDto loin) {
    return new CatalogueEntry(
        loin.getGuid(),
        loin.getName(),
        loin.getVersion(),
        loin.getOrganisation(),
        loin.getObjectTypes(),
        loin.getDomainSpecificModelTypes(),
        loin.getConstructionTypes(),
        loin.getProjectPhases(),
        loin.getPurposes());
  }

  private static List<String> clean(List<String> values) {
    return values == null
        ? List.of()
        : values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty())
            .toList();
  }
}
