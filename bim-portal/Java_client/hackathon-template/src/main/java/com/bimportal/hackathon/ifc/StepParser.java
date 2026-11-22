package com.bimportal.hackathon.ifc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Streaming pure-Java ISO 10303-21 (STEP Part 21) parser for IFC files.
 *
 * <p>Decodes bytes as UTF-8 with per-byte ISO-8859-1 fallback (so both UTF-8 and raw Latin-1
 * strings read cleanly without replacement characters), validates {@code FILE_SCHEMA} before
 * scanning {@code DATA}, and fast-skips entity instances rejected by the caller's predicate without
 * allocating AST nodes for them.
 */
public final class StepParser {

  private static final int MAX_NESTING_DEPTH = 256;

  private StepParser() {}

  /**
   * Parse an IFC STEP stream, keeping only entities whose upper-case type name satisfies {@code
   * keep}.
   *
   * @throws ModelLoadException when the stream is not a valid STEP IFC file or uses an unsupported
   *     schema
   */
  public static StepModel parse(InputStream in, String source, Predicate<String> keep) {
    Objects.requireNonNull(in, "in");
    Objects.requireNonNull(keep, "keep");
    return parseWithFactory(in, label(source), schema -> keep);
  }

  /** Parse every entity instance in an IFC STEP stream. */
  public static StepModel parseAll(InputStream in, String source) {
    Objects.requireNonNull(in, "in");
    return parseWithFactory(in, label(source), schema -> type -> true);
  }

  /**
   * Parse only the entities {@link IfcModelElements} needs: {@code IfcObject} and {@code
   * IfcTypeObject} instances, property/type relations, property sets, and single/enumerated/list
   * property values.
   */
  public static StepModel parseForElements(InputStream in, String source) {
    Objects.requireNonNull(in, "in");
    return parseWithFactory(in, label(source), StepParser::isElementRelated);
  }

  private static Predicate<String> isElementRelated(IfcSchema schema) {
    return type ->
        schema.isObject(type)
            || schema.isTypeObject(type)
            || schema.isSubtypeOf(type, "IFCRELDEFINESBYPROPERTIES")
            || "IFCRELDEFINESBYTYPE".equals(type)
            || "IFCPROPERTYSET".equals(type)
            || "IFCPROPERTYSINGLEVALUE".equals(type)
            || "IFCPROPERTYENUMERATEDVALUE".equals(type)
            || "IFCPROPERTYLISTVALUE".equals(type);
  }

  private static StepModel parseWithFactory(
      InputStream in, String source, Function<IfcSchema, Predicate<String>> keepFactory) {
    try {
      Parser parser = new Parser(new CharReader(in), source);
      return parser.parse(keepFactory);
    } catch (ModelLoadException e) {
      throw e;
    } catch (IOException e) {
      throw new ModelLoadException(source + " konnte nicht gelesen werden: " + reason(e), e);
    }
  }

  private static String label(String source) {
    return source == null || source.isBlank() ? "<stream>" : source;
  }

  private static String reason(Exception e) {
    return e.getMessage() == null || e.getMessage().isBlank()
        ? e.getClass().getSimpleName()
        : e.getMessage();
  }

  /**
   * Buffered byte-to-char stream decoding valid UTF-8 sequences and falling back byte-by-byte to
   * ISO-8859-1 on invalid sequences.
   */
  private static final class CharReader {
    private static final int BUFFER_SIZE = 64 * 1024;

    private final InputStream in;
    private final byte[] buf = new byte[BUFFER_SIZE];
    private int pos;
    private int limit;
    private boolean eof;
    private int pendingLowSurrogate = -1;

    CharReader(InputStream in) {
      this.in = in;
    }

