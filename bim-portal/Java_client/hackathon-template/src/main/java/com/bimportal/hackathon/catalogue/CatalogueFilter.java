package com.bimportal.hackathon.catalogue;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * The context a LOIN has to fit: discipline, phase and use case.
 *
 * <p>The portal's search only filters by name, so these filters run here, on the loaded list. A
 * value matches when one of the entry's values contains it, ignoring case: {@code LPH 5} finds
 * {@code LPH 5: Ausführungsplanung}, and the exact values the GUI offers find themselves.
 *
 * @param discipline the "Fachmodell"; null or blank for any
 * @param phase the phase; null or blank for any
 * @param purpose the use case; null or blank for any
 */
public record CatalogueFilter(String discipline, String phase, String purpose) {

  /** Lets every LOIN through. */
  public static final CatalogueFilter ANY = new CatalogueFilter(null, null, null);

  /** Whether a LOIN fits every dimension that is set. */
  public boolean accepts(CatalogueEntry entry) {
    return matches(entry.disciplines(), discipline)
        && matches(entry.phases(), phase)
        && matches(entry.purposes(), purpose);
  }

  public List<CatalogueEntry> apply(Collection<CatalogueEntry> entries) {
    return entries.stream().filter(this::accepts).toList();
  }

  public boolean isAny() {
    return blank(discipline) && blank(phase) && blank(purpose);
  }

  /** Every value of one dimension in a list of LOINs, sorted: what a filter can offer. */
  public static List<String> values(
      Collection<CatalogueEntry> entries, Function<CatalogueEntry, List<String>> dimension) {
    TreeSet<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    entries.forEach(entry -> values.addAll(dimension.apply(entry)));
    return List.copyOf(values);
  }

  private static boolean matches(List<String> values, String wanted) {
    if (blank(wanted)) {
      return true;
    }
    String needle = wanted.trim().toLowerCase(Locale.ROOT);
    return values.stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(needle));
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
