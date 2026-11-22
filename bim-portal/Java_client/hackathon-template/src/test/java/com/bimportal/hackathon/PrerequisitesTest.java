package com.bimportal.hackathon;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What it takes to build and test all of CheckMate, checked up front.
 *
 * <p>A missing prerequisite skips its test rather than failing it, and the skip message says what
 * to add and where. Gradle lists skipped tests; the messages are in {@code
 * build/reports/tests/test/index.html}. Everything here passes on a machine that has it all.
 */
class PrerequisitesTest {

  @Test
  void javaFxIsAvailableForTheGui() {
    assumeTrue(
        onClasspath("javafx.application.Application"),
        "JavaFX missing: the GUI needs a JDK that bundles it, such as Zulu 21 JDK FX. The IFC"
            + " reader, CLI, MCP server and tests run without it.");
  }

  @Test
  void sampleModelsAreDownloaded() {
    List<String> missing = TestModelFiles.missing();
    assumeTrue(
        missing.isEmpty(),
        () ->
            missing.size()
                + " test model(s) missing in "
                + TestModelFiles.directory()
                + ": "
                + missing
                + " - run "
                + TestModelFiles.DOWNLOAD
                + " (about 5 MB). The IFC reading tests that need them are skipped.");
  }

  private static boolean onClasspath(String className) {
    try {
      Class.forName(className, false, PrerequisitesTest.class.getClassLoader());
      return true;
    } catch (ClassNotFoundException | LinkageError missing) {
      return false;
    }
  }
}
