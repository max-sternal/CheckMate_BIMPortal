package com.bimportal.hackathon.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bimportal.client.model.NamesInLanguageDto;
import com.bimportal.client.model.PropertyDto;
import com.bimportal.client.model.PropertyGroupDto;
import com.bimportal.client.model.PropertyOrGroupForPublicDto;
import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.check.CheckService;
import com.bimportal.hackathon.check.CheckService.Portal;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import com.bimportal.hackathon.ifc.LoadedModel;
import com.bimportal.hackathon.properties.PortalProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  void aViewLoadsUnmetFindingsPageByPage() throws IOException {
    Path ids = Files.write(folder.resolve("kappe.ids"), resource(KAPPE_IDS));
    CheckMateTools tools =
        tools(
            Portal.none(),
            kappe("k1", "Kappe 1", null),
            kappe("k2", "Kappe 2", null),
            kappe("k3", "Kappe 3", null));
    assertThatThrownBy(() -> tools.reportPage(0, 0, 0, 10))
        .hasMessageContaining("zuerst check_model");

    tools.check(List.of(), List.of(ids.toString()), 3);
    Map<String, Object> data = tools.reportData(2);
    Map<String, Object> spec =
        ((List<Map<String, Object>>)
                ((List<Map<String, Object>>) data.get("runs")).get(0).get("specifications"))
            .get(0);

    assertThat(spec.get("unmetTotal")).isEqualTo(3);
    assertThat((List<?>) spec.get("unmet")).hasSize(2);
    Map<String, Object> rest = tools.reportPage(0, 0, 2, 25);
    assertThat(rest.get("total")).isEqualTo(3);
    assertThat((List<?>) rest.get("unmet")).hasSize(1);
    assertThatThrownBy(() -> tools.reportPage(0, 5, 0, 1))
        .hasMessageContaining("Keine Spezifikation");
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

    assertThatThrownBy(() -> tools.elementReport(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Kein Element");
    assertThatThrownBy(() -> tools.elementReport("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Kein Element");
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

  @Test
  void providesStructuredModelAndReportJsonData() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Path ids = Files.write(folder.resolve("kappe.ids"), resource(KAPPE_IDS));
    CheckMateTools tools =
        tools(Portal.none(), kappe("k1", "Kappe Nord", "1"), kappe("k2", "Kappe Süd", "2"), slab());

    Map<String, Object> modelData = tools.modelData();
    assertThat(modelData.get("name")).isEqualTo("Kappen.ifc");
    assertThat(modelData.get("schema")).isEqualTo("IFC2X3");
    assertThat(modelData.get("elementCount")).isEqualTo(3);
    assertThat(modelData.get("mappingName")).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Integer> entityCounts = (Map<String, Integer>) modelData.get("entityCounts");
    assertThat(entityCounts).containsEntry("IfcBeam", 2).containsEntry("IfcSlab", 1);

    JsonNode modelJson = mapper.readTree(tools.currentModelJson());
    assertThat(modelJson.path("name").asText()).isEqualTo("Kappen.ifc");
    assertThat(modelJson.path("elementCount").asInt()).isEqualTo(3);
    assertThat(modelJson.path("entityCounts").path("IfcBeam").asInt()).isEqualTo(2);

    JsonNode beforeCheck = mapper.readTree(tools.lastReportJson());
    assertThat(beforeCheck.path("status").asText()).isEqualTo("none");

    tools.check(List.of(), List.of(ids.toString()), 3);
    JsonNode afterCheck = mapper.readTree(tools.lastReportJson());
    assertThat(afterCheck.path("model").path("name").asText()).isEqualTo("Kappen.ifc");
    assertThat(afterCheck.path("exitCode").asInt()).isZero();
    assertThat(afterCheck.path("requirements")).hasSize(1);
  }

  @Test
  void providesStructuredLoinsDataAfterSearch() {
    UUID project = UUID.randomUUID();
    when(client.searchProjects(any()))
        .thenReturn(List.of(new SimpleAiaProjectPublicDto().guid(project).name("Brücke")));
    when(client.searchLoins(any()))
        .thenReturn(
            List.of(
                loin(KAPPE, "Ueberbau > Kappe", "Kappe", "LPH 8: Objektüberwachung")
                    .domainSpecificModelTypes(List.of("Ingenieurbau"))));
    CheckMateTools tools = tools(Portal.shared(client), kappe("k1", "Kappe Nord", "1"));

    assertThat(tools.loinsData()).isEmpty();
    tools.findLoins("Kappe", CatalogueFilter.ANY, 20, false);

    Map<String, Object> data = tools.loinsData();
    assertThat(data.get("totalLoins")).isEqualTo(1);
    assertThat(data.get("fittingLoins")).isEqualTo(1L);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> projects = (List<Map<String, Object>>) data.get("projects");
    assertThat(projects).hasSize(1);
    assertThat(projects.get(0).get("id")).isEqualTo(project.toString());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> loins = (List<Map<String, Object>>) data.get("loins");
    assertThat(loins).hasSize(1);
    assertThat(loins.get(0).get("id")).isEqualTo(KAPPE.toString());
    assertThat(loins.get(0).get("fits")).isEqualTo(true);
    assertThat(loins.get(0).get("fitKind")).isEqualTo("CLASSIFICATION");
    assertThat(loins.get(0).get("disciplines")).isEqualTo(List.of("Ingenieurbau"));
    assertThat(loins.get(0).get("phases")).isEqualTo(List.of("LPH 8: Objektüberwachung"));
  }

  @Test
  void findsAndDescribesPortalPropertiesAndGroups() {
    UUID propGuid = UUID.randomUUID();
    UUID groupGuid = UUID.randomUUID();
    when(client.searchProperties(any()))
        .thenReturn(
            List.of(
                new PropertyOrGroupForPublicDto()
                    .guid(propGuid)
                    .name("Betongüte")
                    .dataType("IFCLABEL")));
    when(client.searchPropertyGroups(any()))
        .thenReturn(
            List.of(
                new PropertyOrGroupForPublicDto()
                    .guid(groupGuid)
                    .name("Pset_Bauwerk")));
    when(client.getProperty(propGuid))
        .thenReturn(
            Optional.of(
                new PropertyDto()
                    .guid(propGuid)
                    .namesInLanguage(
                        List.of(new NamesInLanguageDto().language("de-DE").name("Betongüte")))
                    .dataType("IFCLABEL")));
    when(client.getProperty(groupGuid)).thenReturn(Optional.empty());
    when(client.getPropertyGroup(groupGuid))
        .thenReturn(
            Optional.of(
                new PropertyGroupDto()
                    .guid(groupGuid)
                    .namesInLanguage(
                        List.of(new NamesInLanguageDto().language("de-DE").name("Pset_Bauwerk")))));

    CheckMateTools tools =
        new CheckMateTools(
            new CheckService(Portal.shared(client)),
            source -> model("Kappen.ifc"),
            () -> model("Kappen.ifc"),
            () -> new PortalProperties(client));

    String found = tools.findProperties("Beton", "all", 10);
    assertThat(found).contains("Merkmale", "Betongüte", "Merkmalsgruppen", "Pset_Bauwerk");
    assertThat(tools.propertiesData()).containsKey("properties").containsKey("groups");

    String propDetail = tools.describeProperty(propGuid.toString(), "auto");
    assertThat(propDetail).contains("Betongüte", "IFCLABEL");
    assertThat(tools.propertyDetailData()).containsEntry("guid", propGuid.toString());

    // Auto mode falls back from property to property group
    String groupDetail =
        tools.describeProperty("https://via.bund.de/bim/merkmale/" + groupGuid, "auto");
    assertThat(groupDetail).contains("Pset_Bauwerk");
    assertThat(tools.propertyDetailData()).containsEntry("guid", groupGuid.toString());

    assertThatThrownBy(() -> tools.findProperties("Beton", "invalid", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unbekannter Typ");
    assertThatThrownBy(() -> tools.describeProperty("not-a-uuid", "auto"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Keine gültige GUID");
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
    return new LoadedModel(name, "IFC2X3", List.of(elements), byId);
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
