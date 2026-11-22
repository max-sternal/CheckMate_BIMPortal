package com.bimportal.hackathon.ifc;

import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reads a parsed {@link StepModel} into the plain {@link ModelElement} records an IDS check works
 * on. IFC2X3, IFC4 and IFC4X3.
 *
 * <p>This is a faithful reading of the file: property addresses are taken exactly as the model
 * spells them. Translating them to what a LOIN asks for happens in {@code PropertyAliases}, so the
 * two concerns stay apart - this class can be pointed at any IFC, and the mapping can be reviewed
 * and tested without one.
 *
 * <p>What is read, and how, follows IfcOpenShell, the library behind ifctester: every {@code
 * IfcObject}; the single, enumerated and list values of its property sets and of its type's, the
 * element's own winning; the PredefinedType as IDS reads it. Quantity sets are skipped - the BIM
 * Portal's LOINs do not ask for quantities.
 */
public final class IfcModelElements {

  private static final int GLOBAL_ID_SLOT = 0;
  private static final int NAME_SLOT = 2;
  private static final int OBJECT_TYPE_SLOT = 4;
  private static final int TYPE_PROPERTY_SETS_SLOT = 5;
  private static final int REL_RELATED_OBJECTS_SLOT = 4;
  private static final int REL_RELATING_TARGET_SLOT = 5;
  private static final int PSET_HAS_PROPERTIES_SLOT = 4;
  private static final int PROP_NAME_SLOT = 0;
  private static final int PROP_VALUE_SLOT = 2;

  private static final String USERDEFINED = "USERDEFINED";
  private static final String NOTDEFINED = "NOTDEFINED";

  private IfcModelElements() {}

  /**
   * Read every object that can carry properties, in the order of the file.
   *
   * <p>Entities in {@link StepModel#entities()} preserve file encounter order, so repeated reads of
   * the same model always list their elements identically.
   */
  public static List<ModelElement> from(StepModel model) {
    IfcSchema schema = model.schema();
    Definitions definitions = Definitions.of(model);
    List<ModelElement> elements = new ArrayList<>();
    for (StepEntity entity : model.entities()) {
      // In IFC2X3 IfcProject is an IfcObject; in IFC4 and IFC4X3 it is an IfcContext and excluded.
      if (schema.isObject(entity.typeUpper())) {
        elements.add(toElement(entity, model, definitions));
      }
    }
    return List.copyOf(elements);
  }

  /**
   * The IFC schema the model declares, e.g. {@code IFC2X3}.
   *
   * <p>Needed to pick the right specification out of an IDS file, which the portal writes once per
   * schema version.
   */
  public static String schemaOf(StepModel model) {
    return model.declaredSchema();
  }

  private static ModelElement toElement(
      StepEntity entity, StepModel model, Definitions definitions) {
    IfcSchema schema = model.schema();
    Long typeId = definitions.types().get(entity.id());
    StepEntity candidateType = typeId == null ? null : model.byId(typeId);
    StepEntity type =
        candidateType != null && schema.isTypeObject(candidateType.typeUpper())
            ? candidateType
            : null;

    Map<PropertyKey, ModelProperty> properties = new LinkedHashMap<>();

    // Properties on the element itself first, then the ones it inherits from its type, so an
    // element that overrides a type property is reported with its own value.
    for (Long definitionId : definitions.propertySets().getOrDefault(entity.id(), List.of())) {
      collect(model.byId(definitionId), model, properties, true);
    }
    if (type != null) {
      for (StepValue item : type.list(TYPE_PROPERTY_SETS_SLOT)) {
        if (item instanceof StepValue.EntityRef ref) {
          collect(model.byId(ref.id()), model, properties, false);
        }
      }
    }

    return new ModelElement(
        idOf(entity),
        entity.text(NAME_SLOT),
        schema.canonicalName(entity.typeUpper()),
        predefinedType(entity, type, schema),
        userDefinedType(entity, type, schema),
        List.copyOf(properties.values()),
        type == null ? null : schema.canonicalName(type.typeUpper()));
  }

  /**
   * What each object is defined by: its own property sets and its type, indexed forward from {@code
   * IfcRelDefinesByProperties} (including {@code IfcRelOverridesProperties}) and {@code
   * IfcRelDefinesByType} via {@code RelatedObjects}.
   */
  private record Definitions(Map<Long, List<Long>> propertySets, Map<Long, Long> types) {

