package jp.ac.meiji.igusso.model;

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

  private Map<Variable, jp.ac.meiji.igusso.scop4j.Variable> variableMap;
  private Set<String> variableNameSpace;

  private Map<Constraint, jp.ac.meiji.igusso.scop4j.Constraint> constraintMap;
  private Set<String> constraintNameSpace;

  private Model2ScopTranslator() {
    this.variableMap = new HashMap<>();
    this.variableNameSpace = new HashSet<>();

    this.constraintMap = new HashMap<>();
    this.constraintNameSpace = new HashSet<>();
  }

  public jp.ac.meiji.igusso.scop4j.Variable translate(Variable variable) {
    List<String> newDomain = new ArrayList<>();
    for (int value : variable.getDomain()) {
      newDomain.add(String.valueOf(value));
    }
    jp.ac.meiji.igusso.scop4j.Variable newVariable =
        jp.ac.meiji.igusso.scop4j.Variable.of(variable.getName(), newDomain);

    variableMap.put(variable, newVariable);
    variableNameSpace.add(variable.getName());
    return newVariable;
  }

  private jp.ac.meiji.igusso.scop4j.Constraint translate(LinearConstraint constraint) {
    jp.ac.meiji.igusso.scop4j.Comparator op = null;
    int rhs = 0;
    switch (constraint.getOp()) {
      case EQ: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.EQ;
        rhs = constraint.getRhs();
      } break;
      case LE: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.LE;
        rhs = constraint.getRhs();
      } break;
      case LT: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.LE;
        rhs = constraint.getRhs() - 1;
      } break;
      case GE: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.GE;
        rhs = constraint.getRhs();
      } break;
      case GT: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.GE;
        rhs = constraint.getRhs() + 1;
      } break;
      default:
        throw new RuntimeException();
    }

    jp.ac.meiji.igusso.scop4j.LinearConstraint.Builder builder =
        jp.ac.meiji.igusso.scop4j.LinearConstraint.of(
            constraint.getName(), op, rhs, constraint.getWeight());

    for (LinearTerm term : constraint) {
      jp.ac.meiji.igusso.scop4j.Variable variable = translate(term.getVariable());
      for (int value : term.getVariable().getDomain()) {
        builder.addTerm(term.getCoeff() * value, variable, String.valueOf(value));
      }
    }

    return builder.build();
  }

  private jp.ac.meiji.igusso.scop4j.Constraint translate(PseudoBooleanConstraint constraint) {
    jp.ac.meiji.igusso.scop4j.Comparator op = null;
    int rhs = 0;
    switch (constraint.getOp()) {
      case EQ: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.EQ;
        rhs = constraint.getRhs();
      } break;
      case LE: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.LE;
        rhs = constraint.getRhs();
      } break;
      case LT: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.LE;
        rhs = constraint.getRhs() - 1;
      } break;
      case GE: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.GE;
        rhs = constraint.getRhs();
      } break;
      case GT: {
        op = jp.ac.meiji.igusso.scop4j.Comparator.GE;
        rhs = constraint.getRhs() + 1;
      } break;
      default:
        throw new RuntimeException();
    }

    jp.ac.meiji.igusso.scop4j.LinearConstraint.Builder builder =
        jp.ac.meiji.igusso.scop4j.LinearConstraint.of(
            constraint.getName(), op, rhs, constraint.getWeight());

    for (PseudoBooleanTerm term : constraint) {
      jp.ac.meiji.igusso.scop4j.Variable variable = translate(term.getVariable());
      builder.addTerm(term.getCoeff(), variable, term.getValue());
    }

    return builder.build();
  }

  private jp.ac.meiji.igusso.scop4j.Constraint translate(ConflictPointConstraint constraint) {
    int negativeTermCount = 0;
    for (PredicateTerm term : constraint) {
      if (!term.isPositive()) {
        negativeTermCount++;
      }
    }
    int rhs = constraint.size() - 1 - negativeTermCount;

    jp.ac.meiji.igusso.scop4j.LinearConstraint.Builder builder =
        jp.ac.meiji.igusso.scop4j.LinearConstraint.of(constraint.getName(),
            jp.ac.meiji.igusso.scop4j.Comparator.LE, rhs, constraint.getWeight());
    for (PredicateTerm term : constraint) {
      jp.ac.meiji.igusso.scop4j.Variable variable = translate(term.getVariable());
      int coeff = term.isPositive() ? 1 : -1;
      builder.addTerm(coeff, variable, term.getValue());
    }

    return builder.build();
  }

  private jp.ac.meiji.igusso.scop4j.Constraint translate(AllDifferentConstraint constraint) {
    jp.ac.meiji.igusso.scop4j.AllDifferentConstraint.Builder builder =
        jp.ac.meiji.igusso.scop4j.AllDifferentConstraint.of(
            constraint.getName(), constraint.getWeight());
    for (Variable variable : constraint) {
      builder.addVariable(translate(variable));
    }

    return builder.build();
  }

  public jp.ac.meiji.igusso.scop4j.Constraint translate(Constraint constraint) {
    if (constraintMap.containsKey(constraint)) {
      return constraintMap.get(constraint);
    }
    if (constraintNameSpace.contains(constraint.getName())) {
      throw new IllegalArgumentException(
          "Duplicated Constraint Declaration: " + constraint.getName());
    }

    jp.ac.meiji.igusso.scop4j.Constraint ret = null;
    if (constraint instanceof LinearConstraint) {
      ret = translate((LinearConstraint) constraint);
    } else if (constraint instanceof PseudoBooleanConstraint) {
      ret = translate((PseudoBooleanConstraint) constraint);
    } else if (constraint instanceof AllDifferentConstraint) {
      ret = translate((AllDifferentConstraint) constraint);
    } else if (constraint instanceof ConflictPointConstraint) {
      ret = translate((ConflictPointConstraint) constraint);
    } else {
      throw new RuntimeException();
    }

    constraintMap.put(constraint, ret);
    constraintNameSpace.add(constraint.getName());
    return ret;
  }
}
