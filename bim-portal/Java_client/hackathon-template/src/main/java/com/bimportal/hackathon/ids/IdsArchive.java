package com.bimportal.hackathon.ids;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * IDS files in a ZIP archive, read as one document: the BIM Portal's export of an AIA project, or
 * any ZIP of IDS files.
 *
 * <p>The portal's project export holds {@code IDS/AIA_<project>_<guid>_<version>.ids}, one file
 * with every LOIN of the project, and for some projects also {@code IDS/LOIN_<name>_<guid>_...ids},
 * one per LOIN. They overlap, but not completely: in the road projects the combined file holds the
 * IFC4 specifications only, while each LOIN's own file holds IFC2X3, IFC4 and IFC4X3_ADD2 (measured
 * 2026-11-21). So all files are read, the combined one first, and a specification is skipped when
 * one for the same LOIN and schema was already taken. LOINs are told apart by the GUID in the
 * specification's identifier, not by name: one project holds two different LOINs both named
 * "Träger - Unterzug - Oberzug".
 */
final class IdsArchive {

  /** Bytes a ZIP archive starts with. */
  private static final byte[] ZIP_MAGIC = {'P', 'K', 3, 4};

  /** Limits against archives that unpack to far more than they weigh. */
  private static final long MAX_ENTRY_BYTES = 50L * 1024 * 1024;

  private static final long MAX_TOTAL_BYTES = 200L * 1024 * 1024;

  private static final int MAX_ENTRIES = 2000;

  private IdsArchive() {}

  /** Whether bytes start like a ZIP archive. */
  static boolean isZip(byte[] head) {
    if (head.length < ZIP_MAGIC.length) {
      return false;
    }
    for (int i = 0; i < ZIP_MAGIC.length; i++) {
      if (head[i] != ZIP_MAGIC[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Read every {@code .ids} file in an archive into one document.
   *
   * <p>The title and version are the combined file's, or the only file's; with several files and
   * no combined one there is none, and the document goes by its source.
   *
   * @throws RequirementsException when the archive cannot be read, holds no IDS file, or one of
   *     them is not IDS
   */
  static IdsDocument read(InputStream archive, String source) {
    List<IdsDocument> combined = new ArrayList<>();
    List<IdsDocument> others = new ArrayList<>();
    try (ZipInputStream zip = new ZipInputStream(archive)) {
      long total = 0;
      int entries = 0;
      for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
        if (++entries > MAX_ENTRIES) {
          throw new RequirementsException(
              source + " enthält mehr als " + MAX_ENTRIES + " Dateien.");
        }
        String name = fileName(entry.getName());
        if (entry.isDirectory() || !name.toLowerCase(Locale.ROOT).endsWith(".ids")) {
          continue;
        }
        byte[] ids = readEntry(zip, source, entry.getName());
        total += ids.length;
        if (total > MAX_TOTAL_BYTES) {
          throw new RequirementsException(
              source + " entpackt zu mehr als " + MAX_TOTAL_BYTES / (1024 * 1024) + " MB.");
        }
        IdsDocument document = IdsParser.read(ids, source + ": " + entry.getName());
        (name.startsWith("AIA_") ? combined : others).add(document);
      }
    } catch (ZipException e) {
      throw new RequirementsException(
          source + " ist kein lesbares ZIP-Archiv: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new RequirementsException(source + " lässt sich nicht lesen: " + e.getMessage(), e);
    }

    List<IdsDocument> all = new ArrayList<>(combined);
    all.addAll(others);
    if (all.isEmpty()) {
      throw new RequirementsException(source + " enthält keine IDS-Datei.");
    }

    IdsDocument named = !combined.isEmpty() ? combined.get(0) : all.size() == 1 ? all.get(0) : null;
    return new IdsDocument(
        named == null ? null : named.title(),
        named == null ? null : named.version(),
        source,
        withoutRepeats(all));
  }

  /**
   * The specifications of all documents in order, each LOIN and schema once. Specifications
   * without a LOIN GUID in their identifier are all kept: there is nothing to tell repeats by.
   */
  private static List<IdsSpecification> withoutRepeats(List<IdsDocument> documents) {
    List<IdsSpecification> specifications = new ArrayList<>();
    Set<String> taken = new HashSet<>();
    for (IdsDocument document : documents) {
      for (IdsSpecification specification : document.specifications()) {
        String schema = specification.ifcVersion().trim().toUpperCase(Locale.ROOT);
        String key = specification.loinGuid().map(guid -> guid + " " + schema).orElse(null);
        if (key == null || taken.add(key)) {
          specifications.add(specification);
        }
      }
    }
    return specifications;
  }

  private static byte[] readEntry(ZipInputStream zip, String source, String entry)
      throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    byte[] buffer = new byte[64 * 1024];
    long size = 0;
    for (int read; (read = zip.read(buffer)) != -1; ) {
      size += read;
      if (size > MAX_ENTRY_BYTES) {
        throw new RequirementsException(
            source + ": " + entry + " ist größer als " + MAX_ENTRY_BYTES / (1024 * 1024) + " MB.");
      }
      bytes.write(buffer, 0, read);
    }
    return bytes.toByteArray();
  }

  /** The last segment of an entry's path: {@code IDS/AIA_x.ids} gives {@code AIA_x.ids}. */
  private static String fileName(String entryName) {
    int slash = Math.max(entryName.lastIndexOf('/'), entryName.lastIndexOf('\\'));
    return entryName.substring(slash + 1);
  }
}
