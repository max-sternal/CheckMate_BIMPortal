package com.bimportal.hackathon.ids;

import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks a model against a LOIN, using the IDS the BIM Portal exports for it.
 *
 * <p>The portal is the source of the "Soll": rather than reassembling a LOIN's requirements from
 * the property and property-group endpoints, this asks for the LOIN as IDS - the buildingSMART
 * format meant for exactly this - and applies it. That keeps the semantics the portal's rather than
 * ours, and leaves the door open to running the same file through an external IDS validator.
 */
public final class LoinIdsCheck {

  private static final Logger logger = LoggerFactory.getLogger(LoinIdsCheck.class);

  private LoinIdsCheck() {}

  /**
   * Fetch a LOIN's IDS, pick the specification for the model's schema and check the model.
   *
   * @param schema the model's IFC schema, e.g. {@code IFC2X3}
   * @param model the elements to check, as read from the IFC file
   * @throws IllegalStateException when the portal has no IDS for this LOIN, or none for the model's
   *     schema - both would otherwise surface as an empty report that reads like a pass
   */
  public static CheckResult check(
      EnhancedBimPortalClient client, UUID loinGuid, String schema, List<ModelElement> model)
      throws Exception {

    byte[] ids =
        client
            .exportLoinIds(loinGuid)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Das BIM Portal liefert für diese LOIN keinen IDS-Export (" + loinGuid + ")."));

    List<IdsSpecification> specifications = IdsParser.parse(ids);
    if (specifications.isEmpty()) {
      throw new IllegalStateException("Der IDS-Export enthält keine Spezifikation.");
    }
    logger.debug(
        "LOIN {}: {} IDS-Spezifikationen, Modellschema {}", loinGuid, specifications.size(), schema);

    Optional<IdsSpecification> specification = IdsParser.forSchema(specifications, schema);
    if (specification.isEmpty()) {
      throw new IllegalStateException(
          "Der IDS-Export deckt das Schema "
              + schema
              + " nicht ab - vorhanden: "
              + specifications.stream().map(IdsSpecification::ifcVersion).toList());
    }

    return IdsValidator.check(specification.get(), model);
  }
}
