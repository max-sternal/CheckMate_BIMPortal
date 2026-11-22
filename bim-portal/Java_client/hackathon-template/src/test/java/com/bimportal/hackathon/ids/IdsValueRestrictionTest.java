package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.LABEL;
import static com.bimportal.hackathon.ids.IdsFixtures.element;
import static com.bimportal.hackathon.ids.IdsFixtures.property;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for XSD {@code <xs:restriction>} facets on IDS property values: {@code pattern}, numeric
 * bounds ({@code minInclusive}, {@code maxInclusive}, {@code minExclusive}, {@code maxExclusive}),
 * and string lengths ({@code length}, {@code minLength}, {@code maxLength}).
 */
class IdsValueRestrictionTest {

  // === Pattern ===

  @Test
  void evaluatesAnchoredPatternOnPropertyValue() {
    IdsSpecification spec =
        requirementSpec(
            "Kennung",
            "IFCLABEL",
            null,
            "<xs:restriction base=\"xs:string\"><xs:pattern value=\"[A-Z]{2}-[0-9]{3}\"/></xs:restriction>");

    assertThat(spec.unsupported().isEmpty()).isTrue();
    PropertyRequirement req = spec.requirements().get(0);
    assertThat(req.hasValueConstraint()).isTrue();
    assertThat(req.describe()).isEqualTo("Pset_Test/Kennung (Muster '[A-Z]{2}-[0-9]{3}')");

    ModelElement ok =
        element("ok", "IfcWall", List.of(property("Pset_Test", "Kennung", "AB-123", LABEL)));
    // Must be full-match, not substring match.
    ModelElement partial =
        element("partial", "IfcWall", List.of(property("Pset_Test", "Kennung", "XAB-123Y", LABEL)));

    CheckResult result = IdsValidator.check(spec, List.of(ok, partial));
    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(tuple("ok", Status.FULFILLED), tuple("partial", Status.WRONG_VALUE));
    assertThat(result.findings().get(1).detail())
        .isEqualTo("gefunden: 'XAB-123Y' (entspricht nicht dem Muster '[A-Z]{2}-[0-9]{3}')");
  }

  @Test
  void evaluatesMultiplePatternsAsAlternatives() {
    IdsSpecification spec =
        requirementSpec(
            "Brandschutz",
            "IFCLABEL",
            null,
            "<xs:restriction base=\"xs:string\">"
                + "<xs:pattern value=\"REI[0-9]{2}\"/>"
                + "<xs:pattern value=\"F[0-9]{2}\"/>"
                + "</xs:restriction>");

    assertThat(spec.unsupported().isEmpty()).isTrue();
    ModelElement rei =
        element("rei", "IfcWall", List.of(property("Pset_Test", "Brandschutz", "REI60", LABEL)));
    ModelElement f30 =
        element("f30", "IfcWall", List.of(property("Pset_Test", "Brandschutz", "F30", LABEL)));
    ModelElement bad =
        element("bad", "IfcWall", List.of(property("Pset_Test", "Brandschutz", "T30", LABEL)));

    CheckResult result = IdsValidator.check(spec, List.of(rei, f30, bad));
    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(
            tuple("rei", Status.FULFILLED),
            tuple("f30", Status.FULFILLED),
            tuple("bad", Status.WRONG_VALUE));
    assertThat(result.findings().get(2).detail())
        .contains("entspricht keinem der Muster 'REI[0-9]{2}' | 'F[0-9]{2}'");
  }

  @Test
  void handlesInvalidRegexPatternCleanlyWithoutCrashing() {
    IdsSpecification spec =
        requirementSpec(
            "Code",
            "IFCLABEL",
            null,
            "<xs:restriction base=\"xs:string\"><xs:pattern value=\"[unclosed\"/></xs:restriction>");

    assertThat(spec.unsupported().requirements())
        .containsExactly(
            "Merkmal Pset_Test/Code: Wertvorgabe mit einem Muster, das sich nicht lesen lässt"
                + " ([unclosed) - geprüft wird nur, ob es vorhanden ist");
    assertThat(spec.requirements().get(0).hasValueConstraint()).isFalse();
  }

