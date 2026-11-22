package com.bimportal.hackathon;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * The downloaded sample IFC models: which there are, where they live, and which are missing.
 *
 * <p>The list is {@code src/test/resources/ifc/samples.properties}, shared with the Gradle task
 * that downloads them, so {@link PrerequisitesTest} and the IFC reading tests share one list.
 */
public final class TestModelFiles {

  /** How to fetch the missing models. */
  public static final String DOWNLOAD = "./gradlew :hackathon-template:downloadTestModels";

  private TestModelFiles() {}

  /** Where the models are downloaded to; the Gradle test task passes it in. */
  public static Path directory() {
    return Path.of(System.getProperty("checkmate.testModels", "test-models")).toAbsolutePath();
  }

  /** Every listed model: path under {@link #directory()} to download URL. */
  public static Map<String, String> listed() {
    Properties list = new Properties();
    try (InputStream in = TestModelFiles.class.getResourceAsStream("/ifc/samples.properties")) {
      if (in == null) {
        throw new IOException("/ifc/samples.properties is missing from the test classpath");
      }
      list.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Map<String, String> models = new TreeMap<>();
    list.forEach((name, url) -> models.put(name.toString(), url.toString()));
    return models;
  }

  /** The listed models not downloaded yet. */
  public static List<String> missing() {
    return listed().keySet().stream()
        .filter(name -> !Files.isRegularFile(directory().resolve(name)))
        .toList();
  }

  /**
   * A listed model's file, or the calling test skipped with a message saying how to get it.
   *
   * @param name a key of {@code samples.properties}, e.g. {@code ifc4/Infra-Bridge.ifc}
   */
  public static Path require(String name) {
    String url = listed().get(name);
    if (url == null) {
      throw new IllegalArgumentException(name + " is not listed in /ifc/samples.properties");
    }
    Path file = directory().resolve(name);
    assumeTrue(
        Files.isRegularFile(file),
        () ->
            "Test model "
                + name
                + " is missing in "
                + directory()
                + " - run "
                + DOWNLOAD
                + " to fetch it from "
                + url);
    return file;
  }
}
