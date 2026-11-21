package com.bimportal.hackathon.ids;

import java.util.Locale;

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
   * The same address, exactly - case included, as IDS and ifctester compare property sets and
   * names. A model that writes {@code pset_slabcommon} does not carry {@code Pset_SlabCommon};
   * {@link #spellingOf} tells the report that it is there under another spelling, so it can be
   * fixed, but it is never accepted.
   */
  public boolean matches(PropertyKey other) {
    return propertySet.equals(other.propertySet) && baseName.equals(other.baseName);
  }

  /** How another address is written, compared with this one. */
  public enum Spelling {
    SAME(""),
    /** Only upper and lower case differ: {@code pset_slabcommon/loadbearing}. */
    CASE_ONLY("nur Groß-/Kleinschreibung weicht ab"),
    /**
     * Otherwise the same once case, umlauts ({@code ä} = {@code ae}, {@code ß} = {@code ss}),
     * spaces, hyphens, underscores and dots are set aside: {@code Feld-Nummer}, {@code
     * _Feldnummer}, {@code Länge} / {@code Laenge}.
     */
    SIMILAR("ähnliche Schreibweise"),
    DIFFERENT("");

    private final String label;

    Spelling(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  /** Whether another address is this one written differently, and how. */
  public Spelling spellingOf(PropertyKey other) {
    if (matches(other)) {
      return Spelling.SAME;
    }
    if (propertySet.equalsIgnoreCase(other.propertySet)
        && baseName.equalsIgnoreCase(other.baseName)) {
      return Spelling.CASE_ONLY;
    }
    if (loose(propertySet).equals(loose(other.propertySet))
        && loose(baseName).equals(loose(other.baseName))) {
      return Spelling.SIMILAR;
    }
    return Spelling.DIFFERENT;
  }

  /**
   * What is left of a name when spelling no longer counts: lower case, umlauts and {@code ß}
   * written out, letters and digits only.
   */
  public static String loose(String text) {
    String lower = text.toLowerCase(Locale.GERMAN);
    lower = lower.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
    return lower.replaceAll("[^a-z0-9]", "");
  }

  @Override
  public String toString() {
    return propertySet + "/" + baseName;
  }
}
