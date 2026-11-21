package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.INTEGER;
import static com.bimportal.hackathon.ids.IdsFixtures.KAPPE_LPH_8_9;
import static com.bimportal.hackathon.ids.IdsFixtures.bytes;
import static com.bimportal.hackathon.ids.IdsFixtures.element;
import static com.bimportal.hackathon.ids.IdsFixtures.feldnummer;
import static com.bimportal.hackathon.ids.IdsFixtures.klassifizierung;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bimportal.client.model.LoinForPublicRequest;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LOINs from the portal by ID and by search, with the portal mocked.
 *
 * <p>The point of the error cases: each of them would otherwise end as an empty report, which reads
 * like a pass.
 */
@ExtendWith(MockitoExtension.class)
class PortalLoinsTest {

  /** Ueberbau &gt; Kappe, LPH 8-9: the GUID the export answers to... */
  private static final UUID GUID = UUID.fromString("be959b9d-daef-5315-9aba-f60b6ad54ccb");

  /** ...and the ID of its version record in the catalogue, which the export does not know. */
  private static final UUID RECORD_ID = UUID.fromString("dd5675ce-408f-4c34-8618-7d543f2a76d4");

  private static final List<ModelElement> MODEL =
      List.of(
          element(
              "kappe",
              "IfcBeam",
              klassifizierung("Ueberbau", "Kappe", "Kappe"),
              feldnummer("1", INTEGER)));

  @Mock private EnhancedBimPortalClient client;

  // The ID in a text

  @Test
  void findsTheIdInALinkOrANote() {
    assertThat(PortalLoins.idIn("be959b9d-daef-5315-9aba-f60b6ad54ccb")).contains(GUID);
    assertThat(PortalLoins.idIn(" BE959B9D-DAEF-5315-9ABA-F60B6AD54CCB ")).contains(GUID);
    assertThat(PortalLoins.idIn("https://via.bund.de/bim/loin/" + GUID + "?tab=ids"))
        .contains(GUID);
    assertThat(PortalLoins.idIn("LOIN " + GUID + " (Kappe), siehe auch " + GUID)).contains(GUID);
  }

  @Test
  void aSearchTermHoldsNoId() {
    assertThat(PortalLoins.idIn("Ueberbau > Kappe")).isEmpty();
    assertThat(PortalLoins.idIn("")).isEmpty();
    // One hex digit too many is not an ID.
    assertThat(PortalLoins.idIn("be959b9d-daef-5315-9aba-f60b6ad54ccb0")).isEmpty();
  }

  @Test
  void refusesToGuessBetweenSeveralIds() {
    assertThatThrownBy(() -> PortalLoins.idIn(GUID + " " + RECORD_ID))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining(GUID.toString())
        .hasMessageContaining(RECORD_ID.toString());
  }

  // Requirements by ID

  @Test
  void readsTheExportOfALoinWithItsNameAndVersion() throws Exception {
    when(client.exportLoinIds(GUID)).thenReturn(Optional.of(bytes(KAPPE_LPH_8_9)));

    IdsDocument document = PortalLoins.requirements(client, "LOIN " + GUID);

    assertThat(document.label()).isEqualTo("Ueberbau > Kappe (Ingenieurbau): LPH 8-9, Version 4.1");
    assertThat(document.source()).isEqualTo("BIM Portal, LOIN " + GUID);
    assertThat(document.specifications()).hasSize(3);
  }

