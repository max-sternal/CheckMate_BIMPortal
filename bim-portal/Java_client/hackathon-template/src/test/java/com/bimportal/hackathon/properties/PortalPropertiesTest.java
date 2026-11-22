package com.bimportal.hackathon.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bimportal.client.model.DefinitionsInLanguageDto;
import com.bimportal.client.model.ListOfPossibleValuesInLanguageDto;
import com.bimportal.client.model.NamesInLanguage;
import com.bimportal.client.model.NamesInLanguageDto;
import com.bimportal.client.model.PropertyDto;
import com.bimportal.client.model.PropertyGroupDto;
import com.bimportal.client.model.PropertyOrGroupForPublicDto;
import com.bimportal.client.model.SimpleInheritedPropertyDto;
import com.bimportal.client.model.SimpleParentGroup;
import com.bimportal.client.model.TagForPublicDto;
import com.bimportal.client.model.TagGroupForPublicDto;
import com.bimportal.hackathon.properties.PortalProperties.FilterGroup;
import com.bimportal.hackathon.properties.PortalProperties.FilterTag;
import com.bimportal.hackathon.properties.PortalProperties.GroupRef;
import com.bimportal.hackathon.properties.PortalProperties.MemberProperty;
import com.bimportal.hackathon.properties.PortalProperties.PropertyDetail;
import com.bimportal.hackathon.properties.PortalProperties.PropertyGroupDetail;
import com.bimportal.hackathon.properties.PortalProperties.PropertySummary;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link PortalProperties} with {@link EnhancedBimPortalClient} mocked. */
@ExtendWith(MockitoExtension.class)
class PortalPropertiesTest {

  private static final UUID PROP_GUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PROP_GUID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID GROUP_GUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID TAG_GROUP_GUID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID TAG_GUID = UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Mock private EnhancedBimPortalClient client;

  private PortalProperties portalProperties;

  @BeforeEach
  void setUp() {
    portalProperties = new PortalProperties(client);
  }

  @Test
  void searchesPropertiesWithTrimmedQueryAndLimit() {
    PropertyOrGroupForPublicDto first =
        new PropertyOrGroupForPublicDto()
            .guid(PROP_GUID)
            .name("Feuerwiderstandsklasse")
            .versionNumber(2L)
            .revisionNumber(1L)
            .definition("Klassifizierung nach DIN EN 13501")
            .dataType("IFCLABEL")
            .units(List.of("min"))
            .organisationName("BIM Deutschland")
            .category(PropertyOrGroupForPublicDto.CategoryEnum.DOMAIN)
            .deprecated(false);
    PropertyOrGroupForPublicDto second =
        new PropertyOrGroupForPublicDto()
            .guid(PROP_GUID_2)
            .name("Feuerwiderstand alt")
            .versionNumber(1L)
            .deprecated(true);

    when(client.searchProperties(any())).thenReturn(List.of(first, second));

    List<PropertySummary> results = portalProperties.searchProperties("  Feuerwiderstand  ", 1);

    assertThat(results).hasSize(1);
    PropertySummary summary = results.get(0);
    assertThat(summary.guid()).isEqualTo(PROP_GUID);
    assertThat(summary.name()).isEqualTo("Feuerwiderstandsklasse");
    assertThat(summary.version()).isEqualTo("2.1");
    assertThat(summary.definition()).isEqualTo("Klassifizierung nach DIN EN 13501");
    assertThat(summary.dataType()).isEqualTo("IFCLABEL");
    assertThat(summary.units()).containsExactly("min");
    assertThat(summary.organisation()).isEqualTo("BIM Deutschland");
    assertThat(summary.category()).isEqualTo("DOMAIN");
    assertThat(summary.deprecated()).isFalse();

    verify(client)
        .searchProperties(
            argThat(
                req ->
                    "Feuerwiderstand".equals(req.getSearchString())
                        && Integer.valueOf(0).equals(req.getPageNumber())));
  }

