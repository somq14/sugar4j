package jp.ac.meiji.igusso.coptool.model;

import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
public final class ConflictPointConstraint implements Constraint {
  String name;
  List<Boolean> phases;
  List<Variable> variables;
  List<Integer> values;
  int weight;

  private ConflictPointConstraint(String name, List<Boolean> phases, List<Variable> variables,
      List<Integer> values, int weight) {
    this.name = name;
    this.phases = Collections.unmodifiableList(new ArrayList<>(phases));
    this.variables = Collections.unmodifiableList(new ArrayList<>(variables));
    this.values = Collections.unmodifiableList(new ArrayList<>(values));
    this.weight = weight;
  }

  public int size() {
    return variables.size();
  }

  public static Builder of(String name) {
    return new Builder(name, -1);
  }

  public static Builder of(String name, int weight) {
    return new Builder(name, weight);
  }

  public static class Builder {
    private String name;
    private final List<Boolean> phases = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();
    private final List<Integer> values = new ArrayList<>();
    private int weight;

    private Builder(@NonNull String name, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException("Invalid Constraint Name: " + name);
      }
      weight = Math.max(-1, weight);

      this.name = name;
      this.weight = weight;
    }

    public Builder addTerm(@NonNull Variable var, int val, boolean phase) {
      if (!var.getDomain().contains(val)) {
        throw new IllegalArgumentException(
            "The Variable's Domain Does Not Contain The Value: " + var + " " + val);
      }

      phases.add(phase);
      variables.add(var);
      values.add(val);
      return this;
    }

    public Builder addTerm(@NonNull Variable var, int val) {
      return addTerm(var, val, true);
    }

    public ConflictPointConstraint build() {
      return new ConflictPointConstraint(name, phases, variables, values, weight);
    }
  }
}

