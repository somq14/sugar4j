package jp.ac.meiji.igusso.coptool.model;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
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

  public Variable addVariable(@NonNull String name, int domainMin, int domainMax) {
    if (domainMin > domainMax) {
      throw new IllegalArgumentException(
          "Invalid Domain Range: [" + domainMin + ", " + domainMax + ")");
    }

    List<Integer> domain = new ArrayList<>(domainMax - domainMin + 1);
    for (int i = domainMin; i <= domainMax; i++) {
      domain.add(i);
    }
    return addVariable(name, domain);
  }

  public Variable addVariable(@NonNull String name, int domainSize) {
    return addVariable(name, 0, domainSize - 1);
  }

  public void addConstraint(@NonNull Constraint constraint) {
    for (Constraint cons : constraints) {
      if (cons.getName().equals(constraint.getName())) {
        throw new IllegalArgumentException(
            "The Constraint Already Has Been Registered: " + constraint.getName());
      }
    }

    constraints.add(constraint);
  }

  public void addConstraints(@NonNull Collection<Constraint> constraints) {
    for (Constraint cons : constraints) {
      addConstraint(cons);
    }
  }

  public List<Variable> getVariables() {
    return Collections.unmodifiableList(variables);
  }

  public List<Constraint> getConstraints() {
    return Collections.unmodifiableList(constraints);
  }
}
