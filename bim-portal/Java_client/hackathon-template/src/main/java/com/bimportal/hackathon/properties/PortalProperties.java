package com.bimportal.hackathon.properties;

import com.bimportal.client.model.DefinitionsInLanguageDto;
import com.bimportal.client.model.ListOfPossibleValuesInLanguageDto;
import com.bimportal.client.model.NamesInLanguage;
import com.bimportal.client.model.NamesInLanguageDto;
import com.bimportal.client.model.PropertyDto;
import com.bimportal.client.model.PropertyGroupDto;
import com.bimportal.client.model.PropertyOrGroupForPublicDto;
import com.bimportal.client.model.PropertyOrGroupForPublicRequest;
import com.bimportal.client.model.SimpleInheritedPropertyDto;
import com.bimportal.client.model.SimpleParentGroup;
import com.bimportal.client.model.TagForPublicDto;
import com.bimportal.client.model.TagGroupForPublicDto;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Access to BIM Portal properties ("Merkmale"), property groups ("Merkmalsgruppen"), and global
 * property filters.
 *
 * <p>Wraps {@link EnhancedBimPortalClient} with null-safe extraction into immutable Java records,
 * German text formatters, and structured {@code Map<String, Object>} views for CLI and MCP tools.
 */
public final class PortalProperties {

  /** Entries the portal puts on one page when {@code currentPage}/{@code maxPage} are absent. */
  static final int PAGE_SIZE = 1000;

  /** Upper bound on pages fetched for one search, preventing runaway loops. */
  private static final int MAX_PAGES = 20;

  private final EnhancedBimPortalClient client;

  /** Create a property catalogue facade backed by the given portal client. */
  public PortalProperties(EnhancedBimPortalClient client) {
    this.client = Objects.requireNonNull(client, "client");
  }

  /**
   * Search public properties ("Merkmale") by optional search term, returning at most {@code limit}
   * results.
   */
  public List<PropertySummary> searchProperties(String query, int limit) {
    if (limit <= 0) {
      return List.of();
    }
    return pagedSearch(query, limit, client::searchProperties);
  }

  /**
   * Search public property groups ("Merkmalsgruppen") by optional search term, returning at most
   * {@code limit} results.
   */
  public List<PropertySummary> searchPropertyGroups(String query, int limit) {
    if (limit <= 0) {
      return List.of();
    }
    return pagedSearch(query, limit, client::searchPropertyGroups);
  }

  /** Fetch detailed information for a single property ("Merkmal") by GUID. */
  public Optional<PropertyDetail> getProperty(UUID guid) {
    if (guid == null) {
      return Optional.empty();
    }
    Optional<PropertyDto> result = client.getProperty(guid);
    return result == null ? Optional.empty() : result.map(PortalProperties::toPropertyDetail);
  }

  /** Fetch detailed information for a single property group ("Merkmalsgruppe") by GUID. */
  public Optional<PropertyGroupDetail> getPropertyGroup(UUID guid) {
    if (guid == null) {
      return Optional.empty();
    }
    Optional<PropertyGroupDto> result = client.getPropertyGroup(guid);
    return result == null ? Optional.empty() : result.map(PortalProperties::toPropertyGroupDetail);
  }

  /** Fetch all global property filter groups ("Merkmalsfilter"). */
  public List<FilterGroup> getPropertyFilters() {
    List<TagGroupForPublicDto> groups = client.getPropertyFilters();
    if (groups == null || groups.isEmpty()) {
      return List.of();
    }
    return groups.stream().filter(Objects::nonNull).map(PortalProperties::toFilterGroup).toList();
  }

  // === Records ===

