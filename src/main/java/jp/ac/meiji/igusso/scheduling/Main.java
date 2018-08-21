package jp.ac.meiji.igusso.scheduling;

import jp.ac.meiji.igusso.model.Constraint;
import jp.ac.meiji.igusso.model.Model;
import jp.ac.meiji.igusso.model.Model2ScopTranslator;
import jp.ac.meiji.igusso.model.Model2SugarTranslator;
import jp.ac.meiji.igusso.model.Variable;
import jp.ac.meiji.igusso.scop4j.Scop4j;
import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.ac.meiji.igusso.sugar4j.DummySolver;
import jp.ac.meiji.igusso.sugar4j.Sugar4j;
import jp.kobe_u.sugar.expression.Expression;

import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Main {
  static void log(String format, Object... objs) {
    System.out.println("[coptool] " + String.format(format, objs));
  }

  private static void usage() {
    log("Main File Method Options...");
    log("  File    : path to instance file (required)");
    log("  Method  : scop|linear|binary|incremental|hybrid|2step|3step|encode (required)");
    log("  Options : parameters for algorithms");
  }

  private static Map<String, String> parseOptions(String[] args) {
    Map<String, String> map = new HashMap<>();
    for (int i = 2; i < args.length; i++) {
      if (!args[i].matches("\\w+=\\w+")) {
        throw new IllegalArgumentException("name=value");
      }
      String[] values = args[i].split("=");
      map.put(values[0], values[1]);
    }
    return map;
  }

  public static void encodeSugar(SchedulingProblem problem, Map<String, String> options)
      throws Exception {
    ModelFormulator mf = new ModelFormulator(problem);
    Model model = mf.encode();

    Model2SugarTranslator translator = Model2SugarTranslator.newInstance();
    Sugar4j sugar4j = Sugar4j.newInstance(DummySolver.getInstance());

    for (Variable variable : model.getVariables()) {
      sugar4j.addExpressions(translator.translate(variable));
    }
    System.out.println(String.format(
        "; %20s, %6d, %6d", "base", sugar4j.getSatVariablesCount(), sugar4j.getSatClausesCount()));

    for (Constraint constraint : model.getConstraints()) {
      int prevVariablesCount = sugar4j.getSatVariablesCount();
      int prevClausesCount = sugar4j.getSatClausesCount();
      sugar4j.addConstraints(translator.translate(constraint));
      if (constraint.isHard()) {
        System.out.println(String.format("; %20s, %6d, %6d", constraint.getName(),
            sugar4j.getSatVariablesCount() - prevVariablesCount,
            sugar4j.getSatClausesCount() - prevClausesCount));
      } else {
        System.out.println(String.format("; %20s, %6d, %6d, %3d, %3d", constraint.getName(),
            sugar4j.getSatVariablesCount() - prevVariablesCount,
            sugar4j.getSatClausesCount() - prevClausesCount, constraint.getWeight(),
            constraint.getPenaltyUpperBound()));
      }
    }

    System.out.println(
        String.format("; ----------------------------------------------------------------"));
    System.out.println(String.format("; %20s, %6d, %6d", "constraints",
        sugar4j.getSatVariablesCount(), sugar4j.getSatClausesCount()));

    int prevVariablesCount = sugar4j.getSatVariablesCount();
    int prevClausesCount = sugar4j.getSatClausesCount();
    sugar4j.addConstraints(translator.translateObjective());
    System.out.println(String.format("; %20s, %6d, %6d", "obj",
        sugar4j.getSatVariablesCount() - prevVariablesCount,
        sugar4j.getSatClausesCount() - prevClausesCount));

    //
    {
      Model2SugarTranslator translator2 = Model2SugarTranslator.newInstance();
      Sugar4j sugar4j2 = Sugar4j.newInstance(DummySolver.getInstance());
      for (Variable variable : model.getVariables()) {
        sugar4j2.addExpressions(translator2.translate(variable));
      }
      int maxWeight = 0;
      for (Constraint constraint : model.getConstraints()) {
        if (constraint.isSoft()) {
          maxWeight = Math.max(maxWeight, constraint.getWeight());
        }
      }
      for (Constraint constraint : model.getConstraints()) {
        if (constraint.isHard() || constraint.getWeight() != maxWeight) {
          sugar4j2.addConstraints(translator2.translate(constraint));
        }
      }
      int prevVariablesCount2 = sugar4j2.getSatVariablesCount();
      int prevClausesCount2 = sugar4j2.getSatClausesCount();
      sugar4j2.addConstraints(translator2.translateObjective());
      System.out.println(String.format("; %20s, %6d, %6d", "lightObj",
          sugar4j2.getSatVariablesCount() - prevVariablesCount2,
          sugar4j2.getSatClausesCount() - prevClausesCount2));
    }

    //
    {
      Model2SugarTranslator translator2 = Model2SugarTranslator.newInstance();
      Sugar4j sugar4j2 = Sugar4j.newInstance(DummySolver.getInstance());
      for (Variable variable : model.getVariables()) {
        sugar4j2.addExpressions(translator2.translate(variable));
      }
      int maxWeight = 0;
      for (Constraint constraint : model.getConstraints()) {
        if (constraint.isSoft()) {
          maxWeight = Math.max(maxWeight, constraint.getWeight());
        }
      }
      for (Constraint constraint : model.getConstraints()) {
        sugar4j2.addConstraints(translator2.translate(constraint));
      }
      List<Expression> terms = new ArrayList<>();
      int penaltySum = 0;
      for (Constraint constraint : model.getConstraints()) {
        if (constraint.getWeight() == maxWeight) {
          terms.add(translator2.getPenaltyVariableOf(constraint));
          penaltySum += constraint.getPenaltyUpperBound();
        }
      }
      int prevVariablesCount2 = sugar4j2.getSatVariablesCount();
      int prevClausesCount2 = sugar4j2.getSatClausesCount();
      sugar4j2.addConstraint(Expression.create(Expression.INT_DEFINITION, Expression.create("_P"),
          Expression.ZERO, Expression.create(penaltySum)));
      sugar4j2.addConstraint(Expression.create(
          Expression.EQ, Expression.create("_P"), Expression.create(Expression.ADD, terms)));
      System.out.println(String.format("; %20s, %6d, %6d", "heavyObj",
          sugar4j2.getSatVariablesCount() - prevVariablesCount2,
          sugar4j2.getSatClausesCount() - prevClausesCount2));
    }

    System.out.println(String.format(
        "; %20s, %6d, %6d", "total", sugar4j.getSatVariablesCount(), sugar4j.getSatClausesCount()));

    System.out.println(translator);
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      usage();
      System.exit(1);
    }
    Map<String, String> options = parseOptions(args);

    String fileName = args[0];
    SchedulingProblemParser spp = new SchedulingProblemParser(new FileReader(fileName));
    SchedulingProblem sp = spp.parse();

    switch (args[1]) {
      case "scop": {
        new Scop4jMethod(options).solve(sp);
      } break;
      case "linear": {
        new LinearMethod(options).solve(sp);
      } break;
      case "binary": {
        new BinaryMethod(options).solve(sp);
      } break;
      case "2step": {
        new TwoStepMethod(options).solve(sp);
      } break;
      case "incremental": {
        new IncrementalMethod(options).solve(sp);
      } break;
      case "hybrid": {
        new HybridMethod(options).solve(sp);
      } break;
      case "encode": {
        encodeSugar(sp, options);
      } break;
      default:
        usage();
    }
  }
}
