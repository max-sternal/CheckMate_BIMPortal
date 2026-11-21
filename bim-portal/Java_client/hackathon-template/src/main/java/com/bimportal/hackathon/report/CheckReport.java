package com.bimportal.hackathon.report;

import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.CheckResult.Verdict;
import com.bimportal.hackathon.ids.IdsDocument;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * One run of CheckMate: a model, checked at a point in time against one or more sets of
 * requirements - LOINs, AIA projects, IDS files.
 *
 * <p>The front ends present it differently - the GUI as a table and a text, the CLI as text and
 * JSON - but they all report from this, so the verdict and the exit code mean the same everywhere.
 *
 * @param runs one per set of requirements, in the order they were checked
 */
public record CheckReport(OffsetDateTime checkedAt, ModelInfo model, List<Run> runs) {

  /** Exit codes of the CLI. 2 (unknown argument) is decided before there is a report. */
  public static final int PASSED = 0;

  public static final int NOT_READ = 1;
  public static final int NOT_MET = 3;
  public static final int NOT_CONFIRMED = 4;

  private static final DateTimeFormatter HEADER_TIME =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  public CheckReport {
    runs = List.copyOf(runs);
  }

  /** What was checked: the model's name, schema and number of elements. */
  public record ModelInfo(String name, String schema, int elements) {

    /** {@code Infra-Bridge.ifc - Schema IFC4, 77 Objekte} */
    public String summary() {
      return name + " - Schema " + schema + ", " + elements + " Objekte";
    }
  }

  /**
   * One set of requirements: checked, or why it could not be.
   *
   * @param name what it is called, for reports: its label once read, or what was asked for
   * @param requirements null when it could not be read
   * @param results one per specification for the model's schema; empty when it could not be
   *     checked
   * @param error why it could not be read or checked; null when it was
   */
  public record Run(
      String name, IdsDocument requirements, List<CheckResult> results, String error) {

    public Run {
      results = List.copyOf(results);
    }

    public static Run checked(IdsDocument requirements, List<CheckResult> results) {
      return new Run(requirements.describe(), requirements, results, null);
    }

    public static Run failed(String name, String error) {
      return new Run(name, null, List.of(), error);
    }

    public boolean failed() {
      return error != null;
    }
  }

  /** Every specification checked, across all runs. */
  public List<CheckResult> results() {
    return runs.stream().flatMap(run -> run.results().stream()).toList();
  }

  /**
   * What the run came to, as an exit code.
   *
   * <ul>
   *   <li>0: every specification checked was met;
   *   <li>1: the model or some of the requirements could not be read or checked - the report is
   *       incomplete, whatever the rest says;
   *   <li>3: at least one specification was not met;
   *   <li>4: none was violated, but not everything was confirmed either: a specification applied to
   *       no element ("nicht prüfbar"), was evaluated only in part, or nothing was checked at all.
   * </ul>
   */
  public int exitCode() {
    if (runs.stream().anyMatch(Run::failed)) {
      return NOT_READ;
    }
    List<CheckResult> results = results();
    if (results.stream().anyMatch(result -> result.verdict() == Verdict.FAILED)) {
      return NOT_MET;
    }
    if (results.isEmpty() || results.stream().anyMatch(r -> r.verdict() != Verdict.PASSED)) {
      return NOT_CONFIRMED;
    }
    return PASSED;
  }

  /** The verdict in words, e.g. {@code 2 Spezifikation(en) für IFC2X3: 2 nicht prüfbar}. */
  public String verdict() {
    List<CheckResult> results = results();
    long unread = runs.stream().filter(Run::failed).count();
    String checked =
        results.isEmpty() ? "nichts geprüft" : CheckResult.tally(results, model.schema());
    return unread == 0 ? checked : checked + "; " + unread + " nicht lesbar";
  }

  /** The first lines of every report: when, and against what. */
  public List<String> header() {
    return List.of(
        "CheckMate-Prüfung vom " + checkedAt.format(HEADER_TIME),
        "Modell: " + model.summary());
  }
}
