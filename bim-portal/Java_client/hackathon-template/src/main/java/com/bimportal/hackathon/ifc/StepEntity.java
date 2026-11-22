package com.bimportal.hackathon.ifc;

import java.util.List;

/**
 * One entity instance from a STEP {@code DATA} section, e.g. {@code #1=IFCSLAB(...);}.
 *
 * @param id the STEP instance number without {@code #}
 * @param typeUpper the entity type name in upper case, e.g. {@code IFCSLAB}
 * @param args positional attributes in declaration order
 */
public record StepEntity(long id, String typeUpper, List<StepValue> args) {

  public StepEntity {
    args = List.copyOf(args);
  }

  /** Attribute at {@code index}, or {@link StepValue.Null#INSTANCE} when out of range. */
  public StepValue arg(int index) {
    return index >= 0 && index < args.size() ? args.get(index) : StepValue.Null.INSTANCE;
  }

  /** Decoded string at {@code index}, or null when absent or not a string. */
  public String text(int index) {
    return arg(index) instanceof StepValue.Text text ? text.value() : null;
  }

  /** Enumeration name at {@code index}, or null when absent or not an enum literal. */
  public String enumName(int index) {
    return arg(index) instanceof StepValue.EnumVal enumeration ? enumeration.name() : null;
  }

  /** Referenced entity id at {@code index}, or null when absent or not a reference. */
  public Long ref(int index) {
    return arg(index) instanceof StepValue.EntityRef ref ? ref.id() : null;
  }

  /** List elements at {@code index}, or an empty list when absent or not a list. */
  public List<StepValue> list(int index) {
    return arg(index) instanceof StepValue.ListVal list ? list.items() : List.of();
  }
}
