package jp.ac.meiji.igusso.coptool;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScopModelEncoder implements ModelEncoder {
  private static final ScopModelEncoder singleton = new ScopModelEncoder();

  public static ScopModelEncoder getInstance() {
    return singleton;
  }

  private ScopModelEncoder() {}

  @Override
  public List<String> encode(Variable variable) {
    StringBuilder domainExp = new StringBuilder();
    for (Integer d : variable.getDomain()) {
      domainExp.append(d).append(", ");
    }
    domainExp.setLength(domainExp.length() - 2);

    String body = format("variable %s in {%s}", variable.getName(), domainExp.toString());
    return Arrays.asList(body);
  }

  @Override
  public List<String> encode(ConflictPointConstraint constraint) {
    String weightExp = constraint.getWeight() < 0 ? "inf" : constraint.getWeight() + "";

    int negativeTermCount = 0;
    for (int i = 0; i < constraint.size(); i++) {
      if (!constraint.getPhases().get(i)) {
        negativeTermCount += 1;
      }
    }
    int rhs = constraint.size() - 1 - negativeTermCount;

    StringBuilder varExp = new StringBuilder();
    for (int i = 0; i < constraint.size(); i++) {
      boolean phase = constraint.getPhases().get(i);
      Variable var = constraint.getVariables().get(i);
      int val = constraint.getValues().get(i);

      varExp.append(' ').append(format("%d(%s, %d)", phase ? 1 : -1, var.getName(), val));
    }

    String cons = format("%s: weight=%s type=linear %s <= %d", constraint.getName(), weightExp,
        varExp.toString(), rhs);
    return Arrays.asList(cons);
  }

  @Override
  public List<String> encode(LinearConstraint constraint) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> encode(PseudoBooleanConstraint constraint) {
    String weightExp = constraint.getWeight() < 0 ? "inf" : constraint.getWeight() + "";

    StringBuilder termsBuilder = new StringBuilder();
    for (int i = 0; i < constraint.size(); i++) {
      int coeff = constraint.getCoeffs().get(i);
      Variable var = constraint.getVariables().get(i);
      int val = constraint.getValues().get(i);

      termsBuilder.append(format("%d(%s, %d)", coeff, var.getName(), val));
      termsBuilder.append(' ');
    }
    String termsExp = termsBuilder.toString();

    String cons = null;

    switch (constraint.getOp()) {
      case EQ:
        cons = format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, "=", constraint.getRhs());
        break;

      case LE:
        cons = format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, "<=", constraint.getRhs());
        break;

      case LT:
        cons = format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, "<=", constraint.getRhs() - 1);
        break;

      case GE:
        cons = format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, ">=", constraint.getRhs());
        break;

      case GT:
        cons = format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, ">=", constraint.getRhs() + 1);
        break;

      default:
        throw new IllegalStateException();
    }

    return Arrays.asList(cons);
  }

  public List<String> encode(AllDifferentConstraint constraint) {
    StringBuilder varExp = new StringBuilder();
    for (int i = 0; i < constraint.size(); i++) {
      String varName = constraint.getVariables().get(i).getName();
      varExp.append(' ').append(varName);
    }

    String weightExp = constraint.getWeight() < 0 ? "inf" : constraint.getWeight() + "";
    String cons = format(
        "%s: weight=%s type=alldiff %s;", constraint.getName(), weightExp, varExp.toString());
    return Arrays.asList(cons);
  }

  @Override
  public List<String> encode(Model model) {
    List<String> body = new ArrayList<>();

    for (Variable var : model.getVariables()) {
      body.addAll(encode(var));
    }

    for (Constraint cons : model.getConstraints()) {
      body.addAll(cons.encode(this));
    }
    return Collections.unmodifiableList(body);
  }
}
