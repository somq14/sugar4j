package jp.ac.meiji.igusso.coptool.model;

import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
public final class LinearConstraint implements Constraint {
  String name;
  List<Integer> coeffs;
  List<Variable> variables;
  Comparator op;
  int rhs;
  int weight;

  private LinearConstraint(String name, List<Integer> coeffs, List<Variable> variables,
      Comparator op, int rhs, int weight) {
    this.name = name;
    this.weight = weight;
    this.coeffs = Collections.unmodifiableList(new ArrayList<>(coeffs));
    this.variables = Collections.unmodifiableList(new ArrayList<>(variables));
    this.op = op;
    this.rhs = rhs;
  }

  public int size() {
    return variables.size();
  }

  public static Builder of(String name, Comparator op, int rhs) {
    return new Builder(name, op, rhs, -1);
  }

  public static Builder of(String name, Comparator op, int rhs, int weight) {
    return new Builder(name, op, rhs, weight);
  }

  public static class Builder {
    private String name;
    private final List<Integer> coeffs = new ArrayList<Integer>();
    private final List<Variable> variables = new ArrayList<Variable>();
    private Comparator op;
    private int rhs;
    private int weight;

    private Builder(@NonNull String name, @NonNull Comparator op, int rhs, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException("Invalid Constraint Name: " + name);
      }
      weight = Math.max(-1, weight);

      this.name = name;
      this.op = op;
      this.rhs = rhs;
      this.weight = weight;
    }

    public Builder addTerm(int coeff, @NonNull Variable var) {
      coeffs.add(coeff);
      variables.add(var);
      return this;
    }

    public LinearConstraint build() {
      return new LinearConstraint(name, coeffs, variables, op, rhs, weight);
    }
  }
}
