package jp.ac.meiji.igusso.coptool.sugar;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

import static jp.kobe_u.sugar.expression.Expression.ADD;
import static jp.kobe_u.sugar.expression.Expression.INT_DEFINITION;
import static jp.kobe_u.sugar.expression.Expression.MUL;
import static jp.kobe_u.sugar.expression.Expression.ONE;
import static jp.kobe_u.sugar.expression.Expression.ZERO;
import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.coptool.model.AllDifferentConstraint;
import jp.ac.meiji.igusso.coptool.model.Comparator;
import jp.ac.meiji.igusso.coptool.model.ConflictPointConstraint;
import jp.ac.meiji.igusso.coptool.model.Constraint;
import jp.ac.meiji.igusso.coptool.model.Domain;
import jp.ac.meiji.igusso.coptool.model.LinearConstraint;
import jp.ac.meiji.igusso.coptool.model.LinearTerm;
import jp.ac.meiji.igusso.coptool.model.PredicateTerm;
import jp.ac.meiji.igusso.coptool.model.PseudoBooleanConstraint;
import jp.ac.meiji.igusso.coptool.model.PseudoBooleanTerm;
import jp.ac.meiji.igusso.coptool.model.Variable;
import jp.kobe_u.sugar.expression.Expression;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ToString
@EqualsAndHashCode
public final class Model2SugarTranslator {
  private List<Expression> expressions;

  private Map<Variable, List<Expression>> variableMap;
  private Set<String> variableNameSpace;

  private Map<Constraint, List<Expression>> constraintMap;
  private Set<String> constraintNameSpace;

  private List<PenaltyVariable> penaltyVariables;
  private Map<Constraint, Expression> penaltyVariableMap;

  @Value
  private static class PenaltyVariable {
    String name;
    int max;
    int weight;
  }

  private Model2SugarTranslator() {
    this.expressions = new ArrayList<>();

    this.variableMap = new HashMap<>();
    this.variableNameSpace = new HashSet<>();

    this.constraintMap = new HashMap<>();
    this.constraintNameSpace = new HashSet<>();

    this.penaltyVariables = new ArrayList<>();
    this.penaltyVariableMap = new HashMap<>();
  }

  public static Model2SugarTranslator newInstance() {
    return new Model2SugarTranslator();
  }

  private static Expression varExp(Variable variable) {
    return create(variable.getName());
  }

  private static Expression varExp(Variable variable, int value) {
    return create(format("_%s__%d", variable.getName(), value));
  }

  private static Expression penaltyVarExp(Constraint constraint) {
    return create(format("_P__%s", constraint.getName()));
  }

  private static Expression penaltyVarExp(Constraint constraint, int index) {
    return create(format("_P%d__%s", index, constraint.getName()));
  }

  private static Expression opExp(Comparator op) {
    switch (op) {
      case EQ:
        return Expression.EQ;
      case LE:
        return Expression.LE;
      case LT:
        return Expression.LT;
      case GE:
        return Expression.GE;
      case GT:
        return Expression.GT;
      default:
        throw new IllegalStateException();
    }
  }

  private static int calcLhsMin(PseudoBooleanConstraint constraint) {
    int lhsMin = 0;
    for (PseudoBooleanTerm term : constraint) {
      int coeff = term.getCoeff();
      if (coeff < 0) {
        lhsMin += coeff;
      }
    }
    return lhsMin;
  }

  private static int calcLhsMin(LinearConstraint constraint) {
    int lhsMin = 0;
    for (LinearTerm term : constraint) {
      int coeff = term.getCoeff();
      List<Integer> domain = term.getVariable().getDomain().getValues();
      lhsMin += Math.min(coeff * domain.get(0), coeff * domain.get(domain.size() - 1));
    }
    return lhsMin;
  }

  private static int calcLhsMax(PseudoBooleanConstraint constraint) {
    int lhsMax = 0;
    for (PseudoBooleanTerm term : constraint) {
      int coeff = term.getCoeff();
      if (coeff > 0) {
        lhsMax += coeff;
      }
    }
    return lhsMax;
  }

