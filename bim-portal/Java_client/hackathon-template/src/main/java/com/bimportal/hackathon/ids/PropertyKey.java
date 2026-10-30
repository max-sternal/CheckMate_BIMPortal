package com.bimportal.hackathon.ids;

/**
 * Address of a property inside an IFC model: the property set it lives in, plus its name.
 *
 * <p>In IDS terms this is a {@code <property>} facet's {@code propertySet} and {@code baseName}.
 */
public record PropertyKey(String propertySet, String baseName) {

  public PropertyKey {
    propertySet = propertySet == null ? "" : propertySet.trim();
    baseName = baseName == null ? "" : baseName.trim();
  }

  /**
   * Compare two addresses ignoring case: authoring tools are not consistent about it, and a casing
   * difference is never what a LOIN check is meant to report.
   */
  public boolean matches(PropertyKey other) {
    return propertySet.equalsIgnoreCase(other.propertySet)
        && baseName.equalsIgnoreCase(other.baseName);
  }

  @Override
  public String toString() {
    return propertySet + "/" + baseName;
  }
}
