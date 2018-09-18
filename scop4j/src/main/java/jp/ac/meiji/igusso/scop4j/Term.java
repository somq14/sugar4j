package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class Term {
  @Getter private final int coeff;
  @Getter private final Variable variable;
  @Getter private final String value;

  private Term(int coeff, @NonNull Variable variable, @NonNull String value) {
    if (!variable.getDomain().contains(value)) {
      throw new IllegalArgumentException();
    }
    this.coeff = coeff;
    this.variable = variable;
    this.value = value;
  }

  public static Term of(int coeff, @NonNull Variable variable, @NonNull String value) {
    return new Term(coeff, variable, value);
  }

  public static Term of(int coeff, @NonNull Variable variable, @NonNull Integer value) {
    return of(coeff, variable, value.toString());
  }
}
