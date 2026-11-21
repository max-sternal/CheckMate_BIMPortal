package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.FENSTER;
import static com.bimportal.hackathon.ids.IdsFixtures.KAPPE_LPH_8_9;
import static com.bimportal.hackathon.ids.IdsFixtures.SCHAFT_LPH_1_9;
import static com.bimportal.hackathon.ids.IdsFixtures.specification;
import static com.bimportal.hackathon.ids.IdsFixtures.specifications;
import static com.bimportal.hackathon.ids.IdsFixtures.typedElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bimportal.hackathon.ids.IdsSpecification.Cardinality;
import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

/** Reads real IDS files from the BIM Portal, see {@code src/test/resources/ids/portal/}. */
class IdsParserTest {

  @Test
  void readsOneSpecificationPerSchema() throws Exception {
    List<IdsSpecification> specifications = specifications(KAPPE_LPH_8_9);

    assertThat(specifications)
        .extracting(IdsSpecification::ifcVersion)
        .containsExactly("IFC4", "IFC4X3_ADD2", "IFC2X3");
    assertThat(specifications)
        .extracting(IdsSpecification::name)
        .containsOnly("Ueberbau > Kappe (Ingenieurbau): LPH 8-9");
  }

  @Test
  void readsTheClassificationTheApplicabilitySelectsOn() throws Exception {
    IdsSpecification kappe = specification(KAPPE_LPH_8_9, "IFC2X3");

    assertThat(kappe.applicableEntity()).isNull();
    assertThat(kappe.applicability())
        .extracting(
            r -> r.key().toString(),
            PropertyRequirement::expectedValue,
            PropertyRequirement::dataType,
            PropertyRequirement::cardinality)
        .containsExactly(
            tuple("Klassifizierung/Objektgruppe", "Ueberbau", "IFCLABEL", Cardinality.REQUIRED),
            tuple("Klassifizierung/Objektklasse", "Kappe", "IFCLABEL", Cardinality.REQUIRED),
            tuple("Klassifizierung/Objekttyp", "Kappe", "IFCLABEL", Cardinality.REQUIRED));
  }

  @Test
  void readsRequirementsWithAndWithoutValue() throws Exception {
    IdsSpecification kappe = specification(KAPPE_LPH_8_9, "IFC2X3");

    assertThat(kappe.requirements())
        .extracting(PropertyRequirement::describe, PropertyRequirement::dataType)
        .containsExactly(
            tuple("Bauteilinformation/Feldnummer", "IFCINTEGER"),
            tuple("Klassifizierung/Objektgruppe = 'Ueberbau'", "IFCLABEL"),
            tuple("Klassifizierung/Objektklasse = 'Kappe'", "IFCLABEL"),
            tuple("Klassifizierung/Objekttyp = 'Kappe'", "IFCLABEL"));

    PropertyRequirement feldnummer = kappe.requirements().get(0);
    assertThat(feldnummer.hasExpectedValue()).isFalse();
    assertThat(feldnummer.cardinality()).isEqualTo(Cardinality.REQUIRED);
  }

  @Test
  void readsEveryCardinality() throws Exception {
    // The portal only writes "required"; the other two come from hand-written IDS files.
    String ids =
        "<ids xmlns=\"http://standards.buildingsmart.org/IDS\"><specifications>"
            + "<specification name=\"Test\" ifcVersion=\"IFC4\"><applicability/><requirements>"
            + propertyFacet("Pflicht", null)
            + propertyFacet("Optional", "optional")
            + propertyFacet("Verboten", "prohibited")
            + propertyFacet("Ausdruecklich", "required")
            + "</requirements></specification></specifications></ids>";

    IdsSpecification specification =
        IdsParser.parse(ids.getBytes(StandardCharsets.UTF_8)).get(0);

    assertThat(specification.requirements())
        .extracting(r -> r.key().baseName(), PropertyRequirement::cardinality)
        .containsExactly(
            tuple("Pflicht", Cardinality.REQUIRED),
            tuple("Optional", Cardinality.OPTIONAL),
            tuple("Verboten", Cardinality.PROHIBITED),
            tuple("Ausdruecklich", Cardinality.REQUIRED));
    assertThat(specification.requirements().get(2).describe())
        .isEqualTo("Pset_Test/Verboten (unzulässig)");
  }

  @Test
  void entityGivenAsSingleNameMatchesThatEntityIgnoringCase() throws Exception {
    EntityRequirement entity = specification(KAPPE_LPH_8_9, "IFC2X3").requiredEntity();

    assertThat(entity.source()).isEqualTo("IFCBEAM");
    assertThat(entity.hasPredefinedType()).isFalse();
    assertThat(entity.matchesName("IfcBeam")).isTrue();
    assertThat(entity.matchesName("IFCBEAM")).isTrue();
    assertThat(entity.matchesName("IfcBuildingElementProxy")).isFalse();
    assertThat(entity.matchesName("IfcBeamStandardCase")).isFalse();
  }

  @Test
  void entityGivenAsPatternMatchesEveryListedEntity() throws Exception {
    assertThat(
            specification(KAPPE_LPH_8_9, "IFC4")
                .requiredEntity()
                .matchesName("IfcBeamStandardCase"))
        .isTrue();

    EntityRequirement schaft = specification(SCHAFT_LPH_1_9, "IFC2X3").requiredEntity();
    assertThat(schaft.matchesName("IfcColumn")).isTrue();
    assertThat(schaft.matchesName("IfcBuildingElementProxy")).isTrue();
  }

