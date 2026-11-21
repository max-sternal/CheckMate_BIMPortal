package com.bimportal.hackathon.catalogue;

import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How well catalogue LOINs fit a model, judged by their object type alone - without downloading
 * any requirements.
 *
 * <p>A LOIN's object type (e.g. {@code Kappe}) is looked for in the model in two places:
 *
 * <ol>
 *   <li>in classification values: properties called {@code Objekttyp}, {@code Objektklasse}, {@code
 *       Objektgruppe} or {@code _IDEbene1..n}, and user-defined types (ObjectType). That is how the
 *       portal's LOINs select elements, so a match here is a strong hint;
 *   <li>as a word of an element's name, e.g. {@code Kappe_Außen_BW}. A weaker hint, used when the
 *       model has no such classification.
 * </ol>
 *
 * <p>Words are compared without case, umlauts written out ({@code Übergang} = {@code Uebergang})
 * and without spaces or punctuation. An object type of several parts ({@code Stütze - Pfeiler},
 * {@code Wand, massiv}) also matches by each part of at least {@value #MIN_WORD} letters.
 *
 * <p>Many LOINs share an object type and differ in their group, the part of the name before
 * {@code >}: {@code Verbau > Verankerung}, {@code Laermschutz > Verankerung}. Elements that also
 * carry the group (e.g. {@code _IDEbene1 = Lager} for {@code Lager > Ankerplatte}) put a LOIN
 * ahead of the others of its kind.
 *
 * <p>This is a ranking to help choose, not a check: whether a LOIN really applies, the check says.
 */
public final class ModelFit {

  /** Shorter words, such as "BW" or "mit", say too little to count. */
  static final int MIN_WORD = 4;

  private static final Pattern CLASSIFYING =
      Pattern.compile("objekttyp|objektklasse|objektgruppe|_?idebene\\d+");

  private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+");

  /** Splits CamelCase: {@code KonstruktionMitDichtprofil} into its three words. */
  private static final Pattern CAMEL_PARTS =
      Pattern.compile("\\p{Lu}?\\p{Ll}+|\\p{Lu}+(?!\\p{Ll})");

  private static final Pattern OBJECT_TYPE_PARTS = Pattern.compile("\\s+-\\s+|,|/");

  /** No model loaded: nothing fits. */
  public static final ModelFit NONE = new ModelFit(Map.of(), Map.of());

  private final Map<String, Set<String>> classified;
  private final Map<String, Set<String>> named;

  private ModelFit(Map<String, Set<String>> classified, Map<String, Set<String>> named) {
    this.classified = classified;
    this.named = named;
  }

  /** What a match was found by: classification is the stronger hint. */
  public enum Kind {
    CLASSIFICATION,
    NAME,
    NONE
  }

  /**
   * How one LOIN fits.
   *
   * @param elements how many model elements carry the object type
   * @param objectType the LOIN's object type that matched; null when none did
   * @param inGroup how many of those elements also carry the LOIN's group
   * @param group the LOIN's group, the part of its name before {@code >}; null when it has none
   */
  public record Fit(Kind kind, int elements, String objectType, int inGroup, String group) {

    public static final Fit NONE = new Fit(Kind.NONE, 0, null, 0, null);

    public boolean fits() {
      return kind != Kind.NONE;
    }

    /**
     * For the list: {@code 6 Element(e) mit 'Kappe' in der Klassifizierung}. Which level of the
     * classification carries it, the ranking does not tell; the description after a click does.
     */
    public String describe() {
      String found =
          switch (kind) {
            case CLASSIFICATION ->
                elements + " Element(e) mit '" + objectType + "' in der Klassifizierung";
            case NAME -> elements + " Element(e) mit '" + objectType + "' im Namen";
            case NONE -> "kein Element mit diesem Objekttyp";
          };
      return inGroup > 0 ? found + ", " + inGroup + " davon in Gruppe '" + group + "'" : found;
    }
  }

  /** A LOIN and how it fits. */
  public record Ranked(CatalogueEntry entry, Fit fit) {}

  /** Index a model's classification values and element names. */
  public static ModelFit of(Collection<ModelElement> model) {
    Map<String, Set<String>> classified = new HashMap<>();
    Map<String, Set<String>> named = new HashMap<>();
    for (ModelElement element : model) {
      for (ModelProperty property : element.properties()) {
        if (CLASSIFYING.matcher(property.key().baseName().toLowerCase(Locale.ROOT)).matches()) {
          property.values().forEach(value -> index(classified, normalize(value), element));
        }
      }
      if (element.userDefinedType() && element.predefinedType() != null) {
        index(classified, normalize(element.predefinedType()), element);
      }
      nameWords(element.name()).forEach(word -> index(named, word, element));
    }
    return new ModelFit(classified, named);
  }

  /**
   * How a LOIN fits: its best object type - classification before name, then elements in the
   * LOIN's group, then all elements.
   */
  public Fit fit(CatalogueEntry entry) {
    String group = group(entry.name());
    Set<String> groupElements =
        group == null ? Set.of() : elementsWith(objectTypeWords(group), classified, named);

    Fit best = Fit.NONE;
    for (String objectType : entry.objectTypes()) {
      List<String> words = objectTypeWords(objectType);
      Kind kind = Kind.CLASSIFICATION;
      Set<String> elements = elementsWith(words, classified);
      if (elements.isEmpty()) {
        kind = Kind.NAME;
        elements = elementsWith(words, named);
      }
      if (elements.isEmpty()) {
        continue;
      }
      Set<String> inGroup = new HashSet<>(elements);
      inGroup.retainAll(groupElements);
      Fit candidate = new Fit(kind, elements.size(), objectType, inGroup.size(), group);
      if (BY_STRENGTH.compare(candidate, best) < 0) {
        best = candidate;
      }
    }
    return best;
  }

  /**
   * LOINs in order of fit: classification matches first, then name matches, then the rest; within
   * each by the number of elements, then by name.
   */
  public List<Ranked> rank(Collection<CatalogueEntry> entries) {
    List<Ranked> ranked = new ArrayList<>();
    entries.forEach(entry -> ranked.add(new Ranked(entry, fit(entry))));
    ranked.sort(
        Comparator.comparing(Ranked::fit, BY_STRENGTH)
            .thenComparing(r -> r.entry().name(), String.CASE_INSENSITIVE_ORDER));
    return ranked;
  }

  /** Stronger first: kind, then more elements in the group, then more elements. */
  private static final Comparator<Fit> BY_STRENGTH =
      Comparator.comparing(Fit::kind)
          .thenComparing(Fit::inGroup, Comparator.reverseOrder())
          .thenComparing(Fit::elements, Comparator.reverseOrder());

  /** The elements any of the words is indexed for, in any of the indexes. */
  @SafeVarargs
  private static Set<String> elementsWith(List<String> words, Map<String, Set<String>>... indexes) {
    Set<String> elements = new HashSet<>();
    for (Map<String, Set<String>> index : indexes) {
      words.forEach(word -> elements.addAll(index.getOrDefault(word, Set.of())));
    }
    return elements;
  }

  /** {@code Lager > Ankerplatte (Ingenieurbau): LPH 1-9} is in the group {@code Lager}. */
  static String group(String name) {
    int separator = name == null ? -1 : name.indexOf(" > ");
    return separator > 0 ? name.substring(0, separator).trim() : null;
  }

  /** The whole object type, and each of its parts that is long enough to say something. */
  static List<String> objectTypeWords(String objectType) {
    List<String> words = new ArrayList<>();
    words.add(normalize(objectType));
    for (String part : OBJECT_TYPE_PARTS.split(objectType)) {
      String word = normalize(part);
      if (word.length() >= MIN_WORD && !words.contains(word)) {
        words.add(word);
      }
    }
    return words;
  }

  /**
   * The words of an element's name, before the first colon, where authoring tools start repeating
   * the family name and an ID: {@code Kappe_Außen_BW:Kappe_Außen_BW:656578}.
   */
  static Set<String> nameWords(String name) {
    Set<String> words = new HashSet<>();
    if (name == null) {
      return words;
    }
    int colon = name.indexOf(':');
    for (String segment : WORDS.split(colon < 0 ? name : name.substring(0, colon))) {
      addWord(words, segment);
      Matcher camel = CAMEL_PARTS.matcher(segment);
      while (camel.find()) {
        addWord(words, camel.group());
      }
    }
    return words;
  }

  private static void addWord(Set<String> words, String word) {
    String normalized = normalize(word);
    if (normalized.length() >= MIN_WORD) {
      words.add(normalized);
    }
  }

  /** Lower case, umlauts written out, letters and digits only - as for property names. */
  static String normalize(String text) {
    return PropertyKey.loose(text);
  }

  private static void index(Map<String, Set<String>> index, String word, ModelElement element) {
    if (!word.isEmpty()) {
      index.computeIfAbsent(word, w -> new HashSet<>()).add(element.id());
    }
  }
}
