package com.bimportal.hackathon.ids;

import java.util.Locale;
import java.util.Set;

/**
 * Whether a model value is the value an IDS facet asks for - compared the way the property's data
 * type demands, as ifctester (IfcOpenShell 0.9) does it.
 *
 * <ul>
 *   <li>Text ({@code IfcLabel}, {@code IfcText}, {@code IfcIdentifier}, ...) exactly, including
 *       case. {@code 42} in an {@code IfcLabel} is text, so {@code 42.0} does not match it.
 *   <li>Integers ({@code IfcInteger}, ...) as numbers, exactly: {@code 42} = {@code 42.0}.
 *   <li>Real numbers ({@code IfcReal}, the measures) as numbers, within a relative tolerance of
 *       1e-6, so a value that went through a floating point calculation still matches.
 *   <li>Booleans ({@code IfcBoolean}, {@code IfcLogical}): the facet's {@code true} or {@code 1},
 *       {@code false} or {@code 0}, as XML Schema writes them.
 * </ul>
 *
 * <p>Values with a unit are compared as stored; ifctester converts them to SI units first. That
 * difference only shows where a model stores e.g. millimetres and the IDS asks for metres.
 */
final class Values {

  /** IDS's relative tolerance for real numbers. */
  static final double TOLERANCE = 1e-6;

  /** Types whose values are text, whatever they look like. */
  private static final Set<String> TEXT =
      Set.of(
          "IFCLABEL",
          "IFCTEXT",
          "IFCIDENTIFIER",
          "IFCDESCRIPTIVEMEASURE",
          "IFCURIREFERENCE",
          "IFCDATE",
          "IFCDATETIME",
          "IFCTIME",
          "IFCDURATION",
          "IFCBINARY");

  /** Types built on INTEGER: compared as numbers, but without tolerance. */
  private static final Set<String> INTEGER =
      Set.of(
          "IFCINTEGER",
          "IFCPOSITIVEINTEGER",
          "IFCTIMESTAMP",
          "IFCDAYINMONTHNUMBER",
          "IFCMONTHINYEARNUMBER",
          "IFCYEARNUMBER",
          "IFCDIMENSIONCOUNT",
          "IFCCARDINALPOINTREFERENCE");

  private static final Set<String> BOOLEAN = Set.of("IFCBOOLEAN", "IFCLOGICAL");

  private Values() {}

  /**
   * Whether a model value is the required one.
   *
   * @param required the value the facet asks for, as written in the IDS
   * @param actual the model's value, as read
   * @param dataType the model value's IFC type; null when unknown, then numbers are compared as
   *     real numbers and anything else as text
   */
  static boolean same(String required, String actual, String dataType) {
    if (required == null || actual == null) {
      return required == null && actual == null;
    }
    String type = DataTypes.normalize(dataType);
    if (BOOLEAN.contains(type)) {
      Boolean wanted = xmlBoolean(required);
      return wanted != null && wanted.equals(modelBoolean(actual));
    }
    if (!TEXT.contains(type)) {
      Double wanted = number(required);
      Double found = number(actual);
      if (wanted != null && found != null) {
        return INTEGER.contains(type)
            ? wanted.doubleValue() == found.doubleValue()
            : withinTolerance(found, wanted);
      }
    }
    return required.equals(actual);
  }

  /** Whether two texts differ only in case - worth saying, since the check counts it. */
  static boolean caseOnly(String required, String actual) {
    return required != null
        && actual != null
        && !required.equals(actual)
        && required.equalsIgnoreCase(actual);
  }

  /**
   * ifctester's comparison: the value lies within the required one times {@code 1 ± 1e-6}. Zero
   * therefore only matches zero.
   */
  static boolean withinTolerance(double value, double required) {
    double low = required * (1.0 - TOLERANCE);
    double high = required * (1.0 + TOLERANCE);
    return required >= 0 ? value >= low && value <= high : value <= low && value >= high;
  }

  /** XML Schema's boolean: {@code true}, {@code 1}, {@code false}, {@code 0} - nothing else. */
  private static Boolean xmlBoolean(String value) {
    return switch (value.trim()) {
      case "true", "1" -> Boolean.TRUE;
      case "false", "0" -> Boolean.FALSE;
      default -> null;
    };
  }

  /** The model's boolean as the reader writes it; {@code UNKNOWN} of a logical is neither. */
  private static Boolean modelBoolean(String value) {
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "true", ".t.", "1" -> Boolean.TRUE;
      case "false", ".f.", "0" -> Boolean.FALSE;
      default -> null;
    };
  }

  private static Double number(String value) {
    try {
      return Double.valueOf(value.trim());
    } catch (NumberFormatException notANumber) {
      return null;
    }
  }
}
