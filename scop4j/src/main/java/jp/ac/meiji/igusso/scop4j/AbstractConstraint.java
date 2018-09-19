package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
abstract class AbstractConstraint implements Constraint {
  @Getter private final String name;
  @Getter private final int weight;

  protected AbstractConstraint(@NonNull String name, int weight) {
    if (!NAME_PATTERN.matcher("name").matches()) {
      throw new IllegalArgumentException();
    }
    this.name = name;
    this.weight = Math.max(weight, -1);
  }

  @Override
  public boolean isHard() {
    return weight < 0;
  }

  @Override
  public boolean isSoft() {
    return weight >= 0;
  }

  @Override
  public int compareTo(Constraint constraint) {
    return name.compareTo(constraint.getName());
  }
}
