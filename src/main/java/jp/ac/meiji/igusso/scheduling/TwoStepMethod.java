package jp.ac.meiji.igusso.scheduling;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.expression.Expression;

import java.util.List;
import java.util.Map;

public final class TwoStepMethod extends Sugar4jMethod {
  private long timeout = -1;

  public TwoStepMethod(Map<String, String> options) {
    if (options.containsKey("timeout")) {
      this.timeout = Math.max(-1, Long.valueOf(options.get("timeout")));
    }
  }

  @Override
  protected void formulate() {
    sugar4j.addExpressions(formulator.getVariableDeclarations());
    sugar4j.addConstraints(formulator.getHardConstraints());
    sugar4j.addConstraints(formulator.getHeavyConstraints());
    sugar4j.addConstraints(formulator.generateHeavyObjective("OBJ1"));
  }

  @Override
  protected void search() throws SugarException {
    log("-------------------------------- 1st STEP --------------------------------");

    Solution solution = invoke(timeout);
    if (solution.isTimeout()) {
      throw new SugarException("timeout!");
    }
    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj1 = create("OBJ1");
    int ans1 = solution.getIntMap().get(obj1);

    while (true) {
      log("Search OBJ <= %d", ans1 - 1);

      sugar4j.addAssumption(obj1, Comparator.LE, ans1 - 1);
      solution = invoke(timeout);

      if (solution.isTimeout()) {
        throw new SugarException("timeout!");
      }
      if (!solution.isSat()) {
        log("Not Found");
        break;
      }

      ans1 = solution.getIntMap().get(obj1);
      sugar4j.addConstraint(create(Expression.LE, obj1, create(ans1)));
      log("Found OBJ = %d", ans1);
      bestSolution = solution;
    }

    Expression obj1Bind = create(Expression.LE, obj1, create(ans1));
    log("Add Constraint: %s", obj1Bind.toString());
    sugar4j.addConstraint(obj1Bind);
    log("Done");

    log("-------------------------------- 2nd STEP --------------------------------");

    sugar4j.addConstraints(formulator.getLightConstraints());
    sugar4j.addConstraints(formulator.generateLightObjective("OBJ2"));
    solution = invoke(timeout);

    if (solution.isTimeout()) {
      throw new SugarException("timeout!");
    }
    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj2 = create("OBJ2");
    int ans2 = solution.getIntMap().get(obj2);
    log("Found OBJ = %d", ans2);

    while (true) {
      log("Search OBJ <= %d", ans2 - 1);

      sugar4j.addAssumption(obj2, Comparator.LE, ans2 - 1);
      solution = invoke(timeout);

      if (solution.isTimeout()) {
        throw new SugarException("timeout!");
      }
      if (!solution.isSat()) {
        log("Not Found");
        break;
      }

      ans2 = solution.getIntMap().get(obj2);
      sugar4j.addConstraint(create(Expression.LE, obj2, create(ans2)));
      log("Found OBJ = %d", ans2);
      bestSolution = solution;
    }
    log("Done");
  }
}
