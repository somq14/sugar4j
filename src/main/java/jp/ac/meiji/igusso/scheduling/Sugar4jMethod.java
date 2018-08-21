package jp.ac.meiji.igusso.scheduling;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.model.Constraint;
import jp.ac.meiji.igusso.model.Model2SugarTranslator;
import jp.ac.meiji.igusso.model.Model;
import jp.ac.meiji.igusso.model.Variable;
import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.ac.meiji.igusso.sugar4j.IpasirSolver;
import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.ac.meiji.igusso.sugar4j.Sugar4j;
import jp.kobe_u.sugar.expression.Expression;
import jp.kobe_u.sugar.SugarException;
import lombok.NonNull;

import java.util.Map;

public abstract class Sugar4jMethod {
  public static void log(String format, Object... objs) {
    System.out.println("c " + String.format(format, objs));
  }

  protected SchedulingProblem problem;
  protected Sugar4jFormulator formulator;
  protected Sugar4j sugar4j;
  protected Solution bestSolution;

  private int solveCount;
  private long timerBegin;
  private long timerEnd;

  public Sugar4jMethod() {}

  private void initialize(SchedulingProblem problem) {
    this.problem = problem;
    this.formulator = new Sugar4jFormulator(problem);
    this.sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));

    this.bestSolution = null;
    this.solveCount = 0;
    this.timerBegin = 0;
    this.timerEnd = 0;
  }

  protected void formulate() {
    sugar4j.addExpressions(formulator.getVariableDeclarations());
    sugar4j.addConstraints(formulator.getHardConstraints());
    sugar4j.addConstraints(formulator.getSoftConstraints());
    sugar4j.addConstraints(formulator.generateObjective());
  }

  protected final void encode() throws SugarException {
    sugar4j.update();
  }

  protected final Solution invoke() throws SugarException {
    return invoke(-1);
  }

  protected final Solution invoke(long timeout) throws SugarException {
    log("%dth SAT Solver Invocation", ++solveCount);
    return sugar4j.solve();
  }

  protected abstract void search() throws SugarException;

  protected final void display() {
    if (bestSolution == null) {
      log("No Solution Found");
      return;
    }

    Map<Expression, Integer> sol = bestSolution.getIntMap();

    log("Solution");
    for (Expression variable : formulator.getVariables()) {
      log("%s = %d", variable.stringValue(), sol.get(variable));
    }
    log("");

    log("Penalty (weight)");
    int ans = 0;
    for (Expression variable : formulator.getPenaltyVariables()) {
      log("%s (%3d) = %d", variable.stringValue(),
          formulator.getPenaltyVariableWeight().get(variable), sol.get(variable));
    }
    log("");

    log("Best OBJ = %d", formulator.evaluateSolution(sol));
    log("Solve Count = %d", solveCount);
    log("Cpu Time = %d [ms]", (timerEnd - timerBegin));
  }

  public final void solve(@NonNull SchedulingProblem problem) throws SugarException {
    initialize(problem);

    log("Generating Constraints...");
    formulate();
    log("Done");

    log("Encoding Constraints...");
    encode();
    log("Done");

    Thread hook = new Thread() {
      @Override
      public void run() {
        timerEnd = System.currentTimeMillis();

        log("Interrupted!");
        display();
      }
    };

    Runtime.getRuntime().addShutdownHook(hook);
    timerBegin = System.currentTimeMillis();

    search();

    Runtime.getRuntime().removeShutdownHook(hook);
    timerEnd = System.currentTimeMillis();

    display();
  }
}
