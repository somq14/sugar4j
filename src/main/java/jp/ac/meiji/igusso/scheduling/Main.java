package jp.ac.meiji.igusso.scheduling;

import jp.ac.meiji.igusso.coptool.model.Model;
import jp.ac.meiji.igusso.coptool.model.Constraint;
import jp.ac.meiji.igusso.coptool.model.Variable;

import jp.ac.meiji.igusso.coptool.scop.Model2ScopTranslator;
import jp.ac.meiji.igusso.coptool.scop.Scop4j;
import jp.ac.meiji.igusso.coptool.scop.Solution;

import java.io.FileReader;
import java.util.List;

public final class Main {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Main InstanceFile");
      System.exit(1);
    }

    String fileName = args[0];
    SchedulingProblemParser spp = new SchedulingProblemParser(new FileReader(fileName));
    SchedulingProblem sp = spp.parse();

    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(sp);
    Model model = spe.encode();

    Model2ScopTranslator translator = Model2ScopTranslator.newInstance();
    Scop4j scop4j = Scop4j.newInstance();
    for (Variable variable : model.getVariables()) {
      scop4j.addVariable(translator.translate(variable));
    }
    for (Constraint constraint : model.getConstraints()) {
      scop4j.addConstraint(translator.translate(constraint));
    }

    Solution solution = scop4j.solve();
    System.out.println("hard penalty = " + solution.getHardPenalty());
    System.out.println("soft penalty = " + solution.getSoftPenalty());
  }
}
