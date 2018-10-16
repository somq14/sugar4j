package jp.ac.meiji.igusso.scheduling;

import jp.ac.meiji.igusso.model.Constraint;
import jp.ac.meiji.igusso.model.Model;
import jp.ac.meiji.igusso.model.Model2ScopTranslator;
import jp.ac.meiji.igusso.model.Model2SugarTranslator;
import jp.ac.meiji.igusso.model.Variable;
import jp.ac.meiji.igusso.scop4j.Scop4j;
import jp.ac.meiji.igusso.sugar4j.DummySolver;
import jp.ac.meiji.igusso.sugar4j.Sugar4j;
import jp.kobe_u.sugar.expression.Expression;

import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
    log("  Method  : scop|linear|binary|1step|2step|incremental|hybrid|encode (required)");
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
    Sugar4jFormulator formulator = new Sugar4jFormulator(problem);
    Sugar4j sugar4j = Sugar4j.newInstance(DummySolver.getInstance());

    System.out.println(
        String.format("; %-12s, %8s, %8s, %8s", "NAME", "NUM", "VARIABLE", "CLAUSE"));

    sugar4j.addExpressions(formulator.getVariableDeclarations());
    System.out.println(String.format("; %-12s, %8d, %8d, %8d", "variable",
        formulator.getVariableDeclarations().size(), sugar4j.getSatVariablesCount(),
        sugar4j.getSatClausesCount()));

    List<List<Expression>> constraints = Arrays.asList(formulator.getConstraint1(),
        formulator.getConstraint2(), formulator.getConstraint3(), formulator.getConstraint4(),
        formulator.getConstraint5(), formulator.getConstraint6(), formulator.getConstraint7(),
        formulator.getConstraint8(), formulator.getConstraint9(), formulator.getConstraint11(),
        formulator.getConstraint12(), formulator.getConstraint13(), formulator.getConstraint14());
    List<String> constraintsName = Arrays.asList("cons1", "cons2", "cons3", "cons4", "cons5",
        "cons6", "cons7", "cons8", "cons9", "cons11", "cons12", "cons13", "cons14");

    int sumOfConstraints = 0;
    for (int i = 0; i < constraints.size(); i++) {
      int prevVariablesCount = sugar4j.getSatVariablesCount();
      int prevClausesCount = sugar4j.getSatClausesCount();
      sugar4j.addConstraints(constraints.get(i));
      System.out.println(String.format("; %-12s, %8d, %8d, %8d", constraintsName.get(i),
          constraints.get(i).size(), sugar4j.getSatVariablesCount() - prevVariablesCount,
          sugar4j.getSatClausesCount() - prevClausesCount));

      sumOfConstraints += constraints.get(i).size();
    }

    System.out.println(
        String.format("; ----------------------------------------------------------------"));
    System.out.println(String.format("; %-12s, %8d, %8d, %8d", "all", sumOfConstraints,
        sugar4j.getSatVariablesCount(), sugar4j.getSatClausesCount()));

    {
      Sugar4j sugar4j2 = Sugar4j.newInstance(DummySolver.getInstance());
      sugar4j2.addConstraints(formulator.getVariableDeclarations());
      sugar4j2.addConstraints(formulator.getHardConstraints());
      sugar4j2.addConstraints(formulator.getHeavyConstraints());

      int prevVariablesCount = sugar4j2.getSatVariablesCount();
      int prevClausesCount = sugar4j2.getSatClausesCount();
      sugar4j2.addConstraints(formulator.generateHeavyObjective("OBJ"));
      System.out.println(String.format("; %-12s, %8d, %8d, %8d", "heavy obj", -1,
          sugar4j2.getSatVariablesCount() - prevVariablesCount,
          sugar4j2.getSatClausesCount() - prevClausesCount));
    }

    {
      Sugar4j sugar4j2 = Sugar4j.newInstance(DummySolver.getInstance());
      sugar4j2.addConstraints(formulator.getVariableDeclarations());
      sugar4j2.addConstraints(formulator.getHardConstraints());
      sugar4j2.addConstraints(formulator.getLightConstraints());

      int prevVariablesCount = sugar4j2.getSatVariablesCount();
      int prevClausesCount = sugar4j2.getSatClausesCount();
      sugar4j2.addConstraints(formulator.generateLightObjective("OBJ"));
      System.out.println(String.format("; %-12s, %8d, %8d, %8d", "light obj", -1,
          sugar4j2.getSatVariablesCount() - prevVariablesCount,
          sugar4j2.getSatClausesCount() - prevClausesCount));
    }
    System.out.println(formulator);
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
      case "1step": {
        new FirstStepMethod(options).solve(sp);
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
      case "hoge": {
        new HogeMethod(options).solve(sp);
      } break;
      case "encode": {
        encodeSugar(sp, options);
      } break;
      default:
        usage();
    }
  }
}
