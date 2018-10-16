package jp.ac.meiji.igusso.scheduling;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.expression.Expression;

import java.util.Map;

public final class LinearMethod extends Sugar4jMethod {
  private long timeout = -1;

  public LinearMethod(Map<String, String> options) {
    if (options.containsKey("timeout")) {
      this.timeout = Math.max(-1, Long.valueOf(options.get("timeout")));
    }
  }

  @Override
  protected void search() throws SugarException {
    log("Searching Initial Solution...");

    Solution solution = invoke(timeout);

    if (solution.isTimeout()) {
      throw new SugarException("timeout!");
    }
    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj = create("OBJ");
    int ans = solution.getIntMap().get(obj);
    log("Found OBJ = %d", ans);

    while (true) {
      log("Search OBJ <= %d", ans - 1);

      sugar4j.addAssumption(obj, Expression.LE, ans - 1);
      solution = invoke(timeout);

      if (solution.isTimeout()) {
        throw new SugarException("timeout!");
      }
      if (!solution.isSat()) {
        log("Not Found");
        break;
      }

      ans = solution.getIntMap().get(obj);
      sugar4j.addConstraint(create(Expression.LE, obj, create(ans)));
      log("Found OBJ = %d", ans);
      bestSolution = solution;
    }
  }
}
