package com.bimportal.hackathon.ids;

import com.bimportal.hackathon.ids.IdsSpecification.Cardinality;
import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads the buildingSMART IDS files the BIM Portal exports per LOIN.
 *
 * <p>Only the subset the portal actually writes is understood: {@code property} facets with their
 * {@code cardinality}, and {@code entity} facets with name and {@code predefinedType}. Values are
 * given either as {@code <simpleValue>} or as an {@code xs:restriction} holding an {@code
 * xs:pattern}. Anything else is skipped rather than guessed at.
 */
public final class IdsParser {

  private static final String IDS_NS = "http://standards.buildingsmart.org/IDS";
  private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

  /** The schema family at the start of a name: version, and the minor version after an X. */
  private static final Pattern SCHEMA_BASE = Pattern.compile("IFC\\d+(?:X\\d+)?");

  private IdsParser() {}

  /** Parse every specification in an IDS document. */
  public static List<IdsSpecification> parse(byte[] ids) throws Exception {
    return parse(new ByteArrayInputStream(ids));
  }

  /** Parse every specification in an IDS document. */
  public static List<IdsSpecification> parse(InputStream ids) throws Exception {
    Document document = newDocumentBuilder().parse(ids);

    List<IdsSpecification> specifications = new ArrayList<>();
    for (Element specification : children(document.getDocumentElement(), "specifications")) {
      for (Element element : children(specification, "specification")) {
        specifications.add(readSpecification(element));
      }
    }
    return specifications;
  }

  /**
   * Pick the specification written for a model's IFC schema.
   *
   * <p>Matching is exact first, then by base schema, so a model declaring {@code IFC4X3} still
   * finds a specification written for {@code IFC4X3_ADD2}, and the other way round. A plain prefix
   * test is not enough for that: {@code IFC4X3} starts with {@code IFC4}, which is a different
   * schema. Returns empty rather than falling back to an arbitrary block: checking IFC2X3 geometry
   * against IFC4 entity names produces findings that look real and are not.
   */
  public static Optional<IdsSpecification> forSchema(
      List<IdsSpecification> specifications, String schema) {
    if (schema == null || schema.isBlank()) {
      return Optional.empty();
    }
    String wanted = schema.trim();

    return specifications.stream()
        .filter(s -> wanted.equalsIgnoreCase(s.ifcVersion()))
        .findFirst()
        .or(
            () ->
                specifications.stream()
                    .filter(s -> s.ifcVersion() != null && !s.ifcVersion().isBlank())
                    .filter(s -> baseSchema(s.ifcVersion()).equals(baseSchema(wanted)))
                    .findFirst());
  }

  /**
   * A schema name without its addendum or corrigendum: {@code IFC4X3_ADD2} becomes {@code IFC4X3},
   * {@code IFC2X3_TC1} becomes {@code IFC2X3}, {@code IFC4} stays {@code IFC4}.
   */
  static String baseSchema(String schema) {
    String upper = schema.trim().toUpperCase();
    Matcher matcher = SCHEMA_BASE.matcher(upper);
    return matcher.lookingAt() ? matcher.group() : upper;
  }

  private static IdsSpecification readSpecification(Element specification) {
    Element applicability = child(specification, "applicability");
    Element requirements = child(specification, "requirements");

    return new IdsSpecification(
        specification.getAttribute("name"),
        specification.getAttribute("ifcVersion"),
        readProperties(applicability),
        readEntity(applicability),
        readProperties(requirements),
        readEntity(requirements));
  }

  private static List<PropertyRequirement> readProperties(Element parent) {
    List<PropertyRequirement> properties = new ArrayList<>();
    if (parent == null) {
      return properties;
    }

    for (Element property : children(parent, "property")) {
      PropertyKey key =
          new PropertyKey(facetText(property, "propertySet"), facetText(property, "baseName"));
      if (key.baseName().isEmpty()) {
        continue;
      }

      // Absent cardinality means required: that is the IDS default, and the portal only
      // writes the attribute out explicitly.
      properties.add(
          new PropertyRequirement(
              key,
              emptyToNull(property.getAttribute("dataType")),
              facetText(property, "value"),
              Cardinality.of(property.getAttribute("cardinality"))));
    }
    return properties;
  }

  private static EntityRequirement readEntity(Element parent) {
    if (parent == null) {
      return null;
    }
    Element entity = child(parent, "entity");
    if (entity == null) {
      return null;
    }

    // Entity names differ in case between IDS (IFCBEAM) and the model (IfcBeam).
    ValuePattern name = readValuePattern(child(entity, "name"), Pattern.CASE_INSENSITIVE);
    if (name == null) {
      return null;
    }
    // PredefinedTypes are enumeration items or user-defined names: compared as written.
    ValuePattern predefinedType = readValuePattern(child(entity, "predefinedType"), 0);

    return predefinedType == null
        ? new EntityRequirement(name.pattern(), name.source())
        : new EntityRequirement(
            name.pattern(), name.source(), predefinedType.pattern(), predefinedType.source());
  }

  /** A facet value compiled for matching, and as it was written. */
  private record ValuePattern(Pattern pattern, String source) {}

  /**
   * A facet value given as {@code <simpleValue>} or as an {@code xs:pattern} restriction.
   *
   * @return null when the element is absent or carries neither
   */
  private static ValuePattern readValuePattern(Element facetValue, int flags) {
    if (facetValue == null) {
      return null;
    }
    String simple = text(child(facetValue, "simpleValue"));
    if (simple != null && !simple.isBlank()) {
      return new ValuePattern(Pattern.compile(Pattern.quote(simple.trim()), flags), simple.trim());
    }
    String pattern = restrictionPattern(facetValue);
    if (pattern == null || pattern.isBlank()) {
      return null;
    }
    return new ValuePattern(Pattern.compile(pattern, flags), pattern);
  }

  /** The {@code value} of an {@code <xs:pattern>} nested in an {@code <xs:restriction>}. */
  private static String restrictionPattern(Element parent) {
    for (Element restriction : childrenNs(parent, XSD_NS, "restriction")) {
      for (Element pattern : childrenNs(restriction, XSD_NS, "pattern")) {
        return pattern.getAttribute("value");
      }
    }
    return null;
  }

  /** Text of a facet child's {@code <simpleValue>}, e.g. {@code <baseName><simpleValue>}. */
  private static String facetText(Element parent, String facet) {
    Element element = child(parent, facet);
    return element == null ? null : emptyToNull(text(child(element, "simpleValue")));
  }

  private static String text(Element element) {
    return element == null ? null : element.getTextContent();
  }

  private static String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static Element child(Element parent, String localName) {
    List<Element> found = children(parent, localName);
    return found.isEmpty() ? null : found.get(0);
  }

  private static List<Element> children(Element parent, String localName) {
    return childrenNs(parent, IDS_NS, localName);
  }

  /**
   * Direct children by namespace and local name.
   *
   * <p>Deliberately not {@code getElementsByTagNameNS}: that searches the whole subtree, so a
   * {@code <property>} inside {@code <requirements>} would also surface while reading
   * {@code <applicability>}.
   */
  private static List<Element> childrenNs(Element parent, String namespace, String localName) {
    List<Element> found = new ArrayList<>();
    if (parent == null) {
      return found;
    }

    NodeList nodes = parent.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE
          && localName.equals(node.getLocalName())
          && namespace.equals(node.getNamespaceURI())) {
        found.add((Element) node);
      }
    }
    return found;
  }

  /** IDS files arrive over the network, so entity resolution stays switched off. */
  private static DocumentBuilder newDocumentBuilder() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setNamespaceAware(true);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder();
  }
}