  private static int calcLhsMax(LinearConstraint constraint) {
    int lhsMax = 0;
    for (LinearTerm term : constraint) {
      int coeff = term.getCoeff();
      List<Integer> domain = term.getVariable().getDomain().getValues();
      lhsMax += Math.max(coeff * domain.get(0), coeff * domain.get(domain.size() - 1));
    }
    return lhsMax;
  }

  public boolean hasObjective() {
    return penaltyVariables.size() > 0;
  }

  public List<Expression> translateObjective() {
    if (!hasObjective()) {
      throw new IllegalStateException();
    }

    List<Expression> res = new ArrayList<>();

    int maxPena = 0;
    for (PenaltyVariable v : penaltyVariables) {
      maxPena += v.getWeight() * v.getMax();
    }
    res.add(create(INT_DEFINITION, create("_P"), ZERO, create(maxPena)));
    res.add(create(Expression.OBJECTIVE_DEFINITION, Expression.MINIMIZE, create("_P")));

    Map<Integer, List<PenaltyVariable>> group = new HashMap<>();
    for (PenaltyVariable v : penaltyVariables) {
      if (!group.containsKey(v.getWeight())) {
        group.put(v.getWeight(), new ArrayList<>());
      }
      group.get(v.getWeight()).add(v);
    }

    for (int weight : group.keySet()) {
      List<PenaltyVariable> vars = group.get(weight);

      int sum = 0;
      for (PenaltyVariable v : vars) {
        sum += v.getMax();
      }

      Expression pena = create(format("_P%03d", weight));
      res.add(create(INT_DEFINITION, pena, ZERO, create(sum)));

      List<Expression> varExp = new ArrayList<>();
      for (PenaltyVariable v : vars) {
        varExp.add(create(v.getName()));
      }

      res.add(create(Expression.EQ, pena, create(ADD, varExp)));
    }

    List<Expression> varExp = new ArrayList<>();
    for (int weight : group.keySet()) {
      varExp.add(create(MUL, create(weight), create(format("_P%03d", weight))));
    }
    res.add(create(Expression.EQ, create("_P"), create(ADD, varExp)));
    return res;
  }

  public Expression getPenaltyVariableOf(Constraint constraint) {
    if (!penaltyVariableMap.containsKey(constraint)) {
      throw new IllegalArgumentException("No Such Soft Constraint: " + constraint);
    }
    return penaltyVariableMap.get(constraint);
  }

  public List<Expression> translate(@NonNull Variable variable) {
    if (variableMap.containsKey(variable)) {
      return variableMap.get(variable);
    }

    if (variableNameSpace.contains(variable.getName())) {
      throw new IllegalArgumentException("Duplicated Variable Declaration: " + variable);
    }

    List<Expression> ret = new ArrayList<>();

    List<Expression> terms = new ArrayList<>();
    for (int value : variable.getDomain()) {
      terms.add(create(value));
    }
    // (int X 0 1 2)
    ret.add(create(INT_DEFINITION, create(variable.getName()), create(terms)));

    // (int X 0 1 2)
    // (int _X__0)
    // (imp (>= _X__0 1) (<= X 0))
    // (imp (>= _X__0 1) (>= X 0))
    // (imp (and (<= X 0) (>= X 0)) (>= X 1))
    // ...
    for (int value : variable.getDomain()) {
      Expression mainName = varExp(variable);
      Expression subName = varExp(variable, value);
      ret.add(create(INT_DEFINITION, subName, ZERO, ONE));
      ret.add(create(Expression.IMP, create(Expression.GE, subName, ONE),
          create(Expression.LE, mainName, create(value))));
      ret.add(create(Expression.IMP, create(Expression.GE, subName, ONE),
          create(Expression.GE, mainName, create(value))));
      ret.add(create(Expression.IMP,
          create(Expression.AND, create(Expression.LE, mainName, create(value)),
              create(Expression.GE, mainName, create(value))),
          create(Expression.GE, subName, ONE)));
    }

    ret = Collections.unmodifiableList(ret);
    variableMap.put(variable, ret);
    variableNameSpace.add(variable.getName());
    return ret;
  }

