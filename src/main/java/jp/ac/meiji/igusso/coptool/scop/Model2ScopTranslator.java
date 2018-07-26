package jp.ac.meiji.igusso.coptool.scop;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ToString
@EqualsAndHashCode
public final class Model2ScopTranslator {
  public static Model2ScopTranslator newInstance() {
    return new Model2ScopTranslator();
  }

  private Map<jp.ac.meiji.igusso.coptool.model.Variable, Variable> variableMap;
  private Set<String> variableNameSpace;

  private Map<jp.ac.meiji.igusso.coptool.model.Constraint, Constraint> constraintMap;
  private Set<String> constraintNameSpace;

  private Model2ScopTranslator() {
    this.variableMap = new HashMap<>();
    this.variableNameSpace = new HashSet<>();

    this.constraintMap = new HashMap<>();
    this.constraintNameSpace = new HashSet<>();
  }

  public Variable translate(jp.ac.meiji.igusso.coptool.model.Variable variable) {
    List<String> newDomain = new ArrayList<>();
    for (int value : variable.getDomain()) {
      newDomain.add(String.valueOf(value));
    }
    Variable newVariable = Variable.of(variable.getName(), newDomain);

    variableMap.put(variable, newVariable);
    variableNameSpace.add(variable.getName());
    return newVariable;
  }

  private Constraint translate(jp.ac.meiji.igusso.coptool.model.LinearConstraint constraint) {
    Comparator op = null;
    int rhs = 0;
    switch (constraint.getOp()) {
      case EQ: {
        op = Comparator.EQ;
        rhs = constraint.getRhs();
      } break;
      case LE: {
        op = Comparator.LE;
        rhs = constraint.getRhs();
      } break;
      case LT: {
        op = Comparator.LE;
        rhs = constraint.getRhs() - 1;
      } break;
      case GE: {
        op = Comparator.GE;
        rhs = constraint.getRhs();
      } break;
      case GT: {
        op = Comparator.GE;
        rhs = constraint.getRhs() + 1;
      } break;
      default:
        throw new RuntimeException();
    }

    LinearConstraint.Builder builder =
        LinearConstraint.of(constraint.getName(), op, rhs, constraint.getWeight());

    for (jp.ac.meiji.igusso.coptool.model.LinearTerm term : constraint) {
      Variable variable = translate(term.getVariable());
      for (int value : term.getVariable().getDomain()) {
        builder.addTerm(term.getCoeff() * value, variable, String.valueOf(value));
      }
    }

    return builder.build();
  }

  private Constraint translate(
      jp.ac.meiji.igusso.coptool.model.PseudoBooleanConstraint constraint) {
    Comparator op = null;
    int rhs = 0;
    switch (constraint.getOp()) {
      case EQ: {
        op = Comparator.EQ;
        rhs = constraint.getRhs();
      } break;
      case LE: {
        op = Comparator.LE;
        rhs = constraint.getRhs();
      } break;
      case LT: {
        op = Comparator.LE;
        rhs = constraint.getRhs() - 1;
      } break;
      case GE: {
        op = Comparator.GE;
        rhs = constraint.getRhs();
      } break;
      case GT: {
        op = Comparator.GE;
        rhs = constraint.getRhs() + 1;
      } break;
      default:
        throw new RuntimeException();
    }

    LinearConstraint.Builder builder =
        LinearConstraint.of(constraint.getName(), op, rhs, constraint.getWeight());

    for (jp.ac.meiji.igusso.coptool.model.PseudoBooleanTerm term : constraint) {
      Variable variable = translate(term.getVariable());
      builder.addTerm(term.getCoeff(), variable, term.getValue());
    }

    return builder.build();
  }

  private Constraint translate(
      jp.ac.meiji.igusso.coptool.model.ConflictPointConstraint constraint) {
    int negativeTermCount = 0;
    for (jp.ac.meiji.igusso.coptool.model.PredicateTerm term : constraint) {
      if (!term.isPositive()) {
        negativeTermCount++;
      }
    }
    int rhs = constraint.size() - 1 - negativeTermCount;

    LinearConstraint.Builder builder =
        LinearConstraint.of(constraint.getName(), Comparator.LE, rhs, constraint.getWeight());
    for (jp.ac.meiji.igusso.coptool.model.PredicateTerm term : constraint) {
      Variable variable = translate(term.getVariable());
      int coeff = term.isPositive() ? 1 : -1;
      builder.addTerm(coeff, variable, term.getValue());
    }

    return builder.build();
  }

  private Constraint translate(jp.ac.meiji.igusso.coptool.model.AllDifferentConstraint constraint) {
    AllDifferentConstraint.Builder builder =
        AllDifferentConstraint.of(constraint.getName(), constraint.getWeight());
    for (jp.ac.meiji.igusso.coptool.model.Variable variable : constraint) {
      builder.addVariable(translate(variable));
    }

    return builder.build();
  }

  public Constraint translate(jp.ac.meiji.igusso.coptool.model.Constraint constraint) {
    if (constraintMap.containsKey(constraint)) {
      return constraintMap.get(constraint);
    }
    if (constraintNameSpace.contains(constraint.getName())) {
      throw new IllegalArgumentException(
          "Duplicated Constraint Declaration: " + constraint.getName());
    }

    Constraint ret = null;
    if (constraint instanceof jp.ac.meiji.igusso.coptool.model.LinearConstraint) {
      ret = translate((jp.ac.meiji.igusso.coptool.model.LinearConstraint) constraint);
    } else if (constraint instanceof jp.ac.meiji.igusso.coptool.model.PseudoBooleanConstraint) {
      ret = translate((jp.ac.meiji.igusso.coptool.model.PseudoBooleanConstraint) constraint);
    } else if (constraint instanceof jp.ac.meiji.igusso.coptool.model.AllDifferentConstraint) {
      ret = translate((jp.ac.meiji.igusso.coptool.model.AllDifferentConstraint) constraint);
    } else if (constraint instanceof jp.ac.meiji.igusso.coptool.model.ConflictPointConstraint) {
      ret = translate((jp.ac.meiji.igusso.coptool.model.ConflictPointConstraint) constraint);
    } else {
      throw new RuntimeException();
    }

    constraintMap.put(constraint, ret);
    constraintNameSpace.add(constraint.getName());
    return ret;
  }
}
