package com.bimportal.hackathon.ids;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One {@code <specification>} block of an IDS file: which elements it applies to, and what those
 * elements have to carry.
 *
 * <p>The BIM Portal exports one block per IFC schema version (IFC4, IFC4X3_ADD2, IFC2X3) with the
 * same content but different entity names, so a check has to pick the blocks matching the model -
 * see {@link #covers} and {@link IdsDocument#forSchema}. Other IDS files may list several schemas
 * in one block.
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
    EntityRequirement requiredEntity,
    Unsupported unsupported,
    String identifier) {

  /** A LOIN's GUID at the start of an identifier, as the portal writes it. */
  private static final Pattern LEADING_UUID =
      Pattern.compile(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=\\s|$)");

  /** The schema family at the start of a name: version, and the minor version after an X. */
  private static final Pattern SCHEMA_BASE = Pattern.compile("IFC\\d+(?:X\\d+)?");

  /** A specification whose every part CheckMate evaluates. */
  public IdsSpecification(
      String name,
      String ifcVersion,
      List<PropertyRequirement> applicability,
      EntityRequirement applicableEntity,
      List<PropertyRequirement> requirements,
      EntityRequirement requiredEntity) {
    this(
        name,
        ifcVersion,
        applicability,
        applicableEntity,
        requirements,
        requiredEntity,
        Unsupported.NONE,
        null);
  }

  /**
   * The GUID of the LOIN this specification was exported from. The portal writes the {@code
   * identifier} as {@code "<LOIN guid> <n>"}, where n only counts the specifications within one
   * file, so the same LOIN and schema have a different n in an AIA project's combined file than in
   * the LOIN's own file.
   *
   * @return empty for identifiers written by other tools, or none
   */
  public Optional<String> loinGuid() {
    if (identifier == null) {
      return Optional.empty();
    }
    Matcher matcher = LEADING_UUID.matcher(identifier.trim());
    return matcher.lookingAt()
        ? Optional.of(matcher.group().toLowerCase(Locale.ROOT))
        : Optional.empty();
  }

  /**
   * The schemas this specification is written for. IDS 1.0 allows a list, e.g. {@code "IFC2X3
   * IFC4"}; the portal writes one per specification.
   */
  public List<String> ifcVersions() {
    if (ifcVersion == null || ifcVersion.isBlank()) {
      return List.of();
    }
    return Arrays.asList(ifcVersion.trim().split("\\s+"));
  }

  /**
   * Whether this specification is written for a model's schema.
   *
   * <p>Compared by base schema, so a model declaring {@code IFC4X3} is covered by a specification
   * for {@code IFC4X3_ADD2}, and the other way round. A plain prefix test is not enough for that:
   * {@code IFC4X3} starts with {@code IFC4}, which is a different schema.
   */
  public boolean covers(String schema) {
    if (schema == null || schema.isBlank()) {
      return false;
    }
    String wanted = baseSchema(schema);
    return ifcVersions().stream().anyMatch(version -> baseSchema(version).equals(wanted));
  }

  /**
   * A schema name without its addendum or corrigendum: {@code IFC4X3_ADD2} becomes {@code IFC4X3},
   * {@code IFC2X3_TC1} becomes {@code IFC2X3}, {@code IFC4} stays {@code IFC4}.
   */
  static String baseSchema(String schema) {
    String upper = schema.trim().toUpperCase(Locale.ROOT);
    Matcher matcher = SCHEMA_BASE.matcher(upper);
    return matcher.lookingAt() ? matcher.group() : upper;
  }

  /**
   * Parts of the specification that CheckMate reads but does not evaluate, worded for the report.
   *
   * <p>They are listed instead of dropped, because dropping changes the verdict. A dropped
   * applicability facet makes the specification select more elements than it should, so with
   * unsupported parts there the specification is not checked at all. A dropped requirement lets an
   * element pass that should not, so with unsupported parts there the rest is checked, but the
   * specification cannot pass.
   *
   * @param applicability unsupported parts of the applicability
   * @param requirements unsupported parts of the requirements
   */
  public record Unsupported(List<String> applicability, List<String> requirements) {

    /** Everything is evaluated. */
    public static final Unsupported NONE = new Unsupported(List.of(), List.of());

    public Unsupported {
      applicability = List.copyOf(applicability);
      requirements = List.copyOf(requirements);
    }

    public boolean isEmpty() {
      return applicability.isEmpty() && requirements.isEmpty();
    }
  }

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
