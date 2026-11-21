package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.LOCAL;
import static com.bimportal.hackathon.ids.IdsFixtures.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ZIP archives of IDS files: the ZIPs the BIM Portal provides for AIA projects, and ZIPs made by
 * hand.
 *
 * <p>The two project ZIPs are real, see {@code src/test/resources/ids/portal/README.md}.
 */
class IdsArchiveTest {

  /** Only the combined file: 36 LOINs, IFC4 only. */
  private static final String RAILWAY = "/ids/portal/aia-neubau-eisenbahnbruecke.zip";

  /** The combined file with 19 LOINs for IFC4, and each LOIN's own file for three schemas. */
  private static final String ROAD = "/ids/portal/aia-strasse-demonstrator.zip";

  @TempDir Path folder;

  @Test
  void readsAnAiaProjectZipWithTheProjectsTitleAndVersion() throws Exception {
    IdsDocument railway = IdsParser.read(bytes(RAILWAY), "Bahn");

    assertThat(railway.label()).isEqualTo("Beispiel-AIA Neubau Eisenbahnbrücke, Version 1.1");
    assertThat(railway.specifications()).hasSize(36);
    assertThat(railway.forSchema("IFC4")).hasSize(36);
    assertThat(railway.forSchema("IFC2X3")).isEmpty();
  }

  @Test
  void takesEachLoinAndSchemaOnceAcrossTheCombinedAndTheLoinFiles() throws Exception {
    IdsDocument road = IdsParser.read(bytes(ROAD), "Strasse");

    assertThat(road.label()).isEqualTo("AIA FB Straße Demonstrator FM Verkehrsanlage, Version 1.1");
    // 19 IFC4 specifications in the combined file; the LOIN files add IFC2X3 and IFC4X3_ADD2.
    assertThat(road.forSchema("IFC4")).hasSize(19);
    assertThat(road.forSchema("IFC2X3")).hasSize(19);
    assertThat(road.forSchema("IFC4X3")).hasSize(19);
    assertThat(road.specifications()).hasSize(57);
    assertThat(road.forSchema("IFC4"))
        .extracting(specification -> specification.loinGuid().orElseThrow())
        .doesNotHaveDuplicates();
  }

  @Test
  void theCombinedFileComesFirst() throws Exception {
    IdsDocument road = IdsParser.read(bytes(ROAD), "Strasse");

    // The combined file holds IFC4 only, and its specifications lead.
    assertThat(road.specifications().subList(0, 19))
        .allSatisfy(specification -> assertThat(specification.ifcVersion()).isEqualTo("IFC4"));
  }

  @Test
  void readsTheLoinGuidFromThePortalsIdentifier() throws Exception {
    IdsSpecification first = IdsParser.read(bytes(RAILWAY), "Bahn").specifications().get(0);

    assertThat(first.identifier()).endsWith(" 1");
    assertThat(first.loinGuid()).hasValueSatisfying(guid -> assertThat(guid).hasSize(36));
  }

  @Test
  void keepsEverySpecificationOfFilesWithoutLoinGuids() throws Exception {
    // The hand-written file has no identifiers, so two copies cannot be told apart from two LOINs.
    byte[] zip = zip(Map.of("a.ids", bytes(LOCAL), "b/c.ids", bytes(LOCAL)));

    IdsDocument document = IdsParser.read(zip, "eigene.zip");

    assertThat(document.specifications()).hasSize(10);
    assertThat(document.label()).isEqualTo("eigene.zip");
  }

  @Test
  void aZipWithOneIdsFileIsCalledByThatFile() throws Exception {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("liesmich.txt", "nicht IDS".getBytes(StandardCharsets.UTF_8));
    entries.put("anforderungen.ids", bytes(LOCAL));

    IdsDocument document = IdsParser.read(zip(entries), "eigene.zip");

    assertThat(document.label()).isEqualTo("CheckMate Testanforderungen, Version 1.2");
    assertThat(document.specifications()).hasSize(5);
  }

  @Test
  void readsAZipFromAFile() throws Exception {
    Path file = Files.write(folder.resolve("Eisenbahnbruecke.zip"), bytes(RAILWAY));

    IdsDocument document = IdsParser.read(file);

    assertThat(document.source()).isEqualTo("Eisenbahnbruecke.zip");
    assertThat(document.specifications()).hasSize(36);
  }

  @Test
  void aZipWithoutIdsFilesIsAnError() throws Exception {
    byte[] zip = zip(Map.of("liesmich.txt", "nicht IDS".getBytes(StandardCharsets.UTF_8)));

    assertThatThrownBy(() -> IdsParser.read(zip, "leer.zip"))
        .isInstanceOf(RequirementsException.class)
        .hasMessage("leer.zip enthält keine IDS-Datei.");
  }

  @Test
  void anIdsFileInTheZipThatIsNotIdsIsAnErrorNamingIt() throws Exception {
    byte[] zip = zip(Map.of("IDS/kaputt.ids", "<loin/>".getBytes(StandardCharsets.UTF_8)));

    assertThatThrownBy(() -> IdsParser.read(zip, "projekt.zip"))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("projekt.zip: IDS/kaputt.ids ist keine IDS-Datei");
  }

  @Test
  void aBrokenZipIsAnError() {
    byte[] broken = {'P', 'K', 3, 4, 20, 0, 0, 0, 99, 0};

    assertThatThrownBy(() -> IdsParser.read(broken, "kaputt.zip"))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("kaputt.zip");
  }

  private static byte[] zip(Map<String, byte[]> entries) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue());
        zip.closeEntry();
      }
    }
    return bytes.toByteArray();
  }
}
