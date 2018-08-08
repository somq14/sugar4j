package jp.ac.meiji.igusso.scheduling;

import static jp.ac.meiji.igusso.scheduling.Main.log;
import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.model.Constraint;
import jp.ac.meiji.igusso.model.Model;
import jp.ac.meiji.igusso.model.Model2SugarTranslator;
import jp.ac.meiji.igusso.model.Variable;
import jp.ac.meiji.igusso.scop4j.Scop4j;
import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.ac.meiji.igusso.sugar4j.IpasirSolver;
import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.ac.meiji.igusso.sugar4j.Sugar4j;
import jp.kobe_u.sugar.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class IncrementalMethod {
  private Model model;
  private Model2SugarTranslator translator;
  private Sugar4j sugar4j;

  private Solution bestSolution;

  private int solveCount;
  private long timerBegin;
  private long timerEnd;

  private List<Constraint> hardConstraints;
  private List<Constraint> heavyConstraints;
  private List<Constraint> lightConstraints;

  IncrementalMethod(Map<String, String> options) {}

  private void setup(SchedulingProblem problem) {
    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(problem);
    model = spe.encode();
    translator = Model2SugarTranslator.newInstance();
    sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));

    log("Classifying Constraints By Weight...");
    int maxWeight = 0;
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isSoft()) {
        maxWeight = Math.max(maxWeight, constraint.getWeight());
      }
    }

    hardConstraints = new ArrayList<>();
    heavyConstraints = new ArrayList<>();
    lightConstraints = new ArrayList<>();
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
    log("Maximum Weight = %d", maxWeight);
    log("Hard  Constraint Count = %d", hardConstraints.size());
    log("Heavy Constraint Count = %d", heavyConstraints.size());
    log("Light Constraint Count = %d", lightConstraints.size());
    log("Done");
  }

  private void translateModel() {
    log("Translating Model...");

    for (Variable variable : model.getVariables()) {
      sugar4j.addExpressions(translator.translate(variable));
    }

    for (Constraint constraint : hardConstraints) {
      sugar4j.addConstraints(translator.translate(constraint));
    }

    for (Constraint constraint : heavyConstraints) {
      sugar4j.addConstraints(translator.translate(constraint));
    }

    // generate objective variable _P
    int maxPenaltyOfHeavyConstraints = 0;
    List<Expression> terms = new ArrayList<>();
    for (Constraint constraint : heavyConstraints) {
      terms.add(translator.getPenaltyVariableOf(constraint));
      maxPenaltyOfHeavyConstraints += constraint.getPenaltyUpperBound();
    }
    sugar4j.addIntVariable("_P", 0, maxPenaltyOfHeavyConstraints);
    sugar4j.addConstraint(create(Expression.EQ, create("_P"), create(Expression.ADD, terms)));

    log("Done");
  }

  private void encodeConstraints() throws Exception {
    log("Encoding Constraints...");

    sugar4j.update();

    log("Done");
  }

  private void searchSolution() throws Exception {
    log("---------------- Optimize Heavy Constraints ----------------");
    log("Searching Initial Solution...");
    timerBegin = System.currentTimeMillis();

    solveCount = 0;
    bestSolution = null;

    Solution solution = sugar4j.solve();
    solveCount++;
    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj = Expression.create("_P");
    int penalty = solution.getIntMap().get(obj);
    log("Found Penalty = %d", penalty);

    while (penalty > 0) {
      log("Searching Penalty <= %d", penalty - 1);
      sugar4j.addAssumption(obj, Comparator.LE, penalty - 1);
      solution = sugar4j.solve();
      solveCount++;

      if (!solution.isSat()) {
        log("Not Found");
        break;
      }
      penalty = solution.getIntMap().get(obj);
      sugar4j.addConstraint(create(Expression.LE, obj, create(penalty)));
      log("Found Penalty = %d", penalty);
      bestSolution = solution;
    }
    log("Optimum Found About Heavy Constraint Penalty = %d", penalty);

    Expression objBind = create(Expression.EQ, obj, create(penalty));
    log("Add Constraint To Bind %s", objBind.toString());
    sugar4j.addConstraint(objBind);
    log("Done");

    log("---------------- Optimize Light Constraints ----------------");
    log("Sorting Light Constraints...");
    Collections.sort(
        lightConstraints, (c1, c2) -> (Integer.compare(c1.getWeight(), c2.getWeight())));
    Collections.reverse(lightConstraints);
    log("Done");

    for (Constraint constraint : lightConstraints) {
      log("Adding Constraint  (Name = %s, Weight = %d)", constraint.getName(),
          constraint.getWeight());
      sugar4j.addConstraints(translator.translate(constraint));
      log("Done");

      log("Searching Initial Solution...");
      solution = sugar4j.solve();
      solveCount++;

      if (!solution.isSat()) {
        log("UNSAT (Something Wrong Happend)");
        return;
      }
      bestSolution = solution;

      Expression penaltyVariable = translator.getPenaltyVariableOf(constraint);
      penalty = solution.getIntMap().get(penaltyVariable);
      log("Found Penalty = %d", penalty);

      while (penalty > 0) {
        log("Search Penalty <= %d", penalty - 1);
        sugar4j.addAssumption(penaltyVariable, Comparator.LE, penalty - 1);

        solution = sugar4j.solve();
        solveCount++;
        if (!solution.isSat()) {
          log("Not Found");
          break;
        }
        penalty = solution.getIntMap().get(penaltyVariable);
        sugar4j.addConstraint(create(Expression.LE, penaltyVariable, create(penalty)));
        log("Found Penalty = %d", penalty);
        bestSolution = solution;
      }
      log("Complete To Improve Penalty = %d", penalty);

      Expression penaltyBind = create(Expression.EQ, penaltyVariable, create(penalty));
      log("Add Constraint To Bind %s", penaltyBind.toString());
      sugar4j.addConstraint(penaltyBind);
      log("Done", penaltyBind.toString());
    }

    timerEnd = System.currentTimeMillis();
  }

  void displaySolution() {
    if (bestSolution == null) {
      log("No Solution Found");
      return;
    }

    log("Solution");
    for (Variable variable : model.getVariables()) {
      log("%s = %d", variable.getName(),
          bestSolution.getIntMap().get(Expression.create(variable.getName())));
    }
    log("");

    log("Penalty (weight)");
    int ans = 0;
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isHard()) {
        continue;
      }
      log("%s (%d) = %d", constraint.getName(), constraint.getWeight(),
          bestSolution.getIntMap().get(translator.getPenaltyVariableOf(constraint)));

      ans += constraint.getWeight()
          * bestSolution.getIntMap().get(translator.getPenaltyVariableOf(constraint));
    }
    log("");

    log("Best Soluiton OBJ = %d", ans);
    log("Solve Count = %d", solveCount);
    log("Cpu Time = %d [ms]", (timerEnd - timerBegin));
  }

  void solve(SchedulingProblem problem) throws Exception {
    setup(problem);
    translateModel();
    encodeConstraints();

    Thread hook = new Thread() {
      @Override
      public void run() {
        timerEnd = System.currentTimeMillis();
      }
    };
    Runtime.getRuntime().addShutdownHook(hook);
    searchSolution();
    Runtime.getRuntime().removeShutdownHook(hook);

    displaySolution();
  }
}
