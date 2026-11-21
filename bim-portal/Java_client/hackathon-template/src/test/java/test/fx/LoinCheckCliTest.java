package test.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bimportal.hackathon.catalogue.CatalogueFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import test.fx.LoinCheckCli.Arguments;

/** The CLI's command line. */
class LoinCheckCliTest {

  /** Ueberbau &gt; Kappe, LPH 8-9. */
  private static final String KAPPE = "be959b9d-daef-5315-9aba-f60b6ad54ccb";

  @Test
  void withoutArgumentsChecksTheConfiguredModelAgainstKappe() {
    assertThat(Arguments.parse()).isEqualTo(new Arguments(null, List.of(), List.of(), "Kappe"));
  }

  @Test
  void joinsTheWordsOfTheSearchTerm() {
    // Gradle splits --args="Ueberbau > Schaft" into three arguments.
    assertThat(Arguments.parse("Ueberbau", ">", "Schaft").search())
        .isEqualTo("Ueberbau > Schaft");
  }

  @Test
  void takesTheModelFromModelWhereverItStands() {
    assertThat(Arguments.parse("--model", "a.ifc", "Kappe"))
        .isEqualTo(new Arguments("a.ifc", List.of(), List.of(), "Kappe"));
    assertThat(Arguments.parse("Ueberbau", "--model", "https://example.org/a.ifc", "Kappe"))
        .isEqualTo(
            new Arguments("https://example.org/a.ifc", List.of(), List.of(), "Ueberbau Kappe"));
  }

  @Test
  void takesLoinsAndIdsFilesAsOftenAsGivenAndThenSearchesNothing() {
    Arguments arguments =
        Arguments.parse(
            "--loin",
            KAPPE,
            "--ids",
            "a.ids",
            "--loin",
            "https://via.bund.de/bim/loin/4a532757-d08f-5f5b-a7a7-198f54ad6c6e");

    assertThat(arguments.portalIds()).hasSize(2);
    assertThat(arguments.idsFiles()).containsExactly("a.ids");
    assertThat(arguments.search()).isNull();
    assertThat(arguments.needsPortal()).isTrue();
  }

  @Test
  void takesAiaProjectsLikeLoins() {
    Arguments arguments =
        Arguments.parse("--aia", "e6f69257-dfda-441b-97bb-e9df44c5cf8b", "--loin", KAPPE);

    assertThat(arguments.portalIds())
        .containsExactly("e6f69257-dfda-441b-97bb-e9df44c5cf8b", KAPPE);
    assertThat(arguments.search()).isNull();
    assertThatThrownBy(() -> Arguments.parse("--aia"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--aia");
  }

  @Test
  void searchesAsWellWhenATermIsGiven() {
    Arguments arguments = Arguments.parse("--ids", "a.ids", "Kappe");

    assertThat(arguments.search()).isEqualTo("Kappe");
    assertThat(Arguments.parse("--ids", "a.ids").needsPortal()).isFalse();
  }

  @Test
  void aSearchTermHoldingAnIdNamesThatLoin() {
    Arguments arguments = Arguments.parse("https://via.bund.de/bim/loin/" + KAPPE);

    assertThat(arguments.portalIds()).containsExactly("https://via.bund.de/bim/loin/" + KAPPE);
    assertThat(arguments.search()).isNull();
  }

  @Test
  void suggestsFromTheWholeCatalogueUnlessATermIsGiven() {
    Arguments whole = Arguments.parse("--suggest");
    Arguments kappe = Arguments.parse("--suggest", "Kappe");

    assertThat(whole.suggest()).isTrue();
    assertThat(whole.search()).isEmpty();
    assertThat(whole.needsPortal()).isTrue();
    assertThat(kappe.search()).isEqualTo("Kappe");
  }

  @Test
  void takesTheCatalogueFilters() {
    Arguments arguments =
        Arguments.parse(
            "--suggest",
            "--phase",
            "LPH 5",
            "--discipline",
            "Ingenieurbau",
            "--purpose",
            "Abnahme");

    assertThat(arguments.filter())
        .isEqualTo(new CatalogueFilter("Ingenieurbau", "LPH 5", "Abnahme"));
    assertThat(Arguments.parse("Kappe").filter().isAny()).isTrue();
    assertThatThrownBy(() -> Arguments.parse("--phase"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--phase");
  }

  @Test
  void rejectsWhatItDoesNotUnderstand() {
    assertThatThrownBy(() -> Arguments.parse("--model"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--model");
    assertThatThrownBy(() -> Arguments.parse("--loin", "--ids", "a.ids"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--loin");
    assertThatThrownBy(() -> Arguments.parse(KAPPE, "4a532757-d08f-5f5b-a7a7-198f54ad6c6e"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mehrere IDs");
    assertThatThrownBy(() -> Arguments.parse("--json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--json");
  }
}