    int nextChar() throws IOException {
      if (pendingLowSurrogate != -1) {
        int ch = pendingLowSurrogate;
        pendingLowSurrogate = -1;
        return ch;
      }
      if (pos >= limit) {
        ensureAvailable(1);
        if (pos >= limit) {
          return -1;
        }
      }
      int b0 = buf[pos] & 0xFF;
      if (b0 < 0x80) {
        pos++;
        return b0;
      }
      if (b0 >= 0xC2 && b0 <= 0xDF) {
        ensureAvailable(2);
        if (limit - pos >= 2) {
          int b1 = buf[pos + 1] & 0xFF;
          if ((b1 & 0xC0) == 0x80) {
            pos += 2;
            return ((b0 & 0x1F) << 6) | (b1 & 0x3F);
          }
        }
        pos++;
        return b0;
      }
      if (b0 >= 0xE0 && b0 <= 0xEF) {
        ensureAvailable(3);
        if (limit - pos >= 3) {
          int b1 = buf[pos + 1] & 0xFF;
          int b2 = buf[pos + 2] & 0xFF;
          if ((b1 & 0xC0) == 0x80
              && (b2 & 0xC0) == 0x80
              && (b0 != 0xE0 || b1 >= 0xA0)
              && (b0 != 0xED || b1 <= 0x9F)) {
            pos += 3;
            return ((b0 & 0x0F) << 12) | ((b1 & 0x3F) << 6) | (b2 & 0x3F);
          }
        }
        pos++;
        return b0;
      }
      if (b0 >= 0xF0 && b0 <= 0xF4) {
        ensureAvailable(4);
        if (limit - pos >= 4) {
          int b1 = buf[pos + 1] & 0xFF;
          int b2 = buf[pos + 2] & 0xFF;
          int b3 = buf[pos + 3] & 0xFF;
          if ((b1 & 0xC0) == 0x80
              && (b2 & 0xC0) == 0x80
              && (b3 & 0xC0) == 0x80
              && (b0 != 0xF0 || b1 >= 0x90)
              && (b0 != 0xF4 || b1 <= 0x8F)) {
            int codePoint =
                ((b0 & 0x07) << 18) | ((b1 & 0x3F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F);
            pos += 4;
            pendingLowSurrogate = Character.lowSurrogate(codePoint);
            return Character.highSurrogate(codePoint);
          }
        }
        pos++;
        return b0;
      }
      pos++;
      return b0;
    }

    private void ensureAvailable(int needed) throws IOException {
      while (limit - pos < needed && !eof) {
        if (pos > 0) {
          int remaining = limit - pos;
          if (remaining > 0) {
            System.arraycopy(buf, pos, buf, 0, remaining);
          }
          pos = 0;
          limit = remaining;
        }
        int read = in.read(buf, limit, buf.length - limit);
        if (read == -1) {
          eof = true;
        } else {
          limit += read;
        }
      }
    }
  }

  private static final class Parser {
    private final CharReader reader;
    private final String source;
    private final StringBuilder tokenBuf = new StringBuilder(128);
    private int current;
    private int nestingDepth;

    Parser(CharReader reader, String source) throws IOException {
      this.reader = reader;
      this.source = source;
      this.current = reader.nextChar();
      if (this.current == '\uFEFF') {
        this.current = reader.nextChar();
      }
    }

    StepModel parse(Function<IfcSchema, Predicate<String>> keepFactory) throws IOException {
      skipTrivia();
      String magic = readSectionKeywordUpper();
      if (!"ISO-10303-21".equals(magic)) {
        throw malformed("Beginnt nicht mit 'ISO-10303-21;'");
      }
      skipTrivia();
      expect(';');

      String declaredSchema = parseHeader();
      IfcSchema schema = validateSchema(declaredSchema);
      Predicate<String> keep = Objects.requireNonNull(keepFactory.apply(schema), "keep");

      List<StepEntity> entities = new ArrayList<>();
      Map<Long, StepEntity> byId = new HashMap<>();
      boolean sawData = false;
      boolean sawEndIso = false;

      while (true) {
        skipTrivia();
        if (current == -1) {
          break;
        }
        String section = readSectionKeywordUpper();
        if (section.isEmpty()) {
          throw malformed("Unerwartetes Zeichen '" + (char) current + "' zwischen Abschnitten");
        }
        switch (section) {
          case "DATA" -> {
            sawData = true;
            skipOptionalSectionHeader();
            parseDataSection(keep, entities, byId);
          }
          case "ANCHOR", "REFERENCE" -> {
            skipOptionalSectionHeader();
            skipStructuredSection();
          }
          case "SIGNATURE" -> {
            skipOptionalSectionHeader();
            skipSignatureSection();
          }
          case "END-ISO-10303-21" -> {
            skipTrivia();
            expect(';');
            sawEndIso = true;
          }
          default -> throw malformed("Unbekannter STEP-Abschnitt '" + section + "'");
        }
        if (sawEndIso) {
          break;
        }
      }

      if (!sawData) {
        throw malformed("DATA-Abschnitt fehlt");
      }
      if (!sawEndIso) {
        throw malformed("END-ISO-10303-21; fehlt (Datei abgeschnitten)");
      }
      return new StepModel(declaredSchema, schema, entities, byId);
    }

