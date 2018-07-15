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

  private static String varName(Variable variable, int value) {
    return format("%s__%d", variable.getName(), value);
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

  private static int calcLhsMin(LinearConstraint constraint) {
    int lhsMin = 0;
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      if (coeff < 0) {
        lhsMin += coeff;
      }
    }
    return lhsMin;
  }

  private static int calcLhsMax(LinearConstraint constraint) {
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
    res.add("; variable " + variable.getName());
    for (Integer d : variable.getDomain()) {
      res.add(format("(bool %s) ; %s", varName(variable, d), variable.getName() + " = " + d));
    }

    StringBuilder orCons = new StringBuilder();
    orCons.append("(or");
    for (Integer d : variable.getDomain()) {
      orCons.append(' ');
      orCons.append(varName(variable, d));
    }
    orCons.append(")");
    res.add(orCons.toString());

    int domainSize = variable.getDomain().size();
    for (int i = 0; i < domainSize; i++) {
      for (int j = i + 1; j < domainSize; j++) {
        int d1 = variable.getDomain().get(i);
        int d2 = variable.getDomain().get(j);
        res.add(format("(or (not %s) (not %s))", varName(variable, d1), varName(variable, d2),
            variable.getName()));
      }
    }
    res.add("");

    return res;
  }

  @Override
  public List<String> encode(LinearConstraint constraint) {
    List<String> res = new ArrayList<>();
    res.add(format("; constraint %s", constraint.getName()));

    StringBuilder lhsExp = new StringBuilder();
    lhsExp.append("(+");
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      Variable var = constraint.getVariables().get(i);
      int val = constraint.getValues().get(i);
      lhsExp.append(format(" (if %s %d 0)", varName(var, val), coeff));
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
          String v1Exp = varName(vars.get(i), d);
          String v2Exp = varName(vars.get(j), d);
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