  @Test
  void checksTheModelAgainstTheSpecificationForItsSchema() throws Exception {
    when(client.exportLoinIds(GUID)).thenReturn(Optional.of(bytes(KAPPE_LPH_8_9)));

    List<CheckResult> results = PortalLoins.requirements(client, GUID).check("IFC2X3", MODEL);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).specification().ifcVersion()).isEqualTo("IFC2X3");
    assertThat(results.get(0).applicableElements()).isEqualTo(1);
    assertThat(results.get(0).passed()).isTrue();
  }

  @Test
  void findsALoinByTheIdOfItsVersionRecord() throws Exception {
    when(client.exportLoinIds(RECORD_ID)).thenReturn(Optional.empty());
    when(client.searchLoins(any())).thenReturn(List.of(catalogueEntry()));
    when(client.exportLoinIds(GUID)).thenReturn(Optional.of(bytes(KAPPE_LPH_8_9)));

    IdsDocument document = PortalLoins.requirements(client, RECORD_ID);

    assertThat(document.source()).isEqualTo("BIM Portal, LOIN " + GUID);
  }

  @Test
  void anUnknownIdIsAnError() {
    UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000000");
    when(client.exportLoinIds(unknown)).thenReturn(Optional.empty());
    when(client.searchLoins(any())).thenReturn(List.of(catalogueEntry()));

    assertThatThrownBy(() -> PortalLoins.requirements(client, unknown))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("keine LOIN mit der ID " + unknown);
  }

  @Test
  void aLoinWithoutExportIsAnError() {
    when(client.exportLoinIds(RECORD_ID)).thenReturn(Optional.empty());
    when(client.searchLoins(any())).thenReturn(List.of(catalogueEntry()));
    when(client.exportLoinIds(GUID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> PortalLoins.requirements(client, RECORD_ID))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("keinen IDS-Export")
        .hasMessageContaining("Ueberbau > Kappe");
  }

  @Test
  void aTextWithoutIdIsAnErrorBeforeThePortalIsAsked() {
    assertThatThrownBy(() -> PortalLoins.requirements(client, "Kappe"))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("Keine LOIN-ID");
    verify(client, never()).exportLoinIds(any());
  }

  @Test
  void anExportThatIsNotIdsIsAnError() {
    byte[] errorPage = "<html><body>Wartung</body></html>".getBytes(StandardCharsets.UTF_8);
    when(client.exportLoinIds(GUID)).thenReturn(Optional.of(errorPage));

    assertThatThrownBy(() -> PortalLoins.requirements(client, GUID))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("keine IDS-Datei");
  }

  @Test
  void anExportWithoutSpecificationIsAnError() {
    byte[] empty =
        "<ids xmlns=\"http://standards.buildingsmart.org/IDS\"><specifications/></ids>"
            .getBytes(StandardCharsets.UTF_8);
    when(client.exportLoinIds(GUID)).thenReturn(Optional.of(empty));

    IdsDocument document = PortalLoins.requirements(client, GUID);

    assertThatThrownBy(() -> document.check("IFC2X3", MODEL))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("keine Spezifikation");
  }

  @Test
  void anExportThatDoesNotCoverTheModelSchemaIsAnError() throws Exception {
    when(client.exportLoinIds(GUID)).thenReturn(Optional.of(bytes(KAPPE_LPH_8_9)));

    IdsDocument document = PortalLoins.requirements(client, GUID);

    assertThatThrownBy(() -> document.check("IFC5", MODEL))
        .isInstanceOf(RequirementsException.class)
        .hasMessageContaining("IFC5")
        .hasMessageContaining("IFC4X3_ADD2")
        .hasMessageContaining("IFC2X3");
  }

  // Search

  @Test
  void readsEveryPageOfASearch() {
    List<SimpleLoinPublicDto> fullPage =
        IntStream.range(0, PortalLoins.PAGE_SIZE).mapToObj(i -> catalogueEntry()).toList();
    when(client.searchLoins(argThat(request -> page(request) == 0))).thenReturn(fullPage);
    when(client.searchLoins(argThat(request -> page(request) == 1)))
        .thenReturn(List.of(catalogueEntry(), catalogueEntry()));

    assertThat(PortalLoins.search(client, "LPH")).hasSize(PortalLoins.PAGE_SIZE + 2);
  }

  @Test
  void passesTheTermTrimmedAndLeavesItOutWhenEmpty() {
    // Matchers see null while the next stub is set up, hence the null checks.
    when(client.searchLoins(argThat(request -> "Kappe".equals(term(request)))))
        .thenReturn(List.of(catalogueEntry()));
    when(client.searchLoins(argThat(request -> request != null && term(request) == null)))
        .thenReturn(Collections.nCopies(3, catalogueEntry()));

    assertThat(PortalLoins.search(client, " Kappe ")).hasSize(1);
    assertThat(PortalLoins.search(client, "")).hasSize(3);
  }

  /** The catalogue's entry for the Kappe LOIN: record ID and GUID differ. */
  private static SimpleLoinPublicDto catalogueEntry() {
    return new SimpleLoinPublicDto()
        .id(RECORD_ID)
        .guid(GUID)
        .name("Ueberbau > Kappe (Ingenieurbau): LPH 8-9");
  }

  private static String term(LoinForPublicRequest request) {
    return request == null ? null : request.getSearchString();
  }

  private static int page(LoinForPublicRequest request) {
    return request == null || request.getPageNumber() == null ? -1 : request.getPageNumber();
  }
}
