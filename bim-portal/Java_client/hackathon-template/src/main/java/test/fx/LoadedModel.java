package test.fx;

import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import com.bimportal.hackathon.ids.ModelElement;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A model as the GUI and the CLI work with it: read once, with the elements the check sees.
 *
 * <p>Immutable, so a check that runs in the background keeps the model it was started on even
 * when another one is loaded meanwhile.
 *
 * @param name what to call the model, e.g. its file name
 * @param ifc the toolbox's model, for what still reads it directly (the GUI's class filter)
 * @param schema the IFC schema the file declares, e.g. {@code IFC4}
 * @param elements the elements as the LOIN check sees them
 * @param elementsById the same by id, to get from a finding back to its element
 */
public record LoadedModel(
    String name,
    IfcModel ifc,
    String schema,
    List<ModelElement> elements,
    Map<String, ModelElement> elementsById) {

  /**
   * Read a model from a file path or a link.
   *
   * @throws ModelLoadException when it cannot be read
   */
  public static LoadedModel read(String source) {
    return of(nameOf(source), IfcResources.load(source));
  }

  /** The bundled bridge model. */
  public static LoadedModel bundled() {
    String resource = IfcResources.DEFAULT_RESOURCE;
    return of(resource.substring(resource.lastIndexOf('/') + 1), IfcResources.loadBundled());
  }

  /**
   * The model {@code CHECKMATE_IFC} or {@code checkmate.ifc} names, the bundled one otherwise.
   *
   * @throws ModelLoadException when the configured model cannot be read
   */
  public static LoadedModel configured() {
    String source = IfcResources.configuredSource();
    return source == null ? bundled() : read(source);
  }

  static LoadedModel of(String name, IfcModel ifc) {
    List<ModelElement> elements = IfcModelElements.from(ifc);
    return new LoadedModel(
        name,
        ifc,
        IfcModelElements.schemaOf(ifc),
        elements,
        elements.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    ModelElement::id, Function.identity(), (first, duplicate) -> first)));
  }

  /** One line for a report: {@code Infra-Bridge.ifc - Schema IFC4, 77 Objekte}. */
  public String summary() {
    return name + " - Schema " + schema + ", " + elements.size() + " Objekte";
  }

  /** A file's name, or the file name at the end of a link. */
  private static String nameOf(String source) {
    String trimmed = source.trim();
    if (ModelLinks.isLink(trimmed)) {
      try {
        return ModelLinks.fileName(ModelLinks.downloadUri(trimmed));
      } catch (IllegalArgumentException e) {
        return trimmed;
      }
    }
    try {
      Path file = Path.of(trimmed).getFileName();
      return file == null ? trimmed : file.toString();
    } catch (InvalidPathException e) {
      return trimmed;
    }
  }
}
