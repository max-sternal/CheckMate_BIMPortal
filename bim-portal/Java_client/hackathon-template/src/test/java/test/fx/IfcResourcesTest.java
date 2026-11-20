package test.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A model that cannot be read is an error naming the file, never an empty model. */
class IfcResourcesTest {

  @TempDir Path folder;

  @Test
  void readsTheBundledModel() {
    assertThat(IfcModelElements.schemaOf(IfcResources.loadBundled())).isEqualTo("IFC2X3");
  }

  @Test
  void aMissingFileIsAnError() {
    Path missing = folder.resolve("fehlt.ifc");

    assertThatThrownBy(() -> IfcResources.load(missing))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("nicht gefunden")
        .hasMessageContaining("fehlt.ifc");
  }

  @Test
  void aFileThatIsNotIfcIsAnError() throws Exception {
    Path text = Files.writeString(folder.resolve("notiz.ifc"), "Das ist kein IFC.");

    assertThatThrownBy(() -> IfcResources.load(text))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("notiz.ifc");
  }

  @Test
  void aSchemaCheckMateDoesNotReadIsAnError() throws Exception {
    Path ifc2x2 =
        Files.writeString(
            folder.resolve("alt.ifc"),
            "ISO-10303-21;\nHEADER;\nFILE_DESCRIPTION((''),'2;1');\n"
                + "FILE_NAME('alt.ifc','2026-11-20T00:00:00',(''),(''),'','','');\n"
                + "FILE_SCHEMA(('IFC2X2_FINAL'));\nENDSEC;\nDATA;\nENDSEC;\n"
                + "END-ISO-10303-21;\n",
            StandardCharsets.US_ASCII);

    assertThatThrownBy(() -> IfcResources.load(ifc2x2))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("alt.ifc");
  }
}
