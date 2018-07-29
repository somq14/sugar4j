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
import java.nio.file.Paths;

public final class Main {
  static void log(String format, Object... objs) {
    System.out.println("[coptool] " + String.format(format, objs));
  }

  private static void usage() {
    log("Main InstanceFile [scop|linear|binary|incremental|hybrid]");
  }

  private static void solveWithScop(SchedulingProblem problem) {
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
    scop4j.setTimeout(180);
    scop4j.setLogFile(Paths.get("scop_log.txt"));
    jp.ac.meiji.igusso.coptool.scop.Solution solution = scop4j.solve();
    log("Done");

    log("Solution");

    if (solution.getHardPenalty() > 0) {
      log("Found No Feasible Solution");
      return;
    }

    for (Variable variable : model.getVariables()) {
      log("%s = %s", variable.getName(),
          solution.getSolution().get(translator.translate(variable)));
    }
    log("Penalty = %d", solution.getSoftPenalty());
    log("Cpu Time = %d [ms]", solution.getCpuTime());
    log("Cpu Time (Last Improved) = %d [ms]", solution.getLastImprovedCpuTime());
    log("Done");
  }

  private static void solveWithSugarLinear(SchedulingProblem problem) throws Exception {
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

  private static void solveWithSugarBinary(SchedulingProblem problem) throws Exception {
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

  private static void solveByIncrementalMethod(SchedulingProblem problem) throws Exception {
    new IncrementalMethod().solve(problem);
  }

  public static void solveByHybridMethod(SchedulingProblem problem) throws Exception {
    new HybridMethod().solve(problem);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      usage();
      System.exit(1);
    }
    jp.kobe_u.sugar.converter.Converter.INCREMENTAL_PROPAGATION = false;

    String fileName = args[0];
    SchedulingProblemParser spp = new SchedulingProblemParser(new FileReader(fileName));
    SchedulingProblem sp = spp.parse();

    if ("scop".equals(args[1])) {
      solveWithScop(sp);
    } else if ("binary".equals(args[1])) {
      solveWithSugarBinary(sp);
    } else if ("linear".equals(args[1])) {
      solveWithSugarLinear(sp);
    } else if ("incremental".equals(args[1])) {
      solveByIncrementalMethod(sp);
    } else if ("hybrid".equals(args[1])) {
      solveByHybridMethod(sp);
    } else {
      usage();
    }
  }
}