  @Test
  void pagesPropertySearchWhenCurrentPageIsBelowMaxPage() {
    PropertyOrGroupForPublicDto page0Item =
        new PropertyOrGroupForPublicDto()
            .guid(PROP_GUID)
            .name("Merkmal A")
            .currentPage(0)
            .maxPage(1);
    PropertyOrGroupForPublicDto page1Item =
        new PropertyOrGroupForPublicDto()
            .guid(PROP_GUID_2)
            .name("Merkmal B")
            .currentPage(1)
            .maxPage(1);

    when(client.searchProperties(
            argThat(req -> req != null && Integer.valueOf(0).equals(req.getPageNumber()))))
        .thenReturn(List.of(page0Item));
    when(client.searchProperties(
            argThat(req -> req != null && Integer.valueOf(1).equals(req.getPageNumber()))))
        .thenReturn(List.of(page1Item));

    List<PropertySummary> results = portalProperties.searchProperties("", 5);

    assertThat(results)
        .extracting(PropertySummary::guid, PropertySummary::name)
        .containsExactly(tuple(PROP_GUID, "Merkmal A"), tuple(PROP_GUID_2, "Merkmal B"));
    verify(client, times(2)).searchProperties(any());
  }

  @Test
  void returnsEmptyForNonPositiveLimitWithoutCallingClient() {
    assertThat(portalProperties.searchProperties("Wand", 0)).isEmpty();
    assertThat(portalProperties.searchPropertyGroups("Wand", -3)).isEmpty();
    verify(client, never()).searchProperties(any());
    verify(client, never()).searchPropertyGroups(any());
  }

  @Test
  void searchesPropertyGroupsAndProducesStructuredMap() {
    PropertyOrGroupForPublicDto group =
        new PropertyOrGroupForPublicDto()
            .guid(GROUP_GUID)
            .name("Pset_WallCommon")
            .versionNumber(1L)
            .revisionNumber(0L)
            .organisationName("buildingSMART")
            .category(PropertyOrGroupForPublicDto.CategoryEnum.NAME_PROPERTY_SET);

    when(client.searchPropertyGroups(any())).thenReturn(List.of(group));

    List<PropertySummary> groups = portalProperties.searchPropertyGroups("Wall", 10);

    assertThat(groups).hasSize(1);
    Map<String, Object> map = groups.get(0).toMap();
    assertThat(map)
        .containsEntry("guid", GROUP_GUID.toString())
        .containsEntry("name", "Pset_WallCommon")
        .containsEntry("version", "1.0")
        .containsEntry("organisation", "buildingSMART")
        .containsEntry("category", "NAME_PROPERTY_SET")
        .containsEntry("deprecated", false);
  }

