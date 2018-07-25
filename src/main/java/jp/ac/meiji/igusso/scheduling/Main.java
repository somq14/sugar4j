package jp.ac.meiji.igusso.scheduling;

import jp.ac.meiji.igusso.coptool.model.Model;

import java.io.FileReader;
import java.util.List;

public final class Main {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Main InstanceFile [sugar|scop]");
      System.exit(1);
    }

    String fileName = args[0];
    SchedulingProblemParser spp = new SchedulingProblemParser(new FileReader(fileName));
    SchedulingProblem sp = spp.parse();

    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(sp);
    Model model = spe.encode();

    // ModelEncoder encoder = null;
    // if (args.length >= 2 && "sugar".equals(args[1])) {
    //   encoder = SugarModelEncoder.getInstance();
    // } else if (args.length >= 2 && "scop".equals(args[1])) {
    //   encoder = ScopModelEncoder.getInstance();
    // }

    // List<String> body = encoder.encode(model);
    // for (String line : body) {
    //   System.out.println(line);
    // }
  }
}
