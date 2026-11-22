package com.bimportal.hackathon.ids;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * XSD {@code <xs:restriction>} value constraints on an IDS {@code <property>} facet.
 *
 * <p>Supports regular expression {@code pattern} (anchored full-match, multiple patterns are
 * alternatives as in XML Schema), numeric bounds ({@code minInclusive}, {@code maxInclusive},
 * {@code minExclusive}, {@code maxExclusive}) compared with {@link BigDecimal}, and string lengths
 * ({@code length}, {@code minLength}, {@code maxLength}). All present facet categories must hold
 * simultaneously.
 */
public record ValueConstraint(
    List<CompiledPattern> patterns,
    BigDecimal minInclusive,
    BigDecimal maxInclusive,
    BigDecimal minExclusive,
    BigDecimal maxExclusive,
    Integer length,
    Integer minLength,
    Integer maxLength) {

  /** Step budget for regex evaluation to prevent catastrophic backtracking on untrusted IDS. */
  private static final int MAX_REGEX_STEPS = 100_000;

  /** A compiled XSD {@code xs:pattern} paired with its source expression for diagnostics. */
  public record CompiledPattern(String source, Pattern compiled) {}

  public ValueConstraint {
    patterns = patterns == null ? List.of() : List.copyOf(patterns);
  }

  /** Whether no restriction facet is configured. */
  public boolean isEmpty() {
    return patterns.isEmpty()
        && !hasNumericBounds()
        && length == null
        && minLength == null
        && maxLength == null;
  }

  /** Whether any numeric bound facet is present. */
  public boolean hasNumericBounds() {
    return minInclusive != null
        || maxInclusive != null
        || minExclusive != null
        || maxExclusive != null;
  }

  /** Whether a model value satisfies every configured restriction facet. */
  public boolean matches(String actual) {
    return violation(actual).isEmpty();
  }

  /**
   * German diagnostic explaining why {@code actual} fails this constraint, or empty when it
   * satisfies all facets.
   */
  public Optional<String> violation(String actual) {
    if (actual == null) {
      return Optional.of("Wert fehlt");
    }
    List<String> problems = new ArrayList<>();

    if (!patterns.isEmpty()) {
      PatternMatchResult match = matchPatterns(actual);
      if (match == PatternMatchResult.TIMED_OUT) {
        problems.add("Musterprüfung abgebrochen (Ausdruck zu komplex)");
      } else if (match == PatternMatchResult.NO_MATCH) {
        if (patterns.size() == 1) {
          problems.add("entspricht nicht dem Muster '" + patterns.get(0).source() + "'");
        } else {
          String all =
              patterns.stream()
                  .map(p -> "'" + p.source() + "'")
                  .collect(Collectors.joining(" | "));
          problems.add("entspricht keinem der Muster " + all);
        }
      }
    }

    if (hasNumericBounds()) {
      BigDecimal number = parseBigDecimal(actual);
      if (number == null) {
        problems.add("ist kein gültiger Zahlenwert für Bereichsprüfung (" + describeBounds() + ")");
      } else {
        if (minInclusive != null && number.compareTo(minInclusive) < 0) {
          problems.add("unterschreitet Minimum >= " + formatNumber(minInclusive));
        }
        if (minExclusive != null && number.compareTo(minExclusive) <= 0) {
          problems.add("muss größer als " + formatNumber(minExclusive) + " sein");
        }
        if (maxInclusive != null && number.compareTo(maxInclusive) > 0) {
          problems.add("überschreitet Maximum <= " + formatNumber(maxInclusive));
        }
        if (maxExclusive != null && number.compareTo(maxExclusive) >= 0) {
          problems.add("muss kleiner als " + formatNumber(maxExclusive) + " sein");
        }
      }
    }

    int codePoints = actual.codePointCount(0, actual.length());
    if (length != null && codePoints != length) {
      problems.add("Länge " + codePoints + " statt exakt " + length + " Zeichen");
    }
    if (minLength != null && codePoints < minLength) {
      problems.add("Länge " + codePoints + " unterschreitet Mindestlänge " + minLength + " Zeichen");
    }
    if (maxLength != null && codePoints > maxLength) {
      problems.add("Länge " + codePoints + " überschreitet Maximallänge " + maxLength + " Zeichen");
    }

    return problems.isEmpty() ? Optional.empty() : Optional.of(String.join("; ", problems));
  }

  /** Concise German summary for report headings, e.g. {@code >= 0, <= 100} or {@code Muster 'A.*'}. */
  public String describe() {
    List<String> parts = new ArrayList<>();
    if (minInclusive != null) {
      parts.add(">= " + formatNumber(minInclusive));
    }
    if (minExclusive != null) {
      parts.add("> " + formatNumber(minExclusive));
    }
    if (maxInclusive != null) {
      parts.add("<= " + formatNumber(maxInclusive));
    }
    if (maxExclusive != null) {
      parts.add("< " + formatNumber(maxExclusive));
    }
    if (length != null) {
      parts.add("Länge = " + length);
    }
    if (minLength != null) {
      parts.add("Länge >= " + minLength);
    }
    if (maxLength != null) {
      parts.add("Länge <= " + maxLength);
    }
    if (!patterns.isEmpty()) {
      String joined =
          patterns.stream().map(CompiledPattern::source).collect(Collectors.joining("|"));
      parts.add("Muster '" + joined + "'");
    }
    return String.join(", ", parts);
  }

  private String describeBounds() {
    List<String> bounds = new ArrayList<>();
    if (minInclusive != null) {
      bounds.add(">= " + formatNumber(minInclusive));
    }
    if (minExclusive != null) {
      bounds.add("> " + formatNumber(minExclusive));
    }
    if (maxInclusive != null) {
      bounds.add("<= " + formatNumber(maxInclusive));
    }
    if (maxExclusive != null) {
      bounds.add("< " + formatNumber(maxExclusive));
    }
    return String.join(", ", bounds);
  }

  /** Avoids OOM when untrusted IDS files specify extreme scientific exponents like 1E10000000. */
  private static String formatNumber(BigDecimal value) {
    if (Math.abs(value.scale()) <= 100 && Math.abs(value.precision() - value.scale()) <= 100) {
      return value.toPlainString();
    }
    return value.toString();
  }

  private enum PatternMatchResult {
    MATCHED,
    NO_MATCH,
    TIMED_OUT
  }

  private PatternMatchResult matchPatterns(String actual) {
    boolean timedOut = false;
    for (CompiledPattern pattern : patterns) {
      try {
        if (pattern.compiled().matcher(new BoundedCharSequence(actual, MAX_REGEX_STEPS)).matches()) {
          return PatternMatchResult.MATCHED;
        }
      } catch (RegexStepLimitExceeded e) {
        timedOut = true;
      }
    }
    return timedOut ? PatternMatchResult.TIMED_OUT : PatternMatchResult.NO_MATCH;
  }

  private static BigDecimal parseBigDecimal(String value) {
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    try {
      return new BigDecimal(trimmed);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static final class RegexStepLimitExceeded extends RuntimeException {
    RegexStepLimitExceeded() {
      super(null, null, false, false);
    }
  }

  /** CharSequence wrapper that aborts regex evaluation after a bounded number of character reads. */
  private static final class BoundedCharSequence implements CharSequence {

    private final CharSequence delegate;
    private final int maxSteps;
    private int steps;

    BoundedCharSequence(CharSequence delegate, int maxSteps) {
      this.delegate = delegate;
      this.maxSteps = maxSteps;
    }

    @Override
    public int length() {
      return delegate.length();
    }

    @Override
    public char charAt(int index) {
      if (++steps > maxSteps) {
        throw new RegexStepLimitExceeded();
      }
      return delegate.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return new BoundedCharSequence(delegate.subSequence(start, end), maxSteps - steps);
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }
}
