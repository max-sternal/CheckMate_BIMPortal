package com.bimportal.hackathon.ids;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of checking one IDS specification against a model.
 *
 * @param specification what was checked against
 * @param examinedElements how many model elements were looked at
 * @param applicableElements how many of them the specification applies to
 * @param findings one finding per applicable element and requirement
 * @param diagnosis why nothing was selected, when nothing was; {@link Diagnosis#NONE} otherwise
 */
public record CheckResult(
    IdsSpecification specification,
    int examinedElements,
    int applicableElements,
    List<Finding> findings,
    Diagnosis diagnosis) {

  /** What a single requirement came to on a single element. */
  public enum Status {
    FULFILLED("erfüllt"),
    MISSING("Merkmal fehlt"),
    WRONG_VALUE("Wert weicht ab"),
    WRONG_DATATYPE("Datentyp weicht ab"),
    WRONG_ENTITY("IFC-Entity nicht zulässig");

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
   * @param matchedElements elements carrying the facet's address with the required value
   * @param addressFound elements carrying that address at all, whatever value it holds
   */
  public record FacetMatch(String facet, int matchedElements, int addressFound) {

    public String line() {
      if (matchedElements > 0) {
        return facet + " trifft " + matchedElements + " Element(e)";
      }
      return addressFound == 0
          ? facet + " trifft 0 - Merkmal im Modell nicht vorhanden"
          : facet + " trifft 0 - " + addressFound + " Element(e) mit abweichendem Wert";
    }
  }

  /**
   * An element that satisfied part of the applicability, and what kept it out.
   *
   * @param matchedFacets how many of {@code totalFacets} held for this element
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
          + ") - "
          + matchedFacets
          + " von "
          + totalFacets
          + " Facetten";
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

  public long count(Status status) {
    return findings.stream().filter(finding -> finding.status() == status).count();
  }

  /** True when the specification applied to something and every requirement was met. */
  public boolean passed() {
    return !notApplicable() && findings.stream().allMatch(finding -> finding.status().ok());
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

    if (notApplicable()) {
      return summary.append(" - nicht prüfbar").toString();
    }

    for (Status status : Status.values()) {
      long count = count(status);
      if (count > 0) {
        summary.append(", ").append(count).append(" ").append(status.label());
      }
    }
    return summary.toString();
  }

  /** The whole report: the summary, then either the findings or the diagnosis. */
  public List<String> report() {
    List<String> lines = new ArrayList<>();
    lines.add(summary());

    if (notApplicable()) {
      lines.addAll(diagnosis.lines());
    } else {
      findings.forEach(finding -> lines.add("   " + finding.line()));
    }
    return lines;
  }
}
