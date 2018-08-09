package jp.ac.meiji.igusso.scheduling;

import static jp.ac.meiji.igusso.scheduling.Main.log;
import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.model.Constraint;
import jp.ac.meiji.igusso.model.Model;
import jp.ac.meiji.igusso.model.Model2SugarTranslator;
import jp.ac.meiji.igusso.model.Variable;
import jp.ac.meiji.igusso.sugar4j.Comparator;
import jp.ac.meiji.igusso.sugar4j.IpasirSolver;
import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.ac.meiji.igusso.sugar4j.Sugar4j;
import jp.kobe_u.sugar.expression.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

final class TwoStepMethod {
  private Model model;
  private Model2SugarTranslator translator;
  private Sugar4j sugar4j;

  private Solution bestSolution;

  private int solveCount;
  private long timerBegin;
  private long timerEnd;

  private List<Constraint> hardConstraints;
  private SortedMap<Integer, List<Constraint>> softConstraints;

  private int timeout = -1;

  TwoStepMethod(Map<String, String> options) {
    if (options.containsKey("timeout")) {
      timeout = Integer.valueOf(options.get("timeout"));
    }
  }

  private void setup(SchedulingProblem problem) {
    SchedulingProblemEncoder spe = new SchedulingProblemEncoder(problem);
    model = spe.encode();
    translator = Model2SugarTranslator.newInstance();
    sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));

    log("Classifying Constraints By Weight...");

    hardConstraints = new ArrayList<>();
    softConstraints = new TreeMap<>();
    for (Constraint constraint : model.getConstraints()) {
      if (constraint.isHard()) {
        hardConstraints.add(constraint);
        continue;
      }

      if (!softConstraints.containsKey(constraint.getWeight())) {
        softConstraints.put(constraint.getWeight(), new ArrayList<>());
      }
      softConstraints.get(constraint.getWeight()).add(constraint);
    }

    log("Hard Constraint  : %d", hardConstraints.size());
    for (int weight : softConstraints.keySet()) {
      log("Weight %3d Constraint : %d", weight, softConstraints.get(weight).size());
    }
    log("Done");
  }

  private void translateModel() {
    log("Translating Model...");

    for (Variable variable : model.getVariables()) {
      sugar4j.addExpressions(translator.translate(variable));
    }

    for (Constraint constraint : hardConstraints) {
      sugar4j.addExpressions(translator.translate(constraint));
    }

    log("Done");
  }

  private void encodeConstraints() throws Exception {
    log("Encoding Constraints...");

    sugar4j.update();

    log("Done");
  }

  private void generateObjective1() throws Exception {
    log("Generating 1st Objective...");
    int maxWeight = softConstraints.lastKey();
    for (Constraint constraint : softConstraints.get(maxWeight)) {
      sugar4j.addConstraints(translator.translate(constraint));
    }

    int penaltyUpperBound = 0;
    List<Expression> terms = new ArrayList<>();
    for (Constraint constraint : softConstraints.get(maxWeight)) {
      terms.add(translator.getPenaltyVariableOf(constraint));
      penaltyUpperBound += constraint.getPenaltyUpperBound();
    }

    Expression obj1 = Expression.create("_P1");

    Expression varExp =
        create(Expression.INT_DEFINITION, obj1, Expression.ZERO, create(penaltyUpperBound));
    sugar4j.addExpression(varExp);
    log("Add Variable %s", varExp.toString());

    Expression consExp = create(Expression.EQ, obj1, create(Expression.ADD, terms));
    sugar4j.addConstraint(consExp);
    log("Add Constraint %s", consExp.toString());

    sugar4j.update();

    log("Done");
  }

  private void generateObjective2() throws Exception {
    int maxWeight = softConstraints.lastKey();

    int penaltyUpperBound = 0;
    List<Expression> terms = new ArrayList<>();
    for (int weight : softConstraints.keySet()) {
      if (weight == maxWeight) {
        continue;
      }

      int localPenaltyUpperBound = 0;
      List<Expression> localTerms = new ArrayList<>();
      for (Constraint constraint : softConstraints.get(weight)) {
        sugar4j.addConstraints(translator.translate(constraint));
        localTerms.add(translator.getPenaltyVariableOf(constraint));
        localPenaltyUpperBound += constraint.getPenaltyUpperBound();
      }

      Expression subObj = create(String.format("_P2_%03d", weight));

      Expression varExp = create(
          Expression.INT_DEFINITION, subObj, Expression.ZERO, create(localPenaltyUpperBound));
      sugar4j.addExpression(varExp);
      log("Add Variable %s", varExp.toString());

      Expression consExp = create(Expression.EQ, subObj, create(Expression.ADD, localTerms));
      sugar4j.addConstraint(consExp);
      log("Add Constraint %s", consExp.toString());

      terms.add(create(Expression.MUL, create(weight), subObj));
      penaltyUpperBound += weight * localPenaltyUpperBound;
    }

    Expression obj2 = create("_P2");

    Expression varExp =
        create(Expression.INT_DEFINITION, obj2, Expression.ZERO, create(penaltyUpperBound));
    sugar4j.addExpression(varExp);
    log("Add Constraint %s", varExp.toString());

    Expression consExp = create(Expression.EQ, obj2, create(Expression.ADD, terms));
    sugar4j.addConstraint(consExp);
    log("Add Constraint %s", consExp.toString());

    sugar4j.update();
    log("Done");
  }

  private void searchSolution() throws Exception {
    timerBegin = System.currentTimeMillis();

    solveCount = 0;
    bestSolution = null;

    log("-------------------------------- 1st STEP --------------------------------");

    generateObjective1();
    Solution solution = sugar4j.solve();
    solveCount++;

    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj1 = Expression.create("_P1");
    int ans1 = solution.getIntMap().get(obj1);
    log("Found OBJ = %d", ans1);

    while (true) {
      log("Search OBJ <= %d", ans1 - 1);

      sugar4j.addAssumption(obj1, Comparator.LE, ans1 - 1);
      solution = sugar4j.solve(timeout);
      solveCount++;

      if (solution.isTimeout()) {
        log("Time Out");
        break;
      }
      if (!solution.isSat()) {
        log("Not Found");
        break;
      }

      ans1 = solution.getIntMap().get(obj1);
      sugar4j.addConstraint(create(Expression.LE, obj1, create(ans1)));
      log("Found OBJ = %d", ans1);
      bestSolution = solution;
    }

    Expression obj1Bind = create(Expression.LE, obj1, create(ans1));
    log("Add Constraint: %s", obj1Bind.toString());
    sugar4j.addConstraint(obj1Bind);
    log("Done");

    log("-------------------------------- 2nd STEP --------------------------------");

    generateObjective2();
    solution = sugar4j.solve();
    solveCount++;

    if (!solution.isSat()) {
      log("UNSAT (There Is No Feasible Solution)");
      return;
    }
    bestSolution = solution;

    Expression obj2 = Expression.create("_P2");
    int ans2 = solution.getIntMap().get(obj2);
    log("Found OBJ = %d", ans2);

    while (true) {
      log("Search OBJ <= %d", ans2 - 1);

      sugar4j.addAssumption(obj2, Comparator.LE, ans2 - 1);
      solution = sugar4j.solve(timeout);
      solveCount++;

      if (solution.isTimeout()) {
        log("Time Out");
        break;
      }
      if (!solution.isSat()) {
        log("Not Found");
        break;
      }

      ans2 = solution.getIntMap().get(obj2);
      sugar4j.addConstraint(create(Expression.LE, obj2, create(ans2)));
      log("Found OBJ = %d", ans2);
      bestSolution = solution;
    }

    Expression obj2Bind = create(Expression.LE, obj2, create(ans2));
    log("Add Constraint: %s", obj2Bind.toString());
    sugar4j.addConstraint(obj2Bind);
    log("Done");

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

    log("Optimum Found OBJ = %d", ans);
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
        displaySolution();
      }
    };
    Runtime.getRuntime().addShutdownHook(hook);
    searchSolution();
    Runtime.getRuntime().removeShutdownHook(hook);

    displaySolution();
  }
}