    private String parseHeader() throws IOException {
      skipTrivia();
      String header = readSectionKeywordUpper();
      if (!"HEADER".equals(header)) {
        throw malformed("HEADER-Abschnitt fehlt");
      }
      skipTrivia();
      expect(';');

      String declaredSchema = null;
      while (true) {
        skipTrivia();
        if (current == -1) {
          throw malformed("Unerwartetes Dateiende im HEADER-Abschnitt");
        }
        if (!isIdentifierStart(current)) {
          throw malformed("Unerwartetes Zeichen '" + (char) current + "' im HEADER-Abschnitt");
        }
        String name = readIdentifierUpper();
        if ("ENDSEC".equals(name)) {
          skipTrivia();
          expect(';');
          return declaredSchema;
        }
        if ("FILE_SCHEMA".equals(name)) {
          skipTrivia();
          StepValue.ListVal args = parseList();
          skipTrivia();
          expect(';');
          String extracted = extractSchema(args);
          if (extracted != null) {
            declaredSchema = extracted;
          }
        } else {
          skipToSemicolon();
        }
      }
    }

    private IfcSchema validateSchema(String declaredSchema) {
      if (declaredSchema == null) {
        throw new ModelLoadException(
            source + " nennt kein IFC-Schema (FILE_SCHEMA) - ist es wirklich eine IFC-Datei?");
      }
      IfcSchema schema = IfcSchema.forDeclaredSchema(declaredSchema);
      if (schema == null) {
        throw new ModelLoadException(
            source
                + " verwendet das Schema "
                + declaredSchema
                + " - CheckMate liest IFC2X3, IFC4 und IFC4X3.");
      }
      return schema;
    }

    private static String extractSchema(StepValue.ListVal args) {
      if (args.items().isEmpty()) {
        return null;
      }
      if (args.items().get(0) instanceof StepValue.ListVal schemas) {
        for (StepValue item : schemas.items()) {
          if (item instanceof StepValue.Text text && !text.value().isBlank()) {
            String trimmed = text.value().trim();
            int space = indexOfWhitespace(trimmed);
            String token = space >= 0 ? trimmed.substring(0, space) : trimmed;
            return token.toUpperCase(Locale.ROOT);
          }
        }
      }
      return null;
    }

    private static int indexOfWhitespace(String value) {
      for (int i = 0; i < value.length(); i++) {
        if (Character.isWhitespace(value.charAt(i))) {
          return i;
        }
      }
      return -1;
    }

    private void skipOptionalSectionHeader() throws IOException {
      skipTrivia();
      if (current == '(') {
        skipToSemicolon();
      } else {
        expect(';');
      }
    }

