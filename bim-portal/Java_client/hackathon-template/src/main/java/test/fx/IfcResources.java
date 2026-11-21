package test.fx;

import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import test.fx.ModelLinks.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locates and reads the IFC model the GUI and the CLI work on.
 *
 * <p>The default model ships inside the JAR under {@code /ifc}, so a fresh clone and a Docker image
 * both run without any local file lying around. To use a different model, point {@code
 * checkmate.ifc} (system property) or {@code CHECKMATE_IFC} (environment variable, also picked up
 * from {@code .env}) at it - a path, or a link such as a GitHub file link. That is also the hook
 * for a mounted volume in a container.
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
    String override = configuredSource();
    return override == null ? loadBundled() : load(override);
  }

  /**
   * Read an IFC model from a file path or a link.
   *
   * @param source a path, or an {@code http(s)} link - a GitHub link to a file works as well
   * @throws ModelLoadException when the model cannot be read; the message says why
   */
  public static IfcModel load(String source) {
    if (source == null || source.isBlank()) {
      throw new ModelLoadException("Kein Modell angegeben.");
    }
    String trimmed = source.trim();
    if (ModelLinks.isLink(trimmed)) {
      return loadLink(trimmed);
    }
    try {
      return load(Path.of(trimmed));
    } catch (InvalidPathException e) {
      throw new ModelLoadException("Kein gültiger Dateipfad: " + trimmed, e);
    }
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
   * Read an IFC model from a file: an {@code .ifc} or an ifcZIP, told apart by content.
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
    return read(absolute, absolute.toString());
  }

  /**
   * Download a model behind a link into a temporary file, read it, and delete the file again.
   *
   * <p>Nothing is kept: a model is read once per load, and keeping every download would fill the
   * disk with copies of files the user has anyway.
   */
  private static IfcModel loadLink(String link) {
    URI uri;
    try {
      uri = ModelLinks.downloadUri(link);
    } catch (IllegalArgumentException e) {
      throw new ModelLoadException("Kein gültiger Link: " + link, e);
    }

    Path download = null;
    try {
      download = Files.createTempFile("checkmate-model-", ".download");
      logger.info("Downloading IFC model from {}", uri);
      ModelLinks.download(uri, download, ModelLinks.MAX_DOWNLOAD_BYTES);

      // A file kept in Git LFS comes back as a pointer; GitHub serves the file elsewhere.
      Optional<URI> media = ModelLinks.lfsMediaUri(uri);
      if (ModelLinks.sniff(download) == Content.LFS_POINTER && media.isPresent()) {
        logger.info("Git LFS pointer - downloading the file from {}", media.get());
        ModelLinks.download(media.get(), download, ModelLinks.MAX_DOWNLOAD_BYTES);
      }
      return read(download, link);
    } catch (IOException e) {
      throw new ModelLoadException(
          "Das Modell hinter " + link + " konnte nicht geladen werden: " + reason(e), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ModelLoadException("Download abgebrochen: " + link, e);
    } finally {
      deleteQuietly(download);
    }
  }

  /**
   * Read a local or downloaded file as what its first bytes say it is.
   *
   * @param source how to name the file in a message: its path, or the link it came from
   */
  private static IfcModel read(Path file, String source) {
    Content content;
    try {
      content = ModelLinks.sniff(file);
    } catch (IOException e) {
      throw new ModelLoadException(source + " konnte nicht gelesen werden: " + reason(e), e);
    }

    switch (content) {
      case HTML ->
          throw new ModelLoadException(
              source
                  + " ist eine Webseite, keine IFC-Datei. Bei GitHub den Link zur Datei selbst"
                  + " verwenden (.../blob/...) oder den 'Raw'-Link.");
      case LFS_POINTER ->
          throw new ModelLoadException(
              source + " ist nur ein Git-LFS-Verweis; die Datei selbst liegt auf dem LFS-Server.");
      case UNKNOWN ->
          throw new ModelLoadException(
              source
                  + " ist keine IFC-Datei: sie beginnt nicht mit 'ISO-10303-21' und ist kein"
                  + " ifcZIP. CheckMate liest .ifc und .ifczip.");
      default -> {
        // STEP or ZIP: read below.
      }
    }

    IfcModel model = new IfcModel();
    // Through a stream closed here: handed the file itself, the toolbox keeps it open after a
    // failed read, which on Windows locks it until the program ends.
    try (InputStream in = Files.newInputStream(file)) {
      if (content == Content.ZIP) {
        model.readZipFile(in);
      } else {
        model.readStepFile(in);
      }
    } catch (Exception e) {
      throw new ModelLoadException(source + " ist kein lesbares IFC-Modell: " + reason(e), e);
    }
    return requireSupportedSchema(model, source);
  }

  private static void deleteQuietly(Path file) {
    if (file == null) {
      return;
    }
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      logger.warn("Could not delete the temporary download {}", file, e);
    }
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

  /**
   * The configured model, a path or a link: {@code checkmate.ifc}, else {@code CHECKMATE_IFC}.
   *
   * @return null when the bundled model should be used
   */
  static String configuredSource() {
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
