package jp.ac.meiji.igusso.coptool;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString
@EqualsAndHashCode
public final class Model {
  private List<Variable> variables = new ArrayList<>();
  private List<Constraint> constraints = new ArrayList<>();

  public Model() {}

  public Variable addVariable(@NonNull String name, @NonNull List<Integer> domain) {
    for (Variable v : variables) {
      if (v.getName().equals(name)) {
        throw new IllegalArgumentException("The Variable Already Has Been Registered: " + name);
      }
    }

    Variable var = new VariableImpl(name, domain);
    variables.add(var);
    return var;
  }

  public void addConstraint(@NonNull Constraint constraint) {
    constraint.feasible(this);
    constraints.add(constraint);
  }

  public List<Variable> getVariables() {
    return Collections.unmodifiableList(variables);
  }

  public List<Constraint> getConstraints() {
    return Collections.unmodifiableList(constraints);
  }
}
