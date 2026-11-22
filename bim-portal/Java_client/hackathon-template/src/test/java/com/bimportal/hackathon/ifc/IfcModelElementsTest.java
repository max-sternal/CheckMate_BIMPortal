package com.bimportal.hackathon.ifc;

import static com.bimportal.hackathon.ifc.TestModels.ARCHITECTURE_IFC4;
import static com.bimportal.hackathon.ifc.TestModels.ARCHITECTURE_IFC4X3;
import static com.bimportal.hackathon.ifc.TestModels.BRIDGE_IFC4;
import static com.bimportal.hackathon.ifc.TestModels.BRIDGE_IFC4X3;
import static com.bimportal.hackathon.ifc.TestModels.LARGE_BUILDING_IFC2X3;
import static com.bimportal.hackathon.ifc.TestModels.byId;
import static com.bimportal.hackathon.ifc.TestModels.byName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reading IFC2X3, IFC4 and IFC4X3 models into {@link ModelElement}s.
 *
 * <p>The expected values come from IfcOpenShell 0.9, the library behind ifctester, read with the
 * same rules (2026-11-20): PredefinedType and "user-defined" by its own {@code get_predefined_type}
 * and {@code is_userdefined_type}, properties from the element and its type. The downloaded sample
 * models are skipped, with a message, when they are missing; our own case files and the bundled
 * bridge model are always there.
 */
class IfcModelElementsTest {

  // Our own case files: the same five slabs in every schema.

