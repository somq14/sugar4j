package jp.ac.meiji.igusso.scheduling.ikegami;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.scop4j.Scop4j;
import jp.ac.meiji.igusso.scop4j.Variable;
import jp.ac.meiji.igusso.sugar4j.Sugar4j;
import jp.ac.meiji.igusso.sugar4j.IpasirSolver;
import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.kobe_u.sugar.expression.Expression;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.util.List;
import java.io.IOException;

public final class Hybrid {
  public static void log(String format, Object... objs) {
    System.out.printf(format, objs);
    System.out.println();
  }

  public static void main(String[] args) throws Exception {
    log("Formulation...");
    Problem problem = Problem.of(new java.io.File(args[0]));
    Scop4jFormulator scopFormulator = new Scop4jFormulator(problem);
    Sugar4jFormulator sugarFormulator = new Sugar4jFormulator(problem);
    log("Done");
    log("");

    log("Scop4j Encoding...");
    Scop4j scop4j = Scop4j.newInstance();
    scop4j.addVariables(scopFormulator.getVariables());
    scop4j.addConstraints(scopFormulator.getAllConstraints());
    scop4j.setTimeout(60);
    log("Done");
    log("");

    log("Sugar4j Encoding...");
    Sugar4j sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));
    sugar4j.addExpressions(sugarFormulator.getAllExpressions());
    sugar4j.update();
    log("Done");
    log("");

    log("Solve With Scop...");
    jp.ac.meiji.igusso.scop4j.Solution scopSol = scop4j.solve();
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

    log("Scop Solution...");
    for (Variable variable : scopFormulator.getVariables()) {
      log("%s = %s", variable.getName(), scopSol.getSolution().get(variable));
    }
    log("Penalty = %d", scopSol.getSoftPenalty());
    log("Cpu Time = %d [ms]", scopSol.getCpuTime());
    log("Cpu Time (Last Improved) = %d [ms]", scopSol.getLastImprovedCpuTime());
    log("Done");
    log("");

    log("Add Assumption...");
    for (int i = 0; i < problem.getStaffs().size(); i++) {
      for (int d = 0; d < problem.getLength(); d++) {
        int t = Integer.valueOf(scopSol.getSolution().get(
            Variable.of(String.format("x_i%02d_d%02d", i, d), problem.getShifts().size() + 1)));
        sugar4j.addAssumption(
            create(String.format("x_i%02d_d%02d_t%02d", i, d, t)), Comparator.EQ, 1);
      }
    }
    log("Done");
    log("");

    log("Solve With Sugar...");
    long beginTime = System.currentTimeMillis();
    int ans = scopSol.getSoftPenalty() + 1;
    jp.ac.meiji.igusso.sugar4j.Solution bestSol = null;
    while (ans > 0) {
      sugar4j.addAssumption(create("OBJ"), Comparator.LE, ans - 1);
      jp.ac.meiji.igusso.sugar4j.Solution sol = sugar4j.solve();
      if (!sol.isSat()) {
        break;
      }
      ans = sol.getIntMap().get(create("OBJ"));
      bestSol = sol;
      sugar4j.addConstraint(create(Expression.LE, create("OBJ"), create(ans)));
    }
    long endTime = System.currentTimeMillis();
    log("Done");
    log("");

    log("Solution = %d", ans);
    log("Sat Milli = %d", (endTime - beginTime));
  }
}
