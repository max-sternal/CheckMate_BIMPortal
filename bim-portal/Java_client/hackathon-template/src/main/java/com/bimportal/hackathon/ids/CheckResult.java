package com.bimportal.hackathon.ids;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of checking one IDS specification against a model.
 *
 * @param specification what was checked against
 * @param examinedElements how many model elements were looked at
 * @param applicableElements how many of them the specification applies to
 * @param findings one finding per applicable element and requirement
 * @param diagnosis why nothing was selected, when nothing was; {@link Diagnosis#NONE} otherwise
 * @param relatedTypes elements turned down only because of a related data type; {@link
 *     RelatedTypes#NONE} when there are none
 */
public record CheckResult(
    IdsSpecification specification,
    int examinedElements,
    int applicableElements,
    List<Finding> findings,
    Diagnosis diagnosis,
    RelatedTypes relatedTypes) {

  /** What a single requirement came to on a single element. */
  public enum Status {
    FULFILLED("erfüllt"),
    MISSING("Merkmal fehlt"),
    WRONG_VALUE("Wert weicht ab"),
    WRONG_DATATYPE("Datentyp weicht ab"),
    WRONG_ENTITY("IFC-Entity nicht zulässig"),
    WRONG_PREDEFINED_TYPE("PredefinedType weicht ab"),
    PROHIBITED_PRESENT("unzulässiges Merkmal vorhanden");

    private final String label;

    Status(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }

    public boolean ok() {
      return this == FULFILLED;
    }
  }

  /** What a specification came to as a whole. */
  public enum Verdict {
    PASSED("erfüllt"),
    FAILED("nicht erfüllt"),
    /** Every evaluated requirement is met, but some were not evaluated. */
    PARTLY_EVALUATED("erfüllt (soweit ausgewertet)"),
    /** It selected no element; see {@link CheckResult#notApplicable()}. */
    NOT_APPLICABLE("nicht prüfbar");

    private final String label;

    Verdict(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  /**
   * One checked requirement on one element.
   *
   * @param requirement the requirement as the LOIN words it
   * @param detail what was actually found, including the address it was found under
   */
  public record Finding(
      String elementId, String elementLabel, String requirement, Status status, String detail) {

    /** One report line: {@code [erfüllt] Kappe - Klassifizierung/Objekttyp = 'Kappe' - ...} */
    public String line() {
      return "[" + status.label() + "] " + elementLabel + " - " + requirement + " - " + detail;
    }

    /** The same without the element, for a report already grouped by element. */
    public String requirementLine() {
      return "[" + status.label() + "] " + requirement + " - " + detail;
    }
  }

  /**
   * Why the applicability selected no element.
   *
   * <p>"0 von 303 anwendbar" is a dead end for whoever has to act on the report, and the reason is
   * rarely that the model lacks the elements - it is that it addresses or names them differently.
   * So every facet is also counted on its own, and the elements that came closest are listed with
   * the values they carry instead.
   *
   * <p>Diagnosing is not checking. Nothing here widens what the specification applies to: an
   * element that fails one applicability facet is not checked against the requirements, because IDS
   * says the specification does not describe it. The diagnosis only states what stood in the way.
   */
  public record Diagnosis(List<FacetMatch> facets, List<NearMiss> nearMisses) {

    /** Nothing to diagnose, because the specification did select elements. */
    public static final Diagnosis NONE = new Diagnosis(List.of(), List.of());

    public boolean isEmpty() {
      return facets.isEmpty() && nearMisses.isEmpty();
    }

    /** The diagnosis as report lines, indented to sit under a summary line. */
    public List<String> lines() {
      List<String> lines = new ArrayList<>();
      facets.forEach(facet -> lines.add("   " + facet.line()));

      if (!nearMisses.isEmpty()) {
        lines.add("   Nächstliegende Elemente:");
        for (NearMiss nearMiss : nearMisses) {
          lines.add("      " + nearMiss.line());
          nearMiss.divergences().forEach(divergence -> lines.add("         " + divergence));
        }
      }
      return lines;
    }
  }

  /**
   * How far one applicability facet gets on its own.
   *
   * @param facet the facet as the LOIN words it
   * @param matchedElements elements meeting the facet: address, value and data type
   * @param addressFound elements carrying that address at all, whatever value it holds
   * @param valueButNotType elements carrying the required value, but not the required data type
   * @param typeDifferences how those types differ, e.g. {@code IFCTEXT statt IFCLABEL}; empty when
   *     {@code valueButNotType} is 0
   */
  public record FacetMatch(
      String facet,
      int matchedElements,
      int addressFound,
      int valueButNotType,
      String typeDifferences) {

    /** A facet without a data type to miss, such as the entity. */
    public FacetMatch(String facet, int matchedElements, int addressFound) {
      this(facet, matchedElements, addressFound, 0, "");
    }

    public String line() {
      String wrongType =
          valueButNotType
              + " Element(e) mit passendem Wert, aber Datentyp "
              + typeDifferences;

      if (matchedElements > 0) {
        String line = facet + " trifft " + matchedElements + " Element(e)";
        return valueButNotType > 0 ? line + "; dazu " + wrongType : line;
      }
      if (addressFound == 0) {
        return facet + " trifft 0 - Merkmal im Modell nicht vorhanden";
      }
      int otherValue = addressFound - valueButNotType;
      if (valueButNotType == 0) {
        return facet + " trifft 0 - " + otherValue + " Element(e) mit abweichendem Wert";
      }
      return otherValue > 0
          ? facet + " trifft 0 - " + wrongType + ", " + otherValue + " mit abweichendem Wert"
          : facet + " trifft 0 - " + wrongType;
    }
  }

  /**
   * An element that satisfied part of the applicability, and what kept it out.
   *
   * @param matchedFacets how many of {@code totalFacets} carry the required value. The data type is
   *     not counted here, so that a model storing every value in a related type still gets its
   *     closest elements named; type differences are listed among the divergences.
   * @param divergences one line per facet that did not hold
   */
  public record NearMiss(
      String elementId,
      String elementLabel,
      String entityName,
      int matchedFacets,
      int totalFacets,
      List<String> divergences) {

    public String line() {
      return elementLabel
          + " ("
          + entityName
          + ") - Wert passt bei "
          + matchedFacets
          + " von "
          + totalFacets
          + " Facetten";
    }
  }

  /**
   * Elements the applicability turned down only because of a related data type.
   *
   * <p>The verdict follows IDS, which compares data types strictly. This is the hint decided for
   * that rule (docs/WORKFLOW.md, section 5): it names what a lenient reading would have checked, so
   * the reader can tell "wrong values" apart from "right values in a related type".
   *
   * @param elementIds the elements that would apply if related types were accepted
   * @param typeDifferences how the types differ, e.g. {@code IFCTEXT statt IFCLABEL}
   */
  public record RelatedTypes(List<String> elementIds, List<String> typeDifferences) {

    /** No element was turned down for a related type alone. */
    public static final RelatedTypes NONE = new RelatedTypes(List.of(), List.of());

    public boolean isEmpty() {
      return elementIds.isEmpty();
    }

    public String line() {
      return "Hinweis: "
          + elementIds.size()
          + " weitere(s) Element(e) wären anwendbar,"
          + " wenn verwandte Datentypen akzeptiert würden ("
          + String.join(", ", typeDifferences)
          + ")";
    }
  }

  /**
   * True when the specification selected no element at all.
   *
   * <p>This is not a pass. An empty finding list means the check could not say anything, usually
   * because the model does not carry the classification the specification selects on, and it has to
   * be shown differently from a model that was checked and came out clean. See {@link #diagnosis()}
   * for what stood in the way.
   */
  public boolean notApplicable() {
    return applicableElements == 0;
  }

  /** Applicable elements with at least one requirement not met. */
  public long failingElements() {
    return findings.stream()
        .filter(finding -> !finding.status().ok())
        .map(Finding::elementId)
        .distinct()
        .count();
  }

  /** Applicable elements that meet every requirement checked. */
  public long passingElements() {
    return applicableElements - failingElements();
  }

  public long count(Status status) {
    return findings.stream().filter(finding -> finding.status() == status).count();
  }

  /**
   * True when every requirement of the specification was evaluated. False when the parser listed
   * requirements CheckMate does not evaluate; the findings then cover only the rest.
   */
  public boolean complete() {
    return specification.unsupported().requirements().isEmpty();
  }

  /**
   * True when the specification applied to something and every requirement was met - all of them,
   * so never when some could not be evaluated.
   */
  public boolean passed() {
    return !notApplicable()
        && complete()
        && findings.stream().allMatch(finding -> finding.status().ok());
  }

  /** The specification's verdict as a whole. */
  public Verdict verdict() {
    if (notApplicable()) {
      return Verdict.NOT_APPLICABLE;
    }
    if (findings.stream().anyMatch(finding -> !finding.status().ok())) {
      return Verdict.FAILED;
    }
    return complete() ? Verdict.PASSED : Verdict.PARTLY_EVALUATED;
  }

  /**
   * How many of several specifications came to which verdict, e.g. {@code 36 Spezifikation(en) für
   * IFC4: 2 erfüllt, 34 nicht prüfbar}.
   *
   * @param schema the model's schema, which picked the specifications
   */
  public static String tally(List<CheckResult> results, String schema) {
    Map<Verdict, Long> counts = new EnumMap<>(Verdict.class);
    results.forEach(result -> counts.merge(result.verdict(), 1L, Long::sum));
    List<String> parts = new ArrayList<>();
    counts.forEach((verdict, count) -> parts.add(count + " " + verdict.label()));
    return results.size() + " Spezifikation(en) für " + schema + ": " + String.join(", ", parts);
  }

  /** The summary with the verdict in front, for an overview of several specifications. */
  public String overviewLine() {
    return "[" + verdict().label() + "] " + summary();
  }

  /** A single line stating what happened, for the top of a report. */
  public String summary() {
    StringBuilder summary = new StringBuilder();
    summary
        .append(specification.name())
        .append(" (")
        .append(specification.ifcVersion())
        .append("): ")
        .append(applicableElements)
        .append(" von ")
        .append(examinedElements)
        .append(" Elementen anwendbar");

    if (!specification.unsupported().applicability().isEmpty()) {
      return summary
          .append(" - nicht prüfbar: die Anwendbarkeit nutzt Teile von IDS,")
          .append(" die CheckMate nicht auswertet")
          .toString();
    }
    if (notApplicable()) {
      return summary.append(" - nicht prüfbar").toString();
    }

    for (Status status : Status.values()) {
      long count = count(status);
      if (count > 0) {
        summary.append(", ").append(count).append(" ").append(status.label());
      }
    }
    if (!complete()) {
      summary
          .append(", ")
          .append(specification.unsupported().requirements().size())
          .append(" Anforderung(en) nicht ausgewertet");
    }
    return summary.toString();
  }

  /**
   * What the reader has to know besides the findings: the hint about related data types, and the
   * parts of the specification that were not evaluated.
   */
  public List<String> notes() {
    List<String> notes = new ArrayList<>();
    if (!relatedTypes.isEmpty()) {
      notes.add(relatedTypes.line());
    }
    specification
        .unsupported()
        .applicability()
        .forEach(part -> notes.add("Nicht ausgewertet (Anwendbarkeit): " + part));
    specification
        .unsupported()
        .requirements()
        .forEach(part -> notes.add("Nicht ausgewertet (Anforderungen): " + part));
    return notes;
  }

  /** The whole report: the summary and the notes, then either the findings or the diagnosis. */
  public List<String> report() {
    List<String> lines = new ArrayList<>();
    lines.add(summary());
    notes().forEach(note -> lines.add("   " + note));

    if (notApplicable()) {
      lines.addAll(diagnosis.lines());
    } else {
      findings.forEach(finding -> lines.add("   " + finding.line()));
    }
    return lines;
  }
}
