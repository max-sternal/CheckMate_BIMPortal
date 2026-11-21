package com.bimportal.hackathon.ids;

import com.bimportal.hackathon.ids.CheckResult.Diagnosis;
import com.bimportal.hackathon.ids.CheckResult.FacetMatch;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.NearMiss;
import com.bimportal.hackathon.ids.CheckResult.RelatedTypes;
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
import java.util.Optional;
import java.util.Set;
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
 * property facet around, and {@code predefinedType} is read the way IFC defines it.
 */
public final class IdsValidator {

  /** Listed near misses: enough to recognise the pattern, few enough to read. */
  private static final int NEAR_MISSES = 5;

  private IdsValidator() {}

  /**
   * Run one specification against a model.
   *
   * <p>A specification whose applicability uses parts CheckMate does not evaluate is not checked:
   * without them it would select more elements than it should. The result lists those parts.
   */
  public static CheckResult check(IdsSpecification specification, Collection<ModelElement> model) {
    if (!specification.unsupported().applicability().isEmpty()) {
      return new CheckResult(
          specification, model.size(), 0, List.of(), Diagnosis.NONE, RelatedTypes.NONE);
    }

    List<ModelElement> applicable =
        model.stream().filter(element -> applies(specification, element, false)).toList();

    List<Finding> findings = new ArrayList<>();
    for (ModelElement element : applicable) {
      checkEntity(element, specification.requiredEntity()).ifPresent(findings::add);
      for (PropertyRequirement requirement : specification.requirements()) {
        checkProperty(element, requirement).ifPresent(findings::add);
      }
    }

    return new CheckResult(
        specification,
        model.size(),
        applicable.size(),
        findings,
        applicable.isEmpty() ? diagnose(specification, model) : Diagnosis.NONE,
        relatedTypes(specification, model, applicable));
  }

  /**
   * Whether a specification selects an element.
   *
   * <p>All applicability facets have to hold - that is IDS's AND semantics - each with address,
   * value and data type. One definition of "this facet holds", shared with the diagnosis and the
   * hint, so the reason a report gives can never drift away from the selection it explains.
   *
   * @param relatedTypesAccepted true only for the hint: also accept a related data type
   */
  private static boolean applies(
      IdsSpecification specification, ModelElement element, boolean relatedTypesAccepted) {
    EntityRequirement entity = specification.applicableEntity();
    if (entity != null && !entity.matches(element)) {
      return false;
    }

    for (PropertyRequirement facet : specification.applicability()) {
      Optional<ModelProperty> found = element.findWithAliases(facet.key());
      boolean holds =
          found.isPresent()
              && valueMatches(facet, found.get())
              && (typeMatches(facet, found.get())
                  || (relatedTypesAccepted && typeRelated(facet, found.get())));
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
      List<ModelElement> applicable) {

    if (specification.applicability().stream().noneMatch(PropertyRequirement::hasDataType)) {
      return RelatedTypes.NONE;
    }

    // By identity: records compare all their properties, and these are the same instances anyway.
    Set<ModelElement> selected = Collections.newSetFromMap(new IdentityHashMap<>());
    selected.addAll(applicable);

    List<ModelElement> keptOut =
        model.stream()
            .filter(element -> !selected.contains(element))
            .filter(element -> applies(specification, element, true))
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
   * Work out why the applicability selected nothing.
   *
   * <p>Only called when that happened, because it is the one case the summary cannot explain on its
   * own. Each facet is counted separately - that separates "the model does not carry this merkmal
   * at all" from "it carries it with another value" and "with the right value in another type" -
   * and the elements satisfying the most facets are named with what kept them out.
   */
  private static Diagnosis diagnose(
      IdsSpecification specification, Collection<ModelElement> model) {

    List<FacetMatch> facets = new ArrayList<>();

    EntityRequirement entity = specification.applicableEntity();
    if (entity != null) {
      int matched = (int) model.stream().filter(entity::matches).count();
      facets.add(new FacetMatch("IFC-Entity " + entity.describe(), matched, model.size()));
    }

    for (PropertyRequirement facet : specification.applicability()) {
      int matched = 0;
      int addressFound = 0;
      int valueButNotType = 0;
      Set<String> typeDifferences = new TreeSet<>();

      for (ModelElement element : model) {
        Optional<ModelProperty> found = element.findWithAliases(facet.key());
        if (found.isEmpty()) {
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
          new FacetMatch(
              facet.describe(),
              matched,
              addressFound,
              valueButNotType,
              String.join(", ", typeDifferences)));
    }

    return new Diagnosis(facets, nearMisses(specification, model));
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
  private static List<String> divergences(
      List<PropertyRequirement> facets, ModelElement element) {

    List<String> divergences = new ArrayList<>();
    for (PropertyRequirement facet : facets) {
      Optional<ModelProperty> found = element.findWithAliases(facet.key());
      if (found.isEmpty()) {
        divergences.add(facet.key() + ": Merkmal fehlt");
        continue;
      }

      ModelProperty property = found.get();
      List<String> differences = new ArrayList<>();
      if (!valueMatches(facet, property)) {
        differences.add("'" + property.value() + "' statt '" + facet.expectedValue() + "'");
      }
      if (!typeMatches(facet, property)) {
        differences.add(
            "Datentyp " + DataTypes.difference(facet.dataType(), property.dataType()));
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
    return !facet.hasExpectedValue()
        || property.values().stream().anyMatch(value -> sameValue(facet.expectedValue(), value));
  }

  /** The facet's data type exactly, or no data type asked for. */
  private static boolean typeMatches(PropertyRequirement facet, ModelProperty property) {
    return !facet.hasDataType() || DataTypes.identical(facet.dataType(), property.dataType());
  }

  /** A different data type of the same family, which only the hint accepts. */
  private static boolean typeRelated(PropertyRequirement facet, ModelProperty property) {
    return facet.hasDataType() && DataTypes.related(facet.dataType(), property.dataType());
  }

  private static Optional<Finding> checkEntity(ModelElement element, EntityRequirement entity) {
    if (entity == null) {
      return Optional.empty();
    }

    if (!entity.matchesName(element.entityName())) {
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

    String detail = element.entityName() + " ist zulässig";
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
      return Optional.of(
          finding(element, requirement, Status.MISSING, "nicht gefunden unter " + searched));
    }

    ModelProperty property = found.get();
    String alias = aliasNote(requirement, property);

    if (!valueMatches(requirement, property)) {
      return Optional.of(
          finding(
              element,
              requirement,
              Status.WRONG_VALUE,
              "gefunden: '" + property.value() + "'" + alias));
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
      return finding(element, requirement, Status.FULFILLED, "nicht vorhanden, wie gefordert");
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

  /**
   * Compare a required value with a model value.
   *
   * <p>Numbers are compared numerically where both sides parse, so a required {@code 0} and a stored
   * {@code 0.} do not read as a deviation.
   */
  private static boolean sameValue(String required, String actual) {
    if (required == null || actual == null) {
      return required == null && actual == null;
    }
    if (required.trim().equalsIgnoreCase(actual.trim())) {
      return true;
    }
    try {
      return Double.compare(Double.parseDouble(required.trim()), Double.parseDouble(actual.trim()))
          == 0;
    } catch (NumberFormatException notNumbers) {
      return false;
    }
  }
}
