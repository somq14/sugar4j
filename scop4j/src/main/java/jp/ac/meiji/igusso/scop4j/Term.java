package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * liner制約を構成する項を表すクラス.
 */
@ToString
@EqualsAndHashCode
public final class Term {
  @Getter private final int coeff;
  @Getter private final Variable variable;
  @Getter private final String value;

  private Term(int coeff, @NonNull Variable variable, @NonNull String value) {
    if (!variable.getDomain().contains(value)) {
      throw new IllegalArgumentException(String.format(
          "domain of variable %s does not contain value %s", variable.getName(), value));
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
