package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * quadratic制約を構成する項を表すクラス.
 */
@ToString
@EqualsAndHashCode
public final class QuadraticTerm {
  @Getter private final int coeff;
  @Getter private final Variable variable1;
  @Getter private final String value1;
  @Getter private final Variable variable2;
  @Getter private final String value2;

  private QuadraticTerm(int coeff, @NonNull Variable variable1, @NonNull String value1,
      @NonNull Variable variable2, @NonNull String value2) {
    if (!variable1.getDomain().contains(value1)) {
      throw new IllegalArgumentException(String.format(
          "domain of variable %s does not contain value %s", variable1.getName(), value1));
    }
    if (!variable2.getDomain().contains(value2)) {
      throw new IllegalArgumentException(String.format(
          "domain of variable %s does not contain value %s", variable2.getName(), value2));
    }

    this.coeff = coeff;
    this.variable1 = variable1;
    this.value1 = value1;
    this.variable2 = variable2;
    this.value2 = value2;
  }

  public static QuadraticTerm of(
      int coeff, Variable variable1, String value1, Variable variable2, String value2) {
    return new QuadraticTerm(coeff, variable1, value1, variable2, value2);
  }

  public static QuadraticTerm of(
      int coeff, Variable variable1, Integer value1, Variable variable2, Integer value2) {
    return new QuadraticTerm(
        coeff, variable1, String.valueOf(value1), variable2, String.valueOf(value2));
  }
}
