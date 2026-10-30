package com.bimportal.hackathon.ids;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates the property addresses a BIM Portal LOIN asks for into the ones a model actually uses.
 *
 * <p>Without this layer a LOIN check on the BIM.Hamburg bridge models reports nothing at all. The
 * portal requires {@code Klassifizierung/Objektgruppe|Objektklasse|Objekttyp}; the models carry none
 * of those names - they put a three-level classification into {@code
 * Pset_Objektinformation/_IDEbene1..3} following the Objektkatalog Ingenieurbau (the model states
 * {@code Ingenieurbau_V005} in {@code Pset_Modellinformation/_GemObjektkatalog}).
 *
 * <p>Only the addresses are mapped here, and only level for level. The values behind them routinely
 * disagree with what a LOIN expects, so an alias getting a hit is not the same as the requirement
 * being met: TBW1 classifies its Kappen as {@code _IDEbene1..3 = Kappe/Kappe/Kappe} where the LOIN
 * "Ueberbau &gt; Kappe" asks for {@code Ueberbau/Kappe/Kappe}, and its pier shaft as {@code
 * Unterbau/PfeilerStuetze/Schaft} where "Ueberbau &gt; Schaft" asks for {@code Ueberbau/Pylon/Schaft}.
 * Those differences are findings, which is why they are left to {@link IdsValidator} rather than
 * smoothed over with value aliases.
 *
 * <p>Aliases are tried after the address the LOIN names, never instead of it, so a model that does
 * carry the portal's own names is checked against those.
 */
public final class PropertyAliases {

  private static final Map<PropertyKey, List<PropertyKey>> ALIASES = defaultAliases();

  private PropertyAliases() {}

  private static Map<PropertyKey, List<PropertyKey>> defaultAliases() {
    Map<PropertyKey, List<PropertyKey>> aliases = new LinkedHashMap<>();
    String objektinformation = "Pset_Objektinformation";

    aliases.put(
        new PropertyKey("Klassifizierung", "Objektgruppe"),
        List.of(new PropertyKey(objektinformation, "_IDEbene1")));
    aliases.put(
        new PropertyKey("Klassifizierung", "Objektklasse"),
        List.of(new PropertyKey(objektinformation, "_IDEbene2")));
    aliases.put(
        new PropertyKey("Klassifizierung", "Objekttyp"),
        List.of(new PropertyKey(objektinformation, "_IDEbene3")));

    return aliases;
  }

  /**
   * Every address worth looking at for a requested one, the requested address first.
   *
   * @return at least the requested key, never empty
   */
  public static List<PropertyKey> candidates(PropertyKey requested) {
    List<PropertyKey> candidates = new ArrayList<>();
    candidates.add(requested);

    ALIASES.entrySet().stream()
        .filter(entry -> entry.getKey().matches(requested))
        .findFirst()
        .ifPresent(entry -> candidates.addAll(entry.getValue()));

    return candidates;
  }

  /** True when the address found is not the one the LOIN asked for, i.e. an alias matched. */
  public static boolean isAlias(PropertyKey requested, PropertyKey found) {
    return !requested.matches(found);
  }
}