  @Test
  void guardsAgainstCatastrophicBacktrackingInRegex() {
    IdsSpecification spec =
        requirementSpec(
            "Code",
            "IFCLABEL",
            null,
            "<xs:restriction base=\"xs:string\"><xs:pattern value=\"(.*a){20}\"/></xs:restriction>");

    ModelElement evil =
        element(
            "evil",
            "IfcWall",
            List.of(property("Pset_Test", "Code", "a".repeat(35) + "X", LABEL)));

    CheckResult result = IdsValidator.check(spec, List.of(evil));
    assertThat(result.findings()).hasSize(1);
    assertThat(result.findings().get(0).status()).isEqualTo(Status.WRONG_VALUE);
    assertThat(result.findings().get(0).detail()).contains("Musterprüfung abgebrochen");
  }

  // === Numeric bounds ===

  @Test
  void evaluatesInclusiveNumericBoundsWithBigDecimal() {
    IdsSpecification spec =
        requirementSpec(
            "Breite",
            "IFCREAL",
            null,
            "<xs:restriction base=\"xs:double\">"
                + "<xs:minInclusive value=\"0.5\"/>"
                + "<xs:maxInclusive value=\"2.5\"/>"
                + "</xs:restriction>");

    assertThat(spec.unsupported().isEmpty()).isTrue();
    assertThat(spec.requirements().get(0).describe())
        .isEqualTo("Pset_Test/Breite (>= 0.5, <= 2.5)");

    ModelElement minEdge =
        element("min", "IfcWall", List.of(property("Pset_Test", "Breite", "0.50", "IfcReal")));
    ModelElement maxEdge =
        element("max", "IfcWall", List.of(property("Pset_Test", "Breite", "2.5", "IfcReal")));
    ModelElement tooLow =
        element("low", "IfcWall", List.of(property("Pset_Test", "Breite", "0.4999", "IfcReal")));
    ModelElement tooHigh =
        element("high", "IfcWall", List.of(property("Pset_Test", "Breite", "2.51", "IfcReal")));
    ModelElement nonNumeric =
        element("nan", "IfcWall", List.of(property("Pset_Test", "Breite", "breit", "IfcReal")));

    CheckResult result =
        IdsValidator.check(spec, List.of(minEdge, maxEdge, tooLow, tooHigh, nonNumeric));
    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(
            tuple("min", Status.FULFILLED),
            tuple("max", Status.FULFILLED),
            tuple("low", Status.WRONG_VALUE),
            tuple("high", Status.WRONG_VALUE),
            tuple("nan", Status.WRONG_VALUE));
    assertThat(result.findings().get(2).detail()).contains("unterschreitet Minimum >= 0.5");
    assertThat(result.findings().get(3).detail()).contains("überschreitet Maximum <= 2.5");
    assertThat(result.findings().get(4).detail()).contains("ist kein gültiger Zahlenwert");
  }

  @Test
  void evaluatesExclusiveNumericBounds() {
    IdsSpecification spec =
        requirementSpec(
            "Temperatur",
            "IFCREAL",
            null,
            "<xs:restriction base=\"xs:double\">"
                + "<xs:minExclusive value=\"0\"/>"
                + "<xs:maxExclusive value=\"100\"/>"
                + "</xs:restriction>");

    assertThat(spec.unsupported().isEmpty()).isTrue();
    ModelElement atMin =
        element("atMin", "IfcWall", List.of(property("Pset_Test", "Temperatur", "0", "IfcReal")));
    ModelElement inside =
        element(
            "inside",
            "IfcWall",
            List.of(property("Pset_Test", "Temperatur", "0.0001", "IfcReal")));
    ModelElement atMax =
        element("atMax", "IfcWall", List.of(property("Pset_Test", "Temperatur", "100", "IfcReal")));

    CheckResult result = IdsValidator.check(spec, List.of(atMin, inside, atMax));
    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(
            tuple("atMin", Status.WRONG_VALUE),
            tuple("inside", Status.FULFILLED),
            tuple("atMax", Status.WRONG_VALUE));
    assertThat(result.findings().get(0).detail()).contains("muss größer als 0 sein");
    assertThat(result.findings().get(2).detail()).contains("muss kleiner als 100 sein");
  }

