package com.bimportal.hackathon.ids;

import java.util.List;

/**
 * Translates the property addresses a BIM Portal LOIN asks for into the ones a model actually uses.
 *
 * <p>The mapping is a convention of a project or modelling guideline, not a rule: no spelling rule
 * can tell that {@code Klassifizierung/Objekttyp} and {@code _IDEbene3} mean the same, and IDS
 * itself knows no aliases, so ifctester reports such properties as missing. It is therefore data
 * ({@link PropertyMapping}, a JSON file), not code. CheckMate starts with the bundled BIM.Hamburg
 * mapping; {@code -Dcheckmate.mapping=<file>} or the environment variable {@code CHECKMATE_MAPPING}
 * name another one, {@code none} switches aliases off, and the CLI ({@code --mapping}) and the MCP
 * tool {@code load_mapping} change it at run time. The report states every use of an alias ("als
 * Pset_Objektinformation/_IDEbene3"), so a result that depends on it can be told apart.
 *
 * <p>Without this layer a LOIN check on the BIM.Hamburg bridge models reports nothing at all. The
 * portal requires {@code Klassifizierung/Objektgruppe|Objektklasse|Objekttyp}; the models carry
 * none of those names - they put a three-level classification into {@code
 * Pset_Objektinformation/_IDEbene1..3} following the Objektkatalog Ingenieurbau (the model states
 * {@code Ingenieurbau_V005} in {@code Pset_Modellinformation/_GemObjektkatalog}).
 *
 * <p>Only the addresses are mapped here, and only level for level. The values behind them routinely
 * disagree with what a LOIN expects, so an alias getting a hit is not the same as the requirement
 * being met: TBW1 classifies its Kappen as {@code _IDEbene1..3 = Kappe/Kappe/Kappe} where the LOIN
 * "Ueberbau &gt; Kappe" asks for {@code Ueberbau/Kappe/Kappe}, and its pier shaft as {@code
 * Unterbau/PfeilerStuetze/Schaft} where "Ueberbau &gt; Schaft" asks for {@code
 * Ueberbau/Pylon/Schaft}. Those differences are findings, which is why they are left to {@link
 * IdsValidator} rather than smoothed over with value aliases.
 *
 * <p>Aliases are tried after the address the LOIN names, never instead of it, so a model that does
 * carry the portal's own names is checked against those.
 */
public final class PropertyAliases {

  private static volatile PropertyMapping current = initial();

  private PropertyAliases() {}

  private static PropertyMapping initial() {
    String named = System.getProperty("checkmate.mapping", System.getenv("CHECKMATE_MAPPING"));
    return named == null || named.isBlank() ? PropertyMapping.bundled() : resolve(named);
  }

  /**
   * The mapping named by a path, {@code none} (no aliases) or {@code default} (the bundled one).
   */
  public static PropertyMapping resolve(String named) {
    String value = named.trim();
    return switch (value.toLowerCase(java.util.Locale.ROOT)) {
      case "none", "keine" -> PropertyMapping.NONE;
      case "default", "standard" -> PropertyMapping.bundled();
      default -> PropertyMapping.load(java.nio.file.Path.of(value));
    };
  }

  /** Use another mapping from now on. */
  public static void use(PropertyMapping mapping) {
    current = java.util.Objects.requireNonNull(mapping);
  }

  /** The mapping in use. */
  public static PropertyMapping current() {
    return current;
  }

  /**
   * Every address worth looking at for a requested one, the requested address first.
   *
   * @return at least the requested key, never empty
   */
  public static List<PropertyKey> candidates(PropertyKey requested) {
    return current.candidates(requested);
  }

  /** True when the address found is not the one the LOIN asked for, i.e. an alias matched. */
  public static boolean isAlias(PropertyKey requested, PropertyKey found) {
    return !requested.matches(found);
  }
}
