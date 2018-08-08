package jp.ac.meiji.igusso.coptool.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public abstract class AbstractConstraint implements Constraint {
  @Getter private String name;
  @Getter private int weight;

  protected AbstractConstraint(@NonNull String name, int weight) {
    if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("Invalid Constraint Name: " + name);
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
