package jp.ac.meiji.igusso.coptool;

import static java.lang.Math.max;
import static java.lang.String.format;

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

  private static String varToString(Variable variable, int value) {
    return format("%s__%d", variable.getName(), value);
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

  private List<String> penaVariableNames;
  private List<Integer> penaMaxs;
  private List<Integer> penaWeights;

  private List<String> encodeObjective(Model model) {
    boolean hasSoftConstraints = false;
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.getWeight() >= 0) {
        hasSoftConstraints = true;
        break;
      }
    }
    if (!hasSoftConstraints) {
      return Arrays.asList();
    }

    List<String> res = new ArrayList<>();
    res.add("; objective declaration");

    int maxPena = 0;
    for (int i = 0; i < penaVariableNames.size(); i++) {
      maxPena += penaWeights.get(i) * penaMaxs.get(i);
    }
    res.add(format("(int _P 0 %d)", maxPena));

    StringBuilder penaExp = new StringBuilder();
    penaExp.append(format("(= _P (+%n"));
    for (int i = 0; i < penaVariableNames.size(); i++) {
      int weight = penaWeights.get(i);
      String name = penaVariableNames.get(i);

      penaExp.append("  ").append(format("(* %d %s)%n", weight, name));
    }
    penaExp.append("))");

    res.add(penaExp.toString());
    res.add(format("(objective minimize _P)"));

    return res;
  }

  @Override
  public List<String> encode(Variable variable) {
    List<String> res = new ArrayList<>();

    res.add(format("(int %s %s)", variable.getName(), domainToString(variable.getDomain())));

    for (Integer d : variable.getDomain()) {
      String intVarName = variable.getName();
      String boolVarName = varToString(variable, d);

      res.add(format("(bool %s) ; means %s = %d", boolVarName, intVarName, d));
      res.add(format("(imp %s (<= %s %d))", boolVarName, intVarName, d));
      res.add(format("(imp %s (>= %s %d))", boolVarName, intVarName, d));
      res.add(format(
          "(imp (and (<= %s %d) (>= %s %d)) %s)", intVarName, d, intVarName, d, boolVarName));
    }

    res.add("");
    return res;
  }

  @Override
  public List<String> encode(ConflictPointConstraint constraint) {
    List<String> res = new ArrayList<>();

    if (constraint.getWeight() >= 0) {
      String penaName = format("_P__%s", constraint.getName());
      res.add(format("(int %s 0 1)", penaName));

      penaVariableNames.add(penaName);
      penaMaxs.add(1);
      penaWeights.add(constraint.getWeight());
    }

    StringBuilder termsExp = new StringBuilder();

    for (int i = 0; i < constraint.size(); i++) {
      boolean phase = constraint.getPhases().get(i);
      Variable var = constraint.getVariables().get(i);
      int val = constraint.getValues().get(i);

      String termExp =
          phase ? format("(not %s) ", varToString(var, val)) : format("%s ", varToString(var, val));
      termsExp.append(termExp);
    }

    if (constraint.getWeight() >= 0) {
      String penaName = format("_P__%s", constraint.getName());
      termsExp.append(format("(>= %s 1) ", penaName));
    }

    termsExp.setLength(termsExp.length() - 1);

    String cons = format("(or %s) ; %s", termsExp.toString(), constraint.getName());
    res.add(cons);
    return Collections.unmodifiableList(res);
  }

  @Override
  public List<String> encode(PseudoBooleanConstraint constraint) {
    List<String> res = new ArrayList<>();
    res.add(format("; constraint %s", constraint.getName()));

    StringBuilder lhsExp = new StringBuilder();
    lhsExp.append("(+");
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      Variable var = constraint.getVariables().get(i);
      int val = constraint.getValues().get(i);
      lhsExp.append(format(" (if %s %d 0)", varToString(var, val), coeff));
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
          res.add(format("(int %s 0 %d)", pena1, maxPena1));
          res.add(format("(>= %s 0)", pena1));

          maxPena2 = max(lhsMax - constraint.getRhs(), 0);
          res.add(format("(int %s 0 %d)", pena2, maxPena2));
          res.add(format("(>= %s 0)", pena2));

          lhsExp.append(format(" %s", pena1));
          lhsExp.append(format(" (- %s)", pena2));
          break;

        case LE:
          maxPena = max(lhsMax - constraint.getRhs(), 0);
          res.add(format("(int %s 0 %d)", pena, maxPena));
          res.add(format("(>= %s 0)", pena));
          lhsExp.append(format(" (- %s)", pena));
          break;

        case LT:
          maxPena = max(lhsMax - constraint.getRhs() + 1, 0);
          res.add(format("(int %s 0 %d)", pena, maxPena));
          res.add(format("(>= %s 0)", pena));
          lhsExp.append(format(" (- %s)", pena));
          break;

        case GE:
          maxPena = max(constraint.getRhs() - lhsMin, 0);
          res.add(format("(int %s 0 %d)", pena, maxPena));
          res.add(format("(>= %s 0)", pena));
          lhsExp.append(format(" %s", pena));
          break;

        case GT:
          maxPena = max(constraint.getRhs() - lhsMin + 1, 0);
          res.add(format("(int %s 0 %d)", pena, maxPena));
          res.add(format("(>= %s 0)", pena));
          lhsExp.append(format(" %s", pena));
          break;

        default:
          throw new IllegalStateException();
      }

      if (constraint.getOp() == Comparator.EQ) {
        penaVariableNames.add(pena1);
        penaWeights.add(constraint.getWeight());
        penaMaxs.add(maxPena1);

        penaVariableNames.add(pena2);
        penaWeights.add(constraint.getWeight());
        penaMaxs.add(maxPena2);
      } else {
        penaVariableNames.add(pena);
        penaWeights.add(constraint.getWeight());
        penaMaxs.add(maxPena);
      }
    }
    lhsExp.append(")");

    String cons = format("(%s %s %d) ; %s", opName(constraint.getOp()), lhsExp.toString(),
        constraint.getRhs(), constraint.getName());
    res.add(cons);
    res.add("");

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
          String v1Exp = varToString(vars.get(i), d);
          String v2Exp = varToString(vars.get(j), d);
          res.add(format("(or (not %s) (not %s))", v1Exp, v2Exp));
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  @Override
  public List<String> encode(Model model) {
    penaVariableNames = new ArrayList<>();
    penaMaxs = new ArrayList<>();
    penaWeights = new ArrayList<>();

    List<String> body = new ArrayList<>();

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
