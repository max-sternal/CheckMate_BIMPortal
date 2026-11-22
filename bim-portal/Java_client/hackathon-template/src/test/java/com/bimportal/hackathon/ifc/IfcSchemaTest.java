package com.bimportal.hackathon.ifc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for bundled EXPRESS metadata tables loaded by {@link IfcSchema}. */
class IfcSchemaTest {

  @Test
  void resolvesSupportedSchemasAndNormalizesVariants() {
    IfcSchema ifc2x3 = IfcSchema.forDeclaredSchema("IFC2X3");
    IfcSchema ifc4 = IfcSchema.forDeclaredSchema("IFC4");
    IfcSchema ifc4x3 = IfcSchema.forDeclaredSchema("IFC4X3_ADD2");

    assertThat(ifc2x3).isNotNull();
    assertThat(ifc4).isNotNull();
    assertThat(ifc4x3).isNotNull();

    assertThat(IfcSchema.forDeclaredSchema("IFC2X3_TC1")).isSameAs(ifc2x3);
    assertThat(IfcSchema.forDeclaredSchema("ifc2x3")).isSameAs(ifc2x3);
    assertThat(IfcSchema.forDeclaredSchema("IFC4_ADD2")).isSameAs(ifc4);
    assertThat(IfcSchema.forDeclaredSchema("IFC4X3")).isSameAs(ifc4x3);
    assertThat(IfcSchema.forDeclaredSchema("IFC4X3_RC4")).isSameAs(ifc4x3);

    assertThat(IfcSchema.forDeclaredSchema("IFC4X1")).isNull();
    assertThat(IfcSchema.forDeclaredSchema("IFC4X2")).isNull();
    assertThat(IfcSchema.forDeclaredSchema("IFC2X3FOO")).isNull();
    assertThat(IfcSchema.forDeclaredSchema("IFC2X2_FINAL")).isNull();
    assertThat(IfcSchema.forDeclaredSchema("")).isNull();
    assertThat(IfcSchema.forDeclaredSchema(null)).isNull();
  }

  @Test
  void ifcProjectIsAnObjectOnlyInIfc2x3() {
    assertThat(IfcSchema.forDeclaredSchema("IFC2X3").isObject("IFCPROJECT")).isTrue();
    assertThat(IfcSchema.forDeclaredSchema("IFC4").isObject("IFCPROJECT")).isFalse();
    assertThat(IfcSchema.forDeclaredSchema("IFC4X3_ADD2").isObject("IFCPROJECT")).isFalse();
  }

  @Test
  void distinguishesPredefinedTypeFromCompositionType() {
    for (String declared : new String[] {"IFC2X3", "IFC4", "IFC4X3_ADD2"}) {
      IfcSchema schema = IfcSchema.forDeclaredSchema(declared);
      // IfcSite, IfcBuilding and IfcBuildingStorey carry CompositionType, never PredefinedType.
      assertThat(schema.predefinedTypeSlot("IFCSITE")).as(declared + " IfcSite").isEqualTo(-1);
      assertThat(schema.predefinedTypeSlot("IFCBUILDING"))
          .as(declared + " IfcBuilding")
          .isEqualTo(-1);
      assertThat(schema.predefinedTypeSlot("IFCBUILDINGSTOREY"))
          .as(declared + " IfcBuildingStorey")
          .isEqualTo(-1);
      assertThat(schema.predefinedTypeSlot("IFCSLAB")).as(declared + " IfcSlab").isEqualTo(8);
      assertThat(schema.predefinedTypeSlot("IFCSLABTYPE"))
          .as(declared + " IfcSlabType")
          .isEqualTo(9);
      assertThat(schema.elementTypeSlot("IFCSLABTYPE"))
          .as(declared + " IfcSlabType ElementType")
          .isEqualTo(8);
      assertThat(schema.processTypeSlot("IFCSLABTYPE"))
          .as(declared + " IfcSlabType ProcessType")
          .isEqualTo(-1);
      assertThat(schema.isTypeObject("IFCSLABTYPE")).isTrue();
      assertThat(schema.isObject("IFCSLABTYPE")).isFalse();
    }

    // IfcSpace has CompositionType at slot 8 and PredefinedType at slot 9 starting in IFC4.
    assertThat(IfcSchema.forDeclaredSchema("IFC2X3").predefinedTypeSlot("IFCSPACE")).isEqualTo(-1);
    assertThat(IfcSchema.forDeclaredSchema("IFC4").predefinedTypeSlot("IFCSPACE")).isEqualTo(9);
    assertThat(IfcSchema.forDeclaredSchema("IFC4X3_ADD2").predefinedTypeSlot("IFCSPACE"))
        .isEqualTo(9);

    // IfcTaskType uses ProcessType instead of ElementType.
    IfcSchema ifc4 = IfcSchema.forDeclaredSchema("IFC4");
    assertThat(ifc4.elementTypeSlot("IFCTASKTYPE")).isEqualTo(-1);
    assertThat(ifc4.processTypeSlot("IFCTASKTYPE")).isEqualTo(8);
    assertThat(ifc4.predefinedTypeSlot("IFCTASKTYPE")).isEqualTo(9);
  }

  @Test
  void walksSupertypeChainsForRelations() {
    IfcSchema ifc2x3 = IfcSchema.forDeclaredSchema("IFC2X3");
    assertThat(ifc2x3.isSubtypeOf("IFCRELOVERRIDESPROPERTIES", "IFCRELDEFINESBYPROPERTIES"))
        .isTrue();
    assertThat(ifc2x3.isSubtypeOf("IfcRelOverridesProperties", "IfcRelDefinesByProperties"))
        .isTrue();
    assertThat(ifc2x3.isSubtypeOf("IFCRELDEFINESBYPROPERTIES", "IFCRELDEFINESBYPROPERTIES"))
        .isTrue();
    assertThat(ifc2x3.isSubtypeOf("IFCRELDEFINESBYTYPE", "IFCRELDEFINESBYPROPERTIES")).isFalse();
  }

  @Test
  void providesCanonicalNamesForEntitiesAndDeclaredTypes() {
    IfcSchema ifc4x3 = IfcSchema.forDeclaredSchema("IFC4X3_ADD2");
    assertThat(ifc4x3.canonicalName("IFCEARTHWORKSFILL")).isEqualTo("IfcEarthworksFill");
    assertThat(ifc4x3.canonicalName("IFCLABEL")).isEqualTo("IfcLabel");
    assertThat(ifc4x3.canonicalName("IFCLOGICAL")).isEqualTo("IfcLogical");
    assertThat(ifc4x3.canonicalName("IFCLENGTHMEASURE")).isEqualTo("IfcLengthMeasure");
    assertThat(ifc4x3.canonicalName("IFCPROPERTYSETDEFINITIONSET"))
        .isEqualTo("IfcPropertySetDefinitionSet");
    assertThat(ifc4x3.canonicalName("IFCUNKNOWNENTITY")).isEqualTo("IFCUNKNOWNENTITY");
  }
}
