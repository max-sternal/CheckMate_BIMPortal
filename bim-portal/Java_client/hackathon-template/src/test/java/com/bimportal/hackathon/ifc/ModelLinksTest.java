package com.bimportal.hackathon.ifc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bimportal.hackathon.ifc.ModelLinks.Content;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Where a link is downloaded from, and what a file is by its first bytes. */
class ModelLinksTest {

  @TempDir Path folder;

  @Test
  void turnsAGithubFilePageIntoItsRawFile() {
    // The link from the bim-whale samples, as copied from the browser.
    String repo = "/andrewisen/bim-whale-ifc-samples";
    String file = "/LargeBuilding/IFC/LargeBuilding.ifc";

    assertThat(ModelLinks.downloadUri("https://github.com" + repo + "/blob/main" + file))
        .hasToString("https://raw.githubusercontent.com" + repo + "/main" + file);
  }

  @Test
  void keepsEscapedCharactersAndEscapesSpaces() {
    String expected =
        "https://raw.githubusercontent.com/buildingSMART/Certification-datasets/main/"
            + "IFC%204.0.2.1%20(IFC%204%20ADD2%20TC1)/Simple-Scene/Infra-Bridge.ifc";

    assertThat(
            ModelLinks.downloadUri(
                "https://github.com/buildingSMART/Certification-datasets/blob/main/"
                    + "IFC%204.0.2.1%20(IFC%204%20ADD2%20TC1)/Simple-Scene/Infra-Bridge.ifc"))
        .hasToString(expected);
    assertThat(
            ModelLinks.downloadUri(
                "https://github.com/buildingSMART/Certification-datasets/blob/main/"
                    + "IFC 4.0.2.1 (IFC 4 ADD2 TC1)/Simple-Scene/Infra-Bridge.ifc"))
        .hasToString(expected);
  }

  @Test
  void dropsWhatAGithubLinkCarriesAfterThePath() {
    assertThat(ModelLinks.downloadUri("https://github.com/o/r/blob/main/a.ifc?raw=true"))
        .hasToString("https://raw.githubusercontent.com/o/r/main/a.ifc");
    assertThat(ModelLinks.downloadUri("https://github.com/o/r/raw/main/a.ifc#L12"))
        .hasToString("https://raw.githubusercontent.com/o/r/main/a.ifc");
  }

  @Test
  void leavesEveryOtherLinkAsItIs() {
    assertThat(ModelLinks.downloadUri("https://raw.githubusercontent.com/o/r/main/a.ifc"))
        .hasToString("https://raw.githubusercontent.com/o/r/main/a.ifc");
    assertThat(ModelLinks.downloadUri("https://example.org/models/a.ifc?token=1"))
        .hasToString("https://example.org/models/a.ifc?token=1");
  }

  @Test
  void rejectsTextThatIsNoLink() {
    assertThatThrownBy(() -> ModelLinks.downloadUri("https://"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tellsLinksFromPaths() {
    assertThat(ModelLinks.isLink("https://example.org/a.ifc")).isTrue();
    assertThat(ModelLinks.isLink(" HTTP://example.org/a.ifc")).isTrue();
    assertThat(ModelLinks.isLink("C:\\Modelle\\a.ifc")).isFalse();
    assertThat(ModelLinks.isLink("test-models/ifc4/Infra-Bridge.ifc")).isFalse();
  }

  @Test
  void findsTheGitLfsFileOfARawLink() {
    URI raw = URI.create("https://raw.githubusercontent.com/o/r/main/a%20b.ifc");

    assertThat(ModelLinks.lfsMediaUri(raw))
        .contains(URI.create("https://media.githubusercontent.com/media/o/r/main/a%20b.ifc"));
    assertThat(ModelLinks.lfsMediaUri(URI.create("https://example.org/a.ifc"))).isEmpty();
  }

  @Test
  void namesTheFileAtTheEndOfALink() {
    assertThat(
            ModelLinks.fileName(
                URI.create("https://raw.githubusercontent.com/o/r/main/IFC%204/Infra-Bridge.ifc")))
        .isEqualTo("Infra-Bridge.ifc");
    assertThat(ModelLinks.fileName(URI.create("https://example.org/"))).isEqualTo("example.org");
  }

  @Test
  void recognisesFilesByTheirFirstBytes() throws Exception {
    assertThat(sniff("ISO-10303-21;\nHEADER;")).isEqualTo(Content.STEP);
    assertThat(sniff("\uFEFF  ISO-10303-21;")).as("BOM and whitespace").isEqualTo(Content.STEP);
    assertThat(sniff("version https://git-lfs.github.com/spec/v1\noid sha256:00\nsize 1\n"))
        .isEqualTo(Content.LFS_POINTER);
    assertThat(sniff("<!DOCTYPE html><html><body>GitHub</body></html>")).isEqualTo(Content.HTML);
    assertThat(sniff("Das ist kein IFC.")).isEqualTo(Content.UNKNOWN);

    Path zip = Files.write(folder.resolve("a.ifczip"), new byte[] {'P', 'K', 3, 4, 0, 0});
    assertThat(ModelLinks.sniff(zip)).isEqualTo(Content.ZIP);
  }

  private Content sniff(String text) throws Exception {
    Path file = Files.writeString(folder.resolve("file"), text, StandardCharsets.UTF_8);
    return ModelLinks.sniff(file);
  }
}
