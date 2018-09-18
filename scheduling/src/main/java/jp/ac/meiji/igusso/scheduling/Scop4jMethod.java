package jp.ac.meiji.igusso.scheduling;

import jp.ac.meiji.igusso.scop4j.Scop4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public final class Scop4jMethod {
  public static void log(String format, Object... objs) {
    System.out.println("# " + String.format(format, objs));
  }

  private int timeout = -1;

  public Scop4jMethod(Map<String, String> options) {
    this.timeout = options.containsKey("timeout") ? Integer.valueOf(options.get("timeout")) : -1;
  }

  public void solve(SchedulingProblem problem) throws IOException {
    log("Generating Constraints");
    Scop4jFormulator formulator = new Scop4jFormulator(problem);
    Scop4j scop4j = Scop4j.newInstance();
    scop4j.addVariables(formulator.generateVariables());
    scop4j.addConstraints(formulator.generateAllConstraints());
    log("Done");
    log("");

    log("Searching...");
    scop4j.setTimeout(timeout);
    final jp.ac.meiji.igusso.scop4j.Solution solution = scop4j.solve();
    log("Done");
    log("");

    log("Scop Log");
    List<String> logBody = Files.readAllLines(scop4j.getLogFile(), Charset.defaultCharset());
    for (String line : logBody) {
      log(line);
    }
    log("");

    if (solution.getHardPenalty() > 0) {
      log("Found No Feasible Solution");
      return;
    }

    log("Solution");
    for (jp.ac.meiji.igusso.scop4j.Variable variable : scop4j.getVariables()) {
      log("%s = %s", variable.getName(), solution.getSolution().get(variable));
    }
    log("Penalty = %d", solution.getSoftPenalty());
    log("Cpu Time = %d [ms]", solution.getCpuTime());
    log("Cpu Time (Last Improved) = %d [ms]", solution.getLastImprovedCpuTime());
    log("Done");
  }
}