    private void parseDataSection(
        Predicate<String> keep, List<StepEntity> entities, Map<Long, StepEntity> byId)
        throws IOException {
      while (true) {
        skipTrivia();
        if (current == -1) {
          throw malformed("Unerwartetes Dateiende im DATA-Abschnitt (ENDSEC; fehlt)");
        }
        if (current == '#') {
          long id = parseEntityId();
          skipTrivia();
          expect('=');
          skipTrivia();
          if (current == '(') {
            // Complex entity instance: #id = (TYPE1(...) TYPE2(...));
            skipToSemicolon();
            continue;
          }
          String typeUpper = readIdentifierUpper();
          if (typeUpper.isEmpty()) {
            throw malformed("Entitätstyp nach '#" + id + "=' fehlt");
          }
          if (!keep.test(typeUpper)) {
            skipToSemicolon();
            continue;
          }
          skipTrivia();
          StepValue.ListVal argList = parseList();
          skipTrivia();
          expect(';');
          StepEntity entity = new StepEntity(id, typeUpper, argList.items());
          entities.add(entity);
          byId.put(id, entity);
        } else if (isIdentifierStart(current)) {
          String keyword = readIdentifierUpper();
          if ("ENDSEC".equals(keyword)) {
            skipTrivia();
            expect(';');
            return;
          }
          throw malformed("Unerwarteter Eintrag '" + keyword + "' im DATA-Abschnitt");
        } else {
          throw malformed("Unerwartetes Zeichen '" + (char) current + "' im DATA-Abschnitt");
        }
      }
    }

    private void skipStructuredSection() throws IOException {
      while (true) {
        skipTrivia();
        if (current == -1) {
          throw malformed("Unerwartetes Dateiende vor 'ENDSEC;'");
        }
        if (isIdentifierStart(current)) {
          String keyword = readIdentifierUpper();
          if ("ENDSEC".equals(keyword)) {
            skipTrivia();
            expect(';');
            return;
          }
        }
        skipSectionStatement();
      }
    }

    /**
     * Skip one statement in an Ed.3 ANCHOR or REFERENCE section, tolerating {@code <URI>} literals.
     */
    private void skipSectionStatement() throws IOException {
      while (true) {
        int c = current;
        if (c == -1) {
          throw malformed("Unerwartetes Dateiende vor ';'");
        }
        if (c == ';') {
          advance();
          return;
        }
        if (c == '<') {
          advance();
          while (current != '>') {
            if (current == -1) {
              throw malformed("Nicht geschlossene URI (<...>)");
            }
            advance();
          }
          advance();
        } else if (c == '\'') {
          skipQuotedString();
        } else if (c == '"') {
          skipBinaryLiteral();
        } else if (c == '/') {
          advance();
          if (current == '*') {
            advance();
            skipBlockCommentBody();
          }
        } else {
          advance();
        }
      }
    }

    private void skipSignatureSection() throws IOException {
      while (true) {
        if (current == -1) {
          throw malformed("Unerwartetes Dateiende im SIGNATURE-Abschnitt (ENDSEC; fehlt)");
        }
        if (current <= ' ') {
          advance();
          continue;
        }
        if (current == 'E' || current == 'e') {
          String word = readIdentifierUpper();
          if ("ENDSEC".equals(word)) {
            skipTrivia();
            if (current == ';') {
              advance();
              return;
            }
          }
          continue;
        }
        // Advance past non-whitespace token characters without tripping on Base64 '/'.
        while (current != -1 && current > ' ' && current != ';') {
          advance();
        }
        if (current == ';') {
          advance();
        }
      }
    }

    private StepValue parseValue() throws IOException {
      skipTrivia();
      if (current == -1) {
        throw malformed("Unerwartetes Dateiende beim Lesen eines Werts");
      }
      return switch (current) {
        case '$' -> {
          advance();
          yield StepValue.Null.INSTANCE;
        }
        case '*' -> {
          advance();
          yield StepValue.Derived.INSTANCE;
        }
        case '#' -> new StepValue.EntityRef(parseEntityId());
        case '\'' -> new StepValue.Text(parseString());
        case '"' -> new StepValue.BinaryVal(parseBinary());
        case '.' -> new StepValue.EnumVal(parseEnum());
        case '(' -> parseList();
        default -> {
          if (current == '+' || current == '-' || (current >= '0' && current <= '9')) {
            yield parseNumber();
          }
          if (isIdentifierStart(current)) {
            yield parseTypedValue();
          }
          throw malformed("Unerwartetes Zeichen '" + (char) current + "' in Wert");
        }
      };
    }

