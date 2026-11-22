package com.bimportal.hackathon.ids;

import com.bimportal.hackathon.ids.CheckResult.Diagnosis;
import com.bimportal.hackathon.ids.CheckResult.FacetMatch;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.NearMiss;
import com.bimportal.hackathon.ids.CheckResult.RelatedTypes;
import com.bimportal.hackathon.ids.CheckResult.SimilarNames;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.IdsSpecification.Cardinality;
import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Checks model elements against one IDS specification.
 *
 * <p>Pure: no IFC toolbox, no network, no UI. Hand a specification and a list of elements in and a
 * {@link CheckResult} comes back, which is what makes this the part worth testing.
 *
 * <p>The rules follow IDS 1.0, with ifctester (IfcOpenShell's implementation) as the reference for
 * where the standard leaves room: data types are compared strictly, {@code prohibited} turns a
 * property facet around, {@code predefinedType} is read the way IFC defines it, values are compared
 * by their data type (see {@link Values}), and in IFC2X3 an element also has the entity of its type
 * object.
 */
public final class IdsValidator {

  /** Listed near misses: enough to recognise the pattern, few enough to read. */
  private static final int NEAR_MISSES = 5;

  private IdsValidator() {}

  /** Run one specification against a model whose schema is not known. */
  public static CheckResult check(IdsSpecification specification, Collection<ModelElement> model) {
    return check(specification, model, null);
  }

  /**
   * Run one specification against a model.
   *
   * <p>A specification whose applicability uses parts CheckMate does not evaluate is not checked:
   * without them it would select more elements than it should. The result lists those parts.
   *
   * @param schema the model's schema; for IFC2X3 an entity facet also accepts an element through
   *     its type object, as ifctester does. Null when unknown.
   */
  public static CheckResult check(
      IdsSpecification specification, Collection<ModelElement> model, String schema) {
    if (!specification.unsupported().applicability().isEmpty()) {
      return new CheckResult(
          specification, model.size(), 0, List.of(), Diagnosis.NONE, RelatedTypes.NONE);
    }
    boolean viaType = schema != null && "IFC2X3".equals(IdsSpecification.baseSchema(schema));

    List<ModelElement> applicable =
        model.stream()
            .filter(element -> applies(specification, element, Leniency.STRICT, viaType))
            .toList();

    List<Finding> findings = new ArrayList<>();
    for (ModelElement element : applicable) {
      checkEntity(element, specification.requiredEntity(), viaType).ifPresent(findings::add);
      for (PropertyRequirement requirement : specification.requirements()) {
        checkProperty(element, requirement).ifPresent(findings::add);
      }
    }

    return new CheckResult(
        specification,
        model.size(),
        applicable.size(),
        findings,
        applicable.isEmpty() ? diagnose(specification, model, viaType) : Diagnosis.NONE,
        relatedTypes(specification, model, applicable, viaType),
        similarNames(specification, model, applicable, viaType));
  }

  /**
   * How strictly the applicability is read. The verdict is always strict; the lenient readings only
   * find the elements the hints name.
   */
  private enum Leniency {
    STRICT,
    /** Also accept a data type of the same family: {@code IfcText} for {@code IFCLABEL}. */
    RELATED_TYPES,
    /** Also accept a property name written differently: {@code pset_test/status}. */
    SIMILAR_NAMES
  }

  /**
   * Whether an element passes an entity facet, PredefinedType included.
   *
   * @param viaType IFC2X3: the type object's entity counts too
   */
  private static boolean entityMatches(
      EntityRequirement entity, ModelElement element, boolean viaType) {
    return entityNameMatches(entity, element, viaType) && entity.matchesPredefinedType(element);
  }

  /**
   * Whether an element has one of the facet's entities: its own, or in IFC2X3 the one its type
   * object stands for - an {@code IfcBuildingElementProxy} typed by an {@code IfcBeamType} counts
   * as an {@code IFCBEAM}, as in ifctester.
   */
  private static boolean entityNameMatches(
      EntityRequirement entity, ModelElement element, boolean viaType) {
    return entity.matchesName(element.entityName())
        || (viaType && entity.matchesName(occurrenceOf(element.typeEntityName())));
  }

  /**
   * The occurrence entity a type object stands for: {@code IfcBeamType} for {@code IfcBeam}, and in
   * IFC2X3 {@code IfcDoorStyle} for {@code IfcDoor}. Null for null.
   */
  static String occurrenceOf(String typeEntity) {
    if (typeEntity == null) {
      return null;
    }
    for (String suffix : List.of("Type", "Style")) {
      if (typeEntity.length() > suffix.length()
          && typeEntity.regionMatches(
              true, typeEntity.length() - suffix.length(), suffix, 0, suffix.length())) {
        return typeEntity.substring(0, typeEntity.length() - suffix.length());
      }
    }
    return typeEntity;
  }

  /**
   * Whether a specification selects an element.
   *
   * <p>All applicability facets have to hold - that is IDS's AND semantics - each with address,
   * value and data type. One definition of "this facet holds", shared with the diagnosis and the
   * hint, so the reason a report gives can never drift away from the selection it explains.
   *
   * @param leniency {@link Leniency#STRICT} for the verdict; the others only for the hints
   * @param viaType IFC2X3: the type object's entity counts too
   */
  private static boolean applies(
      IdsSpecification specification, ModelElement element, Leniency leniency, boolean viaType) {
    EntityRequirement entity = specification.applicableEntity();
    if (entity != null && !entityMatches(entity, element, viaType)) {
      return false;
    }

    for (PropertyRequirement facet : specification.applicability()) {
      Optional<ModelProperty> found = element.findWithAliases(facet.key());
      if (found.isEmpty() && leniency == Leniency.SIMILAR_NAMES) {
        found = element.findSimilar(facet.key()).map(ModelElement.Similar::property);
      }
      boolean holds =
          found.isPresent()
              && valueMatches(facet, found.get())
              && (typeMatches(facet, found.get())
                  || (leniency == Leniency.RELATED_TYPES && typeRelated(facet, found.get())));
      if (!holds) {
        return false;
      }
    }
    return true;
  }

  /**
   * The elements a related data type kept out, for the hint.
   *
   * <p>Computed whatever the verdict was: a specification can apply to five elements and still have
   * turned a sixth down because it stores one value as {@code IfcText}.
   */
  private static RelatedTypes relatedTypes(
      IdsSpecification specification,
      Collection<ModelElement> model,
      List<ModelElement> applicable,
      boolean viaType) {

    if (specification.applicability().stream().noneMatch(PropertyRequirement::hasDataType)) {
      return RelatedTypes.NONE;
    }

    // By identity: records compare all their properties, and these are the same instances anyway.
    Set<ModelElement> selected = Collections.newSetFromMap(new IdentityHashMap<>());
    selected.addAll(applicable);

    List<ModelElement> keptOut =
        model.stream()
            .filter(element -> !selected.contains(element))
            .filter(element -> applies(specification, element, Leniency.RELATED_TYPES, viaType))
            .toList();
    if (keptOut.isEmpty()) {
      return RelatedTypes.NONE;
    }

    Set<String> differences = new TreeSet<>();
    for (ModelElement element : keptOut) {
      for (PropertyRequirement facet : specification.applicability()) {
        element
            .findWithAliases(facet.key())
            .filter(property -> !typeMatches(facet, property))
            .ifPresent(
                property ->
                    differences.add(DataTypes.difference(facet.dataType(), property.dataType())));
      }
    }
    return new RelatedTypes(
        keptOut.stream().map(ModelElement::id).toList(), List.copyOf(differences));
  }

  /**
   * The elements a differently written property name kept out, for the hint. Like {@link
   * #relatedTypes}: computed whatever the verdict was.
   */
  private static SimilarNames similarNames(
      IdsSpecification specification,
      Collection<ModelElement> model,
      List<ModelElement> applicable,
      boolean viaType) {

    if (specification.applicability().isEmpty()) {
      return SimilarNames.NONE;
    }
    Set<ModelElement> selected = Collections.newSetFromMap(new IdentityHashMap<>());
    selected.addAll(applicable);

    List<ModelElement> keptOut =
        model.stream()
            .filter(element -> !selected.contains(element))
            .filter(element -> applies(specification, element, Leniency.SIMILAR_NAMES, viaType))
            .toList();
    if (keptOut.isEmpty()) {
      return SimilarNames.NONE;
    }

    Set<String> spellings = new TreeSet<>();
    for (ModelElement element : keptOut) {
      for (PropertyRequirement facet : specification.applicability()) {
        if (element.findWithAliases(facet.key()).isEmpty()) {
          element
              .findSimilar(facet.key())
              .ifPresent(
                  similar ->
                      spellings.add(
                          "'" + similar.property().key() + "' statt '" + facet.key() + "'"));
        }
      }
    }
    return new SimilarNames(
        keptOut.stream().map(ModelElement::id).toList(), List.copyOf(spellings));
  }

  /**
   * Work out why the applicability selected nothing.
   *
   * <p>Only called when that happened, because it is the one case the summary cannot explain on its
   * own. Each facet is counted separately - that separates "the model does not carry this merkmal
   * at all" from "it carries it with another value" and "with the right value in another type" -
   * and the elements satisfying the most facets are named with what kept them out.
   */
  private static Diagnosis diagnose(
      IdsSpecification specification, Collection<ModelElement> model, boolean viaType) {

    List<FacetMatch> facets = new ArrayList<>();

    EntityRequirement entity = specification.applicableEntity();
    if (entity != null) {
      List<ModelElement> ofEntity =
          model.stream().filter(element -> entityNameMatches(entity, element, viaType)).toList();
      int matched = (int) ofEntity.stream().filter(entity::matchesPredefinedType).count();
      facets.add(
          FacetMatch.entity(
              "IFC-Entity " + entity.describe(),
              matched,
              ofEntity.size(),
              predefinedTypeMismatch(entity, ofEntity)));
    }

    for (PropertyRequirement facet : specification.applicability()) {
      int matched = 0;
      int addressFound = 0;
      int valueButNotType = 0;
      Set<String> typeDifferences = new TreeSet<>();
      int otherSpelling = 0;
      String otherSpellingNote = null;

      for (ModelElement element : model) {
        Optional<ModelProperty> found = element.findWithAliases(facet.key());
        if (found.isEmpty()) {
          Optional<ModelElement.Similar> similar = element.findSimilar(facet.key());
          if (similar.isPresent()) {
            otherSpelling++;
            if (otherSpellingNote == null) {
              otherSpellingNote =
                  "'"
                      + similar.get().property().key()
                      + "' ("
                      + similar.get().spelling().label()
                      + ")";
            }
          }
          continue;
        }
        ModelProperty property = found.get();
        addressFound++;
        if (!valueMatches(facet, property)) {
          continue;
        }
        if (typeMatches(facet, property)) {
          matched++;
        } else {
          valueButNotType++;
          typeDifferences.add(DataTypes.difference(facet.dataType(), property.dataType()));
        }
      }
      facets.add(
          FacetMatch.property(
              facet.describe(),
              matched,
              addressFound,
              valueButNotType,
              String.join(", ", typeDifferences),
              otherSpelling == 0 ? "" : otherSpelling + " Element(e) mit " + otherSpellingNote));
    }

    return new Diagnosis(facets, nearMisses(specification, model));
  }

  /**
   * What the elements of the right entity have instead of the required PredefinedType, e.g. {@code
   * keines mit benutzerdefiniertem Typ (gefunden: NOTDEFINED 8, WINDOW 3)}. Empty when the facet
   * asks for none.
   */
  private static String predefinedTypeMismatch(
      EntityRequirement entity, List<ModelElement> ofEntity) {
    if (!entity.hasPredefinedType()) {
      return "";
    }
    String wanted =
        "USERDEFINED".equals(entity.predefinedTypeSource())
            ? "keines mit benutzerdefiniertem Typ"
            : "keines mit PredefinedType " + entity.predefinedTypeSource();
    Map<String, Integer> found = new TreeMap<>();
    for (ModelElement element : ofEntity) {
      String type = element.predefinedType() == null ? "ohne" : element.predefinedType();
      found.merge(type, 1, Integer::sum);
    }
    List<String> counts = new ArrayList<>();
    found.forEach((type, count) -> counts.add(type + " " + count));
    return counts.isEmpty() ? wanted : wanted + " (gefunden: " + String.join(", ", counts) + ")";
  }

  /**
   * The elements whose values met the most applicability facets, with what diverged.
   *
   * <p>Only the best-scoring elements are listed: every element below that score diverges in the
   * same way and more, so naming them adds length rather than information. The score counts values
   * only, see {@link NearMiss#matchedFacets()}.
   */
  private static List<NearMiss> nearMisses(
      IdsSpecification specification, Collection<ModelElement> model) {

    List<PropertyRequirement> facets = specification.applicability();
    if (facets.isEmpty()) {
      return List.of();
    }

    int best = model.stream().mapToInt(element -> valueScore(facets, element)).max().orElse(0);
    if (best == 0) {
      return List.of();
    }

    List<NearMiss> nearMisses = new ArrayList<>();
    for (ModelElement element : model) {
      if (nearMisses.size() >= NEAR_MISSES) {
        break;
      }
      if (valueScore(facets, element) == best) {
        nearMisses.add(
            new NearMiss(
                element.id(),
                element.label(),
                element.entityName(),
                best,
                facets.size(),
                divergences(facets, element)));
      }
    }
    return nearMisses;
  }

  /** How many applicability facets an element meets by value, whatever the data type. */
  private static int valueScore(List<PropertyRequirement> facets, ModelElement element) {
    int score = 0;
    for (PropertyRequirement facet : facets) {
      if (element.findWithAliases(facet.key()).filter(p -> valueMatches(facet, p)).isPresent()) {
        score++;
      }
    }
    return score;
  }

  /** One line per facet that kept an element out: a missing merkmal, its value or its type. */
  private static List<String> divergences(List<PropertyRequirement> facets, ModelElement element) {

    List<String> divergences = new ArrayList<>();
    for (PropertyRequirement facet : facets) {
      Optional<ModelProperty> found = element.findWithAliases(facet.key());
      if (found.isEmpty()) {
        divergences.add(
            facet.key()
                + ": Merkmal fehlt"
                + element.findSimilar(facet.key()).map(s -> ", " + s.note()).orElse(""));
        continue;
      }

      ModelProperty property = found.get();
      if (!hasValue(property)) {
        divergences.add(facet.key() + ": ohne Wert" + aliasNote(facet, property));
        continue;
      }
      List<String> differences = new ArrayList<>();
      if (!valueMatches(facet, property)) {
        differences.add(
            "'"
                + property.value()
                + "' statt '"
                + facet.expectedValue()
                + "'"
                + caseNote(facet, property));
      }
      if (!typeMatches(facet, property)) {
        differences.add("Datentyp " + DataTypes.difference(facet.dataType(), property.dataType()));
      }
      if (!differences.isEmpty()) {
        divergences.add(
            facet.key() + ": " + String.join(", ", differences) + aliasNote(facet, property));
      }
    }
    return divergences;
  }

  /**
   * The facet's value, or no value asked for. An enumerated or list property meets it when one of
   * its values does, as in ifctester.
   */
  private static boolean valueMatches(PropertyRequirement facet, ModelProperty property) {
    if (!hasValue(property)) {
      return false;
    }
    return !facet.hasExpectedValue()
        || property.values().stream()
            .anyMatch(value -> Values.same(facet.expectedValue(), value, property.dataType()));
  }

  /**
   * Whether the property holds a value at all. An empty one, or the UNKNOWN of a logical, is no
   * value: ifctester then treats the property as missing, so a facet asking only for its presence
   * is not met either.
   */
  private static boolean hasValue(ModelProperty property) {
    boolean logical = "IFCLOGICAL".equals(DataTypes.normalize(property.dataType()));
    return property.values().stream()
        .anyMatch(
            value ->
                value != null
                    && !value.isEmpty()
                    && !(logical && "UNKNOWN".equalsIgnoreCase(value.trim())));
  }

  /**
   * Said when the value differs only in case: the check counts it, as IDS does, but the reader
   * should see at once that it is a spelling and not a different value.
   */
  private static String caseNote(PropertyRequirement facet, ModelProperty property) {
    return property.values().stream()
            .anyMatch(value -> Values.caseOnly(facet.expectedValue(), value))
        ? " (nur Groß-/Kleinschreibung weicht ab)"
        : "";
  }

  /** The facet's data type exactly, or no data type asked for. */
  private static boolean typeMatches(PropertyRequirement facet, ModelProperty property) {
    return !facet.hasDataType() || DataTypes.identical(facet.dataType(), property.dataType());
  }

  /** A different data type of the same family, which only the hint accepts. */
  private static boolean typeRelated(PropertyRequirement facet, ModelProperty property) {
    return facet.hasDataType() && DataTypes.related(facet.dataType(), property.dataType());
  }

  private static Optional<Finding> checkEntity(
      ModelElement element, EntityRequirement entity, boolean viaType) {
    if (entity == null) {
      return Optional.empty();
    }

    if (!entityNameMatches(entity, element, viaType)) {
      return Optional.of(
          entityFinding(
              element,
              Status.WRONG_ENTITY,
              element.entityName() + " steht nicht in der zulässigen Liste"));
    }
    if (!entity.matchesPredefinedType(element)) {
      String actual =
          element.predefinedType() == null
              ? "nicht angegeben"
              : "'" + element.predefinedType() + "'";
      return Optional.of(
          entityFinding(
              element,
              Status.WRONG_PREDEFINED_TYPE,
              "PredefinedType " + actual + ", gefordert " + entity.predefinedTypeSource()));
    }

    String detail =
        entity.matchesName(element.entityName())
            ? element.entityName() + " ist zulässig"
            : element.entityName()
                + " ist über seinen Typ "
                + element.typeEntityName()
                + " zulässig (IFC2X3)";
    if (entity.hasPredefinedType()) {
      detail += " (PredefinedType " + entity.predefinedTypeSource() + ")";
    }
    return Optional.of(entityFinding(element, Status.FULFILLED, detail));
  }

  private static Finding entityFinding(ModelElement element, Status status, String detail) {
    return new Finding(element.id(), element.label(), "IFC-Entity", status, detail);
  }

  private static Optional<Finding> checkProperty(
      ModelElement element, PropertyRequirement requirement) {
    Optional<ModelProperty> found = element.findWithAliases(requirement.key());

    if (requirement.cardinality() == Cardinality.PROHIBITED) {
      return Optional.of(checkProhibited(element, requirement, found));
    }

    if (found.isEmpty()) {
      // An optional requirement only has to hold for elements that carry the property.
      if (requirement.cardinality() == Cardinality.OPTIONAL) {
        return Optional.empty();
      }
      String searched =
          PropertyAliases.candidates(requirement.key()).stream()
              .map(PropertyKey::toString)
              .collect(Collectors.joining(", "));
      String similar = element.findSimilar(requirement.key()).map(s -> " - " + s.note()).orElse("");
      return Optional.of(
          finding(
              element, requirement, Status.MISSING, "nicht gefunden unter " + searched + similar));
    }

    ModelProperty property = found.get();
    String alias = aliasNote(requirement, property);

    if (!hasValue(property)) {
      if (requirement.cardinality() == Cardinality.OPTIONAL) {
        return Optional.empty();
      }
      return Optional.of(
          finding(element, requirement, Status.MISSING, "vorhanden, aber ohne Wert" + alias));
    }
    if (!valueMatches(requirement, property)) {
      return Optional.of(
          finding(
              element,
              requirement,
              Status.WRONG_VALUE,
              "gefunden: '" + property.value() + "'" + caseNote(requirement, property) + alias));
    }

    if (!typeMatches(requirement, property)) {
      String related = typeRelated(requirement, property) ? " - verwandter Typ" : "";
      return Optional.of(
          finding(
              element,
              requirement,
              Status.WRONG_DATATYPE,
              "gefordert "
                  + DataTypes.normalize(requirement.dataType())
                  + ", gefunden "
                  + DataTypes.normalize(property.dataType())
                  + related
                  + alias));
    }

    return Optional.of(
        finding(element, requirement, Status.FULFILLED, "gefunden: " + property + alias));
  }

  /**
   * A prohibited facet is met when it does not match: no such property, or one with another value
   * or data type. That is how IDS defines it, and it is reported either way, so a met prohibition
   * shows up as "erfüllt" rather than as nothing.
   */
  private static Finding checkProhibited(
      ModelElement element, PropertyRequirement requirement, Optional<ModelProperty> found) {

    if (found.isEmpty()) {
      String similar = element.findSimilar(requirement.key()).map(s -> " - " + s.note()).orElse("");
      return finding(
          element, requirement, Status.FULFILLED, "nicht vorhanden, wie gefordert" + similar);
    }

    ModelProperty property = found.get();
    String alias = aliasNote(requirement, property);
    if (valueMatches(requirement, property) && typeMatches(requirement, property)) {
      return finding(
          element, requirement, Status.PROHIBITED_PRESENT, "gefunden: " + property + alias);
    }
    return finding(
        element,
        requirement,
        Status.FULFILLED,
        "gefunden: " + property + ", nicht in der unzulässigen Form" + alias);
  }

  /**
   * Where the property turned up, when that is not the address the LOIN asked for.
   *
   * <p>Worth stating on every finding: someone reading the report should see that a model-specific
   * alias was used rather than the LOIN's own address.
   */
  private static String aliasNote(PropertyRequirement requirement, ModelProperty property) {
    return PropertyAliases.isAlias(requirement.key(), property.key())
        ? " (als " + property.key() + ")"
        : "";
  }

  private static Finding finding(
      ModelElement element, PropertyRequirement requirement, Status status, String detail) {
    return new Finding(element.id(), element.label(), requirement.describe(), status, detail);
  }
}