    static Definitions of(StepModel model) {
      IfcSchema schema = model.schema();
      Map<Long, List<Long>> sets = new HashMap<>();
      Map<Long, Long> types = new HashMap<>();

      for (StepEntity entity : model.entities()) {
        String typeUpper = entity.typeUpper();
        if (schema.isSubtypeOf(typeUpper, "IFCRELDEFINESBYPROPERTIES")) {
          List<Long> definitionIds = definitionIds(entity.arg(REL_RELATING_TARGET_SLOT));
          if (definitionIds.isEmpty()) {
            continue;
          }
          for (StepValue related : entity.list(REL_RELATED_OBJECTS_SLOT)) {
            if (related instanceof StepValue.EntityRef objectRef) {
              sets.computeIfAbsent(objectRef.id(), key -> new ArrayList<>()).addAll(definitionIds);
            }
          }
        } else if ("IFCRELDEFINESBYTYPE".equals(typeUpper)) {
          Long typeId = entity.ref(REL_RELATING_TARGET_SLOT);
          if (typeId == null) {
            continue;
          }
          // An object has one type; IfcOpenShell, too, takes the first relation it finds.
          for (StepValue related : entity.list(REL_RELATED_OBJECTS_SLOT)) {
            if (related instanceof StepValue.EntityRef objectRef) {
              types.putIfAbsent(objectRef.id(), typeId);
            }
          }
        }
      }
      return new Definitions(sets, types);
    }

    /**
     * Extract referenced property-set definition ids: a single {@code #ref}, a list of refs, or an
     * {@code IFCPROPERTYSETDEFINITIONSET((#1,#2))} typed wrapper (IFC4 and later).
     */
    private static List<Long> definitionIds(StepValue relating) {
      if (relating instanceof StepValue.EntityRef single) {
        return List.of(single.id());
      }
      if (relating instanceof StepValue.TypedVal typed) {
        return definitionIds(typed.value());
      }
      if (relating instanceof StepValue.ListVal list) {
        List<Long> ids = new ArrayList<>(list.items().size());
        for (StepValue item : list.items()) {
          if (item instanceof StepValue.EntityRef ref) {
            ids.add(ref.id());
          }
        }
        return ids;
      }
      return List.of();
    }
  }

  private static void collect(
      StepEntity definition,
      StepModel model,
      Map<PropertyKey, ModelProperty> properties,
      boolean overwrite) {
    if (definition == null || !"IFCPROPERTYSET".equals(definition.typeUpper())) {
      return;
    }
    String setName = definition.text(NAME_SLOT);
    for (StepValue item : definition.list(PSET_HAS_PROPERTIES_SLOT)) {
      if (!(item instanceof StepValue.EntityRef ref)) {
        continue;
      }
      ModelProperty read = read(setName, model.byId(ref.id()), model.schema());
      if (read == null) {
        continue;
      }
      if (overwrite) {
        properties.put(read.key(), read);
      } else {
        properties.putIfAbsent(read.key(), read);
      }
    }
  }

  /**
   * One property with its value or values.
   *
   * @return null for kinds an IDS property facet does not compare (bounded, table, complex,
   *     reference) and for properties without a name
   */
  private static ModelProperty read(String setName, StepEntity property, IfcSchema schema) {
    if (property == null) {
      return null;
    }
    List<StepValue> rawValues;
    switch (property.typeUpper()) {
      case "IFCPROPERTYSINGLEVALUE" -> rawValues = List.of(property.arg(PROP_VALUE_SLOT));
      case "IFCPROPERTYENUMERATEDVALUE", "IFCPROPERTYLISTVALUE" ->
          rawValues = property.list(PROP_VALUE_SLOT);
      default -> {
        return null;
      }
    }
    String name = property.text(PROP_NAME_SLOT);
    if (name == null || name.isBlank()) {
      return null;
    }

    List<String> values = new ArrayList<>();
    String dataType = null;
    for (StepValue raw : rawValues) {
      if (raw == null || raw instanceof StepValue.Null || raw instanceof StepValue.Derived) {
        continue;
      }
      String valueTypeUpper = null;
      StepValue inner = raw;
      if (raw instanceof StepValue.TypedVal typed) {
        valueTypeUpper = typed.typeUpper();
        inner = typed.value();
        if (inner == null
            || inner instanceof StepValue.Null
            || inner instanceof StepValue.Derived) {
          continue;
        }
      }
      String text = valueText(inner, valueTypeUpper);
      if (text != null) {
        values.add(text);
        if (dataType == null && valueTypeUpper != null) {
          dataType = schema.canonicalName(valueTypeUpper);
        }
      }
    }

    return new ModelProperty(new PropertyKey(setName, name), values, dataType);
  }

