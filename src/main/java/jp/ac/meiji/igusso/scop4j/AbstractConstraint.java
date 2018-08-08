package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public abstract class AbstractConstraint implements Constraint {
  private final String name;
  private final int weight;

  protected AbstractConstraint(@NonNull String name, int weight) {
    if (!NAME_PATTERN.matcher("name").matches()) {
      throw new IllegalArgumentException();
    }
    this.name = name;
    this.weight = Math.max(weight, -1);
  }

  @Override
  public String getName() {
    return name;
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
  public int getWeight() {
    return weight;
  }

  @Override
  public int compareTo(Constraint constraint) {
    return name.compareTo(constraint.getName());
  }
}
