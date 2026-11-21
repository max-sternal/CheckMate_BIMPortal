package com.bimportal.hackathon.ids;

import java.util.List;
import java.util.Optional;

/**
 * One element of an IFC model, reduced to what an IDS check looks at.
 *
 * <p>This is the "Ist" side of the comparison. It carries no IFC toolbox types on purpose, so the
 * check can be exercised with hand-written elements in a test - and so the whole {@code ids} package
 * still compiles when the Apstex JARs are missing.
 *
 * @param id the element's GlobalId, or its STEP line number when it has none
 * @param name the element's name as authored, for reports
 * @param entityName the IFC entity, e.g. {@code IfcBuildingElementProxy}
 * @param predefinedType the PredefinedType as IDS reads it: the type object's first, then the
 *     element's own; where that is USERDEFINED, the ObjectType or ElementType instead. Null when
 *     unknown.
 * @param userDefinedType whether that PredefinedType is user-defined, which an IDS facet asking for
 *     USERDEFINED checks
 * @param properties every single, enumerated and list property found on the element and on its
 *     type
 * @param typeEntityName the IFC entity of the element's type object, e.g. {@code IfcBeamType};
 *     null when it has none. In IFC2X3 an entity facet also accepts an element through it.
 */
public record ModelElement(
    String id,
    String name,
    String entityName,
    String predefinedType,
    boolean userDefinedType,
    List<ModelProperty> properties,
    String typeEntityName) {

  /** An element without a type object. */
  public ModelElement(
      String id,
      String name,
      String entityName,
      String predefinedType,
      boolean userDefinedType,
      List<ModelProperty> properties) {
    this(id, name, entityName, predefinedType, userDefinedType, properties, null);
  }

  /** An element whose PredefinedType is not known, e.g. one built by hand in a test. */
  public ModelElement(String id, String name, String entityName, List<ModelProperty> properties) {
    this(id, name, entityName, null, false, properties);
  }

  /**
   * A property as the model carries it.
   *
   * @param values the value of a single-value property, or every value of an enumerated or list
   *     property; empty when the property is there but holds no value
   * @param dataType the IFC value type, e.g. {@code IfcText}; for several values the first one's
   */
  public record ModelProperty(PropertyKey key, List<String> values, String dataType) {

    public ModelProperty {
      values = List.copyOf(values);
    }

    /** A single-value property; a null value is a property without a value. */
    public ModelProperty(PropertyKey key, String value, String dataType) {
      this(key, value == null ? List.of() : List.of(value), dataType);
    }

    /** The value for a report: the single value, several joined by commas, null when none. */
    public String value() {
      return values.isEmpty() ? null : String.join(", ", values);
    }

    @Override
    public String toString() {
      return key + " = '" + value() + "' (" + dataType + ")";
    }
  }

  /** Look a property up by its exact address, case included. */
  public Optional<ModelProperty> find(PropertyKey key) {
    return properties.stream().filter(property -> property.key().matches(key)).findFirst();
  }

  /**
   * Look a property up, trying the model-specific aliases of its address as well.
   *
   * @see PropertyAliases
   */
  /**
   * A property found only under another spelling of the address - or of one of its aliases. Never
   * meets a requirement; it tells the report what to fix.
   *
   * @param spelling how it is written differently
   */
  public record Similar(ModelProperty property, PropertyKey.Spelling spelling) {

    /** {@code vorhanden als 'pset_test/status' (nur Groß-/Kleinschreibung weicht ab)} */
    public String note() {
      return "vorhanden als '" + property.key() + "' (" + spelling.label() + ")";
    }
  }

  /** Look for a property written differently from the address and its aliases. */
  public Optional<Similar> findSimilar(PropertyKey key) {
    for (PropertyKey candidate : PropertyAliases.candidates(key)) {
      for (ModelProperty property : properties) {
        PropertyKey.Spelling spelling = candidate.spellingOf(property.key());
        if (spelling == PropertyKey.Spelling.CASE_ONLY
            || spelling == PropertyKey.Spelling.SIMILAR) {
          return Optional.of(new Similar(property, spelling));
        }
      }
    }
    return Optional.empty();
  }

  public Optional<ModelProperty> findWithAliases(PropertyKey key) {
    for (PropertyKey candidate : PropertyAliases.candidates(key)) {
      Optional<ModelProperty> found = find(candidate);
      if (found.isPresent()) {
        return found;
      }
    }
    return Optional.empty();
  }

  /** A short label for reports: the name when there is one, otherwise the id. */
  public String label() {
    return name == null || name.isBlank() ? id : name;
  }
}