  @ParameterizedTest
  @ValueSource(strings = {"ifc2x3", "ifc4", "ifc4x3"})
  void readsEveryWayAPredefinedTypeComesAbout(String schema) {
    List<ModelElement> elements = IfcModelElements.from(TestModels.cases(schema));

    assertThat(elements)
        .extracting(ModelElement::name, ModelElement::predefinedType, ModelElement::userDefinedType)
        .containsExactlyInAnyOrder(
            // The element's own value.
            tuple("platte-werte", "FLOOR", false),
            // The type's value: the element says NOTDEFINED, and the type decides first.
            tuple("platte-typ", "ROOF", false),
            // USERDEFINED on the element: its ObjectType names the type.
            tuple("platte-eigen", "Sonderplatte", true),
            // USERDEFINED on the type: its ElementType names the type.
            tuple("platte-typ-eigen", "Typplatte", true),
            // No PredefinedType at all, but an ObjectType: user-defined as well.
            tuple("platte-objekttyp", "Freitext", true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ifc2x3", "ifc4", "ifc4x3"})
  void readsSingleEnumeratedAndListValuesButNoQuantities(String schema) {
    ModelElement werte = byName(IfcModelElements.from(TestModels.cases(schema)), "platte-werte");

    assertThat(werte.properties())
        .extracting(p -> p.key().toString(), ModelProperty::values, ModelProperty::dataType)
        .containsExactlyInAnyOrder(
            tuple("Pset_Test/Einzel", List.of("eins"), "IfcLabel"),
            tuple("Pset_Test/Auswahl", List.of("A", "B"), "IfcLabel"),
            tuple("Pset_Test/Liste", List.of("1", "2"), "IfcInteger"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ifc2x3", "ifc4", "ifc4x3"})
  void inheritsTypePropertiesAndLetsTheElementOverrideThem(String schema) {
    ModelElement typ = byName(IfcModelElements.from(TestModels.cases(schema)), "platte-typ");

    assertThat(typ.properties())
        .extracting(p -> p.key().toString(), ModelProperty::value)
        .containsExactlyInAnyOrder(
            tuple("Pset_Typ/Geerbt", "vom Typ"), tuple("Pset_Typ/Ueberschrieben", "Element"));
  }

  @Test
  void readsTheSchemaFromTheHeader() {
    assertThat(IfcModelElements.schemaOf(TestModels.cases("ifc2x3"))).isEqualTo("IFC2X3");
    assertThat(IfcModelElements.schemaOf(TestModels.cases("ifc4"))).isEqualTo("IFC4");
    assertThat(IfcModelElements.schemaOf(TestModels.cases("ifc4x3"))).isEqualTo("IFC4X3_ADD2");
  }

  // The bundled bridge model, IFC2X3.

  @Test
  void readsTheBundledBridgeModel() {
    List<ModelElement> elements = IfcModelElements.from(IfcResources.loadBundled());

    assertThat(Counts.of(elements)).isEqualTo(new Counts(303, 295, 6397, 259));
    assertThat(count(elements, "IfcSlab", "FLOOR")).isEqualTo(34);
    assertType(byId(elements, "1cxNZQHk18xe_eYvhIpXRU"), "IfcSlab", "FLOOR", false);

    ModelElement kappe = byId(elements, "1cxNZQHk18xe_eYvhIpXRg");
    assertThat(kappe.entityName()).isEqualTo("IfcBuildingElementProxy");
    assertThat(kappe.find(new PropertyKey("Pset_Objektinformation", "_IDEbene3")))
        .get()
        .extracting(ModelProperty::value, ModelProperty::dataType)
        .containsExactly("Kappe", "IfcText");
  }

  // Downloaded sample models.

  @Test
  void readsAnIfc2x3Building() {
    List<ModelElement> elements = IfcModelElements.from(TestModels.sample(LARGE_BUILDING_IFC2X3));

    assertThat(Counts.of(elements)).isEqualTo(new Counts(188, 127, 6273, 150));
    assertThat(count(elements, "IfcWallStandardCase", "STANDARD")).isEqualTo(18);
    assertType(byId(elements, "2cXV28XOjE6f6irgi0CO22"), "IfcCovering", "CEILING", false);

    ModelElement text = byId(elements, "0GGYMLdkHC8AnJCv0l2s_7");
    assertType(text, "IfcBuildingElementProxy", "Model Text:600mm Arial", true);
    assertThat(text.find(new PropertyKey("Pset_ProductRequirements", "Category")))
        .get()
        .extracting(ModelProperty::value, ModelProperty::dataType)
        .containsExactly("Generic Models", "IfcLabel");
  }

  @Test
  void readsAnIfc4Building() {
    StepModel model = TestModels.sample(ARCHITECTURE_IFC4);
    List<ModelElement> elements = IfcModelElements.from(model);

    assertThat(IfcModelElements.schemaOf(model)).isEqualTo("IFC4");
    assertThat(Counts.of(elements)).isEqualTo(new Counts(21, 1, 5, 10));
    assertThat(count(elements, "IfcWall", "SOLIDWALL")).isEqualTo(3);
    assertType(byId(elements, "2Cv3e8z_D5hxYOcR$bfTHG"), "IfcZone", "living space", true);

    ModelElement floor = byId(elements, "3zR0BOEcLADRKln4HYporH");
    assertThat(floor.properties()).hasSize(5);
    assertThat(floor.find(new PropertyKey("Pset_SlabCommon", "FireRating")))
        .get()
        .extracting(ModelProperty::value, ModelProperty::dataType)
        .containsExactly("REI30", "IfcLabel");
  }

  @Test
  void readsAnIfc4Bridge() {
    List<ModelElement> elements = IfcModelElements.from(TestModels.sample(BRIDGE_IFC4));

    assertThat(Counts.of(elements)).isEqualTo(new Counts(77, 0, 0, 55));
    assertThat(count(elements, "IfcColumn", "pierstem")).isEqualTo(7);
    // No PredefinedType of its own; the type's is USERDEFINED, the ObjectType names it.
    assertType(byId(elements, "1dmYPmycr2LPQju29cHK45"), "IfcBeam", "girder_segment", true);
  }

  @Test
  void readsAnIfc4x3Building() {
    StepModel model = TestModels.sample(ARCHITECTURE_IFC4X3);
    List<ModelElement> elements = IfcModelElements.from(model);

    assertThat(IfcModelElements.schemaOf(model)).isEqualTo("IFC4X3_ADD2");
    assertThat(Counts.of(elements)).isEqualTo(new Counts(21, 1, 5, 8));
    assertType(byId(elements, "3_4VN63S96DfWiJjgG8j1C"), "IfcEarthworksFill", "SUBGRADE", false);
  }

  @Test
  void readsAnIfc4x3Bridge() {
    List<ModelElement> elements = IfcModelElements.from(TestModels.sample(BRIDGE_IFC4X3));

    assertThat(Counts.of(elements)).isEqualTo(new Counts(77, 0, 0, 1));
    assertThat(count(elements, "IfcColumn", "PIERSTEM")).isEqualTo(7);
    assertType(byId(elements, "1Ni63N3g9CwwsFbbNgF65B"), "IfcEarthworksFill", "BACKFILL", false);
    assertType(byId(elements, "3Fit2Fad92zf2f6aWdJtF5"), "IfcBuildingElementProxy", "origin", true);
  }

  /** The figures that tell two readings of a model apart. */
  private record Counts(int objects, int withProperties, int properties, int userDefined) {
    static Counts of(List<ModelElement> elements) {
      return new Counts(
          elements.size(),
          (int) elements.stream().filter(element -> !element.properties().isEmpty()).count(),
          elements.stream().mapToInt(element -> element.properties().size()).sum(),
          (int) elements.stream().filter(ModelElement::userDefinedType).count());
    }
  }

  private static long count(List<ModelElement> elements, String entity, String predefinedType) {
    return elements.stream()
        .filter(element -> element.entityName().equals(entity))
        .filter(element -> predefinedType.equals(element.predefinedType()))
        .count();
  }

  private static void assertType(
      ModelElement element, String entity, String predefinedType, boolean userDefined) {
    assertThat(element)
        .extracting(
            ModelElement::entityName, ModelElement::predefinedType, ModelElement::userDefinedType)
        .containsExactly(entity, predefinedType, userDefined);
  }

  @Test
  void readsTheElementsInTheOrderOfTheFileEveryTime() {
    List<String> first =
        IfcModelElements.from(TestModels.cases("ifc4")).stream().map(ModelElement::id).toList();
    List<String> second =
        IfcModelElements.from(TestModels.cases("ifc4")).stream().map(ModelElement::id).toList();

    assertThat(second).isEqualTo(first);
  }

  @Test
  void knowsTheEntityOfAnElementsType() {
    ModelElement typed = byName(IfcModelElements.from(TestModels.cases("ifc2x3")), "platte-typ");

    assertThat(typed.typeEntityName()).isEqualTo("IfcSlabType");
  }
}
