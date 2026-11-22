package com.bimportal.hackathon.report;

import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.report.CheckReport.Run;
import java.util.ArrayList;
import java.util.List;

/**
 * A report as text, the same for every front end: in full (the GUI, the CLI with {@code --full}),
 * or brief, with a few examples per specification (the CLI).
 */
public final class ReportText {

  private ReportText() {}

  /**
   * Everything: header, an overview line per specification when there are several, then each set of
   * requirements with every finding.
   */
  public static List<String> full(CheckReport report) {
    List<String> lines = new ArrayList<>(report.header());
    List<CheckResult> results = report.results();
    if (report.runs().size() > 1 && !results.isEmpty()) {
      lines.add(report.runs().size() + " Einträge geprüft - " + report.verdict());
      results.forEach(result -> lines.add("   " + result.overviewLine()));
    }
    for (Run run : report.runs()) {
      lines.add("");
      lines.addAll(run(run, report.model().schema()));
    }
    return lines;
  }

  /** One set of requirements with every finding. */
  public static List<String> run(Run run, String schema) {
    if (run.failed()) {
      return List.of(run.error());
    }
    List<String> lines = new ArrayList<>();
    lines.add("Anforderungen: " + run.name());
    List<CheckResult> results = run.results();
    if (results.size() > 1) {
      lines.add(CheckResult.tally(results, schema));
      results.forEach(result -> lines.add("   " + result.overviewLine()));
    }
    for (CheckResult result : results) {
      if (results.size() > 1) {
        lines.add("");
      }
      lines.addAll(result.report());
    }
    return lines;
  }

  /** One specification of a report, with the header and what it belongs to. */
  public static List<String> single(CheckReport report, String requirements, CheckResult result) {
    List<String> lines = new ArrayList<>(report.header());
    lines.add("Anforderungen: " + requirements);
    lines.addAll(result.report());
    return lines;
  }

  /**
   * One set of requirements, briefly: per specification the summary, the notes, and the first
   * findings not met, or the diagnosis when nothing applied.
   *
   * @param examples how many findings to show per specification
   */
  public static List<String> brief(Run run, String schema, int examples) {
    List<String> lines = new ArrayList<>();
    if (run.failed()) {
      lines.add(run.error());
      lines.add("");
      return lines;
    }
    lines.add("Anforderungen: " + run.name());
    if (run.results().size() > 1) {
      lines.add(CheckResult.tally(run.results(), schema));
      lines.add("");
    }
    for (CheckResult result : run.results()) {
      lines.add(result.summary());
      result.notes().forEach(note -> lines.add("   " + note));
      if (result.notApplicable()) {
        lines.addAll(result.diagnosis().lines());
      } else {
        result.findings().stream()
            .filter(finding -> !finding.status().ok())
            .limit(examples)
            .forEach(finding -> lines.add("   " + finding.line()));
        switch (result.verdict()) {
          case PASSED -> lines.add("   alle Anforderungen erfüllt");
          case PARTLY_EVALUATED -> lines.add("   alle ausgewerteten Anforderungen erfüllt");
          default -> {}
        }
      }
      lines.add("");
    }
    return lines;
  }

  /** The last line, e.g. {@code Ergebnis: ... 2 nicht erfüllt (Exit-Code 3)}. */
  public static String verdictLine(CheckReport report) {
    return "Ergebnis: " + report.verdict() + " (Exit-Code " + report.exitCode() + ")";
  }
}
