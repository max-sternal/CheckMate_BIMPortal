package test.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reading a model from a file or a link. A model that cannot be read is an error naming the file,
 * never an empty model.
 *
 * <p>Links are served by a small HTTP server inside the test, so nothing here needs the internet.
 */
class IfcResourcesTest {

  /** Our IFC4 case file: five slabs. */
  private static final String CASES = "/ifc/checkmate-cases-ifc4.ifc";

  @TempDir Path folder;

  private HttpServer server;

  @BeforeEach
  void startServer() throws IOException {
    byte[] ifc = resource(CASES);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    serve("/models/Platten.ifc", ifc, true);
    serve("/models/Platten.ifczip", zipped("Platten.ifc", ifc), true);
    serve("/models/gestreamt.ifc", ifc, false);
    serve("/github-seite", "<!DOCTYPE html><html><body>Datei</body></html>".getBytes(), true);
    serve(
        "/lfs.ifc",
        "version https://git-lfs.github.com/spec/v1\noid sha256:abc\nsize 4096\n".getBytes(),
        true);
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  // Files

  @Test
  void readsTheBundledModel() {
    assertThat(IfcModelElements.schemaOf(IfcResources.loadBundled())).isEqualTo("IFC2X3");
  }

  @Test
  void readsAnIfczipLikeTheFileInside() throws Exception {
    byte[] zipped = zipped("Platten.ifc", resource(CASES));
    Path zip = Files.write(folder.resolve("Platten.ifczip"), zipped);

    LoadedModel model = LoadedModel.read(zip.toString());

    assertThat(model.name()).isEqualTo("Platten.ifczip");
    assertThat(model.schema()).isEqualTo("IFC4");
    assertThat(model.elements()).hasSize(5);
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
        .hasMessageContaining("notiz.ifc")
        .hasMessageContaining("keine IFC-Datei");
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

  @Test
  void anEmptySourceIsAnError() {
    assertThatThrownBy(() -> IfcResources.load(" "))
        .isInstanceOf(ModelLoadException.class)
        .hasMessage("Kein Modell angegeben.");
  }

  // Links

  @Test
  void readsAModelBehindALinkAndKeepsNoCopy() throws Exception {
    long downloadsBefore = temporaryDownloads();

    LoadedModel model = LoadedModel.read(link("/models/Platten.ifc"));

    assertThat(model.name()).isEqualTo("Platten.ifc");
    assertThat(model.schema()).isEqualTo("IFC4");
    assertThat(model.elements()).hasSize(5);
    assertThat(temporaryDownloads()).isEqualTo(downloadsBefore);
  }

  @Test
  void readsAnIfczipBehindALink() {
    assertThat(LoadedModel.read(link("/models/Platten.ifczip")).elements()).hasSize(5);
  }

  @Test
  void readsADownloadWithoutAnnouncedLength() {
    assertThat(LoadedModel.read(link("/models/gestreamt.ifc")).elements()).hasSize(5);
  }

  @Test
  void aLinkToAWebPageIsAnError() {
    assertThatThrownBy(() -> IfcResources.load(link("/github-seite")))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("ist eine Webseite");
  }

  @Test
  void aGitLfsPointerIsAnError() {
    // Off GitHub there is no place to fetch the real file from, so the pointer is reported.
    assertThatThrownBy(() -> IfcResources.load(link("/lfs.ifc")))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("Git-LFS-Verweis");
  }

  @Test
  void aLinkThatAnswersWithAnErrorIsAnError() {
    assertThatThrownBy(() -> IfcResources.load(link("/gibt-es-nicht.ifc")))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("HTTP 404");
  }

  @Test
  void refusesDownloadsLargerThanTheLimit() {
    Path target = folder.resolve("download");

    assertThatThrownBy(
            () -> ModelLinks.download(URI.create(link("/models/Platten.ifc")), target, 100))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("größer als");
    // Without a Content-Length the limit is enforced while copying.
    assertThatThrownBy(
            () -> ModelLinks.download(URI.create(link("/models/gestreamt.ifc")), target, 100))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("größer als");
  }

  private String link(String path) {
    return "http://127.0.0.1:" + server.getAddress().getPort() + path;
  }

  /**
   * Answer a path with fixed bytes.
   *
   * @param announceLength false sends the body chunked, without a Content-Length
   */
  private void serve(String path, byte[] body, boolean announceLength) {
    server.createContext(
        path,
        exchange -> {
          exchange.sendResponseHeaders(200, announceLength ? body.length : 0);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
  }

  private static byte[] resource(String name) throws IOException {
    try (InputStream in = IfcResourcesTest.class.getResourceAsStream(name)) {
      if (in == null) {
        throw new IOException(name + " is missing from the test classpath");
      }
      return in.readAllBytes();
    }
  }

  private static byte[] zipped(String entry, byte[] content) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry(entry));
      zip.write(content);
      zip.closeEntry();
    }
    return bytes.toByteArray();
  }

  /** Temporary downloads left behind in the system's temp folder. */
  private static long temporaryDownloads() throws IOException {
    try (Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
      return files
          .filter(file -> file.getFileName().toString().startsWith("checkmate-model-"))
          .count();
    }
  }
}
