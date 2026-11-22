package com.bimportal.hackathon.ids;

import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Test files and model elements for the {@code ids} tests.
 *
 * <p>The IDS files are real downloads from the BIM Portal, see
 * {@code src/test/resources/ids/portal/README.md}. Model elements are built by hand, so the check
 * is tested without loading an IFC file.
 */
final class IdsFixtures {

  /** Ueberbau &gt; Kappe, LPH 8-9: specifications in the order IFC4, IFC4X3_ADD2, IFC2X3. */
  static final String KAPPE_LPH_8_9 = "/ids/portal/ueberbau-kappe-lph8-9.ids";

  /** Ueberbau &gt; Schaft, LPH 1-9: the entity is a pattern listing many entities. */
  static final String SCHAFT_LPH_1_9 = "/ids/portal/ueberbau-schaft-lph1-9.ids";

  /** Fenster: selects by entity instead of classification, IFC4 only. */
  static final String FENSTER = "/ids/portal/fenster.ids";

  /**
   * Hand-written, the way other tools write IDS: five specifications, one for two schemas, and
   * parts CheckMate does not evaluate. See {@code src/test/resources/ids/local/README.md}.
   */
  static final String LOCAL = "/ids/local/mehrere-spezifikationen.ids";

  static final String LABEL = "IfcLabel";
  static final String TEXT = "IfcText";
  static final String INTEGER = "IfcInteger";

  private IdsFixtures() {}

  static byte[] bytes(String resource) throws IOException {
    try (InputStream in = IdsFixtures.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Test resource " + resource + " is missing");
      }
      return in.readAllBytes();
    }
  }

  static List<IdsSpecification> specifications(String resource) throws Exception {
    return IdsParser.parse(bytes(resource));
  }

  /** A test file as a document, with the resource name as its source. */
  static IdsDocument document(String resource) throws Exception {
    return IdsParser.read(bytes(resource), resource);
  }

  /** The one specification of a test file written for a schema. */
  static IdsSpecification specification(String resource, String schema) throws Exception {
    List<IdsSpecification> found = document(resource).forSchema(schema);
    if (found.size() != 1) {
      throw new IllegalStateException(
          resource + " has " + found.size() + " specifications for " + schema + ", not one");
    }
    return found.get(0);
  }

  /** The classification the portal LOINs select on, under the portal's own names. */
  static List<ModelProperty> klassifizierung(String gruppe, String klasse, String typ) {
    return List.of(
        property("Klassifizierung", "Objektgruppe", gruppe, LABEL),
        property("Klassifizierung", "Objektklasse", klasse, LABEL),
        property("Klassifizierung", "Objekttyp", typ, LABEL));
  }

  /** The same classification under the names the BIM.Hamburg models use, typed as the LOIN asks. */
  static List<ModelProperty> objektinformation(String ebene1, String ebene2, String ebene3) {
    return objektinformation(ebene1, ebene2, ebene3, LABEL);
  }

  /**
   * The classification under the BIM.Hamburg names, in a given type. The real bridge model stores
   * all of it as {@link #TEXT}.
   */
  static List<ModelProperty> objektinformation(
      String ebene1, String ebene2, String ebene3, String dataType) {
    return List.of(
        property("Pset_Objektinformation", "_IDEbene1", ebene1, dataType),
        property("Pset_Objektinformation", "_IDEbene2", ebene2, dataType),
        property("Pset_Objektinformation", "_IDEbene3", ebene3, dataType));
  }

  static ModelProperty feldnummer(String value, String dataType) {
    return property("Bauteilinformation", "Feldnummer", value, dataType);
  }

  static ModelProperty property(String propertySet, String name, String value, String dataType) {
    return new ModelProperty(new PropertyKey(propertySet, name), value, dataType);
  }

  /** An element whose id doubles as its name, so findings are easy to tell apart. */
  static ModelElement element(
      String id, String entity, List<ModelProperty> properties, ModelProperty... more) {
    return typedElement(id, entity, null, false, properties, more);
  }

  /**
   * An element with a PredefinedType, already resolved the way IDS reads it (see {@link
   * ModelElement#predefinedType()}).
   */
  static ModelElement typedElement(
      String id,
      String entity,
      String predefinedType,
      boolean userDefinedType,
      List<ModelProperty> properties,
      ModelProperty... more) {
    List<ModelProperty> all = new ArrayList<>(properties);
    all.addAll(List.of(more));
    return new ModelElement(id, id, entity, predefinedType, userDefinedType, List.copyOf(all));
  }
}
