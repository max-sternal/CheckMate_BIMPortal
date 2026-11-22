package com.bimportal.hackathon.ifc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for ISO 10303-21 Part 21 string decoding in {@link StepStringDecoder}. */
class StepStringDecoderTest {

  @Test
  void decodesDoubledQuotesAndBackslashes() {
    assertThat(StepStringDecoder.decode("O''Brien")).isEqualTo("O'Brien");
    assertThat(StepStringDecoder.decode("''")).isEqualTo("'");
    assertThat(StepStringDecoder.decode("a\\\\b")).isEqualTo("a\\b");
    assertThat(StepStringDecoder.decode("\\\\\\\\")).isEqualTo("\\\\");
  }

  @Test
  void literalBackslashesPreventEscapeInterpretation() {
    // Single-pass left-to-right: \\ emits literal \ and X2 stays plain text.
    assertThat(StepStringDecoder.decode("\\\\X2\\\\00E4\\\\X0\\\\")).isEqualTo("\\X2\\00E4\\X0\\");
    assertThat(StepStringDecoder.decode("\\\\S\\\\d")).isEqualTo("\\S\\d");
    assertThat(StepStringDecoder.decode("\\\\X\\\\FC")).isEqualTo("\\X\\FC");
  }

  @Test
  void decodesIso8859ExtendedCharactersWithS() {
    assertThat(StepStringDecoder.decode("\\S\\d")).isEqualTo("ä");
    assertThat(StepStringDecoder.decode("\\S\\v")).isEqualTo("ö");
    assertThat(StepStringDecoder.decode("\\S\\|")).isEqualTo("ü");
    assertThat(StepStringDecoder.decode("\\S\\_")).isEqualTo("ß");
    assertThat(StepStringDecoder.decode("Br\\S\\|cke Gr\\S\\v\\S\\_e")).isEqualTo("Brücke Größe");
    // 0x5C + 0x80 = 0xDC ('Ü'), 0x27 + 0x80 = 0xA7 ('§')
    assertThat(StepStringDecoder.decode("\\S\\\\")).isEqualTo("Ü");
    assertThat(StepStringDecoder.decode("\\S\\''")).isEqualTo("§");
  }

  @Test
  void switchesIso8859CodepagesAndResetsPerString() {
    assertThat(StepStringDecoder.decode("\\PA\\\\S\\C")).isEqualTo("Ã");
    assertThat(StepStringDecoder.decode("\\PB\\\\S\\C")).isEqualTo("Ă");
    assertThat(StepStringDecoder.decode("\\PC\\\\S\\F")).isEqualTo("Ĉ");
    assertThat(StepStringDecoder.decode("\\PD\\\\S\\P")).isEqualTo("Đ");
    assertThat(StepStringDecoder.decode("\\PE\\\\S\\C")).isEqualTo("У");
    assertThat(StepStringDecoder.decode("\\PF\\\\S\\C")).isEqualTo("أ");
    assertThat(StepStringDecoder.decode("\\PG\\\\S\\C")).isEqualTo("Γ");
    assertThat(StepStringDecoder.decode("\\PH\\\\S\\`")).isEqualTo("א");
    assertThat(StepStringDecoder.decode("\\PI\\\\S\\^")).isEqualTo("Ş");

    // Switching within one string works, and the next string resets to ISO-8859-1 (\PA\).
    assertThat(StepStringDecoder.decode("\\PB\\\\S\\C\\PA\\\\S\\C")).isEqualTo("ĂÃ");
    assertThat(StepStringDecoder.decode("\\S\\C")).isEqualTo("Ã");
  }

  @Test
  void decodesSingleByteHexEscapesWithX() {
    assertThat(StepStringDecoder.decode("\\X\\FCbergang")).isEqualTo("übergang");
    assertThat(StepStringDecoder.decode("\\X\\E4\\X\\F6\\X\\FC\\X\\DF")).isEqualTo("äöüß");
    assertThat(StepStringDecoder.decode("\\X\\dcKO")).isEqualTo("ÜKO");
    assertThat(StepStringDecoder.decode("Zeile1\\X\\0AZeile2")).isEqualTo("Zeile1\nZeile2");
  }

  @Test
  void decodesUtf16CodeUnitsWithX2IncludingMultiUnitAndSurrogatePairs() {
    assertThat(StepStringDecoder.decode("\\X2\\00E4\\X0\\")).isEqualTo("ä");
    assertThat(StepStringDecoder.decode("\\X2\\00E400F6\\X0\\")).isEqualTo("äö");
    assertThat(StepStringDecoder.decode("Br\\X2\\00FC\\X0\\cke")).isEqualTo("Brücke");
    // Surrogate pair U+1F600 (😀) = D83D DE00
    assertThat(StepStringDecoder.decode("\\X2\\D83DDE00\\X0\\")).isEqualTo("\uD83D\uDE00");
  }

  @Test
  void decodesUtf32CodePointsWithX4() {
    assertThat(StepStringDecoder.decode("\\X4\\0001F600\\X0\\")).isEqualTo("\uD83D\uDE00");
    assertThat(StepStringDecoder.decode("\\X4\\0001F6000001F601\\X0\\"))
        .isEqualTo("\uD83D\uDE00\uD83D\uDE01");
  }

  @Test
  void stripsWrappedLineBreaks() {
    assertThat(StepStringDecoder.decode("Hallo\r\nWelt")).isEqualTo("HalloWelt");
    assertThat(StepStringDecoder.decode("Zeile1\nZeile2\rZeile3")).isEqualTo("Zeile1Zeile2Zeile3");
    assertThat(StepStringDecoder.decode("\\X2\\00E4\r\n00F6\\X0\\")).isEqualTo("äö");
  }

  @Test
  void emitsBareWindowsPathsAndMalformedEscapesLiterallyWithoutThrowing() {
    assertThat(StepStringDecoder.decode("C:\\Models\\foo")).isEqualTo("C:\\Models\\foo");
    assertThat(StepStringDecoder.decode("C:\\Project\\Spec\\Xray"))
        .isEqualTo("C:\\Project\\Spec\\Xray");
    assertThat(StepStringDecoder.decode("\\X\\ZZ")).isEqualTo("\\X\\ZZ");
    assertThat(StepStringDecoder.decode("\\X2\\00E4")).isEqualTo("\\X2\\00E4");
    assertThat(StepStringDecoder.decode("\\X2\\123\\X0\\")).isEqualTo("\\X2\\123\\X0\\");
    // Empty \X2\ is not decoded; the middle \\ then decodes as a single literal backslash.
    assertThat(StepStringDecoder.decode("\\X2\\\\X0\\")).isEqualTo("\\X2\\X0\\");
    assertThat(StepStringDecoder.decode("\\X4\\FFFFFFFF\\X0\\")).isEqualTo("\\X4\\FFFFFFFF\\X0\\");
    assertThat(StepStringDecoder.decode("\\PZ\\")).isEqualTo("\\PZ\\");
    assertThat(StepStringDecoder.decode("trailing\\")).isEqualTo("trailing\\");
    assertThat(StepStringDecoder.decode("")).isEmpty();
    assertThat(StepStringDecoder.decode(null)).isNull();
  }
}
