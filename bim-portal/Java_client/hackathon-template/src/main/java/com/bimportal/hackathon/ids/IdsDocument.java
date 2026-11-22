package com.bimportal.hackathon.ids;

import java.util.Collection;
import java.util.List;

/**
 * An IDS file as read: what it calls itself, where it came from, and its specifications.
 *
 * <p>Whatever the requirements come from - a LOIN in the portal, a local IDS file - they end up as
 * one of these, so the check does not care about the source.
 *
 * @param title the IDS {@code <info><title>}; in a LOIN's IDS file from the portal the LOIN's name.
 *     Null when the file has none.
 * @param version the IDS {@code <info><version>}; in a LOIN's IDS file from the portal the LOIN's
 *     version. Null when the file has none.
 * @param source where it came from, for reports: e.g. {@code BIM Portal, LOIN be959b9d-...} or the
 *     file name
 */
public record IdsDocument(
    String title, String version, String source, List<IdsSpecification> specifications) {

  public IdsDocument {
    specifications = List.copyOf(specifications);
  }

  /** How the document reads in a list or a report header: title and version, else the source. */
  public String label() {
    if (title == null || title.isBlank()) {
      return source;
    }
    return version == null || version.isBlank() ? title : title + ", Version " + version;
  }

  /**
   * Label and source, for a report header: {@code Ueberbau > Kappe (Ingenieurbau): LPH 8-9, Version
   * 4.1 (BIM Portal, LOIN be959b9d-...)}.
   */
  public String describe() {
    return label().equals(source) ? source : label() + " (" + source + ")";
  }

  /**
   * The specifications written for a model's schema, in file order.
   *
   * <p>A LOIN's IDS file from the portal holds one per schema. Other IDS files may hold several for
   * the same schema, and IDS checks each of them, so this is a list and not a pick.
   */
  public List<IdsSpecification> forSchema(String schema) {
    return specifications.stream().filter(specification -> specification.covers(schema)).toList();
  }

  /**
   * Check a model against every specification written for its schema.
   *
   * @return one result per specification, in file order
   * @throws RequirementsException when the document holds no specification, or none for the model's
   *     schema; both would otherwise surface as an empty report that reads like a pass
   */
  public List<CheckResult> check(String schema, Collection<ModelElement> model) {
    if (specifications.isEmpty()) {
      throw new RequirementsException(label() + ": die IDS enthält keine Spezifikation.");
    }
    List<IdsSpecification> covering = forSchema(schema);
    if (covering.isEmpty()) {
      throw new RequirementsException(
          label()
              + ": die IDS deckt das Schema "
              + schema
              + " nicht ab - vorhanden: "
              + specifications.stream().map(IdsSpecification::ifcVersion).distinct().toList());
    }
    return covering.stream()
        .map(specification -> IdsValidator.check(specification, model, schema))
        .toList();
  }
}