  @Test
  void getsPropertyDetailPreferringGermanEntries() {
    SimpleParentGroup parent =
        new SimpleParentGroup()
            .guid(GROUP_GUID)
            .namesInLanguage(
                List.of(
                    new NamesInLanguageDto().language("en-GB").name("Wall Common"),
                    new NamesInLanguageDto().language("de-DE").name("Wand Allgemein")));

    PropertyDto dto =
        new PropertyDto()
            .guid(PROP_GUID)
            .versionNumber(3L)
            .revisionNumber(2L)
            .dataType("IFCLABEL")
            .units(List.of("m", "  "))
            .organisationName("BIM Portal Bund")
            .namesInLanguage(
                List.of(
                    new NamesInLanguageDto().language("en").name("Fire Rating"),
                    new NamesInLanguageDto().language("de-DE").name("Feuerwiderstand")))
            .definitionsInLanguage(
                List.of(
                    new DefinitionsInLanguageDto().language("en").definition("Fire resistance"),
                    new DefinitionsInLanguageDto().language("de").definition("Feuerwiderstandsdauer")))
            .listOfPossibleValuesInLanguage(
                List.of(
                    new ListOfPossibleValuesInLanguageDto().language("en").possibleValue("F30-EN"),
                    new ListOfPossibleValuesInLanguageDto().language("de").possibleValue("REI30"),
                    new ListOfPossibleValuesInLanguageDto().language("de").possibleValue("REI60")))
            .groupOfProperties(List.of(parent));

    when(client.getProperty(PROP_GUID)).thenReturn(Optional.of(dto));

    Optional<PropertyDetail> detailOpt = portalProperties.getProperty(PROP_GUID);

    assertThat(detailOpt).isPresent();
    PropertyDetail detail = detailOpt.get();
    assertThat(detail.name()).isEqualTo("Feuerwiderstand");
    assertThat(detail.version()).isEqualTo("3.2");
    assertThat(detail.definition()).isEqualTo("Feuerwiderstandsdauer");
    assertThat(detail.dataType()).isEqualTo("IFCLABEL");
    assertThat(detail.units()).containsExactly("m");
    assertThat(detail.allowedValues()).containsExactly("REI30", "REI60");
    assertThat(detail.groups()).containsExactly(new GroupRef(GROUP_GUID, "Wand Allgemein"));
    assertThat(detail.organisation()).isEqualTo("BIM Portal Bund");

    Map<String, Object> map = detail.toMap();
    assertThat(map)
        .containsEntry("guid", PROP_GUID.toString())
        .containsEntry("name", "Feuerwiderstand")
        .containsEntry("allowedValues", List.of("REI30", "REI60"));
  }

  @Test
  void getsPropertyDetailFallingBackToNonGermanAndHandlingNulls() {
    PropertyDto dto =
        new PropertyDto()
            .guid(PROP_GUID)
            .namesInLanguage(List.of(new NamesInLanguageDto().language("en").name("Width")))
            .definitionsInLanguage(null)
            .units(null)
            .listOfPossibleValuesInLanguage(null)
            .groupOfProperties(null);

    when(client.getProperty(PROP_GUID)).thenReturn(Optional.of(dto));

    PropertyDetail detail = portalProperties.getProperty(PROP_GUID).orElseThrow();
    assertThat(detail.name()).isEqualTo("Width");
    assertThat(detail.definition()).isEmpty();
    assertThat(detail.units()).isEmpty();
    assertThat(detail.allowedValues()).isEmpty();
    assertThat(detail.groups()).isEmpty();
    assertThat(portalProperties.getProperty(null)).isEmpty();
  }

  @Test
  void getsPropertyGroupDetailWithDirectAndInheritedMembers() {
    PropertyDto direct =
        new PropertyDto()
            .guid(PROP_GUID)
            .dataType("IFCLENGTHMEASURE")
            .units(List.of("mm"))
            .namesInLanguage(List.of(new NamesInLanguageDto().language("de").name("Breite")));

    SimpleInheritedPropertyDto duplicate =
        new SimpleInheritedPropertyDto()
            .id(PROP_GUID)
            .names(Set.of(new NamesInLanguage().language("de").name("Breite")));
    SimpleInheritedPropertyDto inherited =
        new SimpleInheritedPropertyDto()
            .id(PROP_GUID_2)
            .names(Set.of(new NamesInLanguage().language("de").name("Material")));

    PropertyGroupDto groupDto =
        new PropertyGroupDto()
            .guid(GROUP_GUID)
            .versionNumber(1L)
            .revisionNumber(4L)
            .organisationName("BIM Deutschland")
            .namesInLanguage(List.of(new NamesInLanguageDto().language("de").name("Bauteilmaße")))
            .definitionsInLanguage(
                List.of(
                    new DefinitionsInLanguageDto()
                        .language("de")
                        .definition("Geometrische Abmessungen")))
            .properties(List.of(direct))
            .simpleInheritedProperties(List.of(duplicate, inherited));

    when(client.getPropertyGroup(GROUP_GUID)).thenReturn(Optional.of(groupDto));

    PropertyGroupDetail group = portalProperties.getPropertyGroup(GROUP_GUID).orElseThrow();

    assertThat(group.name()).isEqualTo("Bauteilmaße");
    assertThat(group.version()).isEqualTo("1.4");
    assertThat(group.definition()).isEqualTo("Geometrische Abmessungen");
    assertThat(group.organisation()).isEqualTo("BIM Deutschland");
    assertThat(group.properties())
        .containsExactly(
            new MemberProperty(PROP_GUID, "Breite", "IFCLENGTHMEASURE", List.of("mm")),
            new MemberProperty(PROP_GUID_2, "Material", "", List.of()));
    assertThat(group.toMap()).containsEntry("name", "Bauteilmaße");
    assertThat(portalProperties.getPropertyGroup(null)).isEmpty();
  }

