package com.bimportal.hackathon.ids;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which property addresses a model uses for the ones a LOIN asks for, read from a JSON file.
 *
 * <pre>{@code
 * {
 *   "name": "BIM.Hamburg Brücken",
 *   "aliases": [
 *     { "from": "Klassifizierung/Objekttyp", "to": ["Pset_Objektinformation/_IDEbene3"] }
 *   ]
 * }
 * }</pre>
 *
 * An address is {@code PropertySet/Name}, split at the first slash. {@code to} lists the addresses
 * to try, in order, after the one the LOIN names. Names are compared exactly, as IDS does. The
 * mapping is a convention of a project or a modelling guideline, not a rule, so it comes with the
 * model, not with the code.
 */
public record PropertyMapping(String name, Map<PropertyKey, List<PropertyKey>> aliases) {

  /** The mapping CheckMate ships: the BIM.Hamburg bridge models' convention. */
  public static final String BUNDLED = "/mapping/bim-hamburg.json";

  /** No aliases: every LOIN is checked against the addresses it names, as ifctester does. */
  public static final PropertyMapping NONE = new PropertyMapping("keine", Map.of());

  private static final ObjectMapper JSON = new ObjectMapper();

  public PropertyMapping {
    aliases = Map.copyOf(aliases);
  }

  /** The bundled mapping. */
  public static PropertyMapping bundled() {
    try (InputStream in = PropertyMapping.class.getResourceAsStream(BUNDLED)) {
      if (in == null) {
        throw new IllegalStateException("Mitgeliefertes Mapping fehlt: " + BUNDLED);
      }
      return parse(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Mitgeliefertes Mapping nicht lesbar: " + e.getMessage(), e);
    }
  }

  /**
   * Read a mapping file.
   *
   * @throws IllegalArgumentException when the file is missing or not a mapping, naming what is
   *     wrong
   */
  public static PropertyMapping load(Path file) {
    try {
      return parse(Files.readString(file));
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Mapping-Datei nicht lesbar: " + file + " (" + e.getMessage() + ")", e);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(file + ": " + e.getMessage(), e);
    }
  }

  /** Parse the JSON text of a mapping. */
  public static PropertyMapping parse(String text) {
    JsonNode root;
    try {
      root = JSON.readTree(text);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException("kein gültiges JSON: " + e.getOriginalMessage(), e);
    }
    if (root == null || !root.isObject() || !root.path("aliases").isArray()) {
      throw new IllegalArgumentException(
          "erwartet {\"aliases\": [{\"from\": ..., \"to\": [...]}]}");
    }
    Map<PropertyKey, List<PropertyKey>> aliases = new LinkedHashMap<>();
    int index = 0;
    for (JsonNode entry : root.get("aliases")) {
      index++;
      PropertyKey from = address(entry.path("from"), "aliases[" + index + "].from");
      JsonNode to = entry.path("to");
      if (!to.isArray() || to.isEmpty()) {
        throw new IllegalArgumentException(
            "aliases[" + index + "].to: Liste mit mindestens einer Adresse erwartet");
      }
      List<PropertyKey> targets = new ArrayList<>();
      for (JsonNode target : to) {
        targets.add(address(target, "aliases[" + index + "].to"));
      }
      aliases.merge(
          from,
          targets,
          (a, b) -> {
            List<PropertyKey> both = new ArrayList<>(a);
            both.addAll(b);
            return both;
          });
    }
    return new PropertyMapping(root.path("name").asText("ohne Namen"), aliases);
  }

  private static PropertyKey address(JsonNode node, String where) {
    String text = node.isTextual() ? node.asText().trim() : "";
    int slash = text.indexOf('/');
    if (slash <= 0 || slash == text.length() - 1) {
      throw new IllegalArgumentException(where + ": Adresse 'PropertySet/Name' erwartet");
    }
    return new PropertyKey(text.substring(0, slash), text.substring(slash + 1));
  }

  /** Every address worth looking at for a requested one, the requested address first. */
  public List<PropertyKey> candidates(PropertyKey requested) {
    List<PropertyKey> candidates = new ArrayList<>();
    candidates.add(requested);
    aliases.entrySet().stream()
        .filter(entry -> entry.getKey().matches(requested))
        .findFirst()
        .ifPresent(entry -> candidates.addAll(entry.getValue()));
    return candidates;
  }

  /** One line per alias, for a person or an AI to read. */
  public List<String> describe() {
    List<String> lines = new ArrayList<>();
    lines.add("Mapping '" + name + "': " + aliases.size() + " Alias(e)");
    aliases.forEach(
        (from, to) ->
            lines.add(
                "   "
                    + from.propertySet()
                    + "/"
                    + from.baseName()
                    + " -> "
                    + String.join(
                        ", ",
                        to.stream().map(k -> k.propertySet() + "/" + k.baseName()).toList())));
    return lines;
  }
}
