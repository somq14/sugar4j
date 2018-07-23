package jp.ac.meiji.igusso.coptool;

import static java.lang.Math.max;
import static java.lang.String.format;

import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SugarModelEncoder implements ModelEncoder {
  public static SugarModelEncoder getInstance() {
    return new SugarModelEncoder();
  }

  private SugarModelEncoder() {}

  private static String intVar(Variable variable, int value) {
    return format("_%s__%d", variable.getName(), value);
  }

  private static String boolVar(Variable variable, int value) {
    return format("(B _%s__%d)", variable.getName(), value);
  }

  private static String penaltyVarToString(Constraint constraint) {
    return format("_P__%s", constraint.getName());
  }

  private static String penaltyVarToString(Constraint constraint, int ind) {
    return format("_P%d__%s", ind, constraint.getName());
  }

  private static String domainToString(List<Integer> domain) {
    StringBuilder res = new StringBuilder();
    res.append('(');

    int seqBegin = 0;
    while (seqBegin < domain.size()) {
      int seqEnd = seqBegin;
      while (seqEnd + 1 < domain.size() && domain.get(seqEnd) + 1 == domain.get(seqEnd + 1)) {
        seqEnd++;
      }
      res.append(format(" (%d %d)", domain.get(seqBegin), domain.get(seqEnd)));

      seqBegin = seqEnd + 1;
    }

    res.append(" )");
    return res.toString();
  }

  private static String opName(Comparator op) {
    switch (op) {
      case EQ:
        return "=";
      case LE:
        return "<=";
      case LT:
        return "<";
      case GE:
        return ">=";
      case GT:
        return ">";
      default:
        throw new IllegalStateException();
    }
  }

  private static int calcLhsMin(PseudoBooleanConstraint constraint) {
    int lhsMin = 0;
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      if (coeff < 0) {
        lhsMin += coeff;
      }
    }
    return lhsMin;
  }

  private static int calcLhsMin(LinearConstraint constraint) {
    int lhsMin = 0;
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      List<Integer> domain = constraint.getVariables().get(i).getDomain();
      lhsMin += Math.min(coeff * domain.get(0), coeff * domain.get(domain.size() - 1));
    }
    return lhsMin;
  }

  private static int calcLhsMax(PseudoBooleanConstraint constraint) {
    int lhsMax = 0;
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      if (coeff > 0) {
        lhsMax += coeff;
      }
    }
    return lhsMax;
  }

  private static int calcLhsMax(LinearConstraint constraint) {
    int lhsMax = 0;
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      List<Integer> domain = constraint.getVariables().get(i).getDomain();
      lhsMax += Math.max(coeff * domain.get(0), coeff * domain.get(domain.size() - 1));
    }
    return lhsMax;
  }

  private List<PenaltyVariable> penaltyVariables;

  @Value
  private static class PenaltyVariable {
    String name;
    int max;
    int weight;
  }

  private List<String> generatePredicates() {
    List<String> res = new ArrayList<>();
    res.add("; predicate definition");
    res.add("(predicate (B v) (>= v 1))");
    res.add("");
    res.add("(predicate (BIND i b v) (and");
    res.add("  (imp (B b) (<= i v))");
    res.add("  (imp (B b) (>= i v))");
    res.add("  (imp (and (<= i v) (>= i v)) (B b))");
    res.add("  ))");
    res.add("");

    return Collections.unmodifiableList(res);
  }

  private List<String> encodeObjective(Model model) {
    if (penaltyVariables.isEmpty()) {
      return Arrays.asList();
    }

    List<String> res = new ArrayList<>();
    res.add("; objective declaration");

    int maxPena = 0;
    for (PenaltyVariable v : penaltyVariables) {
      maxPena += v.getWeight() * v.getMax();
    }
    res.add(format("(int _P 0 %d)", maxPena));

    Map<Integer, List<PenaltyVariable>> group = new HashMap<>();
    for (PenaltyVariable v : penaltyVariables) {
      if (!group.containsKey(v.getWeight())) {
        group.put(v.getWeight(), new ArrayList<>());
      }
      group.get(v.getWeight()).add(v);
    }

    res.add(format("(objective minimize _P)"));
    res.add("");

    for (int weight : group.keySet()) {
      List<PenaltyVariable> vars = group.get(weight);

      int sum = 0;
      for (PenaltyVariable v : vars) {
        sum += v.getMax();
      }
      res.add(format("(int _P%03d 0 %d)", weight, sum));

      StringBuilder varExp = new StringBuilder();
      varExp.append(format("(= _P%03d (+%n", weight));
      for (PenaltyVariable v : vars) {
        varExp.append(format("  %s%n", v.getName()));
      }
      varExp.append("))");
      res.add(varExp.toString());
      res.add("");
    }

    StringBuilder penaExp = new StringBuilder();
    penaExp.append(format("(= _P (+%n"));
    for (int weight : group.keySet()) {
      penaExp.append("  ").append(format("(* %d _P%03d)%n", weight, weight));
    }
    penaExp.append("))");
    res.add(penaExp.toString());

    res.add("");

    return res;
  }

  @Override
  public List<String> encode(Variable variable) {
    List<String> res = new ArrayList<>();

    res.add(format("(int %s %s)", variable.getName(), domainToString(variable.getDomain())));

    for (Integer d : variable.getDomain()) {
      String varName = variable.getName();
      String intVarName = intVar(variable, d);
      String boolVarName = boolVar(variable, d);

      res.add(format("(int %s 0 1) ; %s = %d", intVarName, varName, d));
      res.add(format("(BIND %s %s %d)", varName, intVarName, d));
    }

    res.add("");
    return res;
  }

  @Override
  public List<String> encode(ConflictPointConstraint constraint) {
    List<String> res = new ArrayList<>();

    if (constraint.getWeight() >= 0) {
      String penaName = penaltyVarToString(constraint);
      res.add(format("(int %s 0 1)", penaName));

      penaltyVariables.add(new PenaltyVariable(penaName, 1, constraint.getWeight()));
    }

    StringBuilder termsExp = new StringBuilder();

    for (int i = 0; i < constraint.size(); i++) {
      boolean phase = constraint.getPhases().get(i);
      Variable var = constraint.getVariables().get(i);
      int val = constraint.getValues().get(i);

      String termExp =
          phase ? format("(not %s) ", boolVar(var, val)) : format("%s ", boolVar(var, val));
      termsExp.append(termExp);
    }

    if (constraint.getWeight() >= 0) {
      String penaName = penaltyVarToString(constraint);
      termsExp.append(format("(>= %s 1) ", penaName));
    }

    termsExp.setLength(termsExp.length() - 1);

    String cons = format("(or %s) ; %s", termsExp.toString(), constraint.getName());
    res.add(cons);
    return Collections.unmodifiableList(res);
  }

  @Override
  public List<String> encode(LinearConstraint constraint) {
    List<String> res = new ArrayList<>();

    List<String> lhsTerms = new ArrayList<>(constraint.size());
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      Variable var = constraint.getVariables().get(i);

      lhsTerms.add(format("(* %d %s)", coeff, var.getName()));
    }

    if (constraint.getWeight() >= 0) {
      int lhsMin = calcLhsMin(constraint);
      int lhsMax = calcLhsMax(constraint);

      String pena = penaltyVarToString(constraint);
      String pena1 = penaltyVarToString(constraint, 1);
      String pena2 = penaltyVarToString(constraint, 2);

      int maxPena = -1;
      int maxPena1 = -1;
      int maxPena2 = -1;
      switch (constraint.getOp()) {
        case EQ:
          maxPena1 = max(constraint.getRhs() - lhsMin, 0);
          maxPena2 = max(lhsMax - constraint.getRhs(), 0);
          lhsTerms.add(format("%s", pena1));
          lhsTerms.add(format("(- %s)", pena2));
          break;

        case LE:
          maxPena = max(lhsMax - constraint.getRhs(), 0);
          lhsTerms.add(format("(- %s)", pena));
          break;

        case LT:
          maxPena = max(lhsMax - constraint.getRhs() + 1, 0);
          lhsTerms.add(format("(- %s)", pena));
          break;

        case GE:
          maxPena = max(constraint.getRhs() - lhsMin, 0);
          lhsTerms.add(format("%s", pena));
          break;

        case GT:
          maxPena = max(constraint.getRhs() - lhsMin + 1, 0);
          lhsTerms.add(format("%s", pena));
          break;

        default:
          throw new IllegalStateException();
      }

      if (constraint.getOp() == Comparator.EQ) {
        res.add(format("(int %s 0 %d)", pena1, maxPena1));
        res.add(format("(int %s 0 %d)", pena2, maxPena2));

        penaltyVariables.add(new PenaltyVariable(pena1, maxPena1, constraint.getWeight()));
        penaltyVariables.add(new PenaltyVariable(pena2, maxPena2, constraint.getWeight()));
      } else {
        res.add(format("(int %s 0 %d)", pena, maxPena));

        penaltyVariables.add(new PenaltyVariable(pena, maxPena, constraint.getWeight()));
      }
    }

    res.add(format("(%s (+ %s) %s) ; %s", opName(constraint.getOp()), String.join(" ", lhsTerms),
        constraint.getRhs(), constraint.getName()));
    return Collections.unmodifiableList(res);
  }

  @Override
  public List<String> encode(PseudoBooleanConstraint constraint) {
    List<String> res = new ArrayList<>();

    StringBuilder lhsExp = new StringBuilder();
    lhsExp.append("(+");
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      Variable var = constraint.getVariables().get(i);
      int val = constraint.getValues().get(i);

      lhsExp.append(format(" (* %d %s)", coeff, intVar(var, val)));
    }

    if (constraint.getWeight() >= 0) {
      int lhsMin = calcLhsMin(constraint);
      int lhsMax = calcLhsMax(constraint);

      String pena = format("_P__%s", constraint.getName());
      String pena1 = format("_P1__%s", constraint.getName());
      String pena2 = format("_P2__%s", constraint.getName());

      int maxPena = -1;
      int maxPena1 = -1;
      int maxPena2 = -1;
      switch (constraint.getOp()) {
        case EQ:
          maxPena1 = max(constraint.getRhs() - lhsMin, 0);
          maxPena2 = max(lhsMax - constraint.getRhs(), 0);

          lhsExp.append(format(" %s", pena1));
          lhsExp.append(format(" (- %s)", pena2));
          break;

        case LE:
          maxPena = max(lhsMax - constraint.getRhs(), 0);
          lhsExp.append(format(" (- %s)", pena));
          break;

        case LT:
          maxPena = max(lhsMax - constraint.getRhs() + 1, 0);
          lhsExp.append(format(" (- %s)", pena));
          break;

        case GE:
          maxPena = max(constraint.getRhs() - lhsMin, 0);
          lhsExp.append(format(" %s", pena));
          break;

        case GT:
          maxPena = max(constraint.getRhs() - lhsMin + 1, 0);
          lhsExp.append(format(" %s", pena));
          break;

        default:
          throw new IllegalStateException();
      }

      if (constraint.getOp() == Comparator.EQ) {
        res.add(format("(int %s 0 %d)", pena1, maxPena1));
        res.add(format("(int %s 0 %d)", pena2, maxPena2));

        penaltyVariables.add(new PenaltyVariable(pena1, maxPena1, constraint.getWeight()));
        penaltyVariables.add(new PenaltyVariable(pena2, maxPena2, constraint.getWeight()));
      } else {
        res.add(format("(int %s 0 %d)", pena, maxPena));

        penaltyVariables.add(new PenaltyVariable(pena, maxPena, constraint.getWeight()));
      }
    }
    lhsExp.append(")");

    String cons = format("(%s %s %d) ; %s", opName(constraint.getOp()), lhsExp.toString(),
        constraint.getRhs(), constraint.getName());
    res.add(cons);

    return res;
  }

  public List<String> encode(AllDifferentConstraint constraint) {
    if (constraint.getWeight() >= 0) {
      throw new IllegalStateException("Soft AllDifferentConstraint is not supported");
    }

    List<String> res = new ArrayList<>();
    res.add(format("; constraint %s", constraint.getName()));

    List<Variable> vars = constraint.getVariables();
    if (vars.isEmpty()) {
      return Collections.unmodifiableList(res);
    }

    List<Integer> domain = constraint.getVariables().get(0).getDomain();

    for (int d : domain) {
      for (int i = 0; i < vars.size(); i++) {
        for (int j = i + 1; j < vars.size(); j++) {
          String v1Exp = boolVar(vars.get(i), d);
          String v2Exp = boolVar(vars.get(j), d);
          res.add(format("(or (not %s) (not %s))", v1Exp, v2Exp));
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  @Override
  public List<String> encode(Model model) {
    penaltyVariables = new ArrayList<>();

    List<String> body = new ArrayList<>();
    body.addAll(generatePredicates());

    for (Variable var : model.getVariables()) {
      body.addAll(encode(var));
    }

    for (Constraint cons : model.getConstraints()) {
      body.addAll(cons.encode(this));
    }

    body.addAll(encodeObjective(model));

    body.add("");
    return Collections.unmodifiableList(body);
  }
}
