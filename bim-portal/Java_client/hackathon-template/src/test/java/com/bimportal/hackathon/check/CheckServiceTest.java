package com.bimportal.hackathon.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.check.CheckService.Found;
import com.bimportal.hackathon.check.CheckService.Portal;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import com.bimportal.hackathon.report.CheckReport;
import com.bimportal.hackathon.report.CheckReport.ModelInfo;
import com.bimportal.hackathon.report.CheckReport.Run;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The check every front end runs, with the portal mocked. */
@ExtendWith(MockitoExtension.class)
class CheckServiceTest {

  /** Ueberbau &gt; Kappe, LPH 8-9. */
  private static final UUID KAPPE = UUID.fromString("be959b9d-daef-5315-9aba-f60b6ad54ccb");

  private static final ModelInfo MODEL = new ModelInfo("Kappen.ifc", "IFC2X3", 1);

  /** Classified as the Kappe LOIN asks, with its Feldnummer: it passes. */
  private static final List<ModelElement> ELEMENTS =
      List.of(
          new ModelElement(
              "kappe",
              "Kappe",
              "IfcBeam",
              List.of(
                  klassifizierung("Objektgruppe", "Ueberbau"),
                  klassifizierung("Objektklasse", "Kappe"),
                  klassifizierung("Objekttyp", "Kappe"),
                  new ModelProperty(
                      new PropertyKey("Bauteilinformation", "Feldnummer"), "1", "IfcInteger"))));

  @Mock private EnhancedBimPortalClient client;

  @TempDir Path folder;

  @Test
  void checksALoinAndDownloadsItOnce() throws Exception {
    when(client.exportLoinIds(KAPPE))
        .thenReturn(Optional.of(resource("/ids/portal/ueberbau-kappe-lph8-9.ids")));
    CheckService service = new CheckService(Portal.shared(client));
    Source kappe = new Source.Loin(KAPPE, "Ueberbau > Kappe");

    assertThat(service.known(kappe)).isEmpty();
    CheckReport first = service.check(List.of(kappe), MODEL, ELEMENTS, OffsetDateTime.now());
    CheckReport second = service.check(List.of(kappe), MODEL, ELEMENTS, OffsetDateTime.now());

    assertThat(first.exitCode()).isEqualTo(CheckReport.PASSED);
    assertThat(second.results()).hasSize(1);
    assertThat(service.known(kappe)).isPresent();
    verify(client, times(1)).exportLoinIds(KAPPE);
  }

  @Test
  void aSourceThatCannotBeReadIsAFailedRunNotAnException() {
    when(client.exportLoinIds(any())).thenReturn(Optional.empty());
    when(client.exportProjectIds(any())).thenReturn(Optional.empty());
    CheckService service = new CheckService(Portal.shared(client));

    Run run =
        service.run(new Source.PortalId("00000000-0000-0000-0000-000000000000"), MODEL, ELEMENTS);
    Run noId = service.run(new Source.PortalId("Kappe"), MODEL, ELEMENTS);

    assertThat(run.failed()).isTrue();
    assertThat(run.error()).contains("keine LOIN und kein AIA-Projekt");
    assertThat(noId.error()).isEqualTo("Keine ID in 'Kappe'.");
  }

  @Test
  void checksLocalFilesWithoutThePortal() throws Exception {
    Path ids =
        Files.write(folder.resolve("kappe.ids"), resource("/ids/portal/ueberbau-kappe-lph8-9.ids"));
    CheckService service = new CheckService(Portal.none());
    List<String> order = new ArrayList<>();

    CheckReport report =
        service.check(
            List.of(
                new Source.IdsFile(ids),
                new Source.Read(IdsParser.read(ids)),
                new Source.Loin(KAPPE, "Kappe")),
            MODEL,
            ELEMENTS,
            OffsetDateTime.now(),
            run -> order.add(run.failed() ? "failed" : "checked"));

    assertThat(order).containsExactly("checked", "checked", "failed");
    assertThat(report.runs().get(2).error()).contains("kein Zugang zum BIM Portal");
    assertThat(report.exitCode()).isEqualTo(CheckReport.NOT_READ);
  }

  @Test
  void findsProjectsAndLoins() {
    UUID project = UUID.randomUUID();
    when(client.searchProjects(any()))
        .thenReturn(List.of(new SimpleAiaProjectPublicDto().guid(project).name("Brücke")));
    when(client.searchLoins(any()))
        .thenReturn(List.of(new SimpleLoinPublicDto().guid(KAPPE).name("Ueberbau > Kappe")));

    Found found = new CheckService(Portal.shared(client)).search("Kappe");

    assertThat(found.projects()).containsExactly(new Source.AiaProject(project, "Brücke"));
    assertThat(found.loins()).extracting(entry -> entry.guid()).containsExactly(KAPPE);
    verify(client, never()).logout();
  }

  private static ModelProperty klassifizierung(String name, String value) {
    return new ModelProperty(new PropertyKey("Klassifizierung", name), value, "IfcLabel");
  }

  private static byte[] resource(String name) throws IOException {
    try (InputStream in = CheckServiceTest.class.getResourceAsStream(name)) {
      if (in == null) {
        throw new IOException(name + " is missing from the test classpath");
      }
      return in.readAllBytes();
    }
  }
}
