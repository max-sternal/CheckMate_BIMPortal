package com.bimportal.hackathon.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.check.CheckService;
import com.bimportal.hackathon.check.CheckService.Portal;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import test.fx.LoadedModel;

/** The MCP tools without the protocol, on a model built in code and with the portal mocked. */
@ExtendWith(MockitoExtension.class)
class CheckMateToolsTest {

  /** Ueberbau &gt; Kappe, LPH 8-9. */
  private static final UUID KAPPE = UUID.fromString("be959b9d-daef-5315-9aba-f60b6ad54ccb");

  private static final String KAPPE_IDS = "/ids/portal/ueberbau-kappe-lph8-9.ids";

  @Mock private EnhancedBimPortalClient client;

  @TempDir Path folder;

  @Test
  void usesTheDefaultModelUntilOneIsLoaded() {
    List<String> read = new ArrayList<>();
    CheckMateTools tools =
        new CheckMateTools(
            new CheckService(Portal.none()),
            source -> {
              read.add(source);
              return model("Andere.ifc", kappe("andere", "Kappe", "1"));
            },
            () -> model("Kappen.ifc", kappe("k1", "Kappe Nord", "1"), slab()));

    assertThat(tools.describeModel())
        .contains("Kappen.ifc - Schema IFC2X3, 2 Objekte", "IfcBeam: 1", "IfcSlab: 1");
    assertThat(tools.loadModel("C:/modelle/andere.ifc")).contains("Andere.ifc");
    assertThat(read).containsExactly("C:/modelle/andere.ifc");
  }

  @Test
  void listsElementsByEntityAndName() {
    ModelElement nord = kappe("k1", "Kappe Nord", "1");
    CheckMateTools tools = tools(Portal.none(), nord, kappe("k2", "Kappe Süd", "2"), slab());

    assertThat(tools.listElements("ifcbeam", null, 30))
        .startsWith("2 Element(e)")
        .contains("k1 | IfcBeam | Kappe Nord", "k2 | IfcBeam | Kappe Süd");
    assertThat(tools.listElements(null, "süd", 30)).startsWith("1 Element(e)").contains("k2");
    assertThat(tools.listElements(null, null, 1)).startsWith("3 Element(e), die ersten 1");
  }

  @Test
  void checksAnIdsFileAndReportsOnAnElement() throws IOException {
    Path ids = Files.write(folder.resolve("kappe.ids"), resource(KAPPE_IDS));
    CheckMateTools tools =
        tools(Portal.none(), kappe("k1", "Kappe Nord", "1"), kappe("k2", "Kappe Süd", null));

    String report = tools.check(List.of(), List.of(ids.toString()), 3);

    assertThat(report)
        .contains("Modell: Kappen.ifc", "2 von 2 Elementen anwendbar")
        .contains("Ergebnis: 1 Spezifikation(en) für IFC2X3: 1 nicht erfüllt (Exit-Code 3)");
    assertThat(tools.elementReport("Süd"))
        .contains("IfcBeam 'Kappe Süd' (k2)", "(IFC2X3, LPH 8-9):")
        .contains("[Merkmal fehlt] Bauteilinformation/Feldnummer");
    assertThat(tools.elementReport("k1")).contains("[erfüllt]").doesNotContain("Merkmal fehlt");
    assertThat(tools.elementReport("Brücke")).startsWith("Kein Element");
  }

  @Test
  void theFullReportListsEveryFinding() throws IOException {
    Path ids = Files.write(folder.resolve("kappe.ids"), resource(KAPPE_IDS));
    CheckMateTools tools = tools(Portal.none(), kappe("k1", "Kappe Nord", "1"));

    assertThat(tools.check(List.of(), List.of(ids.toString()), 0))
        .contains("[erfüllt] Kappe Nord - Bauteilinformation/Feldnummer")
        .endsWith("1 erfüllt (Exit-Code 0)");
  }

  @Test
  void saysWhatIsMissingInsteadOfGuessing() {
    CheckMateTools tools = tools(Portal.none(), kappe("k1", "Kappe Nord", "1"));

    assertThatThrownBy(() -> tools.elementReport("k1")).hasMessageContaining("check_model");
    assertThatThrownBy(() -> tools.check(List.of(), List.of(), 3))
        .hasMessageContaining("Keine Anforderungen");
    assertThat(tools.check(List.of(KAPPE.toString()), List.of(), 3))
        .contains("kein Zugang zum BIM Portal", "Exit-Code 1");
  }