  private List<Expression> translate(LinearConstraint constraint) {
    List<Expression> res = new ArrayList<>();

    List<Expression> lhsTerms = new ArrayList<>();
    for (LinearTerm term : constraint) {
      if (!variableMap.containsKey(term.getVariable())) {
        throw new IllegalStateException("Undeclared Variable Is Found: " + term.getVariable());
      }

      lhsTerms.add(create(MUL, create(term.getCoeff()), varExp(term.getVariable())));
    }

    if (constraint.isSoft()) {
      int lhsMin = calcLhsMin(constraint);
      int lhsMax = calcLhsMax(constraint);

      Expression pena = penaltyVarExp(constraint);
      Expression pena1 = penaltyVarExp(constraint, 1);
      Expression pena2 = penaltyVarExp(constraint, 2);

      int maxPena = -1;
      int maxPena1 = -1;
      int maxPena2 = -1;
      switch (constraint.getOp()) {
        case EQ:
          maxPena1 = max(constraint.getRhs() - lhsMin, 0);
          maxPena2 = max(lhsMax - constraint.getRhs(), 0);
          maxPena = max(maxPena1, maxPena2);
          lhsTerms.add(pena1);
          lhsTerms.add(pena2.neg());
          break;

        case LE:
          maxPena = max(lhsMax - constraint.getRhs(), 0);
          lhsTerms.add(pena.neg());
          break;

        case LT:
          maxPena = max(lhsMax - constraint.getRhs() + 1, 0);
          lhsTerms.add(pena.neg());
          break;

        case GE:
          maxPena = max(constraint.getRhs() - lhsMin, 0);
          lhsTerms.add(pena);
          break;

        case GT:
          maxPena = max(constraint.getRhs() - lhsMin + 1, 0);
          lhsTerms.add(pena);
          break;

        default:
          throw new IllegalStateException();
      }

      res.add(create(INT_DEFINITION, pena, ZERO, create(maxPena)));
      penaltyVariables.add(
          new PenaltyVariable(pena.stringValue(), maxPena, constraint.getWeight()));
      penaltyVariableMap.put(constraint, pena);

      if (constraint.getOp() == Comparator.EQ) {
        res.add(create(INT_DEFINITION, pena1, ZERO, create(maxPena1)));
        res.add(create(INT_DEFINITION, pena2, ZERO, create(maxPena2)));
        res.add(create(Expression.EQ, pena, create(ADD, pena1, pena2)));
      }
    }

    Expression cons =
        create(opExp(constraint.getOp()), create(ADD, lhsTerms), create(constraint.getRhs()));
    cons.setComment(constraint.getName());
    res.add(cons);
    return res;
  }

  private List<Expression> translate(PseudoBooleanConstraint constraint) {
    List<Expression> res = new ArrayList<>();

    List<Expression> lhsTerms = new ArrayList<>();
    for (PseudoBooleanTerm term : constraint) {
      if (!variableMap.containsKey(term.getVariable())) {
        throw new IllegalStateException("Undeclared Variable Is Found: " + term.getVariable());
      }

      lhsTerms.add(
          create(MUL, create(term.getCoeff()), varExp(term.getVariable(), term.getValue())));
    }

    if (constraint.isSoft()) {
      int lhsMin = calcLhsMin(constraint);
      int lhsMax = calcLhsMax(constraint);

      Expression pena = penaltyVarExp(constraint);
      Expression pena1 = penaltyVarExp(constraint, 1);
      Expression pena2 = penaltyVarExp(constraint, 2);

      int maxPena = -1;
      int maxPena1 = -1;
      int maxPena2 = -1;
      switch (constraint.getOp()) {
        case EQ:
          maxPena1 = max(constraint.getRhs() - lhsMin, 0);
          maxPena2 = max(lhsMax - constraint.getRhs(), 0);
          maxPena = max(maxPena1, maxPena2);
          lhsTerms.add(pena1);
          lhsTerms.add(pena2.neg());
          break;

        case LE:
          maxPena = max(lhsMax - constraint.getRhs(), 0);
          lhsTerms.add(pena.neg());
          break;

        case LT:
          maxPena = max(lhsMax - constraint.getRhs() + 1, 0);
          lhsTerms.add(pena.neg());
          break;

        case GE:
          maxPena = max(constraint.getRhs() - lhsMin, 0);
          lhsTerms.add(pena);
          break;

        case GT:
          maxPena = max(constraint.getRhs() - lhsMin + 1, 0);
          lhsTerms.add(pena);
          break;

        default:
          throw new IllegalStateException();
      }

      res.add(create(INT_DEFINITION, pena, ZERO, create(maxPena)));

      penaltyVariables.add(
          new PenaltyVariable(pena.stringValue(), maxPena, constraint.getWeight()));
      penaltyVariableMap.put(constraint, pena);

      if (constraint.getOp() == Comparator.EQ) {
        res.add(create(INT_DEFINITION, pena1, ZERO, create(maxPena1)));
        res.add(create(INT_DEFINITION, pena2, ZERO, create(maxPena2)));
        res.add(create(Expression.EQ, pena, create(ADD, pena1, pena2)));
      }
    }

    Expression cons =
        create(opExp(constraint.getOp()), create(ADD, lhsTerms), create(constraint.getRhs()));
    cons.setComment(constraint.getName());
    res.add(cons);
    return res;
  }

