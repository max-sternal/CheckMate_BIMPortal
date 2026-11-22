package com.bimportal.hackathon.ifc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed IFC STEP file: its declared schema, resolved schema metadata, and the kept entity
 * instances in file encounter order.
 *
 * @param declaredSchema the schema declared in {@code FILE_SCHEMA}, e.g. {@code IFC4X3_ADD2}
 * @param schema the loaded schema metadata matching {@code declaredSchema}
 * @param entities kept entity instances in the order they appeared in the file
 * @param byId index from STEP instance id to entity
 */
public record StepModel(
    String declaredSchema,
    IfcSchema schema,
    List<StepEntity> entities,
    Map<Long, StepEntity> byId) {

  public StepModel {
    entities = List.copyOf(entities);
    byId = Collections.unmodifiableMap(new HashMap<>(byId));
  }

  /** Look up an entity by its STEP instance number, or null when not present. */
  public StepEntity byId(long id) {
    return byId.get(id);
  }
}