  @Test
  void getsPropertyFiltersNullSafely() {
    TagGroupForPublicDto filterGroup =
        new TagGroupForPublicDto()
            .guid(TAG_GROUP_GUID)
            .name("Fachmodell")
            .filter(List.of(new TagForPublicDto().guid(TAG_GUID).name("Ingenieurbau")));

    when(client.getPropertyFilters()).thenReturn(List.of(filterGroup));

    List<FilterGroup> filters = portalProperties.getPropertyFilters();

    assertThat(filters)
        .containsExactly(
            new FilterGroup(
                TAG_GROUP_GUID, "Fachmodell", List.of(new FilterTag(TAG_GUID, "Ingenieurbau"))));
    assertThat(filters.get(0).toMap()).containsEntry("name", "Fachmodell");
  }

  @Test
  void formatsGermanOutputForSearchPropertyAndGroupDetails() {
    PropertySummary summary =
        new PropertySummary(
            PROP_GUID,
            "Feuerwiderstand",
            "1.0",
            "Klassifizierung",
            "IFCLABEL",
            List.of("min"),
            "Bund",
            "DOMAIN",
            true);
    String searchText = PortalProperties.formatSearchResults(List.of(summary), "Merkmale");
    assertThat(searchText)
        .contains("Merkmale (1):")
        .contains("1. Feuerwiderstand (Version 1.0) [veraltet]")
        .contains("Datentyp: IFCLABEL")
        .contains("Einheit: min")
        .contains("GUID: " + PROP_GUID);
    assertThat(PortalProperties.formatSearchResults(List.of(), "Merkmale"))
        .isEqualTo("Merkmale: Keine Einträge gefunden.");

    PropertyDetail detail =
        new PropertyDetail(
            PROP_GUID,
            "Feuerwiderstand",
            "1.0",
            "Feuerwiderstandsdauer",
            "IFCLABEL",
            List.of("min"),
            List.of("REI30", "REI60"),
            List.of(new GroupRef(GROUP_GUID, "Pset_WallCommon")),
            "Bund");
    assertThat(PortalProperties.formatPropertyDetail(detail))
        .contains("Merkmal: Feuerwiderstand (Version 1.0)")
        .contains("Zulässige Werte: REI30, REI60")
        .contains("Merkmalsgruppen: Pset_WallCommon");

    PropertyGroupDetail groupDetail =
        new PropertyGroupDetail(
            GROUP_GUID,
            "Pset_WallCommon",
            "2.0",
            "Allgemeine Wandmerkmale",
            List.of(new MemberProperty(PROP_GUID, "Feuerwiderstand", "IFCLABEL", List.of())),
            "Bund");
    assertThat(PortalProperties.formatPropertyGroupDetail(groupDetail))
        .contains("Merkmalsgruppe: Pset_WallCommon (Version 2.0)")
        .contains("Merkmale (1):")
        .contains("- Feuerwiderstand (IFCLABEL | " + PROP_GUID + ")");
  }
}