  private List<Expression> translate(AllDifferentConstraint constraint) {
    if (constraint.isSoft()) {
      throw new UnsupportedOperationException(
          "Sorry, Soft AllDifferentConstraint Is Not Supported");
    }

    List<Expression> vars = new ArrayList<>();
    for (Variable variable : constraint) {
      if (!variableMap.containsKey(variable)) {
        throw new IllegalStateException("Undeclared Variable Is Found: " + variable);
      }
      vars.add(varExp(variable));
    }

    Expression ret = create(Expression.ALLDIFFERENT, vars);
    ret.setComment(constraint.getName());
    return Arrays.asList(ret);
  }

  private List<Expression> translate(ConflictPointConstraint constraint) {
    List<Expression> res = new ArrayList<>();

    if (constraint.isSoft()) {
      res.add(create(INT_DEFINITION, penaltyVarExp(constraint), ZERO, ONE));
      penaltyVariables.add(
          new PenaltyVariable(penaltyVarExp(constraint).stringValue(), 1, constraint.getWeight()));
      penaltyVariableMap.put(constraint, penaltyVarExp(constraint));
    }

    List<Expression> terms = new ArrayList<>();
    for (PredicateTerm term : constraint) {
      if (!variableMap.containsKey(term.getVariable())) {
        throw new IllegalStateException("Undeclared Variable Is Found: " + term.getVariable());
      }

      if (term.isPositive()) {
        terms.add(create(Expression.LE, varExp(term.getVariable(), term.getValue()), ZERO));
      } else {
        terms.add(create(Expression.GE, varExp(term.getVariable(), term.getValue()), ONE));
      }
    }
    if (constraint.isSoft()) {
      terms.add(create(Expression.GE, penaltyVarExp(constraint), ONE));
    }

    Expression cons = create(Expression.OR, terms);
    cons.setComment(constraint.getName());
    res.add(cons);
    return res;
  }

  public List<Expression> translate(Constraint constraint) {
    if (constraintMap.containsKey(constraint)) {
      return constraintMap.get(constraint);
    }

    if (constraintNameSpace.contains(constraint.getName())) {
      throw new IllegalArgumentException("Duplicated Constraint: " + constraint);
    }

    List<Expression> ret = null;

    if (constraint instanceof LinearConstraint) {
      ret = translate((LinearConstraint) constraint);
    } else if (constraint instanceof ConflictPointConstraint) {
      ret = translate((ConflictPointConstraint) constraint);
    } else if (constraint instanceof PseudoBooleanConstraint) {
      ret = translate((PseudoBooleanConstraint) constraint);
    } else if (constraint instanceof AllDifferentConstraint) {
      ret = translate((AllDifferentConstraint) constraint);
    } else {
      throw new RuntimeException();
    }

    ret = Collections.unmodifiableList(ret);
    constraintMap.put(constraint, ret);
    constraintNameSpace.add(constraint.getName());
    return ret;
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder();

    String newLine = String.format("%n");
    for (List<Expression> expressions : variableMap.values()) {
      for (Expression expression : expressions) {
        ret.append(expression).append(newLine);
      }
    }
    for (List<Expression> expressions : constraintMap.values()) {
      for (Expression expression : expressions) {
        ret.append(expression).append(newLine);
      }
    }
    if (hasObjective()) {
      for (Expression expression : translateObjective()) {
        ret.append(expression).append(newLine);
      }
    }
    return ret.toString();
  }
}
