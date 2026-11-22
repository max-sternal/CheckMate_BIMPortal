package com.bimportal.hackathon.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.catalogue.ModelFit.Fit;
import com.bimportal.hackathon.catalogue.ModelFit.Kind;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Catalogue entries from the portal's list, filtering them, and describing one. */
class CatalogueTest {

  private static final UUID KAPPE = UUID.fromString("be959b9d-daef-5315-9aba-f60b6ad54ccb");

  // Entries and filters

  @Test
  void takesTheMetadataFromTheListAndToleratesGaps() {
    SimpleLoinPublicDto dto =
        new SimpleLoinPublicDto()
            .guid(KAPPE)
            .name("Ueberbau > Kappe (Ingenieurbau): LPH 8-9")
            .objectTypes(List.of("Kappe"))
            .domainSpecificModelTypes(List.of("Ingenieurbau"))
            .projectPhases(List.of("LPH 8: Objektüberwachung", "LPH 9: Objektbetreuung"))
            .purposes(null);

    CatalogueEntry entry = CatalogueEntry.of(dto);

    assertThat(entry.guid()).isEqualTo(KAPPE);
    assertThat(entry.objectTypes()).containsExactly("Kappe");
    assertThat(entry.phases()).hasSize(2);
    assertThat(entry.purposes()).isEmpty();
  }

  @Test
  void filtersByAPartOfEachValueIgnoringCase() {
    CatalogueEntry kappe = kappe();

    assertThat(new CatalogueFilter("ingenieurbau", "LPH 8", null).accepts(kappe)).isTrue();
    assertThat(new CatalogueFilter(null, "LPH 5", null).accepts(kappe)).isFalse();
    assertThat(new CatalogueFilter(null, null, "Abnahme").accepts(kappe)).isTrue();
    assertThat(new CatalogueFilter("Verkehrsanlage", null, null).accepts(kappe)).isFalse();
    assertThat(CatalogueFilter.ANY.accepts(kappe)).isTrue();
    assertThat(CatalogueFilter.ANY.isAny()).isTrue();
    assertThat(new CatalogueFilter(" ", "", null).isAny()).isTrue();
  }

  @Test
  void offersEveryValueOfADimensionOnceAndSorted() {
    CatalogueEntry fenster =
        new CatalogueEntry(
            UUID.randomUUID(),
            "Fenster",
            "1.1",
            "Bund",
            List.of("Fenster"),
            List.of("Fachmodell Objektplanung/Architektur"),
            List.of(),
            List.of("LPH 3: Entwurfsplanung"),
            List.of());

    assertThat(CatalogueFilter.values(List.of(kappe(), fenster), CatalogueEntry::phases))
        .containsExactly(
            "LPH 3: Entwurfsplanung", "LPH 8: Objektüberwachung", "LPH 9: Objektbetreuung");
  }

  // Description

  @Test
  void describesWhatTheLoinSelectsAsksAndHowManyElementsItWouldApplyTo() throws Exception {
    List<ModelElement> model = List.of(kappeElement("IfcLabel"), kappeElement("IfcText"));
    Fit fit = new Fit(Kind.CLASSIFICATION, 2, "Kappe", 0, "Ueberbau");

    List<String> lines = LoinDescription.describe(kappe(), fit, kappeIds(), "IFC2X3", model);

    assertThat(lines)
        .contains(
            "Ueberbau > Kappe (Ingenieurbau): LPH 8-9 - Version 4.1 - Pflegestelle Straße",
            "ID: " + KAPPE,
            "Leistungsphasen: LPH 8, LPH 9",
            "Passt zum Modell: 2 Element(e) mit 'Kappe' in der Klassifizierung",
            "Wählt aus: Klassifizierung/Objektgruppe = 'Ueberbau', Klassifizierung/Objektklasse ="
                + " 'Kappe', Klassifizierung/Objekttyp = 'Kappe'",
            "Im Modell: 1 von 2 Elementen anwendbar");
    assertThat(lines)
        .anyMatch(line -> line.startsWith("Verlangt: IFC-Entity IFCBEAM; 4 Merkmal(e):"));
    // The second element stores its classification as IfcText: the hint names it.
    assertThat(lines).anyMatch(line -> line.startsWith("Hinweis: 1 weitere(s) Element(e)"));
  }

  @Test
  void saysWhenTheRequirementsAreStillLoadingOrMissForTheSchema() throws Exception {
    List<String> loading = LoinDescription.describe(kappe(), Fit.NONE, null, "IFC2X3", List.of());
    List<String> otherSchema =
        LoinDescription.describe(kappe(), Fit.NONE, kappeIds(), "IFC5", List.of());

    assertThat(loading)
        .contains(
            "Kein Objekttyp dieser LOIN im Modell gefunden.", "Anforderungen werden geladen ...");
    assertThat(otherSchema)
        .anyMatch(line -> line.startsWith("Keine Anforderungen für IFC5 - vorhanden: [IFC4,"));
  }

  @Test
  void shortensLongEntityPatterns() {
    assertThat(LoinDescription.shortEntities("IFCBEAM")).isEqualTo("IFCBEAM");
    assertThat(LoinDescription.shortEntities("IFCBEAM|IFCCOLUMN|IFCSLAB|IFCWALL|IFCPLATE"))
        .isEqualTo("IFCBEAM | IFCCOLUMN | IFCSLAB ... (5 Entities)");
  }

  private static CatalogueEntry kappe() {
    return new CatalogueEntry(
        KAPPE,
        "Ueberbau > Kappe (Ingenieurbau): LPH 8-9",
        "4.1",
        "Pflegestelle Straße",
        List.of("Kappe"),
        List.of("Ingenieurbau"),
        List.of("Ingenieurbauwerk"),
        List.of("LPH 8: Objektüberwachung", "LPH 9: Objektbetreuung"),
        List.of("170 Abnahme- und Mängelmanagement"));
  }

  /** A Kappe classified as the LOIN asks, with its values in the given type. */
  private static ModelElement kappeElement(String dataType) {
    return new ModelElement(
        "kappe-" + dataType,
        "Kappe",
        "IfcBeam",
        List.of(
            klassifizierung("Objektgruppe", "Ueberbau", dataType),
            klassifizierung("Objektklasse", "Kappe", dataType),
            klassifizierung("Objekttyp", "Kappe", dataType)));
  }

  private static ModelProperty klassifizierung(String name, String value, String dataType) {
    return new ModelProperty(new PropertyKey("Klassifizierung", name), value, dataType);
  }

  private static IdsDocument kappeIds() throws IOException {
    String resource = "/ids/portal/ueberbau-kappe-lph8-9.ids";
    try (InputStream in = CatalogueTest.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException(resource + " is missing from the test classpath");
      }
      return IdsParser.read(in, resource);
    }
  }
}
