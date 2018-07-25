package jp.ac.meiji.igusso.coptool.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@ToString
@EqualsAndHashCode
public final class AllDifferentConstraint extends AbstractConstraint implements Iterable<Variable> {
  @Getter private final List<Variable> variables;

  private AllDifferentConstraint(String name, int weight, List<Variable> variables) {
    super(name, weight);
    this.variables = Collections.unmodifiableList(new ArrayList<>(variables));
  }

  public int size() {
    return variables.size();
  }

  public Variable get(int index) {
    return variables.get(index);
  }

  @Override
  public Iterator<Variable> iterator() {
    return variables.iterator();
  }

  public static Builder of(String name) {
    return new Builder(name, -1);
  }

  public static Builder of(String name, int weight) {
    return new Builder(name, weight);
  }

  @ToString
  @EqualsAndHashCode
  public static class Builder {
    @Getter private String name;
    @Getter private final List<Variable> variables = new ArrayList<Variable>();
    @Getter private int weight;

    private Builder(@NonNull String name, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException("Invalid Constraint Name: " + name);
      }

      this.name = name;
      this.weight = Math.max(-1, weight);
    }

    public Builder addVariable(@NonNull Variable var) {
      if (variables.contains(var)) {
        throw new IllegalArgumentException("Duplicated Variable: " + var);
      }
      variables.add(var);
      return this;
    }

    public AllDifferentConstraint build() {
      return new AllDifferentConstraint(name, weight, variables);
    }
  }
}
