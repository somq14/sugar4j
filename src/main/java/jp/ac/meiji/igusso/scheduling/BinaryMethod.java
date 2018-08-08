package jp.ac.meiji.igusso.scheduling;

import static jp.ac.meiji.igusso.scheduling.Main.log;
import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.coptool.model.Constraint;
import jp.ac.meiji.igusso.coptool.model.Model;
import jp.ac.meiji.igusso.coptool.model.Variable;
import jp.ac.meiji.igusso.coptool.sat.IpasirSolver;
import jp.ac.meiji.igusso.coptool.sugar.Comparator;
import jp.ac.meiji.igusso.coptool.sugar.Model2SugarTranslator;
import jp.ac.meiji.igusso.coptool.sugar.Solution;
import jp.ac.meiji.igusso.coptool.sugar.Sugar4j;
import jp.kobe_u.sugar.expression.Expression;

import java.util.Map;

final class BinaryMethod {
  private Model model;
  private Model2SugarTranslator translator;
  private Sugar4j sugar4j;

  private Solution bestSolution;

  private int solveCount;
  private long timerBegin;
  private long timerEnd;

  BinaryMethod(Map<String, String> options) {}

  private void setup(SchedulingProblem problem) {
    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(problem);
    model = spe.encode();
    translator = Model2SugarTranslator.newInstance();
    sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));
  }

  private void translateModel() {
    log("Translating Model...");

    for (Variable variable : model.getVariables()) {
      sugar4j.addExpressions(translator.translate(variable));
    }

    for (Constraint constraint : model.getConstraints()) {
      sugar4j.addConstraints(translator.translate(constraint));
    }

    sugar4j.addConstraints(translator.translateObjective());

    log("Done");
  }

  private void encodeConstraints() throws Exception {
    log("Encoding Constraints...");

    sugar4j.update();

    log("Done");
  }

  private void searchSolution() throws Exception {
    log("Seaching Initial Solution");
    timerBegin = System.currentTimeMillis();

    solveCount = 0;
    bestSolution = null;

    Solution solution = sugar4j.solve();
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
        sugar4j.addConstraint(create(Expression.LE, obj, create(ub)));
        log("Found OBJ = %d", ub);
        bestSolution = solution;
      } else {
        lb = mid;
        sugar4j.addConstraint(create(Expression.GT, obj, create(lb)));
        log("Not Found");
      }
    }

    timerEnd = System.currentTimeMillis();
    log("Done");
  }

  void displaySolution() {
    if (bestSolution == null) {
      log("No Solution Found");
      return;
    }

    log("Solution");
    for (Variable variable : model.getVariables()) {
      log("%s = %d", variable.getName(),
          bestSolution.getIntMap().get(Expression.create(variable.getName())));
    }
    log("");

    log("Penalty (weight)");
    int ans = 0;
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isHard()) {
        continue;
      }
      log("%s (%d) = %d", constraint.getName(), constraint.getWeight(),
          bestSolution.getIntMap().get(translator.getPenaltyVariableOf(constraint)));

      ans += constraint.getWeight()
          * bestSolution.getIntMap().get(translator.getPenaltyVariableOf(constraint));
    }
    log("");

    log("Optimum Found OBJ = %d", ans);
    log("Solve Count = %d", solveCount);
    log("Cpu Time = %d [ms]", (timerEnd - timerBegin));
  }

  void solve(SchedulingProblem problem) throws Exception {
    setup(problem);
    translateModel();
    encodeConstraints();

    Thread hook = new Thread() {
      @Override
      public void run() {
        timerEnd = System.currentTimeMillis();
        displaySolution();
      }
    };
    Runtime.getRuntime().addShutdownHook(hook);
    searchSolution();
    Runtime.getRuntime().removeShutdownHook(hook);

    displaySolution();
  }
}