    private StepValue.ListVal parseList() throws IOException {
      if (++nestingDepth > MAX_NESTING_DEPTH) {
        throw malformed("Zu tief verschachtelte Liste");
      }
      try {
        expect('(');
        skipTrivia();
        if (current == ')') {
          advance();
          return new StepValue.ListVal(List.of());
        }
        List<StepValue> items = new ArrayList<>();
        while (true) {
          items.add(parseValue());
          skipTrivia();
          if (current == ',') {
            advance();
          } else if (current == ')') {
            advance();
            return new StepValue.ListVal(items);
          } else {
            throw malformed("Erwartet ',' oder ')' in Liste");
          }
        }
      } finally {
        nestingDepth--;
      }
    }

    private StepValue.TypedVal parseTypedValue() throws IOException {
      if (++nestingDepth > MAX_NESTING_DEPTH) {
        throw malformed("Zu tief verschachtelte Liste");
      }
      try {
        String typeUpper = readIdentifierUpper();
        skipTrivia();
        expect('(');
        skipTrivia();
        StepValue inner = parseValue();
        skipTrivia();
        if (current == ',') {
          List<StepValue> items = new ArrayList<>();
          items.add(inner);
          while (current == ',') {
            advance();
            items.add(parseValue());
            skipTrivia();
          }
          expect(')');
          return new StepValue.TypedVal(typeUpper, new StepValue.ListVal(items));
        }
        expect(')');
        return new StepValue.TypedVal(typeUpper, inner);
      } finally {
        nestingDepth--;
      }
    }

    private long parseEntityId() throws IOException {
      expect('#');
      long id = 0;
      int digits = 0;
      while (current >= '0' && current <= '9') {
        int digit = current - '0';
        if (id > (Long.MAX_VALUE - digit) / 10) {
          throw malformed("Entitätsnummer nach '#' zu groß");
        }
        id = id * 10 + digit;
        digits++;
        advance();
      }
      if (digits == 0) {
        throw malformed("Entitätsnummer nach '#' fehlt");
      }
      return id;
    }

    private String parseString() throws IOException {
      expect('\'');
      tokenBuf.setLength(0);
      while (true) {
        if (current == -1) {
          throw malformed("Nicht geschlossene Zeichenkette");
        }
        if (current == '\'') {
          advance();
          if (current == '\'') {
            tokenBuf.append("''");
            advance();
          } else {
            return StepStringDecoder.decode(tokenBuf.toString());
          }
        } else {
          tokenBuf.append((char) current);
          advance();
        }
      }
    }

    private String parseBinary() throws IOException {
      expect('"');
      tokenBuf.setLength(0);
      while (current != '"') {
        if (current == -1) {
          throw malformed("Nicht geschlossenes Binärliteral");
        }
        tokenBuf.append((char) current);
        advance();
      }
      advance();
      return tokenBuf.toString();
    }

    private String parseEnum() throws IOException {
      expect('.');
      tokenBuf.setLength(0);
      while (current != '.') {
        if (current == -1) {
          throw malformed("Nicht geschlossenes Enum-Literal");
        }
        tokenBuf.append((char) current);
        advance();
      }
      advance();
      return tokenBuf.toString().toUpperCase(Locale.ROOT);
    }

    private StepValue parseNumber() throws IOException {
      tokenBuf.setLength(0);
      if (current == '+' || current == '-') {
        tokenBuf.append((char) current);
        advance();
      }
      int digits = 0;
      while (current >= '0' && current <= '9') {
        tokenBuf.append((char) current);
        digits++;
        advance();
      }
      boolean isReal = false;
      if (current == '.') {
        isReal = true;
        tokenBuf.append('.');
        advance();
        while (current >= '0' && current <= '9') {
          tokenBuf.append((char) current);
          advance();
        }
      }
      if (digits == 0 && !isReal) {
        throw malformed("Ungültige Zahl '" + tokenBuf + "'");
      }
      if (current == 'E' || current == 'e') {
        isReal = true;
        tokenBuf.append((char) current);
        advance();
        if (current == '+' || current == '-') {
          tokenBuf.append((char) current);
          advance();
        }
        int expDigits = 0;
        while (current >= '0' && current <= '9') {
          tokenBuf.append((char) current);
          expDigits++;
          advance();
        }
        if (expDigits == 0) {
          throw malformed("Ungültiger Exponent in Zahl '" + tokenBuf + "'");
        }
      }
      String raw = tokenBuf.toString();
      if (!isReal) {
        try {
          return new StepValue.IntegerVal(Long.parseLong(raw));
        } catch (NumberFormatException overflow) {
          try {
            return new StepValue.RealVal(Double.parseDouble(raw), raw);
          } catch (NumberFormatException e) {
            throw malformed("Ungültige Ganzzahl '" + raw + "'");
          }
        }
      }
      try {
        return new StepValue.RealVal(Double.parseDouble(raw), raw);
      } catch (NumberFormatException e) {
        throw malformed("Ungültige Fließkommazahl '" + raw + "'");
      }
    }

