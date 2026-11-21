package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.FENSTER;
import static com.bimportal.hackathon.ids.IdsFixtures.KAPPE_LPH_8_9;
import static com.bimportal.hackathon.ids.IdsFixtures.LOCAL;
import static com.bimportal.hackathon.ids.IdsFixtures.SCHAFT_LPH_1_9;
import static com.bimportal.hackathon.ids.IdsFixtures.document;
import static com.bimportal.hackathon.ids.IdsFixtures.specifications;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bimportal.hackathon.ids.IdsSpecification.Cardinality;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reading IDS files that other tools wrote: several schemas per specification, other ways to give
 * a value, and parts CheckMate does not evaluate - which are listed, never dropped silently.
 */
class LocalIdsTest {

  @TempDir Path folder;

  @Test
  void readsTheTitleAndVersionOfTheFile() throws Exception {
    IdsDocument document = document(LOCAL);

    assertThat(document.title()).isEqualTo("CheckMate Testanforderungen");
    assertThat(document.version()).isEqualTo("1.2");
    assertThat(document.label()).isEqualTo("CheckMate Testanforderungen, Version 1.2");
    assertThat(document.specifications())
        .extracting(IdsSpecification::name)
        .containsExactly("Platten", "Waende", "Klassifiziert", "Keine Proxies", "Bruecken");
  }

  @Test
  void aSpecificationMayCoverSeveralSchemas() throws Exception {
    IdsSpecification platten = specification("Platten");

    assertThat(platten.ifcVersions()).containsExactly("IFC2X3", "IFC4");
    assertThat(platten.covers("IFC2X3")).isTrue();
    assertThat(platten.covers("IFC4")).isTrue();
    assertThat(platten.covers("IFC4X3")).isFalse();
  }

  @Test
  void picksEverySpecificationForASchemaInFileOrder() throws Exception {
    IdsDocument document = document(LOCAL);

    assertThat(document.forSchema("IFC2X3"))
        .extracting(IdsSpecification::name)
        .containsExactly("Platten");
    assertThat(document.forSchema("IFC4"))
        .extracting(IdsSpecification::name)
        .containsExactly("Platten", "Waende", "Klassifiziert", "Keine Proxies");
    assertThat(document.forSchema("IFC4X3"))
        .extracting(IdsSpecification::name)
        .containsExactly("Bruecken");
  }

  @Test
  void readsAnEntityGivenAsEnumeration() throws Exception {
    IdsSpecification waende = specification("Waende");

    assertThat(waende.applicableEntity().source()).isEqualTo("IFCWALL|IFCWALLSTANDARDCASE");
    assertThat(waende.applicableEntity().matchesName("IfcWall")).isTrue();
    assertThat(waende.applicableEntity().matchesName("IfcWallStandardCase")).isTrue();
    assertThat(waende.applicableEntity().matchesName("IfcWallElementedCase")).isFalse();
    assertThat(waende.unsupported().applicability()).isEmpty();
  }

  @Test
  void listsWhatTheRequirementsAskForBeyondWhatIsEvaluated() throws Exception {
    IdsSpecification waende = specification("Waende");

    assertThat(waende.unsupported().requirements())
        .containsExactly(
            "attribute-Facette (Name)",
            "Merkmal Pset_WallCommon/FireRating: Wertvorgabe als Muster oder Auswahl"
                + " (REI30|REI60) - geprüft wird nur, ob es vorhanden ist");
    // What can be evaluated of the property stays: it has to be there, as IFCLABEL.
    assertThat(waende.requirements())
        .extracting(PropertyRequirement::describe, PropertyRequirement::dataType)
        .containsExactly(tuple("Pset_WallCommon/FireRating", "IFCLABEL"));
  }

