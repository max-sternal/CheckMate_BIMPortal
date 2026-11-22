package com.bimportal.hackathon.ifc;

import java.util.List;

/** A parsed ISO 10303-21 (STEP Part 21) attribute value. */
public sealed interface StepValue {

  /** Omitted attribute ({@code $}). */
  record Null() implements StepValue {
    public static final Null INSTANCE = new Null();
  }

  /** Derived attribute placeholder ({@code *}). */
  record Derived() implements StepValue {
    public static final Derived INSTANCE = new Derived();
  }

  /** Reference to another entity instance ({@code #123}). */
  record EntityRef(long id) implements StepValue {}

  /** Decoded STEP string literal ({@code '...'}). */
  record Text(String value) implements StepValue {}

  /** Integer literal without decimal point or exponent. */
  record IntegerVal(long value) implements StepValue {}

  /** Floating-point literal with decimal point or exponent, keeping its raw spelling. */
  record RealVal(double value, String raw) implements StepValue {}

  /** Enumeration or boolean/logical literal ({@code .FLOOR.}, {@code .T.}, {@code .U.}). */
  record EnumVal(String name) implements StepValue {}

  /** Binary literal ({@code "0F"}), keeping the raw content between double quotes. */
  record BinaryVal(String raw) implements StepValue {}

  /**
   * Explicitly typed parameter ({@code IFCLABEL('eins')}, {@code
   * IFCPROPERTYSETDEFINITIONSET((#1,#2))}).
   */
  record TypedVal(String typeUpper, StepValue value) implements StepValue {}

  /** Parenthesised list or set of values ({@code (#1,#2)}). */
  record ListVal(List<StepValue> items) implements StepValue {
    public ListVal {
      items = List.copyOf(items);
    }
  }
}
