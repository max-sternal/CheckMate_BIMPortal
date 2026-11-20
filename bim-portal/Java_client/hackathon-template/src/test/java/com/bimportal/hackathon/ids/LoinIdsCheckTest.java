package com.bimportal.hackathon.ids;

import static com.bimportal.hackathon.ids.IdsFixtures.INTEGER;
import static com.bimportal.hackathon.ids.IdsFixtures.KAPPE_LPH_8_9;
import static com.bimportal.hackathon.ids.IdsFixtures.bytes;
import static com.bimportal.hackathon.ids.IdsFixtures.element;
import static com.bimportal.hackathon.ids.IdsFixtures.feldnummer;
import static com.bimportal.hackathon.ids.IdsFixtures.klassifizierung;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Fetching a LOIN's IDS and picking the block for the model, with the portal mocked.
 *
 * <p>The point of the error cases: each of them would otherwise end as an empty report, which reads
 * like a pass.
 */
@ExtendWith(MockitoExtension.class)
class LoinIdsCheckTest {

  private static final UUID LOIN = UUID.fromString("be959b9d-daef-5315-9aba-f60b6ad54ccb");

  private static final List<ModelElement> MODEL =
      List.of(
          element(
              "kappe",
              "IfcBeam",
              klassifizierung("Ueberbau", "Kappe", "Kappe"),
              feldnummer("1", INTEGER)));

  @Mock private EnhancedBimPortalClient client;

  @Test
  void checksTheModelAgainstTheBlockForItsSchema() throws Exception {
    when(client.exportLoinIds(LOIN)).thenReturn(Optional.of(bytes(KAPPE_LPH_8_9)));

    CheckResult result = LoinIdsCheck.check(client, LOIN, "IFC2X3", MODEL);

    assertThat(result.specification().ifcVersion()).isEqualTo("IFC2X3");
    assertThat(result.applicableElements()).isEqualTo(1);
    assertThat(result.passed()).isTrue();
  }

  @Test
  void failsWhenThePortalHasNoExport() {
    when(client.exportLoinIds(LOIN)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> LoinIdsCheck.check(client, LOIN, "IFC2X3", MODEL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(LOIN.toString());
  }

  @Test
  void failsWhenTheExportHoldsNoSpecification() {
    byte[] empty =
        "<ids xmlns=\"http://standards.buildingsmart.org/IDS\"><specifications/></ids>"
            .getBytes(StandardCharsets.UTF_8);
    when(client.exportLoinIds(LOIN)).thenReturn(Optional.of(empty));

    assertThatThrownBy(() -> LoinIdsCheck.check(client, LOIN, "IFC2X3", MODEL))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void failsWhenTheExportDoesNotCoverTheModelSchema() throws Exception {
    when(client.exportLoinIds(LOIN)).thenReturn(Optional.of(bytes(KAPPE_LPH_8_9)));

    assertThatThrownBy(() -> LoinIdsCheck.check(client, LOIN, "IFC5", MODEL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("IFC5")
        .hasMessageContaining("IFC4X3_ADD2")
        .hasMessageContaining("IFC2X3");
  }
}