  /**
   * The PredefinedType as IDS reads it, the way IfcOpenShell's {@code get_predefined_type} does.
   *
   * <p>The type object decides first, unless it says nothing or NOTDEFINED; then the element. A
   * USERDEFINED value stands for the name the model gives instead: the type's ElementType (or
   * ProcessType), the element's ObjectType.
   */
  static String predefinedType(StepEntity element, StepEntity type, IfcSchema schema) {
    if (type != null) {
      String value = enumAttribute(type, schema);
      if (value == null || USERDEFINED.equals(value)) {
        value = elementType(type, schema);
      }
      if (value != null && !NOTDEFINED.equals(value)) {
        return value;
      }
    }

    String value = enumAttribute(element, schema);
    if (value == null || USERDEFINED.equals(value)) {
      value = blankToNull(element.text(OBJECT_TYPE_SLOT));
    }
    return value;
  }

  /**
   * Whether the PredefinedType is user-defined, the way IfcOpenShell's {@code is_userdefined_type}
   * does it - which is what an IDS facet asking for USERDEFINED checks.
   *
   * <p>Note the last case: an element without any PredefinedType but with an ObjectType counts as
   * user-defined too.
   */
  static boolean userDefinedType(StepEntity element, StepEntity type, IfcSchema schema) {
    if (type != null) {
      String value = enumAttribute(type, schema);
      if (USERDEFINED.equals(value)) {
        return true;
      }
      if (value == null && elementType(type, schema) != null) {
        return true;
      }
      if (value != null && !NOTDEFINED.equals(value)) {
        return false;
      }
    }

    String value = enumAttribute(element, schema);
    if (USERDEFINED.equals(value)) {
      return true;
    }
    return value == null && blankToNull(element.text(OBJECT_TYPE_SLOT)) != null;
  }

  /** The PredefinedType attribute's enum value, e.g. {@code FLOOR}, or null when absent/unset. */
  private static String enumAttribute(StepEntity entity, IfcSchema schema) {
    int slot = schema.predefinedTypeSlot(entity.typeUpper());
    if (slot < 0) {
      return null;
    }
    return blankToNull(entity.enumName(slot));
  }

  /**
   * The name a type gives a user-defined type: ElementType, or ProcessType for process types. Like
   * IfcOpenShell, ProcessType is only consulted when there is no ElementType attribute.
   */
  private static String elementType(StepEntity type, IfcSchema schema) {
    int elementSlot = schema.elementTypeSlot(type.typeUpper());
    if (elementSlot >= 0) {
      return blankToNull(type.text(elementSlot));
    }
    int processSlot = schema.processTypeSlot(type.typeUpper());
    return processSlot >= 0 ? blankToNull(type.text(processSlot)) : null;
  }

  private static String idOf(StepEntity entity) {
    String guid = entity.text(GLOBAL_ID_SLOT);
    return guid == null || guid.isBlank() ? "#" + entity.id() : guid;
  }

  /** Convert an {@code IfcValue} payload to text for comparison with IDS requirements. */
  private static String valueText(StepValue value, String typeUpper) {
    return switch (value) {
      case StepValue.Null ignored -> null;
      case StepValue.Derived ignored -> null;
      case StepValue.Text text -> text.value();
      case StepValue.EnumVal enumeration -> enumValueText(enumeration.name(), typeUpper);
      case StepValue.IntegerVal integer -> Long.toString(integer.value());
      case StepValue.RealVal real -> number(real.value());
      case StepValue.BinaryVal binary -> binary.raw();
      case StepValue.EntityRef ref -> "#" + ref.id();
      case StepValue.TypedVal typed -> valueText(typed.value(), typed.typeUpper());
      case StepValue.ListVal list ->
          list.items().stream()
              .map(item -> valueText(item, null))
              .filter(Objects::nonNull)
              .collect(Collectors.joining(", "));
    };
  }

  private static String enumValueText(String name, String typeUpper) {
    if ("IFCLOGICAL".equals(typeUpper) || "IFCBOOLEAN".equals(typeUpper)) {
      return switch (name) {
        case "T" -> "true";
        case "F" -> "false";
        case "U" -> "UNKNOWN";
        default -> name;
      };
    }
    return name;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  /**
   * Whole doubles within exact IEEE-754 integer precision without the trailing {@code .0}, so 1.0
   * compares equal to a required "1" without overflowing huge values like {@code 1e20}.
   */
  private static String number(double value) {
    return Math.abs(value) < (1L << 53) && value == Math.rint(value)
        ? Long.toString((long) value)
        : Double.toString(value);
  }
}
