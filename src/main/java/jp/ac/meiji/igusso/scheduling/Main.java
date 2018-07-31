package jp.ac.meiji.igusso.scheduling;

import jp.ac.meiji.igusso.coptool.model.Constraint;
import jp.ac.meiji.igusso.coptool.model.Model;
import jp.ac.meiji.igusso.coptool.model.Variable;
import jp.ac.meiji.igusso.coptool.sat.IpasirSolver;
import jp.ac.meiji.igusso.coptool.scop.Model2ScopTranslator;
import jp.ac.meiji.igusso.coptool.scop.Scop4j;
import jp.ac.meiji.igusso.coptool.sugar.Comparator;
import jp.ac.meiji.igusso.coptool.sugar.Model2SugarTranslator;
import jp.ac.meiji.igusso.coptool.sugar.Sugar4j;
import jp.kobe_u.sugar.expression.Expression;

import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public final class Main {
  static void log(String format, Object... objs) {
    System.out.println("[coptool] " + String.format(format, objs));
  }

  private static void usage() {
    log("Main File Method Options...");
    log("  File    : path to instance file (required)");
    log("  Method  : scop|linear|binary|incremental|hybrid|encode (required)");
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

  private static void solveWithScop(SchedulingProblem problem, Map<String, String> options)
      throws Exception {
    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(problem);
    Model model = spe.encode();

    log("Translating Constraints");
    Model2ScopTranslator translator = Model2ScopTranslator.newInstance();
    Scop4j scop4j = Scop4j.newInstance();
    for (Variable variable : model.getVariables()) {
      scop4j.addVariable(translator.translate(variable));
    }
    for (Constraint constraint : model.getConstraints()) {
      scop4j.addConstraint(translator.translate(constraint));
    }
    log("Done");

    log("Searching");
    scop4j.setTimeout(3600);
    final jp.ac.meiji.igusso.coptool.scop.Solution solution = scop4j.solve();
    log("Done");
    log("");

    log("Scop Log");
    List<String> logBody = Files.readAllLines(scop4j.getLogFile(), Charset.defaultCharset());
    for (String line : logBody) {
      log(line);
    }
    log("");

    if (solution.getHardPenalty() > 0) {
      log("Found No Feasible Solution");
      return;
    }

    log("Solution");
    for (Variable variable : model.getVariables()) {
      log("%s = %s", variable.getName(),
          solution.getSolution().get(translator.translate(variable)));
    }
    log("Penalty = %d", solution.getSoftPenalty());
    log("Cpu Time = %d [ms]", solution.getCpuTime());
    log("Cpu Time (Last Improved) = %d [ms]", solution.getLastImprovedCpuTime());
    log("Done");
  }

  private static void solveWithSugarLinear(SchedulingProblem problem, Map<String, String> options)
      throws Exception {
    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(problem);
    Model model = spe.encode();

    Model2SugarTranslator translator = Model2SugarTranslator.newInstance();
    Sugar4j sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));

    log("Translating Model...");
    for (Variable variable : model.getVariables()) {
      sugar4j.addExpressions(translator.translate(variable));
    }
    for (Constraint constraint : model.getConstraints()) {
      sugar4j.addConstraints(translator.translate(constraint));
    }
    sugar4j.addConstraints(translator.translateObjective());
    log("Done");

    log("Encoding Constraints...");
    sugar4j.update();
    log("Done");

    log("Searching Initial Solution...");
    int solveCount = 0;
    final long timerBegin = System.currentTimeMillis();
    jp.ac.meiji.igusso.coptool.sugar.Solution solution = sugar4j.solve();
    jp.ac.meiji.igusso.coptool.sugar.Solution bestSolution = null;
    solveCount++;
    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj = Expression.create("_P");
    int ans = solution.getIntMap().get(obj);
    log("Found OBJ = %d", ans);

    while (true) {
      log("Search OBJ <= %d", ans - 1);

      sugar4j.addAssumption(obj, Comparator.LE, ans - 1);
      solution = sugar4j.solve();
      solveCount++;

      if (!solution.isSat()) {
        log("Not Found");
        break;
      }
      ans = solution.getIntMap().get(obj);
      log("Found OBJ = %d", ans);
      bestSolution = solution;
    }
    final long timerEnd = System.currentTimeMillis();

    log("");
    log("Solution");
    for (Variable variable : model.getVariables()) {
      log("%s = %d", variable.getName(),
          bestSolution.getIntMap().get(Expression.create(variable.getName())));
    }
    log("");
    log("Penalty (weight)");
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isHard()) {
        continue;
      }
      log("%s (%d) = %d", constraint.getName(), constraint.getWeight(),
          bestSolution.getIntMap().get(translator.getPenaltyVariableOf(constraint)));
    }
    log("");
    log("Optimum Found OBJ = %d", ans);
    log("Solve Count = %d", solveCount);
    log("Cpu Time = %d [ms]", (timerEnd - timerBegin));
  }

  private static void solveWithSugarBinary(SchedulingProblem problem, Map<String, String> options)
      throws Exception {
    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(problem);
    Model model = spe.encode();

    Model2SugarTranslator translator = Model2SugarTranslator.newInstance();
    Sugar4j sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));

    log("Translating Model...");
    for (Variable variable : model.getVariables()) {
      sugar4j.addExpressions(translator.translate(variable));
    }
    for (Constraint constraint : model.getConstraints()) {
      sugar4j.addConstraints(translator.translate(constraint));
    }
    sugar4j.addConstraints(translator.translateObjective());
    log("Done");

    log("Encoding Constraints...");
    sugar4j.update();
    log("Done");

    log("Seaching Initial Solution");
    int solveCount = 0;
    final long timerBegin = System.currentTimeMillis();
    jp.ac.meiji.igusso.coptool.sugar.Solution solution = sugar4j.solve();
    jp.ac.meiji.igusso.coptool.sugar.Solution bestSolution = null;
    solveCount++;
    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj = Expression.create("_P");
    log("Found OBJ = %d", solution.getIntMap().get(obj));

    // (lb, ub]
    int lb = -1;
    int ub = solution.getIntMap().get(obj);
    while (ub - lb > 1) {
      log("Bound %d <= OBJ <= %d", lb + 1, ub);

      int mid = lb + (ub - lb) / 2;
      sugar4j.addAssumption(obj, Comparator.LE, mid);

      log("Searching OBJ <= %d", mid);
      solution = sugar4j.solve();
      solveCount++;

      if (solution.isSat()) {
        ub = solution.getIntMap().get(obj);
        log("Found OBJ = %d", ub);
        bestSolution = solution;
      } else {
        lb = mid;
        log("Not Found");
      }
    }

    final long timerEnd = System.currentTimeMillis();

    log("");
    log("Solution");
    for (Variable variable : model.getVariables()) {
      log("%s = %d", variable.getName(),
          bestSolution.getIntMap().get(Expression.create(variable.getName())));
    }
    log("");
    log("Penalty (weight)");
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isHard()) {
        continue;
      }
      log("%s (%d) = %d", constraint.getName(), constraint.getWeight(),
          bestSolution.getIntMap().get(translator.getPenaltyVariableOf(constraint)));
    }
    log("");
    log("Optimum Found OBJ = %d", ub);
    log("Solve Count = %d", solveCount);
    log("Cpu Time = %d [ms]", (timerEnd - timerBegin));
  }

  private static void solveByIncrementalMethod(
      SchedulingProblem problem, Map<String, String> options) throws Exception {
    new IncrementalMethod(options).solve(problem);
  }

  public static void solveByHybridMethod(SchedulingProblem problem, Map<String, String> options)
      throws Exception {
    new HybridMethod(options).solve(problem);
  }

  public static void encodeSugar(SchedulingProblem problem, Map<String, String> options)
      throws Exception {
    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(problem);
    Model model = spe.encode();

    Model2SugarTranslator translator = Model2SugarTranslator.newInstance();
    Sugar4j sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));

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
      Sugar4j sugar4j2 = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));
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
      Sugar4j sugar4j2 = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));
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
      sugar4j2.addConstraint(Expression.create(
          Expression.INT_DEFINITION, Expression.create("_P"), Expression.ZERO, Expression.create(penaltySum)));
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
        solveWithScop(sp, options);
      } break;
      case "linear": {
        solveWithSugarLinear(sp, options);
      } break;
      case "binary": {
        solveWithSugarBinary(sp, options);
      } break;
      case "incremental": {
        solveByIncrementalMethod(sp, options);
      } break;
      case "hybrid": {
        solveByHybridMethod(sp, options);
      } break;
      case "encode": {
        encodeSugar(sp, options);
      } break;
      default:
        usage();
    }
  }
}
