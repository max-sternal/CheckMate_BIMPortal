package com.bimportal.hackathon.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertyMappingTest {

  @TempDir Path folder;

  @Test
  void theBundledMappingIsTheHamburgConvention() {
    PropertyMapping mapping = PropertyMapping.bundled();

    assertThat(mapping.candidates(new PropertyKey("Klassifizierung", "Objekttyp")))
        .containsExactly(
            new PropertyKey("Klassifizierung", "Objekttyp"),
            new PropertyKey("Pset_Objektinformation", "_IDEbene3"));
    assertThat(mapping.aliases()).hasSize(3);
  }

  @Test
  void aRequestedAddressWithoutAliasIsOnlyItself() {
    PropertyKey key = new PropertyKey("Pset_SlabCommon", "LoadBearing");

    assertThat(PropertyMapping.NONE.candidates(key)).containsExactly(key);
    assertThat(PropertyMapping.bundled().candidates(key)).containsExactly(key);
  }

  @Test
  void aFileIsReadAndSeveralTargetsAreTriedInOrder() throws IOException {
    Path file =
        Files.writeString(
            folder.resolve("m.json"),
            """
            {"name": "Test", "aliases": [
              {"from": "A/x", "to": ["B/y", "C/z"]},
              {"from": "A/x", "to": ["D/w"]}]}
            """);

    PropertyMapping mapping = PropertyMapping.load(file);

    assertThat(mapping.name()).isEqualTo("Test");
    assertThat(mapping.candidates(new PropertyKey("A", "x")))
        .containsExactly(
            new PropertyKey("A", "x"),
            new PropertyKey("B", "y"),
            new PropertyKey("C", "z"),
            new PropertyKey("D", "w"));
    assertThat(mapping.describe()).contains("   A/x -> B/y, C/z, D/w");
  }

  @Test
  void aBrokenFileSaysWhatIsWrong() throws IOException {
    assertThatThrownBy(() -> PropertyMapping.parse("{"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("kein gültiges JSON");
    assertThatThrownBy(() -> PropertyMapping.parse("{}")).hasMessageContaining("aliases");
    assertThatThrownBy(
            () -> PropertyMapping.parse("{\"aliases\": [{\"from\": \"ohne\", \"to\": [\"A/b\"]}]}"))
        .hasMessageContaining("aliases[1].from");
    assertThatThrownBy(
            () -> PropertyMapping.parse("{\"aliases\": [{\"from\": \"A/b\", \"to\": []}]}"))
        .hasMessageContaining("aliases[1].to");
    assertThatThrownBy(() -> PropertyMapping.load(folder.resolve("fehlt.json")))
        .hasMessageContaining("nicht lesbar");
  }

  @Test
  void resolveKnowsNoneDefaultAndFiles() throws IOException {
    Path file = Files.writeString(folder.resolve("m.json"), "{\"name\":\"X\",\"aliases\":[]}");

    assertThat(PropertyAliases.resolve("none")).isSameAs(PropertyMapping.NONE);
    assertThat(PropertyAliases.resolve("default").name()).isEqualTo("BIM.Hamburg Brücken");
    assertThat(PropertyAliases.resolve(file.toString()).name()).isEqualTo("X");
    assertThat(List.of(PropertyAliases.current())).isNotEmpty();
  }
}
