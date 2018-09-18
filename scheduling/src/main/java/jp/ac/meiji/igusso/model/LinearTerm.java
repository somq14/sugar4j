package jp.ac.meiji.igusso.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class LinearTerm {
  @Getter private final int coeff;
  @Getter private final Variable variable;

  private LinearTerm(int coeff, @NonNull Variable variable) {
    this.coeff = coeff;
    this.variable = variable;
  }

  public static LinearTerm of(int coeff, @NonNull Variable variable) {
    return new LinearTerm(coeff, variable);
  }
}
