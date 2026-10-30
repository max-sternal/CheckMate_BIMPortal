package com.bimportal.hackathon.ids;

import java.util.List;
import java.util.regex.Pattern;

/**
 * One {@code <specification>} block of an IDS file: which elements it applies to, and what those
 * elements have to carry.
 *
 * <p>The BIM Portal exports one block per IFC schema version (IFC4, IFC4X3_ADD2, IFC2X3) with the
 * same content but different entity names, so a check has to pick the block matching the model -
 * see {@link IdsParser#forSchema}.
 *
 * <p>Note how the portal lays its exports out: {@code applicability} selects elements purely by
 * their classification properties, and the entity type is an {@code requirements} entry rather than
 * a selector. Both lists are honoured here regardless, so a hand-written IDS also works.
 */
public record IdsSpecification(
    String name,
    String ifcVersion,
    List<PropertyRequirement> applicability,
    EntityRequirement applicableEntity,
    List<PropertyRequirement> requirements,
    EntityRequirement requiredEntity) {

  /**
   * A {@code <property>} facet.
   *
   * @param key which property is addressed
   * @param dataType the IFC type the value must have, e.g. {@code IFCLABEL}; null when unspecified
   * @param expectedValue the value the property must carry; null when only presence is required
   * @param required false for {@code cardinality="optional"}
   */
  public record PropertyRequirement(
      PropertyKey key, String dataType, String expectedValue, boolean required) {

    public boolean hasExpectedValue() {
      return expectedValue != null && !expectedValue.isBlank();
    }

    public boolean hasDataType() {
      return dataType != null && !dataType.isBlank();
    }

    /** How this requirement reads in a report, e.g. {@code Klassifizierung/Objekttyp = 'Kappe'}. */
    public String describe() {
      return hasExpectedValue() ? key + " = '" + expectedValue + "'" : key.toString();
    }
  }

  /**
   * An {@code <entity>} facet.
   *
   * <p>The portal does not write a plain name here but an {@code xs:restriction} holding an
   * alternation of every permitted entity, so this always matches as a regular expression.
   *
   * @param namePattern the permitted entity names, already compiled and case-insensitive
   * @param source the pattern as written, for reports
   */
  public record EntityRequirement(Pattern namePattern, String source) {

    public boolean matches(String entityName) {
      return entityName != null && namePattern.matcher(entityName).matches();
    }
  }
}