  @Test
  void readsThePredefinedTypeOfAnEntity() throws Exception {
    // Only the IFC4X3 block names one: Kappen are IfcBeam with PredefinedType CORNICE there.
    EntityRequirement entity = specification(KAPPE_LPH_8_9, "IFC4X3_ADD2").requiredEntity();

    assertThat(entity.predefinedTypeSource()).isEqualTo("CORNICE");
    assertThat(entity.describe()).isEqualTo("IFCBEAM mit PredefinedType CORNICE");
    assertThat(entity.matches(typedElement("a", "IfcBeam", "CORNICE", false, List.of()))).isTrue();
    assertThat(entity.matches(typedElement("b", "IfcBeam", "BEAM", false, List.of()))).isFalse();
    assertThat(entity.matches(typedElement("c", "IfcBeam", null, false, List.of()))).isFalse();
  }

  @Test
  void readsAnApplicabilityThatSelectsByEntity() throws Exception {
    List<IdsSpecification> specifications = specifications(FENSTER);
    assertThat(specifications).extracting(IdsSpecification::ifcVersion).containsExactly("IFC4");

    IdsSpecification fenster = specifications.get(0);
    assertThat(fenster.applicability()).isEmpty();
    assertThat(fenster.applicableEntity().matchesName("IfcWindow")).isTrue();
    assertThat(fenster.applicableEntity().matchesName("IfcWindowStandardCase")).isTrue();
    assertThat(fenster.applicableEntity().matchesName("IfcDoor")).isFalse();
    assertThat(fenster.applicableEntity().predefinedTypeSource()).isEqualTo("USERDEFINED");
    assertThat(fenster.requiredEntity()).isNull();
    assertThat(fenster.requirements())
        .extracting(PropertyRequirement::describe)
        .containsExactly(
            "Fenster/Fenster-ID",
            "Fenster/Fenster-Codierung",
            "Fenster/Raum-Codierung",
            "Fenster/Raum-ID");
  }

  @Test
  void picksTheSpecificationForTheModelSchema() throws Exception {
    List<IdsSpecification> specifications = specifications(KAPPE_LPH_8_9);

    assertThat(schemaPickedFor(specifications, "IFC2X3")).containsExactly("IFC2X3");
    assertThat(schemaPickedFor(specifications, "ifc2x3")).containsExactly("IFC2X3");
    assertThat(schemaPickedFor(specifications, "IFC4")).containsExactly("IFC4");
    assertThat(schemaPickedFor(specifications, "IFC4X3_ADD2")).containsExactly("IFC4X3_ADD2");
  }

  @Test
  void picksTheSpecificationAcrossAddendaAndCorrigenda() throws Exception {
    List<IdsSpecification> specifications = specifications(KAPPE_LPH_8_9);

    assertThat(schemaPickedFor(specifications, "IFC2X3_TC1")).containsExactly("IFC2X3");
    // IFC4X3 starts with IFC4, and the IFC4 block comes first in this file: a prefix test
    // would pick it. IFC4X3 is a schema of its own, so the IFC4X3_ADD2 block is the right one.
    assertThat(schemaPickedFor(specifications, "IFC4X3")).containsExactly("IFC4X3_ADD2");
  }

  @Test
  void picksNothingForAnUnknownOrMissingSchema() throws Exception {
    List<IdsSpecification> specifications = specifications(KAPPE_LPH_8_9);

    assertThat(schemaPickedFor(specifications, "IFC5")).isEmpty();
    assertThat(schemaPickedFor(specifications, "")).isEmpty();
    assertThat(schemaPickedFor(specifications, null)).isEmpty();
  }

  @Test
  void reducesSchemaNamesToTheirBaseSchema() {
    assertThat(IdsSpecification.baseSchema("IFC4X3_ADD2")).isEqualTo("IFC4X3");
    assertThat(IdsSpecification.baseSchema("IFC4X3")).isEqualTo("IFC4X3");
    assertThat(IdsSpecification.baseSchema("IFC2X3_TC1")).isEqualTo("IFC2X3");
    assertThat(IdsSpecification.baseSchema(" ifc4 ")).isEqualTo("IFC4");
  }

  @Test
  void rejectsDocumentsWithDoctype() {
    // IDS files come over the network; a DOCTYPE is how entity-expansion attacks get in.
    byte[] withDoctype =
        ("<?xml version=\"1.0\"?><!DOCTYPE ids [<!ENTITY x \"y\">]>"
                + "<ids xmlns=\"http://standards.buildingsmart.org/IDS\"/>")
            .getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> IdsParser.parse(withDoctype))
        .isInstanceOf(RequirementsException.class)
        .hasCauseInstanceOf(SAXParseException.class);
  }

  private static List<String> schemaPickedFor(
      List<IdsSpecification> specifications, String schema) {
    return specifications.stream()
        .filter(specification -> specification.covers(schema))
        .map(IdsSpecification::ifcVersion)
        .toList();
  }

  /** A {@code <property>} facet in {@code Pset_Test}, with a cardinality attribute or without. */
  private static String propertyFacet(String name, String cardinality) {
    String attribute = cardinality == null ? "" : " cardinality=\"" + cardinality + "\"";
    return "<property"
        + attribute
        + "><propertySet><simpleValue>Pset_Test</simpleValue></propertySet>"
        + "<baseName><simpleValue>"
        + name
        + "</simpleValue></baseName></property>";
  }
}
