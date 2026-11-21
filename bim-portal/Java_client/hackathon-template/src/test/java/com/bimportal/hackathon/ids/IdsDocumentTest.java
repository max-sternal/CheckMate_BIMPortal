package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.LABEL;
import static com.bimportal.hackathon.ids.IdsFixtures.LOCAL;
import static com.bimportal.hackathon.ids.IdsFixtures.document;
import static com.bimportal.hackathon.ids.IdsFixtures.element;
import static com.bimportal.hackathon.ids.IdsFixtures.property;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.Status;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Checking a model against an IDS file with several specifications, some of which use parts of IDS
 * that CheckMate does not evaluate.
 */
class IdsDocumentTest {

  private static final ModelElement PLATTE =
      element(
          "platte",
          "IfcSlab",
          List.of(property("Pset_SlabCommon", "LoadBearing", "TRUE", "IfcBoolean")));

  private static final ModelElement WAND =
      element(
          "wand", "IfcWall", List.of(property("Pset_WallCommon", "FireRating", "REI90", LABEL)));

  private static final List<ModelElement> MODEL = List.of(PLATTE, WAND);

  @Test
  void checksEverySpecificationForTheSchema() throws Exception {
    List<CheckResult> results = document(LOCAL).check("IFC4", MODEL);

    assertThat(results)
        .extracting(result -> result.specification().name())
        .containsExactly("Platten", "Waende", "Klassifiziert", "Keine Proxies");
    assertThat(document(LOCAL).check("IFC2X3", MODEL)).hasSize(1);
  }

  @Test
  void aSpecificationForTwoSchemasIsCheckedOnEither() throws Exception {
    CheckResult ifc2x3 = document(LOCAL).check("IFC2X3", MODEL).get(0);
    CheckResult ifc4 = document(LOCAL).check("IFC4", MODEL).get(0);

    assertThat(ifc2x3.passed()).isTrue();
    assertThat(ifc4.passed()).isTrue();
    assertThat(ifc4.applicableElements()).isEqualTo(1);
  }

  @Test
  void aSpecificationWithUnevaluatedRequirementsCannotPass() throws Exception {
    // REI90 is not among the permitted REI30|REI60, but CheckMate cannot evaluate that list.
    CheckResult waende = result("Waende");

    assertThat(waende.applicableElements()).isEqualTo(1);
    assertThat(waende.findings()).extracting(Finding::status).containsOnly(Status.FULFILLED);
    assertThat(waende.complete()).isFalse();
    assertThat(waende.passed()).isFalse();
    assertThat(waende.summary()).endsWith("1 erfüllt, 2 Anforderung(en) nicht ausgewertet");
    assertThat(waende.report())
        .contains(
            "   Nicht ausgewertet (Anforderungen): attribute-Facette (Name)",
            "   Nicht ausgewertet (Anforderungen): Merkmal Pset_WallCommon/FireRating:"
                + " Wertvorgabe als Muster oder Auswahl (REI30|REI60) - geprüft wird nur, ob es"
                + " vorhanden ist");
  }

  @Test
  void aSpecificationWithUnevaluatedApplicabilityIsNotChecked() throws Exception {
    // Without its classification facet, "Klassifiziert" would apply to every element.
    CheckResult klassifiziert = result("Klassifiziert");

    assertThat(klassifiziert.notApplicable()).isTrue();
    assertThat(klassifiziert.findings()).isEmpty();
    assertThat(klassifiziert.passed()).isFalse();
    assertThat(klassifiziert.summary())
        .isEqualTo(
            "Klassifiziert (IFC4): 0 von 2 Elementen anwendbar - nicht prüfbar: die Anwendbarkeit"
                + " nutzt Teile von IDS, die CheckMate nicht auswertet");
    assertThat(klassifiziert.report())
        .contains("   Nicht ausgewertet (Anwendbarkeit): classification-Facette (Uniclass 2015)");
  }

  @Test
  void aSpecificationProhibitingElementsIsNotChecked() throws Exception {
    // maxOccurs="0" turns the meaning around; checking it as usual would report the opposite.
    assertThat(result("Keine Proxies").notApplicable()).isTrue();
    assertThat(result("Keine Proxies").notes())
        .containsExactly(
            "Nicht ausgewertet (Anwendbarkeit): die Spezifikation verbietet anwendbare Elemente"
                + " (maxOccurs=\"0\")");
  }

  @Test
  void aSchemaTheFileDoesNotCoverIsAnError() {
    assertThatThrownBy(() -> document(LOCAL).check("IFC5", MODEL))
        .isInstanceOf(RequirementsException.class)
        .hasMessageStartingWith("CheckMate Testanforderungen, Version 1.2: die IDS deckt")
        .hasMessageContaining("IFC2X3 IFC4");
  }

  private static CheckResult result(String name) throws Exception {
    return document(LOCAL).check("IFC4", MODEL).stream()
        .filter(result -> result.specification().name().equals(name))
        .findFirst()
        .orElseThrow();
  }
}
