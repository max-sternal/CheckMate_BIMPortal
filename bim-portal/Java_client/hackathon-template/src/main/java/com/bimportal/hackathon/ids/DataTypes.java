package com.bimportal.hackathon.ids;

import java.util.List;
import java.util.Set;

/**
 * Compares the IFC value type a LOIN requires with the one a model carries.
 *
 * <p>Exact equality would be too strict to be useful here. The BIM Portal requires {@code IFCLABEL}
 * for the classification properties while the BIM.Hamburg models store them as {@code IFCTEXT} -
 * both are IFC string types, and reporting all of them as type errors would bury the findings that
 * matter. Types are therefore grouped into families; a difference inside a family is accepted and
 * reported as the actual type, a difference across families is a finding.
 */
public final class DataTypes {

  private static final List<Set<String>> FAMILIES =
      List.of(
          Set.of("IFCLABEL", "IFCTEXT", "IFCIDENTIFIER", "IFCSTRING"),
          Set.of("IFCINTEGER", "IFCCOUNTMEASURE"),
          Set.of("IFCBOOLEAN", "IFCLOGICAL"),
          Set.of(
              "IFCREAL",
              "IFCNUMERICMEASURE",
              "IFCRATIOMEASURE",
              "IFCPOSITIVERATIOMEASURE",
              "IFCNORMALISEDRATIOMEASURE"),
          Set.of("IFCLENGTHMEASURE", "IFCPOSITIVELENGTHMEASURE", "IFCNONNEGATIVELENGTHMEASURE"),
          Set.of("IFCAREAMEASURE", "IFCPOSITIVEAREAMEASURE"),
          Set.of("IFCVOLUMEMEASURE"),
          Set.of("IFCPLANEANGLEMEASURE", "IFCPOSITIVEPLANEANGLEMEASURE"));

  private DataTypes() {}

  /**
   * Whether a model's value type satisfies a required one.
   *
   * @param required the IDS {@code dataType}, e.g. {@code IFCLABEL}
   * @param actual the type the model carries, e.g. {@code IfcText}
   * @return true when both are the same type or belong to the same family; also true when either is
   *     unknown, because an absent type is not evidence of a wrong one
   */
  public static boolean compatible(String required, String actual) {
    String a = normalize(required);
    String b = normalize(actual);
    if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
      return true;
    }
    return FAMILIES.stream().anyMatch(family -> family.contains(a) && family.contains(b));
  }

  /** True when both types are the same, i.e. compatible without relying on a family. */
  public static boolean identical(String required, String actual) {
    String a = normalize(required);
    String b = normalize(actual);
    return !a.isEmpty() && a.equals(b);
  }

  /** Upper-case and trimmed, the form IDS writes: {@code IfcText} becomes {@code IFCTEXT}. */
  public static String normalize(String dataType) {
    return dataType == null ? "" : dataType.trim().toUpperCase();
  }
}
