package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.FENSTER;
import static com.bimportal.hackathon.ids.IdsFixtures.LABEL;
import static com.bimportal.hackathon.ids.IdsFixtures.property;
import static com.bimportal.hackathon.ids.IdsFixtures.specification;
import static com.bimportal.hackathon.ids.IdsFixtures.typedElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bimportal.hackathon.ids.CheckResult.FacetMatch;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.IdsSpecification.Cardinality;
import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Where IDS, read the way ifctester reads it, decides details the portal's exports never touch:
 * case in values, tolerance for numbers, an element's entity through its type in IFC2X3, and how
 * the diagnosis words an entity facet with a PredefinedType.
 */
class IdsConformanceTest {

  // Values

  @Test
  void aValueInOtherCaseIsNotMetButSaysSo() {
    Finding finding =
        only(check("Status", LABEL, "Neu", property("Pset_Test", "Status", "neu", LABEL)));

    assertThat(finding.status()).isEqualTo(Status.WRONG_VALUE);
    assertThat(finding.detail())
        .isEqualTo("gefunden: 'neu' (nur Groß-/Kleinschreibung weicht ab)");
  }

  @Test
  void aRealNumberWithinTheToleranceIsMet() {
    Finding finding =
        only(
            check(
                "Breite",
                "IFCLENGTHMEASURE",
                "2.5",
                property("Pset_Test", "Breite", "2.5000001", "IfcLengthMeasure")));

    assertThat(finding.status()).isEqualTo(Status.FULFILLED);
  }

  @Test
  void aPropertyWithoutValueIsNoValue() {
    ModelElement.ModelProperty empty =
        new ModelElement.ModelProperty(new PropertyKey("Pset_Test", "Status"), "", LABEL);
    ModelElement.ModelProperty unknown =
        new ModelElement.ModelProperty(
            new PropertyKey("Pset_Test", "Geprueft"), "UNKNOWN", "IfcLogical");

    Finding status = only(check("Status", null, null, empty));
    Finding geprueft = only(check("Geprueft", null, null, unknown));

    assertThat(status.status()).isEqualTo(Status.MISSING);
    assertThat(status.detail()).isEqualTo("vorhanden, aber ohne Wert");
    assertThat(geprueft.status()).isEqualTo(Status.MISSING);
  }

  // IFC2X3: the entity through the type object

  @Test
  void inIfc2x3AnElementHasTheEntityOfItsType() {
    ModelElement proxy = typed("proxy", "IfcBuildingElementProxy", "IfcBeamType");

    CheckResult ifc2x3 = IdsValidator.check(beams(), List.of(proxy), "IFC2X3");
    CheckResult ifc4 = IdsValidator.check(beams(), List.of(proxy), "IFC4");

    assertThat(ifc2x3.applicableElements()).isEqualTo(1);
    assertThat(ifc2x3.findings())
        .extracting(Finding::status, Finding::detail)
        .containsExactly(
            tuple(
                Status.FULFILLED,
                "IfcBuildingElementProxy ist über seinen Typ IfcBeamType zulässig (IFC2X3)"));
    // Only IFC2X3 does this, as in ifctester.
    assertThat(ifc4.applicableElements()).isZero();
  }

  @Test
  void aStyleStandsForItsOccurrenceToo() {
    assertThat(IdsValidator.occurrenceOf("IfcBeamType")).isEqualTo("IfcBeam");
    assertThat(IdsValidator.occurrenceOf("IfcDoorStyle")).isEqualTo("IfcDoor");
    assertThat(IdsValidator.occurrenceOf("IFCWINDOWSTYLE")).isEqualTo("IFCWINDOW");
    assertThat(IdsValidator.occurrenceOf("IfcWall")).isEqualTo("IfcWall");
    assertThat(IdsValidator.occurrenceOf(null)).isNull();
  }

  @Test
  void anElementOfTheRightEntityNeedsNoType() {
    ModelElement beam = typed("beam", "IfcBeam", null);

    assertThat(IdsValidator.check(beams(), List.of(beam), "IFC2X3").findings())
        .extracting(Finding::detail)
        .containsExactly("IfcBeam ist zulässig");
  }

  // The diagnosis of an entity facet

  @Test
  void theDiagnosisSaysTheEntityIsThereButNotWithTheType() throws Exception {
    ModelElement window = typedElement("fenster", "IfcWindow", "WINDOW", false, List.of());
    ModelElement other = typedElement("fenster2", "IfcWindow", null, false, List.of());

    CheckResult result =
        IdsValidator.check(specification(FENSTER, "IFC4"), List.of(window, other), "IFC4");

    assertThat(result.diagnosis().facets())
        .extracting(FacetMatch::line)
        .containsExactly(
            "IFC-Entity IFCWINDOW|IFCWINDOWSTANDARDCASE mit PredefinedType USERDEFINED trifft 0"
                + " - 2 Element(e) dieser Entity, keines mit benutzerdefiniertem Typ"
                + " (gefunden: WINDOW 1, ohne 1)");
  }

  @Test
  void theDiagnosisSaysWhenTheEntityIsMissing() throws Exception {
    ModelElement door = typedElement("tuer", "IfcDoor", "DOOR", false, List.of());

    CheckResult result =
        IdsValidator.check(specification(FENSTER, "IFC4"), List.of(door), "IFC4");

    assertThat(result.diagnosis().facets())
        .extracting(FacetMatch::line)
        .containsExactly(
            "IFC-Entity IFCWINDOW|IFCWINDOWSTANDARDCASE mit PredefinedType USERDEFINED trifft 0"
                + " - kein Element dieser Entity im Modell");
  }

  /** IFCBEAM, selected and required. */
  private static IdsSpecification beams() {
    EntityRequirement beam =
        new EntityRequirement(Pattern.compile("IFCBEAM", Pattern.CASE_INSENSITIVE), "IFCBEAM");
    return new IdsSpecification("Balken", "IFC2X3", List.of(), beam, List.of(), beam);
  }

  private static ModelElement typed(String id, String entity, String typeEntity) {
    return new ModelElement(id, id, entity, null, false, List.of(), typeEntity);
  }

  /** One requirement in Pset_Test against one element that carries the given property. */
  private static CheckResult check(
      String name, String dataType, String value, ModelElement.ModelProperty property) {
    PropertyRequirement requirement =
        new PropertyRequirement(
            new PropertyKey("Pset_Test", name), dataType, value, Cardinality.REQUIRED);
    IdsSpecification specification =
        new IdsSpecification("Test", "IFC4", List.of(), null, List.of(requirement), null);
    return IdsValidator.check(
        specification, List.of(new ModelElement("e", "Element", "IfcWall", List.of(property))));
  }

  private static Finding only(CheckResult result) {
    assertThat(result.findings()).hasSize(1);
    return result.findings().get(0);
  }
}
