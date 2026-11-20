package test.fx;

import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locates and reads the IFC model the GUI and the CLI work on.
 *
 * <p>The default model ships inside the JAR under {@code /ifc}, so a fresh clone and a Docker image
 * both run without any local file lying around. To use a different model, point {@code
 * checkmate.ifc} (system property) or {@code CHECKMATE_IFC} (environment variable, also picked up
 * from {@code .env}) at it - that is the hook for a mounted volume in a container.
 *
 * <p>A model that cannot be read is a {@link ModelLoadException}, never an empty model.
 */
public final class IfcResources {

  /**
   * Model bundled as a resource; see {@code src/main/resources/ifc}.
   *
   * <p>The BIM.Hamburg bridge TBW1, in the variant that carries the Merkmale: without
   * {@code Pset_Objektinformation} a LOIN check has nothing to select on and every specification
   * comes out "nicht prüfbar".
   */
  public static final String DEFAULT_RESOURCE = "/ifc/bruecke-tbw1-mit-merkmalen.ifc";

  public static final String PROPERTY = "checkmate.ifc";
  public static final String ENV_VARIABLE = "CHECKMATE_IFC";

  /** The schemas IfcModelElements reads: IFC2X3, IFC4 and IFC4X3, with addenda and corrigenda. */
  private static final Pattern SUPPORTED_SCHEMA = Pattern.compile("IFC(2X3|4|4X3)(_[A-Z0-9]+)?");

  private static final Logger logger = LoggerFactory.getLogger(IfcResources.class);

  private IfcResources() {}

  /**
   * Read the configured IFC model: the override when one is set, the bundled one otherwise.
   *
   * @throws ModelLoadException when the model cannot be read
   */
  public static IfcModel loadDefaultModel() {
    String override = override();
    return override == null ? loadBundled() : load(Path.of(override));
  }

  /** Read the bundled bridge model, whatever override is configured. */
  public static IfcModel loadBundled() {
    logger.info("Reading bundled IFC model {}", DEFAULT_RESOURCE);
    IfcModel model = new IfcModel();
    try (InputStream in = open(DEFAULT_RESOURCE)) {
      model.readStepFile(in);
    } catch (Exception e) {
      throw new ModelLoadException(
          "Das mitgelieferte Modell " + DEFAULT_RESOURCE + " konnte nicht gelesen werden: "
              + reason(e),
          e);
    }
    return requireSupportedSchema(model, DEFAULT_RESOURCE);
  }

  /**
   * Read an IFC model from a file.
   *
   * @throws ModelLoadException when the file is missing, is not an IFC file, or uses a schema
   *     CheckMate does not read
   */
  public static IfcModel load(Path file) {
    Path absolute = file.toAbsolutePath();
    if (!Files.isRegularFile(absolute)) {
      throw new ModelLoadException("IFC-Datei nicht gefunden: " + absolute);
    }

    logger.info("Reading IFC model from {}", absolute);
    IfcModel model = new IfcModel();
    // Through a stream closed here: handed the file itself, the toolbox keeps it open after a
    // failed read, which on Windows locks it until the program ends.
    try (InputStream in = Files.newInputStream(absolute)) {
      model.readStepFile(in);
    } catch (Exception e) {
      throw new ModelLoadException(
          "Die Datei " + absolute + " ist kein lesbares IFC-Modell: " + reason(e), e);
    }
    return requireSupportedSchema(model, absolute.toString());
  }

  /**
   * Turn a read that "succeeded" on something other than IFC into an error.
   *
   * <p>The toolbox accepts files it cannot make sense of and leaves the model empty; the header's
   * schema is what tells an IFC model apart from such a file.
   */
  private static IfcModel requireSupportedSchema(IfcModel model, String source) {
    String schema = IfcModelElements.schemaOf(model);
    if (schema == null) {
      throw new ModelLoadException(
          source + " nennt kein IFC-Schema (FILE_SCHEMA) - ist es wirklich eine IFC-Datei?");
    }
    if (!SUPPORTED_SCHEMA.matcher(schema).matches()) {
      throw new ModelLoadException(
          source
              + " verwendet das Schema "
              + schema
              + " - CheckMate liest IFC2X3, IFC4 und IFC4X3.");
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

  /** The cause's own message, or its type when it has none. */
  private static String reason(Exception e) {
    return e.getMessage() == null || e.getMessage().isBlank()
        ? e.getClass().getSimpleName()
        : e.getMessage();
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
