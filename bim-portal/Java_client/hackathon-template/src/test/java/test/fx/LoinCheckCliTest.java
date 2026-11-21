package test.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import test.fx.LoinCheckCli.Arguments;

/** The CLI's command line. */
class LoinCheckCliTest {

  @Test
  void withoutArgumentsChecksTheConfiguredModelAgainstKappe() {
    assertThat(Arguments.parse()).isEqualTo(new Arguments(null, "Kappe"));
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
        .isEqualTo(new Arguments("a.ifc", "Kappe"));
    assertThat(Arguments.parse("Ueberbau", "--model", "https://example.org/a.ifc", "Kappe"))
        .isEqualTo(new Arguments("https://example.org/a.ifc", "Ueberbau Kappe"));
  }

  @Test
  void rejectsWhatItDoesNotUnderstand() {
    assertThatThrownBy(() -> Arguments.parse("--model"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--model");
    assertThatThrownBy(() -> Arguments.parse("--json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--json");
  }
}
