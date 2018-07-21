package jp.ac.meiji.igusso.coptool;

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

    String body = String.format("variable %s in {%s}", variable.getName(), domainExp.toString());
    return Arrays.asList(body);
  }

  @Override
  public List<String> encode(ConflictPointConstraint constraint) {
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

      termsBuilder.append(String.format("%d(%s, %d)", coeff, var.getName(), val));
      termsBuilder.append(' ');
    }
    String termsExp = termsBuilder.toString();

    String cons = null;

    switch (constraint.getOp()) {
      case EQ:
        cons = String.format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, "=", constraint.getRhs());
        break;

      case LE:
        cons = String.format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, "<=", constraint.getRhs());
        break;

      case LT:
        cons = String.format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, "<=", constraint.getRhs() - 1);
        break;

      case GE:
        cons = String.format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
            termsExp, ">=", constraint.getRhs());
        break;

      case GT:
        cons = String.format("%s: weight=%s type=linear %s %s %d", constraint.getName(), weightExp,
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
    String cons = String.format(
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
