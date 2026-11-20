package test.fx;

import com.apstex.ifctoolbox.ifc.IfcAreaMeasure;
import com.apstex.ifctoolbox.ifc.IfcBoolean;
import com.apstex.ifctoolbox.ifc.IfcElementQuantity;
import com.apstex.ifctoolbox.ifc.IfcGloballyUniqueId;
import com.apstex.ifctoolbox.ifc.IfcIdentifier;
import com.apstex.ifctoolbox.ifc.IfcInteger;
import com.apstex.ifctoolbox.ifc.IfcLabel;
import com.apstex.ifctoolbox.ifc.IfcLengthMeasure;
import com.apstex.ifctoolbox.ifc.IfcObject;
import com.apstex.ifctoolbox.ifc.IfcProperty;
import com.apstex.ifctoolbox.ifc.IfcPropertyEnumeratedValue;
import com.apstex.ifctoolbox.ifc.IfcPropertyListValue;
import com.apstex.ifctoolbox.ifc.IfcPropertySet;
import com.apstex.ifctoolbox.ifc.IfcPropertySetDefinition;
import com.apstex.ifctoolbox.ifc.IfcPropertySingleValue;
import com.apstex.ifctoolbox.ifc.IfcReal;
import com.apstex.ifctoolbox.ifc.IfcRelDefinesByProperties;
import com.apstex.ifctoolbox.ifc.IfcRelDefinesByType;
import com.apstex.ifctoolbox.ifc.IfcText;
import com.apstex.ifctoolbox.ifc.IfcTypeObject;
import com.apstex.ifctoolbox.ifc.IfcValue;
import com.apstex.ifctoolbox.ifc.IfcVolumeMeasure;
import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import com.apstex.step.core.ClassInterface;
import com.apstex.step.core.ENUM;
import com.apstex.step.header.File_Schema;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads an Apstex {@link IfcModel} into the plain {@link ModelElement} records an IDS check works
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

  private static final Pattern SCHEMA_NAME = Pattern.compile("IFC[0-9][0-9A-Z_]*");

  private static final String USERDEFINED = "USERDEFINED";
  private static final String NOTDEFINED = "NOTDEFINED";

  private IfcModelElements() {}

  /** Read every object that can carry properties. */
  public static List<ModelElement> from(IfcModel model) {
    Definitions definitions = Definitions.of(model);
    List<ModelElement> elements = new ArrayList<>();
    for (ClassInterface object : model.getObjects()) {
      // The schema-specific interfaces, not the common one: in IFC4, IfcProject is an
      // IfcContext and not an IfcObject, and it must not be counted as one.
      if (object instanceof IfcObject.Ifc2x3
          || object instanceof IfcObject.Ifc4
          || object instanceof IfcObject.Ifc4x3) {
        elements.add(toElement((IfcObject) object, definitions));
      }
    }
    return elements;
  }

  /**
   * The IFC schema the model declares, e.g. {@code IFC2X3}.
   *
   * <p>Needed to pick the right specification out of an IDS file, which the portal writes once per
   * schema version.
   *
   * @return the schema name, or null when the header does not state one
   */
  public static String schemaOf(IfcModel model) {
    File_Schema schema = model.getFile_Schema();
    if (schema == null) {
      return null;
    }
    Matcher matcher = SCHEMA_NAME.matcher(String.valueOf(schema.getStepLine()));
    return matcher.find() ? matcher.group() : null;
  }

  private static ModelElement toElement(IfcObject object, Definitions definitions) {
    IfcTypeObject type = definitions.types().get(object);
    Map<PropertyKey, ModelProperty> properties = new LinkedHashMap<>();

    // Properties on the element itself first, then the ones it inherits from its type, so an
    // element that overrides a type property is reported with its own value.
    definitions
        .propertySets()
        .getOrDefault(object, List.of())
        .forEach(set -> collect(set, properties, true));
    if (type != null && type.getHasPropertySets() != null) {
      for (IfcPropertySetDefinition set : type.getHasPropertySets()) {
        collect(set, properties, false);
      }
    }

    return new ModelElement(
        idOf(object),
        decoded(object.getName()),
        object.getClassName(),
        predefinedType(object, type),
        userDefinedType(object, type),
        List.copyOf(properties.values()));
  }

  /**
   * What each object is defined by: its own property sets and its type, for the whole model.
   *
   * <p>Built from the relations' forward attribute {@code RelatedObjects}, deliberately not from
   * the inverse attributes {@code IsDefinedBy} / {@code IsTypedBy}. The toolbox fills those while
   * parsing and loses entries at random: on the bundled bridge model the 1027 property relations
   * showed up as between 957 and 1015 inverse links from one read to the next (2026-11-20), so
   * elements came out with properties missing that the file has. The forward attribute is read
   * straight from each relation's own line and is complete.
   *
   * <p>By identity: entities are compared as the same instance, not by content.
   */
  private record Definitions(
      Map<IfcObject, List<IfcPropertySetDefinition>> propertySets,
      Map<IfcObject, IfcTypeObject> types) {

    static Definitions of(IfcModel model) {
      Map<IfcObject, List<IfcPropertySetDefinition>> sets = new IdentityHashMap<>();
      Map<IfcObject, IfcTypeObject> types = new IdentityHashMap<>();

      for (ClassInterface entity : model.getObjects()) {
        if (entity instanceof IfcRelDefinesByProperties.Ifc2x3 relation) {
          add(sets, relation.getRelatedObjects(), relation.getRelatingPropertyDefinition());
        } else if (entity instanceof IfcRelDefinesByProperties.Ifc4 relation) {
          add(sets, relation.getRelatedObjects(), relation.getRelatingPropertyDefinition());
        } else if (entity instanceof IfcRelDefinesByProperties.Ifc4x3 relation) {
          add(sets, relation.getRelatedObjects(), relation.getRelatingPropertyDefinition());
        } else if (entity instanceof IfcRelDefinesByType relation
            && relation.getRelatingType() != null) {
          // An object has one type; IfcOpenShell, too, takes the first relation it finds.
          for (IfcObject object : nonNull(relation.getRelatedObjects())) {
            types.putIfAbsent(object, relation.getRelatingType());
          }
        }
      }
      return new Definitions(sets, types);
    }

    /**
     * Record a relation's property set definition for each object it relates: a single one, or
     * (IFC4 and later) a set of them.
     */
    private static void add(
        Map<IfcObject, List<IfcPropertySetDefinition>> sets,
        Collection<?> relatedObjects,
        Object definition) {
      List<IfcPropertySetDefinition> definitions = new ArrayList<>();
      if (definition instanceof IfcPropertySetDefinition single) {
        definitions.add(single);
      } else if (definition instanceof Iterable<?> several) {
        for (Object each : several) {
          if (each instanceof IfcPropertySetDefinition single) {
            definitions.add(single);
          }
        }
      }

      for (Object related : nonNull(relatedObjects)) {
        // In IFC4 a property relation can also point at a type or a context; only objects count.
        if (related instanceof IfcObject object) {
          sets.computeIfAbsent(object, key -> new ArrayList<>()).addAll(definitions);
        }
      }
    }
  }

  private static void collect(
      IfcPropertySetDefinition definition,
      Map<PropertyKey, ModelProperty> properties,
      boolean overwrite) {
    if (definition instanceof IfcElementQuantity) {
      return;
    }
    if (!(definition instanceof IfcPropertySet set) || set.getHasProperties() == null) {
      return;
    }

    String setName = decoded(set.getName());
    for (IfcProperty property : set.getHasProperties()) {
      ModelProperty read = read(setName, property);
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
  private static ModelProperty read(String setName, IfcProperty property) {
    String name;
    List<? extends IfcValue> values;
    if (property instanceof IfcPropertySingleValue single) {
      name = decoded(single.getName());
      values = single.getNominalValue() == null ? List.of() : List.of(single.getNominalValue());
    } else if (property instanceof IfcPropertyEnumeratedValue enumerated) {
      name = decoded(enumerated.getName());
      values = nonNull(enumerated.getEnumerationValues());
    } else if (property instanceof IfcPropertyListValue list) {
      name = decoded(list.getName());
      values = nonNull(list.getListValues());
    } else {
      return null;
    }
    if (name == null || name.isBlank()) {
      return null;
    }

    return new ModelProperty(
        new PropertyKey(setName, name),
        values.stream().map(IfcModelElements::valueText).toList(),
        values.isEmpty() ? null : dataTypeOf(values.get(0)));
  }

  /**
   * The PredefinedType as IDS reads it, the way IfcOpenShell's {@code get_predefined_type} does.
   *
   * <p>The type object decides first, unless it says nothing or NOTDEFINED; then the element. A
   * USERDEFINED value stands for the name the model gives instead: the type's ElementType, the
   * element's ObjectType.
   */
  static String predefinedType(IfcObject element, IfcTypeObject type) {
    if (type != null) {
      String value = enumAttribute(type);
      if (value == null || USERDEFINED.equals(value)) {
        value = elementType(type);
      }
      if (value != null && !NOTDEFINED.equals(value)) {
        return value;
      }
    }

    String value = enumAttribute(element);
    if (value == null || USERDEFINED.equals(value)) {
      value = blankToNull(decoded(element.getObjectType()));
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
  static boolean userDefinedType(IfcObject element, IfcTypeObject type) {
    if (type != null) {
      String value = enumAttribute(type);
      if (USERDEFINED.equals(value)) {
        return true;
      }
      if (value == null && elementType(type) != null) {
        return true;
      }
      if (value != null && !NOTDEFINED.equals(value)) {
        return false;
      }
    }

    String value = enumAttribute(element);
    if (USERDEFINED.equals(value)) {
      return true;
    }
    return value == null && blankToNull(decoded(element.getObjectType())) != null;
  }

  /**
   * The PredefinedType attribute's value, e.g. {@code FLOOR}.
   *
   * <p>Read reflectively: each of the ~150 entities that have one declares its own {@code
   * getPredefinedType()} with its own enum type, and there is no common interface to call it on.
   *
   * @return null when the entity has no such attribute or it is not set
   */
  private static String enumAttribute(Object entity) {
    Object value = invokeGetter(entity, "getPredefinedType");
    if (value instanceof ENUM enumeration && enumeration.getValue() != null) {
      return blankToNull(enumeration.getValue().name());
    }
    return null;
  }

  /**
   * The name a type gives a user-defined type: ElementType, or ProcessType for process types.
   * Like IfcOpenShell, ProcessType is only consulted when there is no ElementType attribute.
   */
  private static String elementType(IfcTypeObject type) {
    String getter = hasGetter(type, "getElementType") ? "getElementType" : "getProcessType";
    return blankToNull(decoded(invokeGetter(type, getter)));
  }

  private static boolean hasGetter(Object target, String getter) {
    try {
      target.getClass().getMethod(getter);
      return true;
    } catch (NoSuchMethodException absent) {
      return false;
    }
  }

  private static Object invokeGetter(Object target, String getter) {
    if (target == null) {
      return null;
    }
    try {
      Method method = target.getClass().getMethod(getter);
      method.setAccessible(true);
      return method.invoke(target);
    } catch (NoSuchMethodException absent) {
      return null;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Could not read " + getter + " of " + target.getClass().getName(), e);
    }
  }

  private static String idOf(IfcObject object) {
    String guid = decoded(object.getGlobalId());
    return guid == null || guid.isBlank() ? "#" + object.getStepLineNumber() : guid;
  }

  /** The IFC type of a value, e.g. {@code IfcText}, as the toolbox names it. */
  private static String dataTypeOf(IfcValue value) {
    return value == null ? null : value.getClassName();
  }

  /**
   * A value as text, so that it can be compared with the string an IDS requirement carries.
   *
   * <p>Deliberately an if-chain rather than a switch: these are unrelated interfaces from a closed
   * library, and the fallback has to cover every measure type that is not listed.
   */
  private static String valueText(IfcValue value) {
    if (value == null) {
      return null;
    }
    if (value instanceof IfcLabel label) {
      return label.getDecodedValue();
    }
    if (value instanceof IfcText text) {
      return text.getDecodedValue();
    }
    if (value instanceof IfcIdentifier identifier) {
      return identifier.getDecodedValue();
    }
    if (value instanceof IfcBoolean flag) {
      return String.valueOf(flag.getValue());
    }
    if (value instanceof IfcInteger number) {
      return String.valueOf(number.getValue());
    }
    if (value instanceof IfcReal number) {
      return number(number.getValue());
    }
    if (value instanceof IfcLengthMeasure measure) {
      return number(measure.getValue());
    }
    if (value instanceof IfcAreaMeasure measure) {
      return number(measure.getValue());
    }
    if (value instanceof IfcVolumeMeasure measure) {
      return number(measure.getValue());
    }
    // Every other measure type: the STEP serialisation is uniform, 'text' or a bare number.
    return unquote(value.getStepParameter(false));
  }

  private static String decoded(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof IfcGloballyUniqueId guid) {
      return guid.getDecodedValue();
    }
    if (value instanceof IfcLabel label) {
      return label.getDecodedValue();
    }
    if (value instanceof IfcText text) {
      return text.getDecodedValue();
    }
    if (value instanceof IfcIdentifier identifier) {
      return identifier.getDecodedValue();
    }
    return value.toString();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static <T> Collection<T> nonNull(Collection<T> collection) {
    return collection == null ? List.of() : collection;
  }

  private static <T> List<T> nonNull(List<T> list) {
    return list == null ? List.of() : list;
  }

  /** Whole doubles without the trailing {@code .0}, so 1.0 compares equal to a required "1". */
  private static String number(double value) {
    return Double.isFinite(value) && value == Math.rint(value)
        ? String.valueOf((long) value)
        : String.valueOf(value);
  }

  private static String unquote(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")
        ? trimmed.substring(1, trimmed.length() - 1)
        : trimmed;
  }
}