    private void skipToSemicolon() throws IOException {
      while (true) {
        int c = current;
        if (c == -1) {
          throw malformed("Unerwartetes Dateiende vor ';'");
        }
        if (c == ';') {
          advance();
          return;
        }
        if (c == '\'') {
          skipQuotedString();
        } else if (c == '"') {
          skipBinaryLiteral();
        } else if (c == '/') {
          advance();
          if (current != '*') {
            throw malformed("Unerwartetes '/' außerhalb eines Kommentars");
          }
          advance();
          skipBlockCommentBody();
        } else {
          advance();
        }
      }
    }

    private void skipQuotedString() throws IOException {
      advance();
      while (true) {
        if (current == -1) {
          throw malformed("Nicht geschlossene Zeichenkette");
        }
        if (current == '\'') {
          advance();
          if (current == '\'') {
            advance();
          } else {
            return;
          }
        } else {
          advance();
        }
      }
    }

    private void skipBinaryLiteral() throws IOException {
      advance();
      while (current != '"') {
        if (current == -1) {
          throw malformed("Nicht geschlossenes Binärliteral");
        }
        advance();
      }
      advance();
    }

    private void skipTrivia() throws IOException {
      while (true) {
        while (current != -1 && current <= ' ') {
          advance();
        }
        if (current == '/') {
          advance();
          if (current != '*') {
            throw malformed("Unerwartetes '/' außerhalb eines Kommentars");
          }
          advance();
          skipBlockCommentBody();
        } else {
          return;
        }
      }
    }

    private void skipBlockCommentBody() throws IOException {
      while (true) {
        if (current == -1) {
          throw malformed("Nicht geschlossener Kommentar (/* ... */)");
        }
        if (current == '*') {
          advance();
          if (current == '/') {
            advance();
            return;
          }
        } else {
          advance();
        }
      }
    }

    private String readSectionKeywordUpper() throws IOException {
      tokenBuf.setLength(0);
      while (isSectionKeywordChar(current)) {
        tokenBuf.append(toAsciiUpper(current));
        advance();
      }
      return tokenBuf.toString();
    }

    private String readIdentifierUpper() throws IOException {
      tokenBuf.setLength(0);
      if (isIdentifierStart(current)) {
        tokenBuf.append(toAsciiUpper(current));
        advance();
        while (isIdentifierPart(current)) {
          tokenBuf.append(toAsciiUpper(current));
          advance();
        }
      }
      return tokenBuf.toString();
    }

    private void expect(char expected) throws IOException {
      if (current != expected) {
        String actual = current == -1 ? "Dateiende" : "'" + (char) current + "'";
        throw malformed("Erwartet '" + expected + "', gefunden: " + actual);
      }
      advance();
    }

    private void advance() throws IOException {
      current = reader.nextChar();
    }

    private ModelLoadException malformed(String detail) {
      return new ModelLoadException(source + " ist kein lesbares IFC-Modell: " + detail);
    }

    private static boolean isSectionKeywordChar(int c) {
      return (c >= 'A' && c <= 'Z')
          || (c >= 'a' && c <= 'z')
          || (c >= '0' && c <= '9')
          || c == '_'
          || c == '-';
    }

    private static boolean isIdentifierStart(int c) {
      return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_' || c == '!';
    }

    private static boolean isIdentifierPart(int c) {
      return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
    }

    private static char toAsciiUpper(int c) {
      return (c >= 'a' && c <= 'z') ? (char) (c - 32) : (char) c;
    }
  }
}
