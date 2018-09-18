package jp.ac.meiji.igusso.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class PseudoBooleanTerm {
  @Getter private final int coeff;
  @Getter private final Variable variable;
  @Getter private final int value;

  private PseudoBooleanTerm(int coeff, @NonNull Variable variable, int value) {
    if (!variable.getDomain().contains(value)) {
      throw new IllegalArgumentException(
          "Value's Domain Does Not Contain The Value: " + variable + ", " + value);
    }
    this.coeff = coeff;
    this.variable = variable;
    this.value = value;
  }

  public static PseudoBooleanTerm of(int coeff, @NonNull Variable variable, int value) {
    return new PseudoBooleanTerm(coeff, variable, value);
  }
}
