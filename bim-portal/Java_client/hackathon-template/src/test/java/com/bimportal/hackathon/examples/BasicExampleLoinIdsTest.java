package com.bimportal.hackathon.examples;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.bimportal.client.model.SimpleLoinPublicDto;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicExampleLoinIdsTest {

  @Mock private EnhancedBimPortalClient mockClient;

  @Test
  void testLoinIdsExport_SuccessfulDownload() {
    // Arrange
    SimpleLoinPublicDto loin1 = new SimpleLoinPublicDto();
    loin1.setGuid(UUID.randomUUID());
    loin1.setName("Test LOIN 1");

    SimpleLoinPublicDto loin2 = new SimpleLoinPublicDto();
    loin2.setGuid(UUID.randomUUID());
    loin2.setName("Test LOIN 2");

    List<SimpleLoinPublicDto> mockLoins = Arrays.asList(loin1, loin2);
    byte[] mockIdsContent = "<ids>Test IDS Content</ids>".getBytes();

    when(mockClient.searchLoins()).thenReturn(mockLoins);
    when(mockClient.exportLoinIds(any(UUID.class))).thenReturn(Optional.of(mockIdsContent));

    // Act
    BasicExample_old.demonstrateBasicLoinIdsExport(mockClient);

    // Assert
    verify(mockClient, times(1)).searchLoins();
    verify(mockClient, atLeastOnce()).exportLoinIds(any(UUID.class));
  }

  @Test
  void testLoinIdsExport_NoLoinsAvailable() {
    // Arrange
    when(mockClient.searchLoins()).thenReturn(List.of());

    // Act
    BasicExample_old.demonstrateBasicLoinIdsExport(mockClient);

    // Assert
    verify(mockClient, times(1)).searchLoins();
    verify(mockClient, never()).exportLoinIds(any(UUID.class));
  }

  @Test
  void testLoinIdsExport_EmptyExportResult() {
    // Arrange
    SimpleLoinPublicDto loin = new SimpleLoinPublicDto();
    loin.setGuid(UUID.randomUUID());
    loin.setName("Test LOIN");

    when(mockClient.searchLoins()).thenReturn(List.of(loin));
    when(mockClient.exportLoinIds(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    BasicExample_old.demonstrateBasicLoinIdsExport(mockClient);

    // Assert
    verify(mockClient, times(1)).searchLoins();
    verify(mockClient, atLeastOnce()).exportLoinIds(any(UUID.class));
  }
}
