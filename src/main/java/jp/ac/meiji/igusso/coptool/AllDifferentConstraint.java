package jp.ac.meiji.igusso.coptool;

import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
public final class AllDifferentConstraint implements Constraint {
  String name;
  List<Variable> variables;
  int weight;

  private AllDifferentConstraint(String name, List<Variable> variables, int weight) {
    this.name = name;
    this.variables = Collections.unmodifiableList(new ArrayList<>(variables));
    this.weight = weight;
  }

  public int size() {
    return variables.size();
  }

  @Override
  public boolean feasible(Model model) {
    List<Variable> modelVariables = model.getVariables();
    for (Variable v : variables) {
      if (!modelVariables.contains(v)) {
        throw new IllegalStateException("Variable " + v + " Is Not In Model");
      }
    }
    return true;
  }

  @Override
  public List<String> encode(ModelEncoder encoder) {
    return encoder.encode(this);
  }

  public static Builder of(String name) {
    return new Builder(name, -1);
  }

  public static Builder of(String name, int weight) {
    return new Builder(name, weight);
  }

  public static class Builder {
    private String name;
    private final List<Variable> variables = new ArrayList<Variable>();
    private int weight;

    private Builder(@NonNull String name, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException("Invalid Constraint Name: " + name);
      }
      weight = Math.max(-1, weight);

      this.name = name;
      this.weight = weight;
    }

    public Builder addVariable(@NonNull Variable var) {
      if (!variables.isEmpty() && !var.getDomain().equals(variables.get(0).getDomain())) {
        throw new IllegalStateException(
            "Variables Domain Must Be Same: " + var + ", " + variables.get(0).getDomain());
      }
      variables.add(var);
      return this;
    }

    public AllDifferentConstraint build() {
      return new AllDifferentConstraint(name, variables, weight);
    }
  }
}
