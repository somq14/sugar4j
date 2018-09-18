package jp.ac.meiji.igusso.scheduling.ikegami;

import static jp.kobe_u.sugar.expression.Expression.create;

import java.io.File;

import jp.ac.meiji.igusso.sugar4j.Sugar4j;
import jp.ac.meiji.igusso.sugar4j.IpasirSolver;
import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.kobe_u.sugar.expression.Expression;

public class Sugar4jMethod {
  public static void main(String[] args) throws Exception {
    Problem problem = Problem.of(new File(args[0]));
    Sugar4jFormulator formulator = new Sugar4jFormulator(problem);

    Sugar4j sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));
    sugar4j.addExpressions(formulator.getAllExpressions());

    long encodeBeginTime = System.currentTimeMillis();
    sugar4j.update();
    long encodeEndTime = System.currentTimeMillis();
    System.out.println("ENCODE TIME = " + (encodeEndTime - encodeBeginTime));

    Expression obj = create("OBJ");
    for (int ans = 0; ans <= 4; ans++) {
      sugar4j.addAssumption(obj, Comparator.LE, ans);

      long beginTime = System.currentTimeMillis();
      Solution sol = sugar4j.solve();
      long endTime = System.currentTimeMillis();

      System.out.println("TIME = " + (endTime - beginTime));
      System.out.println(sol.isSat() ? "SATISFIABLE" : "UNSATISFIABLE");
    }
  }
}
