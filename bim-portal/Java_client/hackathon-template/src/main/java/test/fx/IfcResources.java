package test.fx;

import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locates the IFC model the GUI works on.
 *
 * <p>The default model ships inside the JAR under {@code /ifc}, so a fresh clone and a Docker image
 * both run without any local file lying around. To use a different model, point {@code
 * checkmate.ifc} (system property) or {@code CHECKMATE_IFC} (environment variable, also picked up
 * from {@code .env}) at it - that is the hook for a mounted volume in a container.
 */
public final class IfcResources {

  /** Model bundled as a resource; see {@code src/main/resources/ifc}. */
  public static final String DEFAULT_RESOURCE = "/ifc/bruecke-tbw1-ohne-merkmale.ifc";

  public static final String PROPERTY = "checkmate.ifc";
  public static final String ENV_VARIABLE = "CHECKMATE_IFC";

  private static final Logger logger = LoggerFactory.getLogger(IfcResources.class);

  private IfcResources() {}

  /**
   * Read the configured IFC model.
   *
   * @return the parsed model, or an empty one if it could not be read
   */
  public static IfcModel loadDefaultModel() {
    IfcModel model = new IfcModel();

    String override = override();
    try {
      if (override != null) {
        File file = new File(override);
        if (!file.isFile()) {
          logger.error("IFC override {} does not point at a file - returning an empty model", file);
          return model;
        }
        logger.info("Reading IFC model from {}", file.getAbsolutePath());
        model.readStepFile(file);
      } else {
        logger.info("Reading bundled IFC model {}", DEFAULT_RESOURCE);
        try (InputStream in = open(DEFAULT_RESOURCE)) {
          model.readStepFile(in);
        }
      }
    } catch (Exception e) {
      logger.error("Could not read IFC model - returning an empty model", e);
    }

    return model;
  }

  /** The configured override path, or null when the bundled model should be used. */
  private static String override() {
    String fromProperty = System.getProperty(PROPERTY);
    if (fromProperty != null && !fromProperty.isBlank()) {
      return fromProperty.trim();
    }

    String fromEnvironment = System.getenv(ENV_VARIABLE);
    if (fromEnvironment != null && !fromEnvironment.isBlank()) {
      return fromEnvironment.trim();
    }

    return null;
  }

  /** Open a bundled resource, failing loudly rather than handing back a null stream. */
  private static InputStream open(String resource) throws IOException {
    InputStream in = IfcResources.class.getResourceAsStream(resource);
    if (in == null) {
      throw new IOException(
          "Resource " + resource + " is missing from the classpath - was the module rebuilt?");
    }
    return in;
  }
}
