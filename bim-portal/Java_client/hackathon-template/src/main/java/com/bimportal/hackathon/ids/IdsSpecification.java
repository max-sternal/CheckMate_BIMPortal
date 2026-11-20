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

  /** How a {@code <property>} facet in the requirements is to be read. */
  public enum Cardinality {
    /** The element has to carry the property. The IDS default. */
    REQUIRED,
    /** Only elements that carry the property are checked against it. */
    OPTIONAL,
    /** The element must not carry the property - with this value and type, where given. */
    PROHIBITED;

    /** The IDS {@code cardinality} attribute; absent or unknown means required, as in IDS. */
    public static Cardinality of(String attribute) {
      if ("optional".equalsIgnoreCase(attribute)) {
        return OPTIONAL;
      }
      if ("prohibited".equalsIgnoreCase(attribute)) {
        return PROHIBITED;
      }
      return REQUIRED;
    }
  }

  /**
   * A {@code <property>} facet.
   *
   * @param key which property is addressed
   * @param dataType the IFC type the value must have, e.g. {@code IFCLABEL}; null when unspecified
   * @param expectedValue the value the property must carry; null when only presence is required
   * @param cardinality required, optional or prohibited
   */
  public record PropertyRequirement(
      PropertyKey key, String dataType, String expectedValue, Cardinality cardinality) {

    public boolean hasExpectedValue() {
      return expectedValue != null && !expectedValue.isBlank();
    }

    public boolean hasDataType() {
      return dataType != null && !dataType.isBlank();
    }

    /**
     * How this requirement reads in a report, e.g. {@code Klassifizierung/Objekttyp = 'Kappe'}.
     * Optional and prohibited ones say so, because the reader cannot tell from the finding alone.
     */
    public String describe() {
      String address = hasExpectedValue() ? key + " = '" + expectedValue + "'" : key.toString();
      return switch (cardinality) {
        case REQUIRED -> address;
        case OPTIONAL -> address + " (optional)";
        case PROHIBITED -> address + " (unzulässig)";
      };
    }
  }

  /**
   * An {@code <entity>} facet.
   *
   * <p>The portal mostly writes an {@code xs:restriction} holding an alternation of every permitted
   * entity rather than a plain name, so the name always matches as a regular expression.
   *
   * @param namePattern the permitted entity names, already compiled and case-insensitive
   * @param source the name as written, for reports
   * @param predefinedType the permitted PredefinedType, compiled as written; null when the facet
   *     names none
   * @param predefinedTypeSource the PredefinedType as written, for reports; null when none
   */
  public record EntityRequirement(
      Pattern namePattern, String source, Pattern predefinedType, String predefinedTypeSource) {

    /**
     * In a facet, USERDEFINED does not name a value: it asks for an element whose type is
     * user-defined, whatever that type is called. That is how IDS and ifctester read it.
     */
    private static final String USERDEFINED = "USERDEFINED";

    /** An entity facet that names no PredefinedType. */
    public EntityRequirement(Pattern namePattern, String source) {
      this(namePattern, source, null, null);
    }

    public boolean hasPredefinedType() {
      return predefinedType != null;
    }

    /** Whether the entity name is permitted, ignoring the PredefinedType. */
    public boolean matchesName(String entityName) {
      return entityName != null && namePattern.matcher(entityName).matches();
    }

    /** Whether the element's PredefinedType is permitted; always true when the facet names none. */
    public boolean matchesPredefinedType(ModelElement element) {
      if (predefinedType == null) {
        return true;
      }
      if (USERDEFINED.equals(predefinedTypeSource)) {
        return element.userDefinedType();
      }
      return element.predefinedType() != null
          && predefinedType.matcher(element.predefinedType()).matches();
    }

    /** Name and PredefinedType both permitted. */
    public boolean matches(ModelElement element) {
      return matchesName(element.entityName()) && matchesPredefinedType(element);
    }

    /** How this facet reads in a report, e.g. {@code IFCBEAM mit PredefinedType CORNICE}. */
    public String describe() {
      return hasPredefinedType() ? source + " mit PredefinedType " + predefinedTypeSource : source;
    }
  }
}