  /** Summary of a property or property group from a BIM Portal search response. */
  public record PropertySummary(
      UUID guid,
      String name,
      String version,
      String definition,
      String dataType,
      List<String> units,
      String organisation,
      String category,
      boolean deprecated) {

    public PropertySummary {
      name = cleanText(name);
      version = cleanText(version);
      definition = cleanText(definition);
      dataType = cleanText(dataType);
      units = cleanList(units);
      organisation = cleanText(organisation);
      category = cleanText(category);
    }

    /** Structured representation suitable for JSON/MCP responses. */
    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("guid", guid == null ? null : guid.toString());
      map.put("name", name);
      map.put("version", version);
      map.put("definition", definition);
      map.put("dataType", dataType);
      map.put("units", units);
      map.put("organisation", organisation);
      map.put("category", category);
      map.put("deprecated", deprecated);
      return map;
    }
  }

  /** Reference to a parent property group that contains a property. */
  public record GroupRef(UUID guid, String name) {

    public GroupRef {
      name = cleanText(name);
    }

    /** Structured representation suitable for JSON/MCP responses. */
    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("guid", guid == null ? null : guid.toString());
      map.put("name", name);
      return map;
    }
  }

  /** Full details of a single BIM Portal property ("Merkmal"). */
  public record PropertyDetail(
      UUID guid,
      String name,
      String version,
      String definition,
      String dataType,
      List<String> units,
      List<String> allowedValues,
      List<GroupRef> groups,
      String organisation) {

    public PropertyDetail {
      name = cleanText(name);
      version = cleanText(version);
      definition = cleanText(definition);
      dataType = cleanText(dataType);
      units = cleanList(units);
      allowedValues = cleanList(allowedValues);
      groups = groups == null ? List.of() : List.copyOf(groups);
      organisation = cleanText(organisation);
    }

    /** Structured representation suitable for JSON/MCP responses. */
    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("guid", guid == null ? null : guid.toString());
      map.put("name", name);
      map.put("version", version);
      map.put("definition", definition);
      map.put("dataType", dataType);
      map.put("units", units);
      map.put("allowedValues", allowedValues);
      map.put("groups", groups.stream().map(GroupRef::toMap).toList());
      map.put("organisation", organisation);
      return map;
    }
  }

  /** Member property listed inside a {@link PropertyGroupDetail}. */
  public record MemberProperty(UUID guid, String name, String dataType, List<String> units) {

    public MemberProperty {
      name = cleanText(name);
      dataType = cleanText(dataType);
      units = cleanList(units);
    }

    /** Structured representation suitable for JSON/MCP responses. */
    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("guid", guid == null ? null : guid.toString());
      map.put("name", name);
      map.put("dataType", dataType);
      map.put("units", units);
      return map;
    }
  }

  /** Full details of a BIM Portal property group ("Merkmalsgruppe"). */
  public record PropertyGroupDetail(
      UUID guid,
      String name,
      String version,
      String definition,
      List<MemberProperty> properties,
      String organisation) {

    public PropertyGroupDetail {
      name = cleanText(name);
      version = cleanText(version);
      definition = cleanText(definition);
      properties = properties == null ? List.of() : List.copyOf(properties);
      organisation = cleanText(organisation);
    }

    /** Structured representation suitable for JSON/MCP responses. */
    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("guid", guid == null ? null : guid.toString());
      map.put("name", name);
      map.put("version", version);
      map.put("definition", definition);
      map.put("properties", properties.stream().map(MemberProperty::toMap).toList());
      map.put("organisation", organisation);
      return map;
    }
  }

  /** A single filter tag within a {@link FilterGroup}. */
  public record FilterTag(UUID guid, String name) {

    public FilterTag {
      name = cleanText(name);
    }

    /** Structured representation suitable for JSON/MCP responses. */
    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("guid", guid == null ? null : guid.toString());
      map.put("name", name);
      return map;
    }
  }

  /** A group of global property filters from the BIM Portal. */
  public record FilterGroup(UUID guid, String name, List<FilterTag> tags) {

    public FilterGroup {
      name = cleanText(name);
      tags = tags == null ? List.of() : List.copyOf(tags);
    }

    /** Structured representation suitable for JSON/MCP responses. */
    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("guid", guid == null ? null : guid.toString());
      map.put("name", name);
      map.put("tags", tags.stream().map(FilterTag::toMap).toList());
      return map;
    }
  }

  // === German formatting helpers ===

  /** Format a list of search results as readable German text. */
  public static String formatSearchResults(List<PropertySummary> results) {
    return formatSearchResults(results, "Ergebnisse");
  }

  /** Format a list of search results with a custom German heading. */
  public static String formatSearchResults(List<PropertySummary> results, String heading) {
    String title = heading == null || heading.isBlank() ? "Ergebnisse" : heading.trim();
    if (results == null || results.isEmpty()) {
      return title + ": Keine Einträge gefunden.";
    }
    StringBuilder out = new StringBuilder();
    out.append(title).append(" (").append(results.size()).append("):\n");
    for (int i = 0; i < results.size(); i++) {
      PropertySummary item = results.get(i);
      out.append(i + 1).append(". ").append(item.name().isEmpty() ? "(ohne Name)" : item.name());
      if (!item.version().isEmpty()) {
        out.append(" (Version ").append(item.version()).append(")");
      }
      if (item.deprecated()) {
        out.append(" [veraltet]");
      }
      List<String> meta = new ArrayList<>();
      if (!item.dataType().isEmpty()) {
        meta.add("Datentyp: " + item.dataType());
      }
      if (!item.units().isEmpty()) {
        meta.add("Einheit: " + String.join(", ", item.units()));
      }
      if (!item.organisation().isEmpty()) {
        meta.add("Organisation: " + item.organisation());
      }
      if (item.guid() != null) {
        meta.add("GUID: " + item.guid());
      }
      if (!meta.isEmpty()) {
        out.append("\n   ").append(String.join(" | ", meta));
      }
      if (!item.definition().isEmpty()) {
        out.append("\n   Definition: ").append(item.definition());
      }
      if (i + 1 < results.size()) {
        out.append('\n');
      }
    }
    return out.toString();
  }

  /** Format a {@link PropertyDetail} as readable German text. */
  public static String formatPropertyDetail(PropertyDetail detail) {
    if (detail == null) {
      return "Merkmal nicht gefunden.";
    }
    StringBuilder out = new StringBuilder();
    out.append("Merkmal: ").append(detail.name().isEmpty() ? "(ohne Name)" : detail.name());
    if (!detail.version().isEmpty()) {
      out.append(" (Version ").append(detail.version()).append(")");
    }
    if (detail.guid() != null) {
      out.append("\nGUID: ").append(detail.guid());
    }
    if (!detail.organisation().isEmpty()) {
      out.append("\nOrganisation: ").append(detail.organisation());
    }
    if (!detail.dataType().isEmpty()) {
      out.append("\nDatentyp: ").append(detail.dataType());
    }
    if (!detail.units().isEmpty()) {
      out.append("\nEinheiten: ").append(String.join(", ", detail.units()));
    }
    if (!detail.definition().isEmpty()) {
      out.append("\nDefinition: ").append(detail.definition());
    }
    if (!detail.allowedValues().isEmpty()) {
      out.append("\nZulässige Werte: ").append(String.join(", ", detail.allowedValues()));
    }
    if (!detail.groups().isEmpty()) {
      out.append("\nMerkmalsgruppen: ");
      out.append(
          String.join(
              ", ",
              detail.groups().stream()
                  .map(
                      g ->
                          g.name().isEmpty()
                              ? (g.guid() == null ? "(ohne Name)" : g.guid().toString())
                              : g.name())
                  .toList()));
    }
    return out.toString();
  }

  /** Format a {@link PropertyGroupDetail} as readable German text. */
  public static String formatPropertyGroupDetail(PropertyGroupDetail detail) {
    if (detail == null) {
      return "Merkmalsgruppe nicht gefunden.";
    }
    StringBuilder out = new StringBuilder();
    out.append("Merkmalsgruppe: ").append(detail.name().isEmpty() ? "(ohne Name)" : detail.name());
    if (!detail.version().isEmpty()) {
      out.append(" (Version ").append(detail.version()).append(")");
    }
    if (detail.guid() != null) {
      out.append("\nGUID: ").append(detail.guid());
    }
    if (!detail.organisation().isEmpty()) {
      out.append("\nOrganisation: ").append(detail.organisation());
    }
    if (!detail.definition().isEmpty()) {
      out.append("\nDefinition: ").append(detail.definition());
    }
    out.append("\nMerkmale (").append(detail.properties().size()).append("):");
    if (detail.properties().isEmpty()) {
      out.append(" keine");
    } else {
      for (MemberProperty member : detail.properties()) {
        out.append("\n- ").append(member.name().isEmpty() ? "(ohne Name)" : member.name());
        List<String> meta = new ArrayList<>();
        if (!member.dataType().isEmpty()) {
          meta.add(member.dataType());
        }
        if (!member.units().isEmpty()) {
          meta.add(String.join(", ", member.units()));
        }
        if (member.guid() != null) {
          meta.add(member.guid().toString());
        }
        if (!meta.isEmpty()) {
          out.append(" (").append(String.join(" | ", meta)).append(")");
        }
      }
    }
    return out.toString();
  }

  // === Internal mapping & paging ===

  @FunctionalInterface
  private interface SearchPage {
    List<PropertyOrGroupForPublicDto> fetch(PropertyOrGroupForPublicRequest request);
  }

  private static List<PropertySummary> pagedSearch(String query, int limit, SearchPage searchPage) {
    List<PropertySummary> results = new ArrayList<>();
    for (int page = 0; page < MAX_PAGES && results.size() < limit; page++) {
      PropertyOrGroupForPublicRequest request = new PropertyOrGroupForPublicRequest();
      request.setPageNumber(page);
      if (query != null && !query.isBlank()) {
        request.setSearchString(query.trim());
      }
      List<PropertyOrGroupForPublicDto> batch = searchPage.fetch(request);
      if (batch == null || batch.isEmpty()) {
        break;
      }
      for (PropertyOrGroupForPublicDto dto : batch) {
        if (dto == null) {
          continue;
        }
        results.add(toSummary(dto));
        if (results.size() >= limit) {
          break;
        }
      }
      if (!hasMorePages(batch)) {
        break;
      }
    }
    return List.copyOf(results);
  }

  private static boolean hasMorePages(List<PropertyOrGroupForPublicDto> batch) {
    for (PropertyOrGroupForPublicDto dto : batch) {
      if (dto != null && dto.getCurrentPage() != null && dto.getMaxPage() != null) {
        return dto.getCurrentPage() < dto.getMaxPage();
      }
    }
    return batch.size() >= PAGE_SIZE;
  }

  private static PropertySummary toSummary(PropertyOrGroupForPublicDto dto) {
    return new PropertySummary(
        dto.getGuid(),
        dto.getName(),
        formatVersion(dto.getVersionNumber(), dto.getRevisionNumber()),
        dto.getDefinition(),
        dto.getDataType(),
        dto.getUnits(),
        dto.getOrganisationName(),
        dto.getCategory() == null ? "" : dto.getCategory().getValue(),
        Boolean.TRUE.equals(dto.getDeprecated()));
  }

  private static PropertyDetail toPropertyDetail(PropertyDto dto) {
    List<GroupRef> groups =
        dto.getGroupOfProperties() == null
            ? List.of()
            : dto.getGroupOfProperties().stream()
                .filter(Objects::nonNull)
                .map(PortalProperties::toGroupRef)
                .toList();
    return new PropertyDetail(
        dto.getGuid(),
        extractName(dto.getNamesInLanguage()),
        formatVersion(dto.getVersionNumber(), dto.getRevisionNumber()),
        extractDefinition(dto.getDefinitionsInLanguage()),
        dto.getDataType(),
        dto.getUnits(),
        extractAllowedValues(dto.getListOfPossibleValuesInLanguage()),
        groups,
        dto.getOrganisationName());
  }

  private static GroupRef toGroupRef(SimpleParentGroup group) {
    UUID guid = group.getGuid() != null ? group.getGuid() : group.getId();
    return new GroupRef(guid, extractName(group.getNamesInLanguage()));
  }

  private static PropertyGroupDetail toPropertyGroupDetail(PropertyGroupDto dto) {
    List<MemberProperty> members = new ArrayList<>();
    Set<UUID> seen = new LinkedHashSet<>();
    if (dto.getProperties() != null) {
      for (PropertyDto property : dto.getProperties()) {
        if (property == null) {
          continue;
        }
        UUID guid = property.getGuid() != null ? property.getGuid() : property.getId();
        if (guid != null) {
          seen.add(guid);
        }
        members.add(
            new MemberProperty(
                guid,
                extractName(property.getNamesInLanguage()),
                property.getDataType(),
                property.getUnits()));
      }
    }
    if (dto.getSimpleInheritedProperties() != null) {
      for (SimpleInheritedPropertyDto inherited : dto.getSimpleInheritedProperties()) {
        if (inherited == null) {
          continue;
        }
        UUID id = inherited.getId();
        if (id != null && !seen.add(id)) {
          continue;
        }
        members.add(
            new MemberProperty(id, extractInheritedName(inherited.getNames()), "", List.of()));
      }
    }
    return new PropertyGroupDetail(
        dto.getGuid(),
        extractName(dto.getNamesInLanguage()),
        formatVersion(dto.getVersionNumber(), dto.getRevisionNumber()),
        extractDefinition(dto.getDefinitionsInLanguage()),
        members,
        dto.getOrganisationName());
  }

  private static FilterGroup toFilterGroup(TagGroupForPublicDto dto) {
    List<FilterTag> tags =
        dto.getFilter() == null
            ? List.of()
            : dto.getFilter().stream()
                .filter(Objects::nonNull)
                .map(PortalProperties::toFilterTag)
                .toList();
    return new FilterGroup(dto.getGuid(), dto.getName(), tags);
  }

  private static FilterTag toFilterTag(TagForPublicDto dto) {
    return new FilterTag(dto.getGuid(), dto.getName());
  }

  private static String extractName(List<NamesInLanguageDto> names) {
    if (names == null || names.isEmpty()) {
      return "";
    }
    String fallback = "";
    for (NamesInLanguageDto entry : names) {
      if (entry == null) {
        continue;
      }
      String text = firstNonBlank(entry.getName(), entry.getValue());
      if (text.isEmpty()) {
        continue;
      }
      if (isGerman(entry.getLanguage())) {
        return text;
      }
      if (fallback.isEmpty()) {
        fallback = text;
      }
    }
    return fallback;
  }

  private static String extractInheritedName(Collection<NamesInLanguage> names) {
    if (names == null || names.isEmpty()) {
      return "";
    }
    String fallback = "";
    for (NamesInLanguage entry : names) {
      if (entry == null) {
        continue;
      }
      String text = firstNonBlank(entry.getName(), entry.getValue());
      if (text.isEmpty()) {
        continue;
      }
      if (isGerman(entry.getLanguage())) {
        return text;
      }
      if (fallback.isEmpty()) {
        fallback = text;
      }
    }
    return fallback;
  }

  private static String extractDefinition(List<DefinitionsInLanguageDto> definitions) {
    if (definitions == null || definitions.isEmpty()) {
      return "";
    }
    String fallback = "";
    for (DefinitionsInLanguageDto entry : definitions) {
      if (entry == null) {
        continue;
      }
      String text = firstNonBlank(entry.getDefinition(), entry.getValue());
      if (text.isEmpty()) {
        continue;
      }
      if (isGerman(entry.getLanguage())) {
        return text;
      }
      if (fallback.isEmpty()) {
        fallback = text;
      }
    }
    return fallback;
  }

  private static List<String> extractAllowedValues(List<ListOfPossibleValuesInLanguageDto> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    Set<String> german = new LinkedHashSet<>();
    Set<String> all = new LinkedHashSet<>();
    for (ListOfPossibleValuesInLanguageDto entry : values) {
      if (entry == null) {
        continue;
      }
      String text = firstNonBlank(entry.getPossibleValue(), entry.getValue());
      if (text.isEmpty()) {
        continue;
      }
      all.add(text);
      if (isGerman(entry.getLanguage())) {
        german.add(text);
      }
    }
    return List.copyOf(german.isEmpty() ? all : german);
  }

  private static boolean isGerman(String language) {
    if (language == null || language.isBlank()) {
      return false;
    }
    String normalized = language.trim().toLowerCase(Locale.ROOT);
    return normalized.equals("de") || normalized.startsWith("de-") || normalized.startsWith("de_");
  }

  private static String formatVersion(Long versionNumber, Long revisionNumber) {
    if (versionNumber == null && revisionNumber == null) {
      return "";
    }
    if (versionNumber == null) {
      return "0." + revisionNumber;
    }
    return revisionNumber == null ? versionNumber.toString() : versionNumber + "." + revisionNumber;
  }

  private static String firstNonBlank(String first, String second) {
    String cleanFirst = cleanText(first);
    return cleanFirst.isEmpty() ? cleanText(second) : cleanFirst;
  }

  private static String cleanText(String value) {
    return value == null ? "" : value.trim();
  }

  private static List<String> cleanList(List<String> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    return values.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }
}