  @Test
  void reportsInvalidNumericBoundInUnsupported() {
    IdsSpecification spec =
        requirementSpec(
            "Breite",
            "IFCREAL",
            null,
            "<xs:restriction base=\"xs:double\"><xs:minInclusive value=\"abc\"/></xs:restriction>");

    assertThat(spec.unsupported().requirements())
        .containsExactly(
            "Merkmal Pset_Test/Breite: Wertvorgabe mit ungültigem xs:minInclusive (abc)"
                + " - geprüft wird nur, ob es vorhanden ist");
  }

  // === String lengths ===

  @Test
  void evaluatesExactLengthRestriction() {
    IdsSpecification spec =
        requirementSpec(
            "Kuerzel",
            "IFCLABEL",
            null,
            "<xs:restriction base=\"xs:string\"><xs:length value=\"4\"/></xs:restriction>");

    assertThat(spec.unsupported().isEmpty()).isTrue();
    ModelElement ok =
        element("ok", "IfcWall", List.of(property("Pset_Test", "Kuerzel", "ABCD", LABEL)));
    ModelElement shortVal =
        element("short", "IfcWall", List.of(property("Pset_Test", "Kuerzel", "AB", LABEL)));

    CheckResult result = IdsValidator.check(spec, List.of(ok, shortVal));
    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(tuple("ok", Status.FULFILLED), tuple("short", Status.WRONG_VALUE));
    assertThat(result.findings().get(1).detail()).contains("Länge 2 statt exakt 4 Zeichen");
  }

  @Test
  void evaluatesMinAndMaxLengthRestrictions() {
    IdsSpecification spec =
        requirementSpec(
            "Bezeichnung",
            "IFCLABEL",
            null,
            "<xs:restriction base=\"xs:string\">"
                + "<xs:minLength value=\"3\"/>"
                + "<xs:maxLength value=\"6\"/>"
                + "</xs:restriction>");

    assertThat(spec.unsupported().isEmpty()).isTrue();
    ModelElement tooShort =
        element("short", "IfcWall", List.of(property("Pset_Test", "Bezeichnung", "AB", LABEL)));
    ModelElement ok =
        element("ok", "IfcWall", List.of(property("Pset_Test", "Bezeichnung", "Wand01", LABEL)));
    ModelElement tooLong =
        element(
            "long", "IfcWall", List.of(property("Pset_Test", "Bezeichnung", "Wand0123", LABEL)));

    CheckResult result = IdsValidator.check(spec, List.of(tooShort, ok, tooLong));
    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(
            tuple("short", Status.WRONG_VALUE),
            tuple("ok", Status.FULFILLED),
            tuple("long", Status.WRONG_VALUE));
    assertThat(result.findings().get(0).detail())
        .contains("Länge 2 unterschreitet Mindestlänge 3 Zeichen");
    assertThat(result.findings().get(2).detail())
        .contains("Länge 8 überschreitet Maximallänge 6 Zeichen");
  }

  @Test
  void reportsNegativeLengthAsUnsupported() {
    IdsSpecification spec =
        requirementSpec(
            "Bezeichnung",
            "IFCLABEL",
            null,
            "<xs:restriction base=\"xs:string\"><xs:minLength value=\"-1\"/></xs:restriction>");

    assertThat(spec.unsupported().requirements())
        .containsExactly(
            "Merkmal Pset_Test/Bezeichnung: Wertvorgabe mit ungültigem xs:minLength (-1)"
                + " - geprüft wird nur, ob es vorhanden ist");
  }

  // === Combined restrictions, applicability, and prohibited ===

  @Test
  void evaluatesCombinedLengthAndPatternRestrictions() {
    IdsSpecification spec =
        requirementSpec(
            "BauteilId",
            "IFCLABEL",
            null,
            "<xs:restriction base=\"xs:string\">"
                + "<xs:minLength value=\"5\"/>"
                + "<xs:maxLength value=\"10\"/>"
                + "<xs:pattern value=\"[A-Z]+-[0-9]+\"/>"
                + "</xs:restriction>");

    assertThat(spec.unsupported().isEmpty()).isTrue();
    ModelElement ok =
        element("ok", "IfcWall", List.of(property("Pset_Test", "BauteilId", "BW-101", LABEL)));
    ModelElement failsBoth =
        element("bad", "IfcWall", List.of(property("Pset_Test", "BauteilId", "ab", LABEL)));

    CheckResult result = IdsValidator.check(spec, List.of(ok, failsBoth));
    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(tuple("ok", Status.FULFILLED), tuple("bad", Status.WRONG_VALUE));
    assertThat(result.findings().get(1).detail())
        .contains("entspricht nicht dem Muster '[A-Z]+-[0-9]+'")
        .contains("Länge 2 unterschreitet Mindestlänge 5 Zeichen");
  }

