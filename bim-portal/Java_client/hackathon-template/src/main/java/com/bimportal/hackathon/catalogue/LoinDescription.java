package com.bimportal.hackathon.catalogue;

import com.bimportal.hackathon.catalogue.ModelFit.Fit;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsSpecification;
import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import com.bimportal.hackathon.ids.IdsValidator;
import com.bimportal.hackathon.ids.ModelElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A short description of a LOIN, to decide whether it is worth checking: what the catalogue says
 * about it, and what its requirements ask of this model.
 *
 * <p>The requirements part comes from the LOIN's IDS file, not from the portal's LOIN detail: the
 * generated client loses the detail's IFC classes and property names (they arrive as {@code null}),
 * while the IDS file holds exactly what the check will use. It is one small download per LOIN,
 * kept for the check.
 */
public final class LoinDescription {

  /** Enough use cases and properties to get the idea, few enough to read. */
  private static final int SHOWN = 5;

  private LoinDescription() {}

  /**
   * Describe a catalogue LOIN.
   *
   * @param requirements the LOIN's IDS file; null while it is not downloaded yet
   * @param schema the model's schema
   */
  public static List<String> describe(
      CatalogueEntry entry,
      Fit fit,
      IdsDocument requirements,
      String schema,
      Collection<ModelElement> model) {

    List<String> lines = new ArrayList<>();
    lines.add(entry.name() + " - Version " + entry.version() + " - " + entry.organisation());
    lines.add("ID: " + entry.guid());
    lines.add(
        "Objekttyp: "
            + String.join(", ", entry.objectTypes())
            + " | Fachmodell: "
            + String.join(", ", entry.disciplines())
            + " | Bauart: "
            + String.join(", ", entry.constructionTypes()));
    List<String> phases = entry.phases().stream().map(LoinDescription::beforeColon).toList();
    lines.add("Leistungsphasen: " + String.join(", ", phases));
    lines.add("Anwendungsfälle (" + entry.purposes().size() + "): " + firstFew(entry.purposes()));
    lines.add(
        fit.fits()
            ? "Passt zum Modell: " + fit.describe()
            : "Kein Objekttyp dieser LOIN im Modell gefunden.");

    if (requirements == null) {
      lines.add("Anforderungen werden geladen ...");
    } else {
      lines.addAll(requirementLines(requirements, schema, model));
    }
    return lines;
  }

  /** Describe requirements that are read already: an IDS file, a project, a LOIN by ID. */
  public static List<String> describe(
      IdsDocument requirements, String schema, Collection<ModelElement> model) {
    List<String> lines = new ArrayList<>();
    lines.add(requirements.describe());
    lines.addAll(requirementLines(requirements, schema, model));
    return lines;
  }

  /** Per specification for the model's schema: what it selects, what it asks, how many apply. */
  private static List<String> requirementLines(
      IdsDocument requirements, String schema, Collection<ModelElement> model) {
    List<String> lines = new ArrayList<>();
    List<IdsSpecification> specifications = requirements.forSchema(schema);
    if (specifications.isEmpty()) {
      lines.add(
          "Keine Anforderungen für "
              + schema
              + " - vorhanden: "
              + requirements.specifications().stream()
                  .map(IdsSpecification::ifcVersion)
                  .distinct()
                  .toList());
      return lines;
    }
    if (specifications.size() > 1) {
      lines.add(specifications.size() + " Spezifikationen für " + schema + ":");
      for (IdsSpecification specification : specifications) {
        CheckResult result = IdsValidator.check(specification, model, schema);
        lines.add("   " + specification.name() + " - " + applicable(result));
      }
      return lines;
    }

    IdsSpecification specification = specifications.get(0);
    lines.add("Wählt aus: " + selects(specification));
    lines.add("Verlangt: " + asks(specification));
    CheckResult result = IdsValidator.check(specification, model, schema);
    lines.add("Im Modell: " + applicable(result));
    if (!result.relatedTypes().isEmpty()) {
      lines.add(result.relatedTypes().line());
    }
    if (!specification.unsupported().isEmpty()) {
      lines.add("Enthält Teile, die CheckMate nicht auswertet; die Prüfung nennt sie.");
    }
    return lines;
  }

  private static String selects(IdsSpecification specification) {
    List<String> parts = new ArrayList<>();
    if (specification.applicableEntity() != null) {
      parts.add(specification.applicableEntity().describe());
    }
    specification.applicability().forEach(facet -> parts.add(facet.describe()));
    return parts.isEmpty() ? "alle Elemente" : String.join(", ", parts);
  }

  private static String asks(IdsSpecification specification) {
    List<String> properties =
        specification.requirements().stream().map(PropertyRequirement::describe).toList();
    EntityRequirement required = specification.requiredEntity();
    String entity =
        required == null
            ? ""
            : "IFC-Entity "
                + shortEntities(required.source())
                + (required.hasPredefinedType()
                    ? " mit PredefinedType " + required.predefinedTypeSource()
                    : "")
                + "; ";
    return entity + properties.size() + " Merkmal(e): " + firstFew(properties);
  }

  private static String applicable(CheckResult result) {
    return result.applicableElements()
        + " von "
        + result.examinedElements()
        + " Elementen anwendbar";
  }

  private static String firstFew(List<String> values) {
    if (values.size() <= SHOWN) {
      return String.join(", ", values);
    }
    return String.join(", ", values.subList(0, SHOWN)) + ", ... (" + values.size() + ")";
  }

  /**
   * The portal often permits two dozen entities as one pattern ({@code IFCBEAM|IFCCOLUMN|...}); the
   * first few tell the kind.
   */
  static String shortEntities(String entities) {
    String[] alternatives = entities.split("\\|");
    if (alternatives.length <= 3) {
      return entities;
    }
    return String.join(" | ", List.of(alternatives).subList(0, 3))
        + " ... ("
        + alternatives.length
        + " Entities)";
  }

  /** {@code LPH 5: Ausführungsplanung} reads as {@code LPH 5}. */
  private static String beforeColon(String phase) {
    int colon = phase.indexOf(':');
    return colon < 0 ? phase : phase.substring(0, colon);
  }
}
