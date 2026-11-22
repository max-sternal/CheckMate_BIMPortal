package com.bimportal.hackathon.ids;

import com.bimportal.hackathon.ids.IdsSpecification.Cardinality;
import com.bimportal.hackathon.ids.IdsSpecification.EntityRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.PropertyRequirement;
import com.bimportal.hackathon.ids.IdsSpecification.Unsupported;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads buildingSMART IDS 1.0 files: the ones downloaded from the BIM Portal, and local ones.
 *
 * <p>What is evaluated: {@code entity} facets with name and {@code predefinedType}, and {@code
 * property} facets with property set, name, data type, a fixed value and {@code cardinality}. Names
 * and PredefinedTypes may be given as {@code <simpleValue>} or as an {@code xs:restriction} holding
 * {@code xs:pattern} or {@code xs:enumeration}. That is everything the portal writes.
 *
 * <p>Everything else - attribute, classification, material and partOf facets, value restrictions on
 * properties, patterns in property names - is not dropped but listed in {@link
 * IdsSpecification#unsupported()}, because dropping a facet changes the verdict.
 */
public final class IdsParser {

  private static final String IDS_NS = "http://standards.buildingsmart.org/IDS";
  private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

  /** Enough of an unsupported facet's values to recognise it in the report. */
  private static final int DESCRIBED_VALUES = 3;

  private IdsParser() {}

  /**
   * Read an IDS file, or a ZIP archive of IDS files such as an AIA project's (see {@link
   * IdsArchive}).
   *
   * @throws RequirementsException when the file is missing, unreadable or not IDS
   */
  public static IdsDocument read(Path file) {
    if (!Files.isRegularFile(file)) {
      throw new RequirementsException("Die IDS-Datei " + file + " wurde nicht gefunden.");
    }
    try (InputStream in = Files.newInputStream(file)) {
      return read(in, file.getFileName().toString());
    } catch (IOException e) {
      throw new RequirementsException(
          "Die IDS-Datei " + file + " lässt sich nicht lesen: " + e.getMessage(), e);
    }
  }

  /**
   * Read an IDS document, or a ZIP archive of them.
   *
   * @param source where it came from, for reports and error messages
   * @throws RequirementsException when the bytes are not an IDS document
   */
  public static IdsDocument read(byte[] ids, String source) {
    return read(new ByteArrayInputStream(ids), source);
  }

  /**
   * Read an IDS document, or a ZIP archive of them; which one it is, the first bytes tell.
   *
   * @param source where it came from, for reports and error messages
   * @throws RequirementsException when the stream is not an IDS document
   */
  public static IdsDocument read(InputStream ids, String source) {
    BufferedInputStream in = new BufferedInputStream(ids);
    try {
      in.mark(4);
      byte[] head = in.readNBytes(4);
      in.reset();
      if (IdsArchive.isZip(head)) {
        return IdsArchive.read(in, source);
      }
    } catch (IOException e) {
      throw new RequirementsException(source + " lässt sich nicht lesen: " + e.getMessage(), e);
    }

    Document document;
    try {
      document = newDocumentBuilder().parse(in);
    } catch (SAXException | IOException e) {
      throw new RequirementsException(
          source + " ist keine lesbare IDS-Datei: " + e.getMessage(), e);
    }

    Element root = document.getDocumentElement();
    if (!"ids".equals(root.getLocalName()) || !IDS_NS.equals(root.getNamespaceURI())) {
      throw new RequirementsException(
          source
              + " ist keine IDS-Datei: sie beginnt mit <"
              + root.getTagName()
              + "> statt mit <ids> aus "
              + IDS_NS
              + ".");
    }

    Element info = child(root, "info");
    List<IdsSpecification> specifications = new ArrayList<>();
    for (Element specification : children(root, "specifications")) {
      for (Element element : children(specification, "specification")) {
        specifications.add(readSpecification(element));
      }
    }
    return new IdsDocument(
        emptyToNull(text(child(info, "title"))),
        emptyToNull(text(child(info, "version"))),
        source,
        specifications);
  }

  /** Every specification in an IDS document. */
  public static List<IdsSpecification> parse(byte[] ids) {
    return read(ids, "IDS").specifications();
  }

  /** Every specification in an IDS document. */
  public static List<IdsSpecification> parse(InputStream ids) {
    return read(ids, "IDS").specifications();
  }

  private static IdsSpecification readSpecification(Element specification) {
    Element applicability = child(specification, "applicability");
    Element requirements = child(specification, "requirements");

    List<String> unsupportedApplicability = new ArrayList<>();
    List<String> unsupportedRequirements = new ArrayList<>();
    Facets applies = readFacets(applicability, unsupportedApplicability);
    Facets requires = readFacets(requirements, unsupportedRequirements);

    // IDS 1.0 puts the specification's own cardinality on the applicability. maxOccurs="0"
    // turns the meaning around - no element may match - which the check does not do.
    if (applicability != null && "0".equals(applicability.getAttribute("maxOccurs").trim())) {
      unsupportedApplicability.add(
          "die Spezifikation verbietet anwendbare Elemente (maxOccurs=\"0\")");
    }

    return new IdsSpecification(
        specification.getAttribute("name"),
        specification.getAttribute("ifcVersion"),
        applies.properties(),
        applies.entity(),
        requires.properties(),
        requires.entity(),
        new Unsupported(unsupportedApplicability, unsupportedRequirements),
        emptyToNull(specification.getAttribute("identifier")),
        emptyToNull(specification.getAttribute("instructions")));
  }

  /** The facets of an applicability or requirements block that the check evaluates. */
  private record Facets(List<PropertyRequirement> properties, EntityRequirement entity) {}

  /**
   * Read the facets of one block.
   *
   * @param unsupported collects, as report lines, the facets and parts of facets not evaluated
   */
  private static Facets readFacets(Element block, List<String> unsupported) {
    List<PropertyRequirement> properties = new ArrayList<>();
    EntityRequirement entity = null;
    if (block == null) {
      return new Facets(properties, null);
    }

    for (Element facet : children(block, null)) {
      switch (facet.getLocalName()) {
        case "entity" -> {
          if (entity != null) {
            unsupported.add("weitere entity-Facette" + describeValues(facet));
          } else {
            entity = readEntity(facet, unsupported);
          }
        }
        case "property" -> readProperty(facet, unsupported).ifPresent(properties::add);
        default -> unsupported.add(facet.getLocalName() + "-Facette" + describeValues(facet));
      }
    }
    return new Facets(properties, entity);
  }

  private static EntityRequirement readEntity(Element entity, List<String> unsupported) {
    // Entity names differ in case between IDS (IFCBEAM) and the model (IfcBeam).
    FacetValue name = readValue(child(entity, "name"), Pattern.CASE_INSENSITIVE);
    if (!name.readable()) {
      unsupported.add("entity-Facette: Name " + name.problem());
      return null;
    }

    // PredefinedTypes are enumeration items or user-defined names: compared as written.
    FacetValue predefinedType = readValue(child(entity, "predefinedType"), 0);
    if (predefinedType.absent()) {
      return new EntityRequirement(name.pattern(), name.source());
    }
    if (!predefinedType.readable()) {
      unsupported.add(
          "entity-Facette " + name.source() + ": PredefinedType " + predefinedType.problem());
      return new EntityRequirement(name.pattern(), name.source());
    }
    return new EntityRequirement(
        name.pattern(), name.source(), predefinedType.pattern(), predefinedType.source());
  }

  /**
   * A {@code <property>} facet.
   *
   * @return empty when the facet cannot be evaluated even in part; it is then listed in {@code
   *     unsupported}
   */
  private static Optional<PropertyRequirement> readProperty(
      Element property, List<String> unsupported) {

    FacetValue propertySet = readValue(child(property, "propertySet"), 0);
    FacetValue baseName = readValue(child(property, "baseName"), 0);
    if (baseName.absent()) {
      unsupported.add("property-Facette ohne baseName");
      return Optional.empty();
    }
    if (!baseName.simple() || !(propertySet.absent() || propertySet.simple())) {
      unsupported.add(
          "Merkmal mit Muster im Namen ("
              + (propertySet.absent() ? "" : propertySet.source() + "/")
              + baseName.source()
              + ")");
      return Optional.empty();
    }

    PropertyKey key =
        new PropertyKey(propertySet.absent() ? null : propertySet.source(), baseName.source());
    // Absent cardinality means required: that is the IDS default, and the portal only
    // writes the attribute out explicitly.
    Cardinality cardinality = Cardinality.of(property.getAttribute("cardinality"));
    String dataType = emptyToNull(property.getAttribute("dataType"));

    FacetValue value = readValue(child(property, "value"), 0);
    if (!value.absent() && !value.simple()) {
      String what =
          value.readable()
              ? "Wertvorgabe als Muster oder Auswahl (" + value.source() + ")"
              : "Wertvorgabe " + value.problem();
      if (cardinality == Cardinality.PROHIBITED) {
        // Without its value a prohibited property would forbid every value: dropped whole.
        unsupported.add("Merkmal " + key + ": " + what + " - nicht geprüft");
        return Optional.empty();
      }
      unsupported.add("Merkmal " + key + ": " + what + " - geprüft wird nur, ob es vorhanden ist");
      return Optional.of(new PropertyRequirement(key, dataType, null, cardinality));
    }
    String expectedValue = value.absent() ? null : value.source();
    return Optional.of(new PropertyRequirement(key, dataType, expectedValue, cardinality));
  }

  /**
   * A facet value, compiled for matching and as written - or why it could not be read.
   *
   * @param simple true when given as {@code <simpleValue>}, i.e. one fixed value
   * @param problem why the value cannot be evaluated; null when it can, or when it is absent
   */
  private record FacetValue(Pattern pattern, String source, boolean simple, String problem) {

    static final FacetValue ABSENT = new FacetValue(null, null, false, null);

    static FacetValue unreadable(String problem) {
      return new FacetValue(null, null, false, problem);
    }

    boolean absent() {
      return pattern == null && problem == null;
    }

    boolean readable() {
      return pattern != null;
    }
  }

  /**
   * A facet value given as {@code <simpleValue>}, or as an {@code xs:restriction} holding {@code
   * xs:pattern} (several are alternatives, as in XML Schema) or {@code xs:enumeration}.
   */
  private static FacetValue readValue(Element facetValue, int flags) {
    if (facetValue == null) {
      return FacetValue.ABSENT;
    }
    String simple = text(child(facetValue, "simpleValue"));
    if (simple != null && !simple.isBlank()) {
      String value = simple.trim();
      return new FacetValue(Pattern.compile(Pattern.quote(value), flags), value, true, null);
    }

    List<Element> restrictions = childrenNs(facetValue, XSD_NS, "restriction");
    if (restrictions.isEmpty()) {
      return FacetValue.unreadable("ohne Wert");
    }
    List<String> patterns = new ArrayList<>();
    List<String> enumeration = new ArrayList<>();
    Set<String> others = new LinkedHashSet<>();
    for (Element restrictionFacet : childrenNs(restrictions.get(0), XSD_NS, null)) {
      String value = restrictionFacet.getAttribute("value");
      switch (restrictionFacet.getLocalName()) {
        case "pattern" -> patterns.add(value);
        case "enumeration" -> enumeration.add(value);
        default -> others.add("xs:" + restrictionFacet.getLocalName());
      }
    }

    if (patterns.isEmpty() && enumeration.isEmpty()) {
      return FacetValue.unreadable(
          others.isEmpty() ? "ohne Wert" : "als Einschränkung mit " + String.join(", ", others));
    }
    if (!others.isEmpty() || (!patterns.isEmpty() && !enumeration.isEmpty())) {
      // Bounds, lengths, or pattern and enumeration together: all of them would have to hold.
      Set<String> used = new LinkedHashSet<>();
      if (!patterns.isEmpty()) {
        used.add("xs:pattern");
      }
      if (!enumeration.isEmpty()) {
        used.add("xs:enumeration");
      }
      used.addAll(others);
      return FacetValue.unreadable("als Einschränkung mit " + String.join(", ", used));
    }

    String source = String.join("|", patterns.isEmpty() ? enumeration : patterns);
    String regex =
        patterns.isEmpty()
            ? enumeration.stream().map(Pattern::quote).collect(Collectors.joining("|"))
            : patterns.size() == 1
                ? patterns.get(0)
                : patterns.stream().map(p -> "(?:" + p + ")").collect(Collectors.joining("|"));
    try {
      return new FacetValue(Pattern.compile(regex, flags), source, false, null);
    } catch (PatternSyntaxException e) {
      return FacetValue.unreadable("mit einem Muster, das sich nicht lesen lässt (" + source + ")");
    }
  }

  /**
   * The values an unsupported facet names, so the report line can be matched to the file: e.g.
   * {@code " (Name, Wand)"} for an attribute facet. Empty when it names none.
   */
  private static String describeValues(Element facet) {
    List<String> values = new ArrayList<>();
    collectValues(facet, values);
    if (values.isEmpty()) {
      return "";
    }
    List<String> shown = values.stream().distinct().limit(DESCRIBED_VALUES).toList();
    return " (" + String.join(", ", shown) + (values.size() > shown.size() ? ", ..." : "") + ")";
  }

  private static void collectValues(Element element, List<String> values) {
    NodeList nodes = element.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i) instanceof Element child) {
        if ("simpleValue".equals(child.getLocalName())) {
          values.add(child.getTextContent().trim());
        } else if (XSD_NS.equals(child.getNamespaceURI()) && child.hasAttribute("value")) {
          values.add(child.getAttribute("value"));
        } else {
          collectValues(child, values);
        }
      }
    }
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

  /** Direct IDS children by local name; every IDS child when the name is null. */
  private static List<Element> children(Element parent, String localName) {
    return childrenNs(parent, IDS_NS, localName);
  }

  /**
   * Direct children by namespace and local name; every child in the namespace when the name is
   * null.
   *
   * <p>Deliberately not {@code getElementsByTagNameNS}: that searches the whole subtree, so a
   * {@code <property>} inside {@code <requirements>} would also surface while reading {@code
   * <applicability>}.
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
          && (localName == null || localName.equals(node.getLocalName()))
          && namespace.equals(node.getNamespaceURI())) {
        found.add((Element) node);
      }
    }
    return found;
  }

  /** IDS files arrive over the network, so entity resolution stays switched off. */
  private static DocumentBuilder newDocumentBuilder() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setNamespaceAware(true);
      factory.setExpandEntityReferences(false);
      return factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException("The JDK's XML parser lacks a required feature", e);
    }
  }
}
