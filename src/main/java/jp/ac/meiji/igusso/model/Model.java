package jp.ac.meiji.igusso.model;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@ToString
@EqualsAndHashCode
public final class Model {
  private List<Variable> variables = new ArrayList<>();
  private Map<String, Variable> variableMap = new TreeMap<>();

  private List<Constraint> constraints = new ArrayList<>();
  private Map<String, Constraint> constraintMap = new TreeMap<>();

  public Model() {}

  public void addVariable(@NonNull Variable variable) {
    if (variableMap.containsKey(variable.getName())) {
      throw new IllegalArgumentException(
          "The Variable Already Has Been Registered: " + variable.getName());
    }

    variables.add(variable);
    variableMap.put(variable.getName(), variable);
  }

  public void addVariables(@NonNull Collection<Variable> variables) {
    for (Variable variable : variables) {
      addVariable(variable);
    }
  }

  public void addConstraint(@NonNull Constraint constraint) {
    if (constraintMap.containsKey(constraint.getName())) {
      throw new IllegalArgumentException(
          "The Constraint Already Has Been Registered: " + constraint.getName());
    }

    constraints.add(constraint);
    constraintMap.put(constraint.getName(), constraint);
  }

  public void addConstraints(@NonNull Collection<Constraint> constraints) {
    for (Constraint constraint : constraints) {
      addConstraint(constraint);
    }
  }

  public List<Variable> getVariables() {
    return Collections.unmodifiableList(variables);
  }

  public List<Constraint> getConstraints() {
    return Collections.unmodifiableList(constraints);
  }
}
