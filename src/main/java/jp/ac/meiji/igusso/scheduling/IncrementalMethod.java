package jp.ac.meiji.igusso.scheduling;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class IncrementalMethod extends Sugar4jMethod {
  public IncrementalMethod(Map<String, String> options) {}

  @Override
  protected void formulate() {
    sugar4j.addExpressions(formulator.getVariableDeclarations());
    sugar4j.addConstraints(formulator.getHardConstraints());
    sugar4j.addConstraints(formulator.getHeavyConstraints());
    sugar4j.addConstraints(formulator.generateHeavyObjective("OBJ_HEAVY"));
  }

  private void optimizeHeavyConstraints() throws SugarException {
    log("Searching Initial Solution...");
    Solution solution = invoke();
    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj = create("OBJ_HEAVY");
    int penalty = solution.getIntMap().get(obj);
    log("Found %s = %d", obj.stringValue(), penalty);

    while (penalty > 0) {
      log("Searching %s <= %d", obj.stringValue(), penalty - 1);
      sugar4j.addAssumption(obj, Comparator.LE, penalty - 1);
      solution = invoke();

      if (!solution.isSat()) {
        log("Not Found");
        break;
      }
      penalty = solution.getIntMap().get(obj);
      sugar4j.addConstraint(create(Expression.LE, obj, create(penalty)));
      log("Found %s = %d", obj.stringValue(), penalty);
      bestSolution = solution;
    }
    log("Optimum Found About Heavy Constraint OBJ_HEAVY = %d", penalty);

    Expression objBind = create(Expression.EQ, obj, create(penalty));
    log("Add Constraint To Bind %s", objBind.toString());
    sugar4j.addConstraint(objBind);
  }

  private void optimizeLightConstraints() throws SugarException {
    log("Sorting Light Constraints...");
    final List<Expression> penaltyVariables = new ArrayList<>(formulator.getPenaltyVariables());
    final Map<Expression, Integer> penaltyVariableWeight = formulator.getPenaltyVariableWeight();
    final Map<Expression, List<Expression>> penaltyVariableConstraint =
        formulator.getPenaltyVariableConstraint();

    Collections.sort(penaltyVariables, new java.util.Comparator<Expression>() {
      @Override
      public int compare(Expression v1, Expression v2) {
        int w1 = penaltyVariableWeight.get(v1);
        int w2 = penaltyVariableWeight.get(v2);
        if (w1 > w2) {
          return 1;
        }
        if (w1 < w2) {
          return -1;
        }
        return 0;
      }
    });
    Collections.reverse(penaltyVariables);

    int maxWeight = 0;
    for (Expression v : formulator.getPenaltyVariables()) {
      maxWeight = Math.max(maxWeight, penaltyVariableWeight.get(v));
    }
    log("Done");

    for (Expression penaltyVariable : penaltyVariables) {
      int weight = penaltyVariableWeight.get(penaltyVariable);
      if (weight == maxWeight) {
        continue;
      }

      log("Adding Constraint (Name = %s, Weight = %d)", penaltyVariable.stringValue(),
          penaltyVariableWeight.get(penaltyVariable));
      sugar4j.addConstraints(penaltyVariableConstraint.get(penaltyVariable));
      log("Done");

      log("Searching Initial Solution...");
      Solution solution = invoke();

      if (!solution.isSat()) {
        log("UNSAT (Something Wrong Happend)");
        return;
      }
      bestSolution = solution;

      int penalty = solution.getIntMap().get(penaltyVariable);
      log("Found %s = %d", penaltyVariable.stringValue(), penalty);

      while (penalty > 0) {
        log("Search %s <= %d", penaltyVariable.stringValue(), penalty - 1);
        sugar4j.addAssumption(penaltyVariable, Comparator.LE, penalty - 1);

        solution = invoke();
        if (!solution.isSat()) {
          log("Not Found");
          break;
        }
        penalty = solution.getIntMap().get(penaltyVariable);
        sugar4j.addConstraint(create(Expression.LE, penaltyVariable, create(penalty)));
        log("Found %s = %d", penaltyVariable.stringValue(), penalty);
        bestSolution = solution;
      }
      log("Complete To Improve %s = %d", penaltyVariable.stringValue(), penalty);

      Expression penaltyBind = create(Expression.EQ, penaltyVariable, create(penalty));
      log("Add Constraint To Bind %s", penaltyBind.toString());
      sugar4j.addConstraint(penaltyBind);
      log("Done");
    }
  }

  @Override
  protected void search() throws SugarException {
    log("---------------- Optimize Heavy Constraints ----------------");
    optimizeHeavyConstraints();
    log("Done");

    log("---------------- Optimize Lihgt Constraints ----------------");
    optimizeLightConstraints();
    log("Done");
  }
}
