package com.bimportal.hackathon.ifc;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Single-pass left-to-right decoder for ISO 10303-21 (STEP Part 21) string literals.
 *
 * <p>Handles wrapped lines ({@code \r}, {@code \n}), doubled quotes ({@code ''}) and backslashes
 * ({@code \\}), ISO-8859 codepage switches ({@code \PA\}..{@code \PI\}), extended characters
 * ({@code \S\<c>}), single-byte hex escapes ({@code \X\<hh>}), UTF-16 code-unit runs ({@code
 * \X2\...\X0\}) and UTF-32 code-point runs ({@code \X4\...\X0\}). Bare or malformed backslashes
 * such as Windows paths are emitted literally without throwing.
 */
public final class StepStringDecoder {

  /** Lookup table mapping codepage index 0..8 (ISO-8859-1..9) and byte offset 0..127 to chars. */
  private static final char[][] CODEPAGES = buildCodepages();

  private StepStringDecoder() {}

  /** Decode the raw content of a STEP string literal (without the surrounding apostrophes). */
  public static String decode(String raw) {
    if (raw == null || raw.isEmpty()) {
      return raw;
    }
    boolean hasLineBreak = raw.indexOf('\r') >= 0 || raw.indexOf('\n') >= 0;
    if (!hasLineBreak && raw.indexOf('\\') < 0 && raw.indexOf('\'') < 0) {
      return raw;
    }
    String input = hasLineBreak ? stripLineBreaks(raw) : raw;
    int len = input.length();
    StringBuilder out = new StringBuilder(len);
    int codepage = 0; // Reset to ISO-8859-1 (\PA\) per string.

    int i = 0;
    while (i < len) {
      char c = input.charAt(i);
      if (c == '\'') {
        out.append('\'');
        i += (i + 1 < len && input.charAt(i + 1) == '\'') ? 2 : 1;
      } else if (c == '\\') {
        if (i + 1 < len && input.charAt(i + 1) == '\\') {
          out.append('\\');
          i += 2;
        } else if (i + 3 < len && input.charAt(i + 1) == 'P' && input.charAt(i + 3) == '\\') {
          char page = input.charAt(i + 2);
          if (page >= 'A' && page <= 'I') {
            codepage = page - 'A';
            i += 4;
          } else {
            out.append('\\');
            i++;
          }
        } else if (i + 3 < len && input.charAt(i + 1) == 'S' && input.charAt(i + 2) == '\\') {
          char encoded = input.charAt(i + 3);
          if (encoded < 128) {
            out.append(CODEPAGES[codepage][encoded]);
            if (encoded == '\'' && i + 4 < len && input.charAt(i + 4) == '\'') {
              i += 5;
            } else {
              i += 4;
            }
          } else {
            out.append('\\');
            i++;
          }
        } else if (i + 4 < len && input.charAt(i + 1) == 'X' && input.charAt(i + 2) == '\\') {
          int value = parseHexByte(input, i + 3);
          if (value >= 0) {
            out.append((char) value);
            i += 5;
          } else {
            out.append('\\');
            i++;
          }
        } else if (i + 3 < len
            && input.charAt(i + 1) == 'X'
            && input.charAt(i + 2) == '2'
            && input.charAt(i + 3) == '\\') {
          int next = decodeX2(input, i + 4, out);
          if (next > 0) {
            i = next;
          } else {
            out.append('\\');
            i++;
          }
        } else if (i + 3 < len
            && input.charAt(i + 1) == 'X'
            && input.charAt(i + 2) == '4'
            && input.charAt(i + 3) == '\\') {
          int next = decodeX4(input, i + 4, out);
          if (next > 0) {
            i = next;
          } else {
            out.append('\\');
            i++;
          }
        } else {
          out.append('\\');
          i++;
        }
      } else {
        out.append(c);
        i++;
      }
    }
    return out.toString();
  }

  private static String stripLineBreaks(String raw) {
    int len = raw.length();
    StringBuilder cleaned = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      char c = raw.charAt(i);
      if (c != '\r' && c != '\n') {
        cleaned.append(c);
      }
    }
    return cleaned.toString();
  }

  private static int decodeX2(String input, int start, StringBuilder out) {
    int end = input.indexOf("\\X0\\", start);
    if (end <= start || (end - start) % 4 != 0) {
      return -1;
    }
    for (int pos = start; pos < end; pos++) {
      if (hexDigit(input.charAt(pos)) < 0) {
        return -1;
      }
    }
    for (int pos = start; pos < end; pos += 4) {
      int codeUnit =
          (hexDigit(input.charAt(pos)) << 12)
              | (hexDigit(input.charAt(pos + 1)) << 8)
              | (hexDigit(input.charAt(pos + 2)) << 4)
              | hexDigit(input.charAt(pos + 3));
      out.append((char) codeUnit);
    }
    return end + 4;
  }

  private static int decodeX4(String input, int start, StringBuilder out) {
    int end = input.indexOf("\\X0\\", start);
    if (end <= start || (end - start) % 8 != 0) {
      return -1;
    }
    for (int pos = start; pos < end; pos += 8) {
      int codePoint = parseHexInt(input, pos);
      if (codePoint < 0 || !Character.isValidCodePoint(codePoint)) {
        return -1;
      }
    }
    for (int pos = start; pos < end; pos += 8) {
      out.appendCodePoint(parseHexInt(input, pos));
    }
    return end + 4;
  }

  private static int parseHexByte(String input, int pos) {
    int high = hexDigit(input.charAt(pos));
    int low = hexDigit(input.charAt(pos + 1));
    return high < 0 || low < 0 ? -1 : (high << 4) | low;
  }

  private static int parseHexInt(String input, int pos) {
    int value = 0;
    for (int j = 0; j < 8; j++) {
      int digit = hexDigit(input.charAt(pos + j));
      if (digit < 0) {
        return -1;
      }
      value = (value << 4) | digit;
    }
    return value;
  }

  private static int hexDigit(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    }
    if (c >= 'A' && c <= 'F') {
      return c - 'A' + 10;
    }
    if (c >= 'a' && c <= 'f') {
      return c - 'a' + 10;
    }
    return -1;
  }

  private static char[][] buildCodepages() {
    byte[] upperHalf = new byte[128];
    for (int i = 0; i < 128; i++) {
      upperHalf[i] = (byte) (i + 128);
    }
    char[][] tables = new char[9][128];
    for (int page = 0; page < 9; page++) {
      String name = "ISO-8859-" + (page + 1);
      Charset charset =
          Charset.isSupported(name) ? Charset.forName(name) : StandardCharsets.ISO_8859_1;
      String decoded = new String(upperHalf, charset);
      decoded.getChars(0, 128, tables[page], 0);
    }
    return tables;
  }
}
