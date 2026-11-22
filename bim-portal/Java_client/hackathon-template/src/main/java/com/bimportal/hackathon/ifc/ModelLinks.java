package com.bimportal.hackathon.ifc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Models behind links: where to download them from, and what a download turned out to be.
 *
 * <p>A GitHub link to a file is the page showing it, not the file, so it is turned into the raw
 * file's address. A file kept in Git LFS comes back as a small pointer; GitHub serves the real file
 * under another host. And whatever arrives is recognised by its first bytes before the STEP parser
 * sees it, so a web page or an error page is reported as such instead of as "kein lesbares IFC".
 */
final class ModelLinks {

  /** What a file is, by its first bytes. */
  enum Content {
    /** An IFC file: ISO 10303-21, the STEP physical file format. */
    STEP,
    /** A ZIP archive, which is what an ifcZIP is. */
    ZIP,
    /** The stand-in Git LFS keeps in the repository instead of the file. */
    LFS_POINTER,
    /** A web page, typically a link to a page that shows the file. */
    HTML,
    UNKNOWN
  }

  /** Downloads larger than this are refused; GitHub's own limit for a file is 100 MB. */
  static final long MAX_DOWNLOAD_BYTES = 200L * 1024 * 1024;

  private static final Pattern GITHUB_FILE =
      Pattern.compile("https?://(?:www\\.)?github\\.com/([^/]+)/([^/]+)/(?:blob|raw)/(.+)");

  private static final String RAW_HOST = "raw.githubusercontent.com";

  private ModelLinks() {}

  /** Whether a model source is a link rather than a file path. */
  static boolean isLink(String source) {
    String lower = source.trim().toLowerCase(Locale.ROOT);
    return lower.startsWith("http://") || lower.startsWith("https://");
  }

  /**
   * The address to download a link from.
   *
   * <p>{@code github.com/<owner>/<repo>/blob/<ref>/<path>} (and {@code .../raw/...}) becomes {@code
   * raw.githubusercontent.com/<owner>/<repo>/<ref>/<path>}; the query and fragment such a link
   * carries ({@code ?raw=true}, {@code #L12}) are dropped. Every other link is used as given.
   * Spaces, as a copied path may contain, are escaped.
   *
   * @throws IllegalArgumentException when the text is not a usable link
   */
  static URI downloadUri(String link) {
    String text = link.trim().replace(" ", "%20");
    Matcher github = GITHUB_FILE.matcher(withoutQueryAndFragment(text));
    if (github.matches()) {
      text =
          "https://"
              + RAW_HOST
              + "/"
              + github.group(1)
              + "/"
              + github.group(2)
              + "/"
              + github.group(3);
    }
    URI uri = URI.create(text);
    if (uri.getHost() == null) {
      throw new IllegalArgumentException("no host in " + link);
    }
    return uri;
  }

  /**
   * Where GitHub serves the file behind a Git LFS pointer, for a raw file address.
   *
   * @return empty for addresses that are not on {@code raw.githubusercontent.com}
   */
  static Optional<URI> lfsMediaUri(URI raw) {
    if (!RAW_HOST.equalsIgnoreCase(raw.getHost())) {
      return Optional.empty();
    }
    return Optional.of(URI.create("https://media.githubusercontent.com/media" + raw.getRawPath()));
  }

  /** The file name at the end of a link, decoded, for display; the host when there is none. */
  static String fileName(URI uri) {
    String path = uri.getPath();
    if (path == null || path.isBlank() || path.endsWith("/")) {
      return uri.getHost();
    }
    return path.substring(path.lastIndexOf('/') + 1);
  }

  /** Recognise a file by its first bytes, ignoring a byte order mark and leading whitespace. */
  static Content sniff(Path file) throws IOException {
    byte[] head;
    try (InputStream in = Files.newInputStream(file)) {
      head = in.readNBytes(512);
    }
    if (head.length >= 4 && head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4) {
      return Content.ZIP;
    }

    String text = new String(head, StandardCharsets.ISO_8859_1);
    if (text.startsWith("ï»¿")) {
      text = text.substring(3);
    }
    text = text.stripLeading();
    String lower = text.toLowerCase(Locale.ROOT);

    if (text.startsWith("ISO-10303-21")) {
      return Content.STEP;
    }
    if (text.startsWith("version https://git-lfs.github.com/spec/")) {
      return Content.LFS_POINTER;
    }
    if (lower.startsWith("<!doctype html") || lower.contains("<html")) {
      return Content.HTML;
    }
    return Content.UNKNOWN;
  }

  /**
   * Download into a file, following redirects.
   *
   * @throws ModelLoadException when the server answers with anything but 200, or the file is larger
   *     than {@code maxBytes}
   */
  static void download(URI uri, Path target, long maxBytes)
      throws IOException, InterruptedException {
    HttpClient client =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build();
    HttpResponse<InputStream> response =
        client.send(request, HttpResponse.BodyHandlers.ofInputStream());

    try (InputStream body = response.body()) {
      if (response.statusCode() != 200) {
        throw new ModelLoadException(
            "Der Link " + uri + " liefert HTTP " + response.statusCode() + " statt der Datei.");
      }
      OptionalLong announced = response.headers().firstValueAsLong("Content-Length");
      if (announced.isPresent() && announced.getAsLong() > maxBytes) {
        throw tooLarge(uri, maxBytes);
      }
      copyAtMost(body, target, maxBytes, uri);
    }
  }

  /** Copy a stream into a file, stopping as soon as it grows past the limit. */
  private static void copyAtMost(InputStream in, Path target, long maxBytes, URI uri)
      throws IOException {
    byte[] buffer = new byte[64 * 1024];
    long total = 0;
    try (OutputStream out = Files.newOutputStream(target)) {
      int read;
      while ((read = in.read(buffer)) != -1) {
        total += read;
        if (total > maxBytes) {
          throw tooLarge(uri, maxBytes);
        }
        out.write(buffer, 0, read);
      }
    }
  }

  private static ModelLoadException tooLarge(URI uri, long maxBytes) {
    return new ModelLoadException(
        "Die Datei hinter " + uri + " ist größer als " + maxBytes / (1024 * 1024) + " MB.");
  }

  private static String withoutQueryAndFragment(String link) {
    int cut = link.length();
    int query = link.indexOf('?');
    int fragment = link.indexOf('#');
    if (query >= 0) {
      cut = Math.min(cut, query);
    }
    if (fragment >= 0) {
      cut = Math.min(cut, fragment);
    }
    return link.substring(0, cut);
  }
}
