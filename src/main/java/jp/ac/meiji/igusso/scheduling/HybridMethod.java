package jp.ac.meiji.igusso.scheduling;

import static jp.ac.meiji.igusso.scheduling.Main.log;
import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.coptool.model.Constraint;
import jp.ac.meiji.igusso.coptool.model.Model;
import jp.ac.meiji.igusso.coptool.model.Variable;
import jp.ac.meiji.igusso.coptool.sat.IpasirSolver;
import jp.ac.meiji.igusso.coptool.scop.Model2ScopTranslator;
import jp.ac.meiji.igusso.coptool.scop.Scop4j;
import jp.ac.meiji.igusso.coptool.sugar.Comparator;
import jp.ac.meiji.igusso.coptool.sugar.Model2SugarTranslator;
import jp.ac.meiji.igusso.coptool.sugar.Sugar4j;
import jp.ac.meiji.igusso.scheduling.SchedulingProblem;
import jp.ac.meiji.igusso.scheduling.SchedulingProblemEncoder;
import jp.kobe_u.sugar.expression.Expression;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class HybridMethod {
  void solve(SchedulingProblem problem) throws Exception {
    final long timerBegin = System.currentTimeMillis();

    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(problem);
    Model model = spe.encode();

    log("Classifying Constraints By Weight...");
    int maxWeight = 0;
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isSoft()) {
        maxWeight = Math.max(maxWeight, constraint.getWeight());
      }
    }

    List<Constraint> hardConstraints = new ArrayList<>();
    List<Constraint> heavyConstraints = new ArrayList<>();
    List<Constraint> lightConstraints = new ArrayList<>();
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isHard()) {
        hardConstraints.add(constraint);
        continue;
      }
      if (constraint.getWeight() >= maxWeight) {
        heavyConstraints.add(constraint);
      } else {
        lightConstraints.add(constraint);
      }
    }
    log("Done");

    log("---------------- Search Initial Solution With SCOP ----------------");

    log("Translating Constraints For SCOP...");
    Scop4j scop4j = Scop4j.newInstance();
    Model2ScopTranslator scopTranslator = Model2ScopTranslator.newInstance();
    for (Variable variable : model.getVariables()) {
      scop4j.addVariable(scopTranslator.translate(variable));
    }
    for (Constraint constraint : model.getConstraints()) {
      scop4j.addConstraint(scopTranslator.translate(constraint));
    }
    log("Done");

    log("Searching Initial Solution With SCOP...");
    scop4j.setTimeout(180);
    final jp.ac.meiji.igusso.coptool.scop.Solution scopSolution = scop4j.solve();
    log("Done");
    log("");

    log("Scop Log");
    List<String> logBody = Files.readAllLines(scop4j.getLogFile(), Charset.defaultCharset());
    for (String line : logBody) {
      log(line);
    }
    log("Done");
    log("");

    if (scopSolution.getHardPenalty() > 0) {
      log("Found No Feasible Solution");
      return;
    }

    log("Scop Solution");
    for (Variable variable : model.getVariables()) {
      log("%s = %s", variable.getName(),
          scopSolution.getSolution().get(scopTranslator.translate(variable)));
    }
    log("Penalty = %d", scopSolution.getSoftPenalty());
    log("Cpu Time = %d [ms]", scopSolution.getCpuTime());
    log("Cpu Time (Last Improved) = %d [ms]", scopSolution.getLastImprovedCpuTime());
    log("Done");

    int scopHeavyConstraintViolation = 0;
    for (Constraint constraint : heavyConstraints) {
      Map<jp.ac.meiji.igusso.coptool.scop.Constraint, Integer> violatedMap =
          scopSolution.getViolatedConstraints();
      if (violatedMap.containsKey(scopTranslator.translate(constraint))) {
        scopHeavyConstraintViolation += violatedMap.get(scopTranslator.translate(constraint));
      }
    }
    log("Scop Violates Heavy Constraints %d Times", scopHeavyConstraintViolation);

    Model2SugarTranslator sugarTranslator = Model2SugarTranslator.newInstance();
    Sugar4j sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));

    log("---------------- Improve Solution With SAT Solver ----------------");

    log("Translating Constraints For Sugar...");
    for (Variable variable : model.getVariables()) {
      sugar4j.addExpressions(sugarTranslator.translate(variable));
    }
    for (Constraint constraint : hardConstraints) {
      sugar4j.addConstraints(sugarTranslator.translate(constraint));
    }
    for (Constraint constraint : heavyConstraints) {
      sugar4j.addConstraints(sugarTranslator.translate(constraint));
    }

    // generate objective variable _P
    int maxPenaltyOfHeavyConstraints = 0;
    List<Expression> terms = new ArrayList<>();
    for (Constraint constraint : heavyConstraints) {
      terms.add(sugarTranslator.getPenaltyVariableOf(constraint));
      maxPenaltyOfHeavyConstraints += constraint.getPenaltyUpperBound();
    }
    sugar4j.addIntVariable("_P", 0, maxPenaltyOfHeavyConstraints);
    sugar4j.addConstraint(create(Expression.EQ, create("_P"), create(Expression.ADD, terms)));
    log("Done");

    log("SAT Encoding...");
    sugar4j.update();
    log("Done");

    log("Add Constraint About SCOP's Solution");
    Expression heavyBoundConstraint =
        create(Expression.LE, create("_P"), create(scopHeavyConstraintViolation));
    log("(%s)", heavyBoundConstraint.toString());
    sugar4j.addConstraint(heavyBoundConstraint);
    log("Done");

    log("Add SCOP Solution To SAT Solver As Assumtion...");
    for (Variable variable : model.getVariables()) {
      if (!variable.getName().startsWith("x_")) {
        continue;
      }
      int scopValue =
          Integer.valueOf(scopSolution.getSolution().get(scopTranslator.translate(variable)));
      sugar4j.addAssumption(create(variable.getName()), Comparator.EQ, scopValue);
    }
    log("Done");

    log("Searching First Solution...");
    int solveCount = 0;
    jp.ac.meiji.igusso.coptool.sugar.Solution sugarSolution = sugar4j.solve();
    jp.ac.meiji.igusso.coptool.sugar.Solution sugarBestSolution = null;
    if (!sugarSolution.isSat()) {
      log("UNSAT (Something Wrong Happend)");
      return;
    }
    solveCount++;
    log("Done");

    log("Sorting Light Constraints...");
    Collections.sort(
        lightConstraints, (c1, c2) -> (Integer.compare(c1.getWeight(), c2.getWeight())));
    Collections.reverse(lightConstraints);
    log("Done");

    for (Constraint constraint : lightConstraints) {
      log("Adding Constraint  (Name = %s, Weight = %d)", constraint.getName(),
          constraint.getWeight());
      sugar4j.addConstraints(sugarTranslator.translate(constraint));
      log("Done");

      log("Searching Initial Solution...");
      sugarSolution = sugar4j.solve();
      solveCount++;

      if (!sugarSolution.isSat()) {
        log("UNSAT (Something Wrong Happend)");
        return;
      }
      sugarBestSolution = sugarSolution;

      Expression penaltyVariable = sugarTranslator.getPenaltyVariableOf(constraint);
      int penalty = sugarSolution.getIntMap().get(penaltyVariable);
      log("Found Penalty = %d", penalty);

      while (penalty > 0) {
        log("Search Penalty <= %d", penalty - 1);
        sugar4j.addAssumption(penaltyVariable, Comparator.LE, penalty - 1);

        sugarSolution = sugar4j.solve();
        solveCount++;
        if (!sugarSolution.isSat()) {
          log("Not Found");
          break;
        }
        penalty = sugarSolution.getIntMap().get(penaltyVariable);
        log("Found Penalty = %d", penalty);
        sugarBestSolution = sugarSolution;
      }
      log("Complete To Improve Penalty = %d", penalty);

      Expression penaltyBind = create(Expression.EQ, penaltyVariable, create(penalty));
      log("Add Constraint To Bind %s", penaltyBind.toString());
      sugar4j.addConstraint(penaltyBind);
      log("Done", penaltyBind.toString());
    }

    final long timerEnd = System.currentTimeMillis();

    log("");
    log("Solution");
    for (Variable variable : model.getVariables()) {
      log("%s = %d", variable.getName(),
          sugarBestSolution.getIntMap().get(create(variable.getName())));
    }

    log("");
    log("Penalty (weight)");
    int sumOfPenalty = 0;
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isHard()) {
        continue;
      }
      log("%s (%d) = %d", constraint.getName(), constraint.getWeight(),
          sugarBestSolution.getIntMap().get(sugarTranslator.getPenaltyVariableOf(constraint)));
      sumOfPenalty += constraint.getWeight()
          * sugarBestSolution.getIntMap().get(sugarTranslator.getPenaltyVariableOf(constraint));
    }
    log("");

    log("Optimum Found OBJ = %d", sumOfPenalty);
    log("Solve Count = %d", solveCount);
    log("Cpu Time = %d [ms]", (timerEnd - timerBegin));
  }
}
