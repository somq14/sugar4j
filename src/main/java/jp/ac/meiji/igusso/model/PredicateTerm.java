package jp.ac.meiji.igusso.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class PredicateTerm {
  @Getter private final Variable variable;
  @Getter private final int value;
  @Getter private final boolean positive;

  private PredicateTerm(@NonNull Variable variable, int value, boolean positive) {
    if (!variable.getDomain().contains(value)) {
      throw new IllegalArgumentException(
          "The Variable's Domain Does Not Contain The Value: " + variable + " " + value);
    }

    this.variable = variable;
    this.value = value;
    this.positive = positive;
  }

  public PredicateTerm not() {
    return new PredicateTerm(variable, value, !positive);
  }

  public static PredicateTerm of(@NonNull Variable variable, int value, boolean positive) {
    return new PredicateTerm(variable, value, positive);
  }

  public static PredicateTerm of(@NonNull Variable variable, int value) {
    return of(variable, value, true);
  }
}
