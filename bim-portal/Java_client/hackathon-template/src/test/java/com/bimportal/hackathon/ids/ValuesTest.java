package com.bimportal.hackathon.ids;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Comparing values by their data type, with the outcomes ifctester 0.9's {@code facet.py} gives
 * ({@code cast_to_value}, {@code is_x}, plain {@code ==} for text).
 */
class ValuesTest {

  @Test
  void comparesTextExactlyIncludingCase() {
    assertThat(Values.same("Kappe", "Kappe", "IfcLabel")).isTrue();
    assertThat(Values.same("Kappe", "kappe", "IfcLabel")).isFalse();
    assertThat(Values.same("Kappe", "Kappe ", "IfcText")).isFalse();
    assertThat(Values.caseOnly("Kappe", "kappe")).isTrue();
    assertThat(Values.caseOnly("Kappe", "Kappe")).isFalse();
    assertThat(Values.caseOnly("Kappe", "Kap")).isFalse();
  }

  @Test
  void textThatLooksLikeANumberIsStillText() {
    assertThat(Values.same("42", "42.0", "IfcLabel")).isFalse();
    assertThat(Values.same("42", "42", "IfcIdentifier")).isTrue();
  }

  @Test
  void comparesIntegersAsNumbersWithoutTolerance() {
    assertThat(Values.same("42", "42", "IfcInteger")).isTrue();
    assertThat(Values.same("42.0", "42", "IFCINTEGER")).isTrue();
    assertThat(Values.same("1000000", "1000001", "IfcInteger")).isFalse();
  }

  @Test
  void comparesRealNumbersWithinARelativeToleranceOf1e6() {
    assertThat(Values.same("1.0", "1.0000005", "IfcReal")).isTrue();
    assertThat(Values.same("1.0", "1.00001", "IfcReal")).isFalse();
    assertThat(Values.same("2500", "2500.002", "IfcLengthMeasure")).isTrue();
    assertThat(Values.same("-2.0", "-2.000001", "IfcReal")).isTrue();
    assertThat(Values.same("-2.0", "-2.1", "IfcReal")).isFalse();
    // Zero leaves no room: 0 * (1 +- 1e-6) is 0.
    assertThat(Values.same("0", "0.0", "IfcReal")).isTrue();
    assertThat(Values.same("0", "1e-9", "IfcReal")).isFalse();
  }

  @Test
  void readsBooleansTheWayXmlSchemaWritesThem() {
    assertThat(Values.same("true", "true", "IfcBoolean")).isTrue();
    assertThat(Values.same("1", "true", "IfcBoolean")).isTrue();
    assertThat(Values.same("false", ".F.", "IfcBoolean")).isTrue();
    // "TRUE" is not an XML Schema boolean; ifctester's cast gives None, which matches nothing.
    assertThat(Values.same("TRUE", "true", "IfcBoolean")).isFalse();
    assertThat(Values.same("true", "UNKNOWN", "IfcLogical")).isFalse();
  }

  @Test
  void withoutDataTypeNumbersAreNumbersAndTheRestIsText() {
    assertThat(Values.same("0", "0.", null)).isTrue();
    assertThat(Values.same("Kappe", "Kappe", null)).isTrue();
    assertThat(Values.same("Kappe", "KAPPE", null)).isFalse();
    assertThat(Values.same(null, null, null)).isTrue();
    assertThat(Values.same("Kappe", null, "IfcLabel")).isFalse();
  }
}
