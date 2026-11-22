package com.bimportal.hackathon.check;

import com.bimportal.hackathon.ids.IdsDocument;
import java.nio.file.Path;
import java.util.UUID;

/** Something a model can be checked against, before its requirements are read. */
public sealed interface Source {

  /** What it is called before it is read, for messages. */
  String name();

  /** A LOIN from the portal, by its GUID. */
  record Loin(UUID guid, String name) implements Source {}

  /** An AIA project from the portal, by its GUID: all its LOINs at once. */
  record AiaProject(UUID guid, String name) implements Source {}

  /** Any ID the portal hands out, a LOIN's or a project's, or a text holding one (a link). */
  record PortalId(String text) implements Source {

    @Override
    public String name() {
      return text;
    }
  }

  /** A local IDS file, or a ZIP of them. */
  record IdsFile(Path file) implements Source {

    @Override
    public String name() {
      return file.toString();
    }
  }

  /** Requirements read already. */
  record Read(IdsDocument document) implements Source {

    @Override
    public String name() {
      return document.describe();
    }
  }
}
