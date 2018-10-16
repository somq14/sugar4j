package jp.ac.meiji.igusso.scheduling;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.expression.Expression;

import java.util.List;
import java.util.Map;

public final class FirstStepMethod extends Sugar4jMethod {
  private long timeout = -1;

  public FirstStepMethod(Map<String, String> options) {
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

      sugar4j.addAssumption(obj1, Expression.LE, ans1 - 1);
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
  }
}
