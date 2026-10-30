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
import com.apstex.ifctoolbox.ifc.IfcPropertySet;
import com.apstex.ifctoolbox.ifc.IfcPropertySetDefinition;
import com.apstex.ifctoolbox.ifc.IfcPropertySingleValue;
import com.apstex.ifctoolbox.ifc.IfcReal;
import com.apstex.ifctoolbox.ifc.IfcRelDefines;
import com.apstex.ifctoolbox.ifc.IfcRelDefinesByProperties;
import com.apstex.ifctoolbox.ifc.IfcRelDefinesByType;
import com.apstex.ifctoolbox.ifc.IfcText;
import com.apstex.ifctoolbox.ifc.IfcTypeObject;
import com.apstex.ifctoolbox.ifc.IfcValue;
import com.apstex.ifctoolbox.ifc.IfcVolumeMeasure;
import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import com.apstex.step.core.ClassInterface;
import com.apstex.step.header.File_Schema;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads an Apstex {@link IfcModel} into the plain {@link ModelElement} records an IDS check works
 * on.
 *
 * <p>This is a faithful reading of the file: property addresses are taken exactly as the model
 * spells them. Translating them to what a LOIN asks for happens in {@code PropertyAliases}, so the
 * two concerns stay apart - this class can be pointed at any IFC, and the mapping can be reviewed
 * and tested without one.
 *
 * <p>Only single-value properties are collected. IDS addresses quantities through a separate facet
 * which the BIM Portal does not export, so {@code IfcElementQuantity} is skipped.
 */
public final class IfcModelElements {

  private static final Pattern SCHEMA_NAME = Pattern.compile("IFC[0-9][0-9A-Z_]*");

  private IfcModelElements() {}

  /** Read every object that can carry properties. */
  public static List<ModelElement> from(IfcModel model) {
    List<ModelElement> elements = new ArrayList<>();
    for (ClassInterface object : model.getObjects()) {
      if (object instanceof IfcObject.Ifc2x3 ifcObject) {
        elements.add(toElement(ifcObject));
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

  private static ModelElement toElement(IfcObject.Ifc2x3 object) {
    Map<PropertyKey, ModelProperty> properties = new LinkedHashMap<>();

    // Properties on the element itself first, then the ones it inherits from its type, so an
    // element that overrides a type property is reported with its own value.
    forEachRelation(object, relation -> collectDirect(relation, properties));
    forEachRelation(object, relation -> collectInherited(relation, properties));

    return new ModelElement(
        idOf(object), decoded(object.getName()), object.getClassName(), List.copyOf(properties.values()));
  }

  private static void forEachRelation(
      IfcObject.Ifc2x3 object, java.util.function.Consumer<IfcRelDefines.Ifc2x3> action) {
    if (object.getIsDefinedBy_Inverse() == null) {
      return;
    }
    object.getIsDefinedBy_Inverse().forEach(action);
  }

  private static void collectDirect(
      IfcRelDefines.Ifc2x3 relation, Map<PropertyKey, ModelProperty> properties) {
    if (relation instanceof IfcRelDefinesByProperties.Ifc2x3 byProperties) {
      collect(byProperties.getRelatingPropertyDefinition(), properties, true);
    }
  }

  private static void collectInherited(
      IfcRelDefines.Ifc2x3 relation, Map<PropertyKey, ModelProperty> properties) {
    if (!(relation instanceof IfcRelDefinesByType.Ifc2x3 byType)) {
      return;
    }
    IfcTypeObject.Ifc2x3 type = byType.getRelatingType();
    if (type == null || type.getHasPropertySets() == null) {
      return;
    }
    for (IfcPropertySetDefinition.Ifc2x3 set : type.getHasPropertySets()) {
      collect(set, properties, false);
    }
  }

  private static void collect(
      IfcPropertySetDefinition.Ifc2x3 definition,
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
      if (!(property instanceof IfcPropertySingleValue single)) {
        continue;
      }
      String name = decoded(single.getName());
      if (name == null || name.isBlank()) {
        continue;
      }

      PropertyKey key = new PropertyKey(setName, name);
      ModelProperty value =
          new ModelProperty(
              key, valueText(single.getNominalValue()), dataTypeOf(single.getNominalValue()));

      if (overwrite) {
        properties.put(key, value);
      } else {
        properties.putIfAbsent(key, value);
      }
    }
  }

  private static String idOf(IfcObject.Ifc2x3 object) {
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
