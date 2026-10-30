package com.bimportal.hackathon.ids;

import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
 * <p>Only the subset the portal actually writes is understood: {@code property} and {@code entity}
 * facets, with values given either as {@code <simpleValue>} or as an {@code xs:restriction} holding
 * an {@code xs:pattern}. Anything else is skipped rather than guessed at.
 */
public final class IdsParser {

  private static final String IDS_NS = "http://standards.buildingsmart.org/IDS";
  private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

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
   * <p>Matching is exact first and prefix-based second, so a model declaring {@code IFC4X3_ADD2}
   * still finds a specification written for {@code IFC4X3}. Returns empty rather than falling back
   * to an arbitrary block: checking IFC2X3 geometry against IFC4 entity names produces findings
   * that look real and are not.
   */
  public static Optional<IdsSpecification> forSchema(
      List<IdsSpecification> specifications, String schema) {
    if (schema == null || schema.isBlank()) {
      return Optional.empty();
    }
    String wanted = schema.trim().toUpperCase();

    return specifications.stream()
        .filter(s -> wanted.equalsIgnoreCase(s.ifcVersion()))
        .findFirst()
        .or(
            () ->
                specifications.stream()
                    .filter(s -> s.ifcVersion() != null && !s.ifcVersion().isBlank())
                    .filter(
                        s -> {
                          String candidate = s.ifcVersion().toUpperCase();
                          return wanted.startsWith(candidate) || candidate.startsWith(wanted);
                        })
                    .findFirst());
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
      boolean required = !"optional".equalsIgnoreCase(property.getAttribute("cardinality"));

      properties.add(
          new PropertyRequirement(
              key, emptyToNull(property.getAttribute("dataType")), facetText(property, "value"),
              required));
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
    Element name = child(entity, "name");
    if (name == null) {
      return null;
    }

    String simple = text(child(name, "simpleValue"));
    if (simple != null && !simple.isBlank()) {
      return new EntityRequirement(
          Pattern.compile(Pattern.quote(simple.trim()), Pattern.CASE_INSENSITIVE), simple.trim());
    }

    String pattern = restrictionPattern(name);
    if (pattern == null || pattern.isBlank()) {
      return null;
    }
    return new EntityRequirement(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), pattern);
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
