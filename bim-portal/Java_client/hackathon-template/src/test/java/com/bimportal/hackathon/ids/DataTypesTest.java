package com.bimportal.hackathon.ids;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Strict type comparison, and the related types that only feed the hint. */
class DataTypesTest {

  @Test
  void identicalMeansTheSameTypeIgnoringCase() {
    assertThat(DataTypes.identical("IFCLABEL", "IfcLabel")).isTrue();
    assertThat(DataTypes.identical("IFCLABEL", "IfcText")).isFalse();
    assertThat(DataTypes.identical("IFCINTEGER", "IfcLabel")).isFalse();
  }

  @Test
  void anUnknownTypeIsNeverIdentical() {
    assertThat(DataTypes.identical("IFCLABEL", null)).isFalse();
    assertThat(DataTypes.identical(null, "IfcLabel")).isFalse();
    assertThat(DataTypes.identical(" ", " ")).isFalse();
  }

  @Test
  void relatedMeansADifferentTypeOfTheSameFamily() {
    assertThat(DataTypes.related("IFCLABEL", "IfcText")).isTrue();
    assertThat(DataTypes.related("IFCLENGTHMEASURE", "IfcPositiveLengthMeasure")).isTrue();
    assertThat(DataTypes.related("IFCLABEL", "IfcLabel")).as("identical is not related").isFalse();
    assertThat(DataTypes.related("IFCLABEL", "IfcInteger")).as("other family").isFalse();
    assertThat(DataTypes.related("IFCLABEL", null)).isFalse();
  }

  @Test
  void describesADifferenceTheWayReportsShowIt() {
    assertThat(DataTypes.difference("IFCLABEL", "IfcText")).isEqualTo("IFCTEXT statt IFCLABEL");
    assertThat(DataTypes.difference("IFCLABEL", null)).isEqualTo("ohne Typ statt IFCLABEL");
  }
}