  @Test
  void listsWhatTheApplicabilitySelectsOnBeyondWhatIsEvaluated() throws Exception {
    assertThat(specification("Klassifiziert").unsupported().applicability())
        .containsExactly("classification-Facette (Uniclass 2015)");
    assertThat(specification("Keine Proxies").unsupported().applicability())
        .containsExactly("die Spezifikation verbietet anwendbare Elemente (maxOccurs=\"0\")");
    assertThat(specification("Platten").unsupported().isEmpty()).isTrue();
    assertThat(specification("Bruecken").unsupported().isEmpty()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {KAPPE_LPH_8_9, SCHAFT_LPH_1_9, FENSTER})
  void evaluatesEverythingThePortalWrites(String export) throws Exception {
    assertThat(specifications(export))
        .allSatisfy(specification -> assertThat(specification.unsupported().isEmpty()).isTrue());
  }

  @Test
  void dropsAPropertyWhoseNameIsAPattern() {
    IdsSpecification specification =
        requirementsOnly(
            "<property><propertySet><simpleValue>Pset_WallCommon</simpleValue></propertySet>"
                + "<baseName><xs:restriction><xs:pattern value=\"Fire.*\"/></xs:restriction>"
                + "</baseName></property>");

    assertThat(specification.requirements()).isEmpty();
    assertThat(specification.unsupported().requirements())
        .containsExactly("Merkmal mit Muster im Namen (Pset_WallCommon/Fire.*)");
  }

  @Test
  void dropsAProhibitedPropertyWhoseValueIsARestriction() {
    // Kept without its value, it would forbid every value instead of the listed ones.
    IdsSpecification specification =
        requirementsOnly(
            "<property cardinality=\"prohibited\">"
                + "<propertySet><simpleValue>Pset_WallCommon</simpleValue></propertySet>"
                + "<baseName><simpleValue>FireRating</simpleValue></baseName>"
                + "<value><xs:restriction><xs:enumeration value=\"F0\"/></xs:restriction></value>"
                + "</property>");

    assertThat(specification.requirements()).isEmpty();
    assertThat(specification.unsupported().requirements())
        .containsExactly(
            "Merkmal Pset_WallCommon/FireRating: Wertvorgabe als Muster oder Auswahl (F0)"
                + " - nicht geprüft");
  }

  @Test
  void keepsAnOptionalPropertyWhoseValueIsARange() {
    IdsSpecification specification =
        requirementsOnly(
            "<property cardinality=\"optional\" dataType=\"IFCREAL\">"
                + "<propertySet><simpleValue>Pset_Test</simpleValue></propertySet>"
                + "<baseName><simpleValue>Breite</simpleValue></baseName>"
                + "<value><xs:restriction base=\"xs:double\"><xs:minInclusive value=\"1\"/>"
                + "</xs:restriction></value></property>");

    assertThat(specification.requirements())
        .extracting(PropertyRequirement::cardinality, PropertyRequirement::hasExpectedValue)
        .containsExactly(tuple(Cardinality.OPTIONAL, false));
    assertThat(specification.unsupported().requirements())
        .containsExactly(
            "Merkmal Pset_Test/Breite: Wertvorgabe als Einschränkung mit xs:minInclusive"
                + " - geprüft wird nur, ob es vorhanden ist");
  }

  @Test
  void readsSeveralPatternsAsAlternatives() {
    IdsSpecification specification =
        requirementsOnly(
            "<entity><name><xs:restriction><xs:pattern value=\"IFCSLAB\"/>"
                + "<xs:pattern value=\"IFCPLATE.*\"/></xs:restriction></name></entity>");

    assertThat(specification.requiredEntity().matchesName("IfcSlab")).isTrue();
    assertThat(specification.requiredEntity().matchesName("IfcPlateStandardCase")).isTrue();
    assertThat(specification.requiredEntity().matchesName("IfcWall")).isFalse();
    assertThat(specification.unsupported().isEmpty()).isTrue();
  }

  @Test
  void listsAnEntityItCannotRead() {
    IdsSpecification specification =
        requirementsOnly(
            "<entity><name><xs:restriction><xs:minLength value=\"3\"/></xs:restriction>"
                + "</name></entity>");

    assertThat(specification.requiredEntity()).isNull();
    assertThat(specification.unsupported().requirements())
        .containsExactly("entity-Facette: Name als Einschränkung mit xs:minLength");
  }

  // Files

  @Test
  void readsAFileAndNamesItAsTheSource() throws Exception {
    Path file = folder.resolve("anforderungen.ids");
    Files.write(file, IdsFixtures.bytes(LOCAL));

    IdsDocument document = IdsParser.read(file);

    assertThat(document.source()).isEqualTo("anforderungen.ids");
    assertThat(document.describe())
        .isEqualTo("CheckMate Testanforderungen, Version 1.2 (anforderungen.ids)");
  }

  @Test
  void aFileWithoutTitleIsCalledByItsName() throws Exception {
    Path file =
        Files.writeString(
            folder.resolve("ohne-titel.ids"),
            "<ids xmlns=\"http://standards.buildingsmart.org/IDS\"><specifications/></ids>");

    assertThat(IdsParser.read(file).describe()).isEqualTo("ohne-titel.ids");
  }

  @Test
  void aMissingFileIsAnError() {
    assertThatThrownBy(() -> IdsParser.read(folder.resolve("fehlt.ids")))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("fehlt.ids")
        .hasMessageContaining("nicht gefunden");
  }

  @Test
  void aFileThatIsNoXmlIsAnError() throws Exception {
    Path file = Files.writeString(folder.resolve("notiz.ids"), "Das ist kein IDS.");

    assertThatThrownBy(() -> IdsParser.read(file))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("notiz.ids ist keine lesbare IDS-Datei");
  }

  @Test
  void anXmlFileThatIsNotIdsIsAnError() throws Exception {
    // The portal's other export, loinXML, is XML too - and easily picked by mistake.
    Path file = Files.writeString(folder.resolve("loin.xml"), "<loin><name>Kappe</name></loin>");

    assertThatThrownBy(() -> IdsParser.read(file))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("loin.xml ist keine IDS-Datei")
        .hasMessageContaining("<loin>");
  }

  private static IdsSpecification specification(String name) throws Exception {
    return document(LOCAL).specifications().stream()
        .filter(specification -> specification.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  /** A specification for IFC4 that applies to everything and requires the given facets. */
  private static IdsSpecification requirementsOnly(String facets) {
    String ids =
        "<ids xmlns=\"http://standards.buildingsmart.org/IDS\""
            + " xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><specifications>"
            + "<specification name=\"Test\" ifcVersion=\"IFC4\"><applicability/><requirements>"
            + facets
            + "</requirements></specification></specifications></ids>";
    return IdsParser.parse(ids.getBytes(StandardCharsets.UTF_8)).get(0);
  }
}
