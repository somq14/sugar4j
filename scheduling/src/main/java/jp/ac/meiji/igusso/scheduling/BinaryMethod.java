package jp.ac.meiji.igusso.scheduling;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.expression.Expression;

import java.util.Map;

public final class BinaryMethod extends Sugar4jMethod {
  private long timeout = -1;

  public BinaryMethod(Map<String, String> options) {
    if (options.containsKey("timeout")) {
      this.timeout = Math.max(-1, Long.valueOf(options.get("timeout")));
    }
  }

  @Override
  protected void search() throws SugarException {
    log("Seaching Initial Solution");

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
    log("Found OBJ = %d", solution.getIntMap().get(obj));

    // (lb, ub]
    int lb = -1;
    int ub = solution.getIntMap().get(obj);
    while (ub - lb > 1) {
      log("Bound %d <= OBJ <= %d", lb + 1, ub);

      int mid = lb + (ub - lb) / 2;
      sugar4j.addAssumption(obj, Comparator.LE, mid);

      log("Searching OBJ <= %d", mid);
      solution = invoke(timeout);

      if (solution.isTimeout()) {
        throw new SugarException("timeout!");
      }
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
  }
}