  @Test
  void evaluatesValueRestrictionInApplicabilityAndDiagnosesMismatch() {
    String xml =
        "<ids xmlns=\"http://standards.buildingsmart.org/IDS\""
            + " xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><specifications>"
            + "<specification name=\"Hohe Waende\" ifcVersion=\"IFC4\">"
            + "<applicability>"
            + "<property dataType=\"IFCREAL\">"
            + "<propertySet><simpleValue>Pset_Test</simpleValue></propertySet>"
            + "<baseName><simpleValue>Hoehe</simpleValue></baseName>"
            + "<value><xs:restriction base=\"xs:double\"><xs:minInclusive value=\"3.0\"/>"
            + "</xs:restriction></value>"
            + "</property>"
            + "</applicability>"
            + "<requirements>"
            + "<property><propertySet><simpleValue>Pset_Test</simpleValue></propertySet>"
            + "<baseName><simpleValue>Status</simpleValue></baseName></property>"
            + "</requirements>"
            + "</specification></specifications></ids>";

    IdsSpecification spec = IdsParser.parse(xml.getBytes(StandardCharsets.UTF_8)).get(0);
    assertThat(spec.unsupported().isEmpty()).isTrue();

    ModelElement tall =
        element(
            "tall",
            "IfcWall",
            List.of(
                property("Pset_Test", "Hoehe", "3.5", "IfcReal"),
                property("Pset_Test", "Status", "Neu", LABEL)));
    ModelElement shortWall =
        element("short", "IfcWall", List.of(property("Pset_Test", "Hoehe", "2.0", "IfcReal")));

    CheckResult mixed = IdsValidator.check(spec, List.of(tall, shortWall));
    assertThat(mixed.applicableElements()).isEqualTo(1);
    assertThat(mixed.passed()).isTrue();

    CheckResult noneApplicable = IdsValidator.check(spec, List.of(shortWall));
    assertThat(noneApplicable.notApplicable()).isTrue();
    assertThat(noneApplicable.diagnosis().facets().get(0).facet())
        .isEqualTo("Pset_Test/Hoehe (>= 3.0)");
  }

  @Test
  void evaluatesValueRestrictionOnProhibitedProperty() {
    IdsSpecification spec =
        requirementSpec(
            "Status",
            "IFCLABEL",
            "prohibited",
            "<xs:restriction base=\"xs:string\"><xs:pattern value=\"TEMP_.*\"/></xs:restriction>");

    assertThat(spec.unsupported().isEmpty()).isTrue();
    ModelElement forbidden =
        element("bad", "IfcWall", List.of(property("Pset_Test", "Status", "TEMP_01", LABEL)));
    ModelElement allowed =
        element("ok", "IfcWall", List.of(property("Pset_Test", "Status", "FINAL", LABEL)));

    CheckResult result = IdsValidator.check(spec, List.of(forbidden, allowed));
    assertThat(result.findings())
        .extracting(Finding::elementId, Finding::status)
        .containsExactly(
            tuple("bad", Status.PROHIBITED_PRESENT), tuple("ok", Status.FULFILLED));
  }

  private static IdsSpecification requirementSpec(
      String baseName, String dataType, String cardinality, String restrictionXml) {
    String dt = dataType == null ? "" : " dataType=\"" + dataType + "\"";
    String card = cardinality == null ? "" : " cardinality=\"" + cardinality + "\"";
    String xml =
        "<ids xmlns=\"http://standards.buildingsmart.org/IDS\""
            + " xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><specifications>"
            + "<specification name=\"Test\" ifcVersion=\"IFC4\"><applicability/><requirements>"
            + "<property"
            + dt
            + card
            + "><propertySet><simpleValue>Pset_Test</simpleValue></propertySet>"
            + "<baseName><simpleValue>"
            + baseName
            + "</simpleValue></baseName>"
            + "<value>"
            + restrictionXml
            + "</value></property>"
            + "</requirements></specification></specifications></ids>";
    return IdsParser.parse(xml.getBytes(StandardCharsets.UTF_8)).get(0);
  }
}