  @Test
  void findsLoinsThatFitTheModelFirstAndDescribesThem() throws IOException {
    UUID schaft = UUID.randomUUID();
    UUID project = UUID.randomUUID();
    when(client.searchProjects(any()))
        .thenReturn(List.of(new SimpleAiaProjectPublicDto().guid(project).name("Brücke")));
    when(client.searchLoins(any()))
        .thenReturn(
            List.of(
                loin(schaft, "Unterbau > Schaft", "Schaft", "LPH 5: Ausführungsplanung"),
                loin(KAPPE, "Ueberbau > Kappe", "Kappe", "LPH 8: Objektüberwachung")));
    when(client.exportLoinIds(KAPPE)).thenReturn(Optional.of(resource(KAPPE_IDS)));
    CheckMateTools tools = tools(Portal.shared(client), kappe("k1", "Kappe Nord", "1"));

    String all = tools.findLoins("", CatalogueFilter.ANY, 20, false);
    String phase5 = tools.findLoins("", new CatalogueFilter(null, "LPH 5", null), 20, true);

    assertThat(all)
        .startsWith("2 LOIN(s), 2 nach Filter, 1 passend zum Modell; 1 AIA-Projekt(e)")
        .contains("AIA-Projekt | " + project + " | Brücke", "LOIN | " + KAPPE)
        .doesNotContain(schaft.toString());
    assertThat(phase5).contains("LOIN | " + schaft, "passt nicht erkennbar");

    tools.findLoins("Kappe", CatalogueFilter.ANY, 20, false);
    assertThat(tools.describeRequirements("https://via.bund.de/bim/loin/" + KAPPE))
        .contains("Objekttyp: Kappe", "Verlangt:", "Im Modell: 1 von 1 Elementen anwendbar");
    tools.check(List.of(KAPPE.toString()), List.of(), 3);
    verify(client, times(1)).exportLoinIds(KAPPE);
  }

  private CheckMateTools tools(Portal portal, ModelElement... elements) {
    LoadedModel model = model("Kappen.ifc", elements);
    return new CheckMateTools(
        new CheckService(portal),
        source -> {
          throw new IllegalStateException("not read in this test: " + source);
        },
        () -> model);
  }

  static LoadedModel model(String name, ModelElement... elements) {
    Map<String, ModelElement> byId =
        List.of(elements).stream().collect(Collectors.toMap(ModelElement::id, Function.identity()));
    return new LoadedModel(name, null, "IFC2X3", List.of(elements), byId);
  }

  /** Classified as the Kappe LOIN asks; without a Feldnummer it fails. */
  static ModelElement kappe(String id, String name, String feldnummer) {
    List<ModelProperty> properties = new ArrayList<>();
    properties.add(klassifizierung("Objektgruppe", "Ueberbau"));
    properties.add(klassifizierung("Objektklasse", "Kappe"));
    properties.add(klassifizierung("Objekttyp", "Kappe"));
    if (feldnummer != null) {
      properties.add(
          new ModelProperty(
              new PropertyKey("Bauteilinformation", "Feldnummer"), feldnummer, "IfcInteger"));
    }
    return new ModelElement(id, name, "IfcBeam", properties);
  }

  private static ModelElement slab() {
    return new ModelElement("p1", "Platte", "IfcSlab", List.of());
  }

  private static ModelProperty klassifizierung(String name, String value) {
    return new ModelProperty(new PropertyKey("Klassifizierung", name), value, "IfcLabel");
  }

  private static SimpleLoinPublicDto loin(UUID guid, String name, String objectType, String phase) {
    return new SimpleLoinPublicDto()
        .guid(guid)
        .name(name)
        .objectTypes(List.of(objectType))
        .projectPhases(List.of(phase));
  }

  static byte[] resource(String name) throws IOException {
    try (InputStream in = CheckMateToolsTest.class.getResourceAsStream(name)) {
      if (in == null) {
        throw new IOException(name + " is missing from the test classpath");
      }
      return in.readAllBytes();
    }
  }
}
