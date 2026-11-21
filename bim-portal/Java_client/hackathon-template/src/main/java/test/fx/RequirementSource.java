package test.fx;

import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.ids.IdsDocument;
import java.util.UUID;

/**
 * One entry of the GUI's requirements list: what a model can be checked against.
 *
 * <p>Two kinds, because a search lists hundreds of LOINs and fetching each IDS up front would take
 * minutes: a LOIN or AIA project from a search is fetched only when it is checked, while a local
 * IDS file or anything named by its ID is read right away, so that a wrong file or ID is reported
 * at once.
 */
sealed interface RequirementSource {

  /** How the entry reads in the list. */
  String label();

  /** A LOIN from a portal search; its IDS is fetched when it is checked. */
  record PortalLoin(UUID guid, String name) implements RequirementSource {

    static PortalLoin of(SimpleLoinPublicDto loin) {
      return new PortalLoin(loin.getGuid(), loin.getName());
    }

    @Override
    public String label() {
      return name;
    }
  }

  /** An AIA project from a portal search: all its LOINs at once, fetched when it is checked. */
  record PortalProject(UUID guid, String name) implements RequirementSource {

    static PortalProject of(SimpleAiaProjectPublicDto project) {
      return new PortalProject(project.getGuid(), project.getName());
    }

    @Override
    public String label() {
      return "AIA-Projekt: " + name;
    }
  }

  /** Requirements already read: a local IDS file or project ZIP, or a LOIN or project by ID. */
  record Loaded(IdsDocument document) implements RequirementSource {

    @Override
    public String label() {
      return document.describe();
    }
  }
}
