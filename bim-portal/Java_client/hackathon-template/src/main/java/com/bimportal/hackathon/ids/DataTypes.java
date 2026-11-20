package com.bimportal.hackathon.ids;

import java.util.List;
import java.util.Set;

/**
 * Compares the IFC value type a LOIN requires with the one a model carries.
 *
 * <p>Strictly, as IDS defines it: a required {@code IFCLABEL} is met by an {@code IfcLabel} value
 * and by nothing else. The BIM Portal requires {@code IFCLABEL} for the classification properties
 * while the BIM.Hamburg models store them as {@code IFCTEXT}, so a strict check turns those
 * elements down - which is what every other IDS tool reports for the same files.
 *
 * <p>The families below never change a verdict. They only recognise a related type, so that a
 * report can say "this would pass if IfcText were accepted for IFCLABEL" instead of leaving the
 * reader to work out why an element with the right value was rejected.
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
   * Whether a model's value type meets a required one: the same type, ignoring case.
   *
   * @param required the IDS {@code dataType}, e.g. {@code IFCLABEL}
   * @param actual the type the model carries, e.g. {@code IfcLabel}
   * @return false when either side is unknown - a value without a type does not meet a type
   */
  public static boolean identical(String required, String actual) {
    String a = normalize(required);
    String b = normalize(actual);
    return !a.isEmpty() && a.equals(b);
  }

  /**
   * Whether two types differ but belong to the same family, e.g. {@code IFCTEXT} for {@code
   * IFCLABEL}. Only for hints: a related type does not meet the requirement.
   */
  public static boolean related(String required, String actual) {
    String a = normalize(required);
    String b = normalize(actual);
    if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
      return false;
    }
    return FAMILIES.stream().anyMatch(family -> family.contains(a) && family.contains(b));
  }

  /** Upper-case and trimmed, the form IDS writes: {@code IfcText} becomes {@code IFCTEXT}. */
  public static String normalize(String dataType) {
    return dataType == null ? "" : dataType.trim().toUpperCase();
  }

  /** How a type difference reads in a report: {@code IFCTEXT statt IFCLABEL}. */
  public static String difference(String required, String actual) {
    String found = normalize(actual);
    return (found.isEmpty() ? "ohne Typ" : found) + " statt " + normalize(required);
  }
}
