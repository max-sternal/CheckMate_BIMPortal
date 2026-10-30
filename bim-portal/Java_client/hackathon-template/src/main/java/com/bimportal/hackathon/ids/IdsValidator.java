package com.bimportal.hackathon.ids;

import com.bimportal.hackathon.ids.CheckResult.Diagnosis;
import com.bimportal.hackathon.ids.CheckResult.FacetMatch;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.NearMiss;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Checks model elements against one IDS specification.
 *
 * <p>Pure: no IFC toolbox, no network, no UI. Hand a specification and a list of elements in and a
 * {@link CheckResult} comes back, which is what makes this the part worth testing.
 */
public final class IdsValidator {

  /** Listed near misses: enough to recognise the pattern, few enough to read. */
  private static final int NEAR_MISSES = 5;

  private IdsValidator() {}

  /** Run one specification against a model. */
  public static CheckResult check(IdsSpecification specification, Collection<ModelElement> model) {
    List<ModelElement> applicable =
        model.stream().filter(element -> applies(specification, element)).toList();

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
        applicable.isEmpty() ? diagnose(specification, model) : Diagnosis.NONE);
  }

  /**
   * Whether a specification selects an element.
   *
   * <p>All applicability facets have to hold - that is IDS's AND semantics. Only presence and value
   * are considered here, not the data type: a type mismatch should surface as a finding on an
   * element that was checked, not quietly drop the element out of scope.
   */
  private static boolean applies(IdsSpecification specification, ModelElement element) {
    EntityRequirement entity = specification.applicableEntity();
    if (entity != null && !entity.matches(element.entityName())) {
      return false;
    }

    // One definition of "this facet holds", shared with the diagnosis, so the reason a report
    // gives can never drift away from the selection it explains.
    for (PropertyRequirement requirement : specification.applicability()) {
      if (element.findWithAliases(requirement.key()).stream()
          .noneMatch(property -> satisfies(requirement, property))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Work out why the applicability selected nothing.
   *
   * <p>Only called when that happened, because it is the one case the summary cannot explain on its
   * own. Each facet is counted separately - that separates "the model does not carry this merkmal
   * at all" from "it carries it with another value" - and the elements satisfying the most facets
   * are named with the values that kept them out.
   */
  private static Diagnosis diagnose(
      IdsSpecification specification, Collection<ModelElement> model) {

    List<FacetMatch> facets = new ArrayList<>();

    EntityRequirement entity = specification.applicableEntity();
    if (entity != null) {
      int matched = (int) model.stream().filter(e -> entity.matches(e.entityName())).count();
      facets.add(new FacetMatch("IFC-Entity " + entity.source(), matched, model.size()));
    }

    for (PropertyRequirement facet : specification.applicability()) {
      int matched = 0;
      int addressFound = 0;
      for (ModelElement element : model) {
        Optional<ModelProperty> found = element.findWithAliases(facet.key());
        if (found.isEmpty()) {
          continue;
        }
        addressFound++;
        if (satisfies(facet, found.get())) {
          matched++;
        }
      }
      facets.add(new FacetMatch(facet.describe(), matched, addressFound));
    }

    return new Diagnosis(facets, nearMisses(specification, model));
  }

  /**
   * The elements that satisfied the most applicability facets, with what diverged.
   *
   * <p>Only the best-scoring elements are listed: every element below that score diverges in the
   * same way and more, so naming them adds length rather than information.
   */
  private static List<NearMiss> nearMisses(
      IdsSpecification specification, Collection<ModelElement> model) {

    List<PropertyRequirement> facets = specification.applicability();
    if (facets.isEmpty()) {
      return List.of();
    }

    int best = model.stream().mapToInt(element -> score(facets, element)).max().orElse(0);
    if (best == 0) {
      return List.of();
    }

    List<NearMiss> nearMisses = new ArrayList<>();
    for (ModelElement element : model) {
      if (nearMisses.size() >= NEAR_MISSES) {
        break;
      }
      if (score(facets, element) == best) {
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

  /** How many applicability facets hold for an element. */
  private static int score(List<PropertyRequirement> facets, ModelElement element) {
    int score = 0;
    for (PropertyRequirement facet : facets) {
      if (element.findWithAliases(facet.key()).filter(p -> satisfies(facet, p)).isPresent()) {
        score++;
      }
    }
    return score;
  }

  /** One line per facet that kept an element out, naming what it carries instead. */
  private static List<String> divergences(
      List<PropertyRequirement> facets, ModelElement element) {

    List<String> divergences = new ArrayList<>();
    for (PropertyRequirement facet : facets) {
      Optional<ModelProperty> found = element.findWithAliases(facet.key());

      if (found.isEmpty()) {
        divergences.add(facet.key() + ": Merkmal fehlt");
      } else if (!satisfies(facet, found.get())) {
        ModelProperty property = found.get();
        String where =
            PropertyAliases.isAlias(facet.key(), property.key()) ? " (als " + property.key() + ")" : "";
        divergences.add(
            facet.key()
                + ": '"
                + property.value()
                + "' statt '"
                + facet.expectedValue()
                + "'"
                + where);
      }
    }
    return divergences;
  }

  /**
   * Whether a property satisfies an applicability facet.
   *
   * <p>Presence and value only, not the data type - see {@link #applies}.
   */
  private static boolean satisfies(PropertyRequirement facet, ModelProperty property) {
    return !facet.hasExpectedValue() || sameValue(facet.expectedValue(), property.value());
  }

  private static Optional<Finding> checkEntity(ModelElement element, EntityRequirement entity) {
    if (entity == null) {
      return Optional.empty();
    }

    boolean matches = entity.matches(element.entityName());
    return Optional.of(
        new Finding(
            element.id(),
            element.label(),
            "IFC-Entity",
            matches ? Status.FULFILLED : Status.WRONG_ENTITY,
            matches
                ? element.entityName() + " ist zulässig"
                : element.entityName() + " steht nicht in der zulässigen Liste"));
  }

  private static Optional<Finding> checkProperty(
      ModelElement element, PropertyRequirement requirement) {
    Optional<ModelProperty> found = element.findWithAliases(requirement.key());

    if (found.isEmpty()) {
      // An optional requirement only has to hold for elements that carry the property.
      if (!requirement.required()) {
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
    String where = foundUnder(requirement, property);

    if (requirement.hasExpectedValue() && !sameValue(requirement.expectedValue(), property.value())) {
      return Optional.of(
          finding(
              element,
              requirement,
              Status.WRONG_VALUE,
              "gefunden: '" + property.value() + "'" + where));
    }

    if (requirement.hasDataType()
        && !DataTypes.compatible(requirement.dataType(), property.dataType())) {
      return Optional.of(
          finding(
              element,
              requirement,
              Status.WRONG_DATATYPE,
              "gefordert "
                  + DataTypes.normalize(requirement.dataType())
                  + ", gefunden "
                  + DataTypes.normalize(property.dataType())
                  + where));
    }

    return Optional.of(
        finding(element, requirement, Status.FULFILLED, "gefunden: " + property + where));
  }

  /**
   * Where and how the property turned up, when that differs from what the LOIN asked for.
   *
   * <p>Both differences are worth stating even on a fulfilled requirement: they are the reason it
   * was fulfilled, and someone reading the report should see that an alias or a related data type
   * was accepted rather than an exact match.
   */
  private static String foundUnder(PropertyRequirement requirement, ModelProperty property) {
    List<String> notes = new ArrayList<>();
    if (PropertyAliases.isAlias(requirement.key(), property.key())) {
      notes.add("als " + property.key());
    }
    if (requirement.hasDataType()
        && !DataTypes.identical(requirement.dataType(), property.dataType())
        && DataTypes.compatible(requirement.dataType(), property.dataType())) {
      notes.add(
          "Datentyp "
              + DataTypes.normalize(property.dataType())
              + " statt "
              + DataTypes.normalize(requirement.dataType()));
    }
    return notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")";
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
