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
 * @param properties every single-value property found on the element and on its type
 */
public record ModelElement(
    String id, String name, String entityName, List<ModelProperty> properties) {

  /**
   * A property as the model carries it.
   *
   * @param dataType the IFC value type, e.g. {@code IfcText}
   */
  public record ModelProperty(PropertyKey key, String value, String dataType) {

    @Override
    public String toString() {
      return key + " = '" + value + "' (" + dataType + ")";
    }
  }

  /** Look a property up by its exact address, ignoring case. */
  public Optional<ModelProperty> find(PropertyKey key) {
    return properties.stream().filter(property -> property.key().matches(key)).findFirst();
  }

  /**
   * Look a property up, trying the model-specific aliases of its address as well.
   *
   * @see PropertyAliases
   */
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
