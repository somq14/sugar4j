package jp.ac.meiji.igusso.scheduling;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.scop4j.Constraint;
import jp.ac.meiji.igusso.scop4j.Scop4j;
import jp.ac.meiji.igusso.scop4j.Variable;
import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.expression.Expression;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Java CHECKSTYLE:OFF LocalVariableName
public final class HogeMethod extends Sugar4jMethod {
  private int scopTimeout = -1;
  private int timeout = -1;

  public HogeMethod(Map<String, String> options) {
    if (options.containsKey("scopTimeout")) {
      this.scopTimeout = Integer.valueOf(options.get("scopTimeout"));
    }
    if (options.containsKey("timeout")) {
      this.timeout = Integer.valueOf(options.get("timeout"));
    }

    log("timeout = %d", timeout);
    log("scopTimeout = %d", scopTimeout);
  }

  @Override
  protected void formulate() {
    sugar4j.addExpressions(formulator.getVariableDeclarations());
    sugar4j.addConstraints(formulator.getHardConstraints());
    sugar4j.addConstraints(formulator.getHeavyConstraints());
    sugar4j.addConstraints(formulator.generateHeavyObjective("OBJ_HEAVY"));
    sugar4j.addConstraints(formulator.getLightConstraints());
    sugar4j.addConstraints(formulator.generateLightObjective("OBJ_LIGHT"));
  }

  private void searchInitialSolution(SchedulingProblem problem) throws SugarException {
    Scop4j scop4j = Scop4j.newInstance();
    Scop4jFormulator scopFormulator = new Scop4jFormulator(problem);
    scop4j.addVariables(scopFormulator.generateVariables());
    scop4j.addConstraints(scopFormulator.generateAllConstraints());
    scop4j.setTimeout(scopTimeout);

    log("Searching Solution...");
    final jp.ac.meiji.igusso.scop4j.Solution solution = scop4j.solve();
    log("Done");
    log("");

    log("Scop Log");
    try {
      List<String> logBody = Files.readAllLines(scop4j.getLogFile(), Charset.defaultCharset());
      for (String line : logBody) {
        log(line);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    log("Done");
    log("");

    if (solution.getHardPenalty() > 0) {
      log("Found No Feasible Solution");
      return;
    }

    log("Scop Solution");
    for (Variable variable : scopFormulator.generateVariables()) {
      log("%s = %s", variable.getName(), solution.getSolution().get(variable));
    }
    log("Penalty = %d", solution.getSoftPenalty());
    log("Cpu Time = %d [ms]", solution.getCpuTime());
    log("Cpu Time (Last Improved) = %d [ms]", solution.getLastImprovedCpuTime());
    log("");

    log("Add SCOP Solution To SAT Solver As Assumtion...");
    int maxWeight = 0;
    for (Constraint cons : scopFormulator.generateAllConstraints()) {
      if (cons.isSoft()) {
        maxWeight = Math.max(maxWeight, cons.getWeight());
      }
    }

    int violation = 0;
    for (Constraint cons : scopFormulator.generateAllConstraints()) {
      if (cons.isSoft() && cons.getWeight() == maxWeight
          && solution.getViolatedConstraints().containsKey(cons)) {
        violation += solution.getViolatedConstraints().get(cons);
      }
    }
    Expression heavyBind = create(Expression.LE, create("OBJ_HEAVY"), create(violation));
    log("%s", heavyBind.toString());
    sugar4j.addExpression(heavyBind);

    Expression lightBind = create(Expression.LE, create("OBJ_LIGHT"),
        create(solution.getSoftPenalty() - violation * maxWeight));
    log("%s", lightBind.toString());
    sugar4j.addConstraint(lightBind);

    SchedulingProblemParameter param = new SchedulingProblemParameter(problem);
    int[] I = param.getI();
    int[] D = param.getD();
    int[] T = param.getT();
    for (int i : I) {
      for (int d : D) {
        int t = Integer.valueOf(solution.getSolution().get(
            Variable.of(String.format("x_i%02d_d%02d", i, d), T.length)));
        sugar4j.addAssumption(
            create(String.format("x_i%02d_d%02d_t%02d", i, d, t)), Comparator.EQ, 1);
      }
    }
    log("Done");
  }

  private void improveSolution() throws SugarException {
    log("Searching First Solution...");
    jp.ac.meiji.igusso.sugar4j.Solution sugarSolution = invoke(timeout);
    if (sugarSolution.isTimeout()) {
      throw new SugarException("timeout!");
    }
    if (!sugarSolution.isSat()) {
      log("UNSAT (Something Wrong Happend)");
      return;
    }
    bestSolution = sugarSolution;
    log("OBJ = %d", formulator.evaluateSolution(bestSolution.getIntMap()));
    log("Done");

    sugarSolution = invoke(timeout);

    if (sugarSolution.isTimeout()) {
      throw new SugarException("timeout!");
    }
    if (!sugarSolution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = sugarSolution;

    Expression obj = create("OBJ_LIGHT");
    int ans = sugarSolution.getIntMap().get(obj);
    log("Found OBJ = %d", ans);

    while (true) {
      log("Search OBJ <= %d", ans - 1);

      sugar4j.addAssumption(obj, Comparator.LE, ans - 1);
      sugarSolution = invoke(timeout);

      if (sugarSolution.isTimeout()) {
        throw new SugarException("timeout!");
      }
      if (!sugarSolution.isSat()) {
        log("Not Found");
        break;
      }

      ans = sugarSolution.getIntMap().get(obj);
      sugar4j.addConstraint(create(Expression.LE, obj, create(ans)));
      log("Found OBJ = %d", ans);
      bestSolution = sugarSolution;
    }
    log("Done");
  }

  @Override
  protected void search() throws SugarException {
    log("---------------- Search Initial Solution With SCOP ----------------");
    searchInitialSolution(problem);
    log("Done");

    log("---------------- Improve Solution With SAT Solver ----------------");
    improveSolution();
    log("Done");
  }
}
// Java CHECKSTYLE:ON LocalVariableName
