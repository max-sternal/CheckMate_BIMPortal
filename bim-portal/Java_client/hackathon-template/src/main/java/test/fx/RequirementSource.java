package test.fx;

import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.hackathon.catalogue.CatalogueEntry;
import com.bimportal.hackathon.catalogue.ModelFit.Fit;
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

  /**
   * A LOIN from the portal's catalogue; its IDS is downloaded when it is looked at or checked.
   *
   * @param fit how its object type fits the loaded model, shown after the name
   */
  record PortalLoin(CatalogueEntry entry, Fit fit) implements RequirementSource {

    UUID guid() {
      return entry.guid();
    }

    String name() {
      return entry.name();
    }

    @Override
    public String label() {
      return fit.fits() ? entry.name() + "  -  " + fit.describe() : entry.name();
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
