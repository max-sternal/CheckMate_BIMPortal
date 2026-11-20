package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.FENSTER;
import static com.bimportal.hackathon.ids.IdsFixtures.INTEGER;
import static com.bimportal.hackathon.ids.IdsFixtures.KAPPE_LPH_8_9;
import static com.bimportal.hackathon.ids.IdsFixtures.LABEL;
import static com.bimportal.hackathon.ids.IdsFixtures.SCHAFT_LPH_1_9;
import static com.bimportal.hackathon.ids.IdsFixtures.TEXT;
import static com.bimportal.hackathon.ids.IdsFixtures.element;
import static com.bimportal.hackathon.ids.IdsFixtures.feldnummer;
import static com.bimportal.hackathon.ids.IdsFixtures.klassifizierung;
import static com.bimportal.hackathon.ids.IdsFixtures.objektinformation;
import static com.bimportal.hackathon.ids.IdsFixtures.property;
import static com.bimportal.hackathon.ids.IdsFixtures.specification;
import static com.bimportal.hackathon.ids.IdsFixtures.typedElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bimportal.hackathon.ids.CheckResult.FacetMatch;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.NearMiss;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.IdsSpecification.Cardinality;
import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The check against real portal specifications, with hand-built elements.
 *
 * <p>The cases follow a cross-check against ifctester (IfcOpenShell's IDS implementation) on a
 * copy of the bridge model, 2026-11-20, and ifctester's rules for data types, {@code prohibited}
 * and {@code predefinedType}. The data type rule is the one decided in docs/WORKFLOW.md, section 5:
 * strict, with a hint.
 */
class IdsValidatorTest {

  @Test
  void checksEveryRequirementOnAnApplicableElement() throws Exception {
    ModelElement kappe =
        element("kappe", "IfcBuildingElementProxy", ueberbauKappe(), feldnummer("1", INTEGER));

    CheckResult result = checkKappe(kappe);

    assertThat(result.applicableElements()).isEqualTo(1);
    assertThat(result.findings())
        .extracting(Finding::requirement, Finding::status)
        .containsExactly(
            tuple("IFC-Entity", Status.WRONG_ENTITY),
            tuple("Bauteilinformation/Feldnummer", Status.FULFILLED),
            tuple("Klassifizierung/Objektgruppe = 'Ueberbau'", Status.FULFILLED),
            tuple("Klassifizierung/Objektklasse = 'Kappe'", Status.FULFILLED),
            tuple("Klassifizierung/Objekttyp = 'Kappe'", Status.FULFILLED));
    assertThat(result.passed()).isFalse();
  }

  @Test
  void passesWhenEveryRequirementIsMet() throws Exception {
    ModelElement kappe = element("kappe", "IfcBeam", ueberbauKappe(), feldnummer("1", INTEGER));
    ModelElement schaft =
        element("schaft", "IfcColumn", klassifizierung("Ueberbau", "Pylon", "Schaft"));

    assertThat(checkKappe(kappe).passed()).isTrue();
    assertThat(checkSchaft(schaft).passed()).isTrue();
    assertThat(checkKappe(kappe).relatedTypes().isEmpty()).isTrue();
  }

  @Test
  void reportsAMissingProperty() throws Exception {
    ModelElement kappe = element("kappe", "IfcBeam", ueberbauKappe());

    Finding feldnummer = findingFor(kappe, "Bauteilinformation/Feldnummer");

    assertThat(feldnummer.status()).isEqualTo(Status.MISSING);
    assertThat(feldnummer.detail()).contains("Bauteilinformation/Feldnummer");
  }

  @Test
  void reportsAWrongDataType() throws Exception {
    ModelElement kappe = element("kappe", "IfcBeam", ueberbauKappe(), feldnummer("3", LABEL));

    Finding feldnummer = findingFor(kappe, "Bauteilinformation/Feldnummer");

    assertThat(feldnummer.status()).isEqualTo(Status.WRONG_DATATYPE);
    assertThat(feldnummer.detail()).isEqualTo("gefordert IFCINTEGER, gefunden IFCLABEL");
  }

  @Test
  void findsTheClassificationUnderTheModelsOwnNames() throws Exception {
    ModelElement kappe =
        element(
            "kappe",
            "IfcBeam",
            objektinformation("Ueberbau", "Kappe", "Kappe"),
            feldnummer("1", INTEGER));

    CheckResult result = checkKappe(kappe);

    assertThat(result.applicableElements()).isEqualTo(1);
    assertThat(result.passed()).isTrue();
    // A reader has to see that an alias was used, not the address the LOIN names.
    assertThat(findingFor(kappe, "Klassifizierung/Objektgruppe = 'Ueberbau'").detail())
        .contains("(als Pset_Objektinformation/_IDEbene1)");
  }

  @Test
  void checksOnlyTheElementsTheSpecificationSelects() throws Exception {
    ModelElement kappe = element("kappe", "IfcBeam", ueberbauKappe(), feldnummer("1", INTEGER));
    ModelElement fuge =
        element("fuge", "IfcBuildingElementProxy", objektinformation("Fuge", "Fuge", "Fuge"));

    CheckResult result = checkKappe(kappe, fuge);

    assertThat(result.examinedElements()).isEqualTo(2);
    assertThat(result.applicableElements()).isEqualTo(1);
    assertThat(result.findings()).extracting(Finding::elementId).containsOnly("kappe");
  }

  @Test
  void anUnselectedModelIsNotCheckableAndExplainsWhy() throws Exception {
    // How the bridge model classifies its Kappen: level 1 says Kappe, the LOIN wants Ueberbau.
    ModelElement kappe =
        element("kappe", "IfcBuildingElementProxy", objektinformation("Kappe", "Kappe", "Kappe"));

    CheckResult result = checkKappe(kappe);

    assertThat(result.notApplicable()).isTrue();
    assertThat(result.passed()).isFalse();
    assertThat(result.findings()).isEmpty();
    assertThat(result.summary()).contains("nicht prüfbar");

    assertThat(result.diagnosis().facets())
        .extracting(FacetMatch::facet, FacetMatch::matchedElements, FacetMatch::addressFound)
        .containsExactly(
            tuple("Klassifizierung/Objektgruppe = 'Ueberbau'", 0, 1),
            tuple("Klassifizierung/Objektklasse = 'Kappe'", 1, 1),
            tuple("Klassifizierung/Objekttyp = 'Kappe'", 1, 1));

    assertThat(result.diagnosis().nearMisses()).hasSize(1);
    NearMiss nearMiss = result.diagnosis().nearMisses().get(0);
    assertThat(nearMiss.matchedFacets()).isEqualTo(2);
    assertThat(nearMiss.totalFacets()).isEqualTo(3);
    assertThat(nearMiss.divergences())
        .containsExactly(
            "Klassifizierung/Objektgruppe: 'Kappe' statt 'Ueberbau'"
                + " (als Pset_Objektinformation/_IDEbene1)");
  }

  @Test
  void aModelWithoutTheClassificationHasNoNearMisses() throws Exception {
    CheckResult result = checkKappe(element("stab", "IfcReinforcingBar", List.of()));

    assertThat(result.notApplicable()).isTrue();
    assertThat(result.diagnosis().facets()).extracting(FacetMatch::addressFound).containsOnly(0);
    assertThat(result.diagnosis().nearMisses()).isEmpty();
  }

  @Test
  void anOptionalPropertyOnlyCountsWhereItExists() {
    PropertyRequirement optional = requirement("Hinweis", null, null, Cardinality.OPTIONAL);
    IdsSpecification specification = selectAll(List.of(optional));

    ModelElement without = element("ohne", "IfcWall", List.of());
    ModelElement with =
        element("mit", "IfcWall", List.of(property("Pset_Test", "Hinweis", "x", LABEL)));

    assertThat(IdsValidator.check(specification, List.of(without)).findings()).isEmpty();
    assertThat(IdsValidator.check(specification, List.of(with)).findings())
        .extracting(Finding::status)
        .containsExactly(Status.FULFILLED);
  }

  @Test
  void comparesNumbersAsNumbers() {
    PropertyRequirement zero = requirement("Wert", null, "0", Cardinality.REQUIRED);
    IdsSpecification specification = selectAll(List.of(zero));

    ModelElement stored =
        element("a", "IfcWall", List.of(property("Pset_Test", "Wert", "0.", null)));
    ModelElement other =
        element("b", "IfcWall", List.of(property("Pset_Test", "Wert", "0.5", null)));

    assertThat(IdsValidator.check(specification, List.of(stored, other)).findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(tuple("a", Status.FULFILLED), tuple("b", Status.WRONG_VALUE));
  }

  @Test
  void anEntityInTheApplicabilitySelectsByEntity() {
    IdsSpecification walls =
        new IdsSpecification(
            "Wände",
            "IFC4",
            List.of(),
            new EntityRequirement(Pattern.compile("IFCWALL", Pattern.CASE_INSENSITIVE), "IFCWALL"),
            List.of(),
            null);

    ModelElement wand = element("wand", "IfcWall", List.of());
    ModelElement tuer = element("tuer", "IfcDoor", List.of());

    assertThat(IdsValidator.check(walls, List.of(wand, tuer)).applicableElements()).isEqualTo(1);
  }

  // Data types: strict as in IDS, with a hint for related types.

  @Test
  void aRelatedTypeKeepsAnElementOutAndIsNamedInAHint() throws Exception {
    // The planted case from the ifctester cross-check: one Kappe stores Objektklasse as IfcText.
    ModelElement label =
        element("kappe-label", "IfcBeam", ueberbauKappe(), feldnummer("1", INTEGER));
    ModelElement text =
        element(
            "kappe-text",
            "IfcBeam",
            List.of(
                property("Klassifizierung", "Objektgruppe", "Ueberbau", LABEL),
                property("Klassifizierung", "Objektklasse", "Kappe", TEXT),
                property("Klassifizierung", "Objekttyp", "Kappe", LABEL)),
            feldnummer("2", INTEGER));

    CheckResult result = checkKappe(label, text);

    assertThat(result.applicableElements()).isEqualTo(1);
    assertThat(result.findings()).extracting(Finding::elementId).containsOnly("kappe-label");
    assertThat(result.relatedTypes().elementIds()).containsExactly("kappe-text");
    assertThat(result.relatedTypes().typeDifferences()).containsExactly("IFCTEXT statt IFCLABEL");
    assertThat(result.report())
        .contains(
            "   Hinweis: 1 weitere(s) Element(e) wären anwendbar, wenn verwandte Datentypen"
                + " akzeptiert würden (IFCTEXT statt IFCLABEL)");
  }

  @Test
  void rightValuesInARelatedTypeAreNotCheckableButExplained() throws Exception {
    // A BIM.Hamburg model with the right classification: own names, every value as IfcText.
    ModelElement kappe =
        element(
            "kappe",
            "IfcBuildingElementProxy",
            objektinformation("Ueberbau", "Kappe", "Kappe", TEXT));

    CheckResult result = checkKappe(kappe);

    assertThat(result.notApplicable()).isTrue();
    assertThat(result.relatedTypes().elementIds()).containsExactly("kappe");
    assertThat(result.diagnosis().facets())
        .extracting(
            FacetMatch::matchedElements,
            FacetMatch::valueButNotType,
            FacetMatch::typeDifferences)
        .containsOnly(tuple(0, 1, "IFCTEXT statt IFCLABEL"));
    assertThat(result.diagnosis().facets().get(0).line())
        .isEqualTo(
            "Klassifizierung/Objektgruppe = 'Ueberbau' trifft 0 - 1 Element(e) mit passendem"
                + " Wert, aber Datentyp IFCTEXT statt IFCLABEL");

    NearMiss nearMiss = result.diagnosis().nearMisses().get(0);
    assertThat(nearMiss.matchedFacets()).isEqualTo(3);
    assertThat(nearMiss.divergences())
        .hasSize(3)
        .allMatch(divergence -> divergence.contains("Datentyp IFCTEXT statt IFCLABEL"));
  }

  @Test
  void theBridgeModelsNearMissesNameBothValueAndType() throws Exception {
    // The real bridge model: the wrong first level, and every value as IfcText.
    ModelElement kappe =
        element(
            "kappe", "IfcBuildingElementProxy", objektinformation("Kappe", "Kappe", "Kappe", TEXT));

    CheckResult result = checkKappe(kappe);

    assertThat(result.relatedTypes().isEmpty()).isTrue();
    assertThat(result.diagnosis().facets())
        .extracting(
            FacetMatch::matchedElements, FacetMatch::addressFound, FacetMatch::valueButNotType)
        .containsExactly(tuple(0, 1, 0), tuple(0, 1, 1), tuple(0, 1, 1));

    NearMiss nearMiss = result.diagnosis().nearMisses().get(0);
    assertThat(nearMiss.matchedFacets()).isEqualTo(2);
    assertThat(nearMiss.divergences())
        .containsExactly(
            "Klassifizierung/Objektgruppe: 'Kappe' statt 'Ueberbau', Datentyp IFCTEXT statt"
                + " IFCLABEL (als Pset_Objektinformation/_IDEbene1)",
            "Klassifizierung/Objektklasse: Datentyp IFCTEXT statt IFCLABEL"
                + " (als Pset_Objektinformation/_IDEbene2)",
            "Klassifizierung/Objekttyp: Datentyp IFCTEXT statt IFCLABEL"
                + " (als Pset_Objektinformation/_IDEbene3)");
  }

  @Test
  void aRelatedTypeInTheRequirementsIsAFindingThatSaysSo() {
    PropertyRequirement name = requirement("Name", "IFCLABEL", null, Cardinality.REQUIRED);
    IdsSpecification specification = selectAll(List.of(name));

    ModelElement text =
        element("text", "IfcWall", List.of(property("Pset_Test", "Name", "x", TEXT)));
    ModelElement number =
        element("zahl", "IfcWall", List.of(property("Pset_Test", "Name", "1", INTEGER)));

    List<Finding> findings = IdsValidator.check(specification, List.of(text, number)).findings();

    assertThat(findings)
        .extracting(Finding::status, Finding::detail)
        .containsExactly(
            tuple(Status.WRONG_DATATYPE, "gefordert IFCLABEL, gefunden IFCTEXT - verwandter Typ"),
            tuple(Status.WRONG_DATATYPE, "gefordert IFCLABEL, gefunden IFCINTEGER"));
  }

  // Cardinality prohibited.

  @Test
  void aProhibitedPropertyIsMetWhenAbsentOrDifferent() {
    PropertyRequirement prohibited = requirement("Status", null, "Abriss", Cardinality.PROHIBITED);
    IdsSpecification specification = selectAll(List.of(prohibited));

    ModelElement absent = element("ohne", "IfcWall", List.of());
    ModelElement other =
        element("anders", "IfcWall", List.of(property("Pset_Test", "Status", "Bestand", LABEL)));
    ModelElement present =
        element("verboten", "IfcWall", List.of(property("Pset_Test", "Status", "Abriss", LABEL)));

    CheckResult result = IdsValidator.check(specification, List.of(absent, other, present));

    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::requirement, Finding::status)
        .containsExactly(
            tuple("ohne", "Pset_Test/Status = 'Abriss' (unzulässig)", Status.FULFILLED),
            tuple("anders", "Pset_Test/Status = 'Abriss' (unzulässig)", Status.FULFILLED),
            tuple(
                "verboten",
                "Pset_Test/Status = 'Abriss' (unzulässig)",
                Status.PROHIBITED_PRESENT));
    assertThat(result.passed()).isFalse();
  }

  @Test
  void aProhibitedPropertyWithoutValueForbidsEveryValue() {
    PropertyRequirement prohibited = requirement("Altlast", null, null, Cardinality.PROHIBITED);
    ModelElement present =
        element("mit", "IfcWall", List.of(property("Pset_Test", "Altlast", "nein", LABEL)));

    assertThat(IdsValidator.check(selectAll(List.of(prohibited)), List.of(present)).findings())
        .extracting(Finding::status)
        .containsExactly(Status.PROHIBITED_PRESENT);
  }

  // PredefinedType.

  @Test
  void checksThePredefinedTypeTheEntityRequires() throws Exception {
    // The IFC4X3 block of Kappe wants IfcBeam with PredefinedType CORNICE.
    IdsSpecification kappe = specification(KAPPE_LPH_8_9, "IFC4X3_ADD2");
    ModelProperty feld = feldnummer("1", INTEGER);
    ModelElement cornice =
        typedElement("gesims", "IfcBeam", "CORNICE", false, ueberbauKappe(), feld);
    ModelElement beam = typedElement("balken", "IfcBeam", "BEAM", false, ueberbauKappe(), feld);
    ModelElement unknown = element("unbekannt", "IfcBeam", ueberbauKappe(), feld);

    List<Finding> entityFindings =
        IdsValidator.check(kappe, List.of(cornice, beam, unknown)).findings().stream()
            .filter(finding -> finding.requirement().equals("IFC-Entity"))
            .toList();

    assertThat(entityFindings)
        .extracting(Finding::elementId, Finding::status, Finding::detail)
        .containsExactly(
            tuple("gesims", Status.FULFILLED, "IfcBeam ist zulässig (PredefinedType CORNICE)"),
            tuple(
                "balken", Status.WRONG_PREDEFINED_TYPE, "PredefinedType 'BEAM', gefordert CORNICE"),
            tuple(
                "unbekannt",
                Status.WRONG_PREDEFINED_TYPE,
                "PredefinedType nicht angegeben, gefordert CORNICE"));
  }

  @Test
  void userDefinedSelectsElementsWithAUserDefinedType() throws Exception {
    // Fenster selects windows whose type is user-defined, whatever that type is called.
    ModelElement custom = typedElement("eigen", "IfcWindow", "Dachfenster", true, List.of());
    ModelElement standard = typedElement("standard", "IfcWindow", "WINDOW", false, List.of());
    ModelElement door = typedElement("tuer", "IfcDoor", "Haustuer", true, List.of());

    CheckResult result =
        IdsValidator.check(specification(FENSTER, "IFC4"), List.of(custom, standard, door));

    assertThat(result.applicableElements()).isEqualTo(1);
    assertThat(result.findings()).extracting(Finding::elementId).containsOnly("eigen");
  }

  @Test
  void theDiagnosisNamesTheEntityWithItsPredefinedType() throws Exception {
    ModelElement standard = typedElement("standard", "IfcWindow", "WINDOW", false, List.of());

    CheckResult result = IdsValidator.check(specification(FENSTER, "IFC4"), List.of(standard));

    assertThat(result.notApplicable()).isTrue();
    assertThat(result.diagnosis().facets())
        .extracting(FacetMatch::facet, FacetMatch::matchedElements)
        .containsExactly(
            tuple("IFC-Entity IFCWINDOW|IFCWINDOWSTANDARDCASE mit PredefinedType USERDEFINED", 0));
  }

  /** The classification "Ueberbau > Kappe" asks for, under the portal's own names. */
  private static List<ModelProperty> ueberbauKappe() {
    return klassifizierung("Ueberbau", "Kappe", "Kappe");
  }

  /** Check elements against Ueberbau &gt; Kappe, LPH 8-9, IFC2X3 block. */
  private static CheckResult checkKappe(ModelElement... elements) throws Exception {
    return IdsValidator.check(specification(KAPPE_LPH_8_9, "IFC2X3"), List.of(elements));
  }

  /** Check elements against Ueberbau &gt; Schaft, LPH 1-9, IFC2X3 block. */
  private static CheckResult checkSchaft(ModelElement... elements) throws Exception {
    return IdsValidator.check(specification(SCHAFT_LPH_1_9, "IFC2X3"), List.of(elements));
  }

  /** The one finding for a requirement on the element, checked against Kappe. */
  private static Finding findingFor(ModelElement element, String requirement) throws Exception {
    return checkKappe(element).findings().stream()
        .filter(finding -> finding.requirement().equals(requirement))
        .findFirst()
        .orElseThrow();
  }

  /** A requirement on a property in {@code Pset_Test}. */
  private static PropertyRequirement requirement(
      String name, String dataType, String value, Cardinality cardinality) {
    return new PropertyRequirement(
        new PropertyKey("Pset_Test", name), dataType, value, cardinality);
  }

  /** A specification without applicability, so that it applies to every element. */
  private static IdsSpecification selectAll(List<PropertyRequirement> requirements) {
    return new IdsSpecification("Test", "IFC4", List.of(), null, requirements, null);
  }
}
