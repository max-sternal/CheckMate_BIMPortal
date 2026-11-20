package test.fx;

import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import com.bimportal.hackathon.TestModelFiles;
import com.bimportal.hackathon.ids.ModelElement;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/** The IFC models the reading tests use. */
final class TestModels {

  static final String LARGE_BUILDING_IFC2X3 = "ifc2x3/LargeBuilding.ifc";
  static final String ARCHITECTURE_IFC4 = "ifc4/Building-Architecture.ifc";
  static final String BRIDGE_IFC4 = "ifc4/Infra-Bridge.ifc";
  static final String ARCHITECTURE_IFC4X3 = "ifc4x3/Building-Architecture.ifc";
  static final String BRIDGE_IFC4X3 = "ifc4x3/Infra-Bridge.ifc";

  private TestModels() {}

  /** A downloaded sample model; the calling test is skipped, saying why, when it is missing. */
  static IfcModel sample(String name) {
    return IfcResources.load(TestModelFiles.require(name));
  }

  /**
   * One of our own small case files, written by {@code src/test/python/make_ifc_fixtures.py}.
   *
   * @param schema {@code ifc2x3}, {@code ifc4} or {@code ifc4x3}
   */
  static IfcModel cases(String schema) {
    String resource = "/ifc/checkmate-cases-" + schema + ".ifc";
    URL url = TestModels.class.getResource(resource);
    if (url == null) {
      throw new IllegalStateException(resource + " is missing from the test classpath");
    }
    try {
      return IfcResources.load(Path.of(url.toURI()));
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  static ModelElement byId(List<ModelElement> elements, String id) {
    return elements.stream()
        .filter(element -> element.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No element with GlobalId " + id));
  }

  static ModelElement byName(List<ModelElement> elements, String name) {
    return elements.stream()
        .filter(element -> name.equals(element.name()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No element named " + name));
  }
}
