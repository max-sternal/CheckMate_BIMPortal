package com.bimportal.hackathon.ifc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compact IFC schema metadata for IFC2X3, IFC4 and IFC4X3, loaded from the bundled TSV tables under
 * {@code /ifc/schema/*.tsv}.
 *
 * <p>Provides canonical entity and value-type names, subtype checks ({@code IfcObject}, {@code
 * IfcTypeObject}, {@code IfcRelDefinesByProperties}) and attribute slot indices for {@code
 * PredefinedType}, {@code ElementType} and {@code ProcessType}, generated from the EXPRESS schemas
 * by {@code src/main/python/generate_ifc_schemas.py}.
 */
public final class IfcSchema {

  private static final int MAX_INHERITANCE_DEPTH = 64;

  /** The schemas CheckMate reads: IFC2X3, IFC4 and IFC4X3, with addenda and corrigenda. */
  private static final Pattern SUPPORTED_SCHEMA = Pattern.compile("IFC(2X3|4|4X3)(_[A-Z0-9]+)?");

  private static final class Holder {
    static final IfcSchema IFC2X3 = load("IFC2X3", "/ifc/schema/ifc2x3.tsv");
    static final IfcSchema IFC4 = load("IFC4", "/ifc/schema/ifc4.tsv");
    static final IfcSchema IFC4X3 = load("IFC4X3_ADD2", "/ifc/schema/ifc4x3.tsv");
  }

  private record EntityInfo(
      String canonicalName,
      String supertypeUpper,
      boolean isObject,
      boolean isTypeObject,
      int predefinedTypeSlot,
      int elementTypeSlot,
      int processTypeSlot) {}

  private final String name;
  private final Map<String, EntityInfo> entities;
  private final Map<String, String> canonicalNames;

  private IfcSchema(
      String name, Map<String, EntityInfo> entities, Map<String, String> canonicalNames) {
    this.name = name;
    this.entities = Map.copyOf(entities);
    this.canonicalNames = Map.copyOf(canonicalNames);
  }

  /**
   * Schema identifier of the loaded table, e.g. {@code IFC2X3}, {@code IFC4}, {@code IFC4X3_ADD2}.
   */
  public String name() {
    return name;
  }

  /**
   * Look up the schema table matching a {@code FILE_SCHEMA} declaration.
   *
   * <p>Accepts {@code IFC2X3}, {@code IFC4} and {@code IFC4X3}, optionally followed by an
   * underscore suffix such as {@code _TC1} or {@code _ADD2}.
   *
   * @return the matching schema, or null when the schema is unsupported
   */
  public static IfcSchema forDeclaredSchema(String declaredSchema) {
    if (declaredSchema == null || declaredSchema.isBlank()) {
      return null;
    }
    Matcher matcher = SUPPORTED_SCHEMA.matcher(declaredSchema.trim().toUpperCase(Locale.ROOT));
    if (!matcher.matches()) {
      return null;
    }
    return switch (matcher.group(1)) {
      case "2X3" -> Holder.IFC2X3;
      case "4" -> Holder.IFC4;
      case "4X3" -> Holder.IFC4X3;
      default -> null;
    };
  }

  /**
   * The EXPRESS-cased name of an entity or declared type, e.g. {@code IFCSLAB} -> {@code IfcSlab},
   * {@code IFCLABEL} -> {@code IfcLabel}. Falls back to {@code upper} when unknown.
   */
  public String canonicalName(String upper) {
    if (upper == null) {
      return null;
    }
    String canonical = canonicalNames.get(normalize(upper));
    return canonical != null ? canonical : upper;
  }

  /** Whether the entity inherits from {@code IfcObject} in this schema. */
  public boolean isObject(String upper) {
    EntityInfo info = entity(upper);
    return info != null && info.isObject();
  }

  /** Whether the entity inherits from {@code IfcTypeObject} in this schema. */
  public boolean isTypeObject(String upper) {
    EntityInfo info = entity(upper);
    return info != null && info.isTypeObject();
  }

  /** Whether {@code upper} is {@code targetUpper} or a subtype of it in this schema. */
  public boolean isSubtypeOf(String upper, String targetUpper) {
    if (upper == null || targetUpper == null) {
      return false;
    }
    String current = normalize(upper);
    String target = normalize(targetUpper);
    for (int depth = 0; current != null && depth < MAX_INHERITANCE_DEPTH; depth++) {
      if (current.equals(target)) {
        return true;
      }
      EntityInfo info = entities.get(current);
      if (info == null) {
        return false;
      }
      current = info.supertypeUpper();
    }
    return false;
  }

  /** 0-based attribute slot of {@code PredefinedType}, or -1 when the entity has none. */
  public int predefinedTypeSlot(String upper) {
    EntityInfo info = entity(upper);
    return info != null ? info.predefinedTypeSlot() : -1;
  }

  /** 0-based attribute slot of {@code ElementType}, or -1 when the entity has none. */
  public int elementTypeSlot(String upper) {
    EntityInfo info = entity(upper);
    return info != null ? info.elementTypeSlot() : -1;
  }

  /** 0-based attribute slot of {@code ProcessType}, or -1 when the entity has none. */
  public int processTypeSlot(String upper) {
    EntityInfo info = entity(upper);
    return info != null ? info.processTypeSlot() : -1;
  }

  private EntityInfo entity(String upper) {
    return upper == null ? null : entities.get(normalize(upper));
  }

  private static String normalize(String name) {
    return name.toUpperCase(Locale.ROOT);
  }

  private static IfcSchema load(String name, String resource) {
    InputStream in = IfcSchema.class.getResourceAsStream(resource);
    if (in == null) {
      throw new IllegalStateException(
          "IFC schema resource " + resource + " is missing from the classpath");
    }
    Map<String, EntityInfo> entities = new HashMap<>();
    Map<String, String> canonicalNames = new HashMap<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line;
      int lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        String[] cols = line.split("\t", -1);
        if ("E".equals(cols[0]) && cols.length == 9) {
          String upper = cols[1];
          String canonical = cols[2];
          String supertype = cols[3].isEmpty() ? null : cols[3];
          boolean isObject = "1".equals(cols[4]);
          boolean isTypeObject = "1".equals(cols[5]);
          int predefinedSlot = Integer.parseInt(cols[6]);
          int elementSlot = Integer.parseInt(cols[7]);
          int processSlot = Integer.parseInt(cols[8]);
          entities.put(
              upper,
              new EntityInfo(
                  canonical,
                  supertype,
                  isObject,
                  isTypeObject,
                  predefinedSlot,
                  elementSlot,
                  processSlot));
          canonicalNames.put(upper, canonical);
        } else if ("T".equals(cols[0]) && cols.length == 3) {
          canonicalNames.put(cols[1], cols[2]);
        } else {
          throw new IllegalStateException(
              "Malformed row in " + resource + " at line " + lineNumber + ": " + line);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not load IFC schema resource " + resource, e);
    }
    return new IfcSchema(name, entities, canonicalNames);
  }
}
