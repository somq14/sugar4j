import jp.ac.meiji.igusso.scop4j.AllDifferentConstraint;
import jp.ac.meiji.igusso.scop4j.Comparator;
import jp.ac.meiji.igusso.scop4j.Constraint;
import jp.ac.meiji.igusso.scop4j.LinearConstraint;
import jp.ac.meiji.igusso.scop4j.Scop4j;
import jp.ac.meiji.igusso.scop4j.Solution;
import jp.ac.meiji.igusso.scop4j.Variable;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class MagicSquare {
  private final int size;

  public MagicSquare(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException();
    }
    this.size = size;
  }

  // Java CHECKSTYLE:OFF LocalVariableName
  public void solve() {
    int sum = (size * size) * ((size * size) + 1) / 2 / size;

    Scop4j scop4j = Scop4j.newInstance();

    Variable[][] x = new Variable[size][size];
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        x[i][j] = Variable.of(String.format("x%02d%02d", i, j), 1, size * size);
        scop4j.addVariable(x[i][j]);
      }
    }

    AllDifferentConstraint.Builder allDiff = AllDifferentConstraint.of("diff");
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        allDiff.addVariable(x[i][j]);
      }
    }
    scop4j.addConstraint(allDiff.build());

    for (int i = 0; i < size; i++) {
      LinearConstraint.Builder constraint = LinearConstraint.of("row" + i, Comparator.EQ, sum);
      for (int j = 0; j < size; j++) {
        for (int v = 1; v <= size * size; v++) {
          constraint.addTerm(v, x[i][j], v);
        }
      }
      scop4j.addConstraint(constraint.build());
    }

    for (int j = 0; j < size; j++) {
      LinearConstraint.Builder constraint = LinearConstraint.of("col" + j, Comparator.EQ, sum);
      for (int i = 0; i < size; i++) {
        for (int v = 1; v <= size * size; v++) {
          constraint.addTerm(v, x[i][j], v);
        }
      }
      scop4j.addConstraint(constraint.build());
    }

    LinearConstraint.Builder constraint1 = LinearConstraint.of("cross1", Comparator.EQ, sum);
    for (int i = 0; i < size; i++) {
      for (int v = 1; v <= size * size; v++) {
        constraint1.addTerm(v, x[i][i], v);
      }
    }
    scop4j.addConstraint(constraint1.build());

    LinearConstraint.Builder constraint2 = LinearConstraint.of("cross2", Comparator.EQ, sum);
    for (int i = 0; i < size; i++) {
      for (int v = 1; v <= size * size; v++) {
        constraint2.addTerm(v, x[i][size - 1 - i], v);
      }
    }
    scop4j.addConstraint(constraint2.build());

    scop4j.setTimeout(10);
    Solution solution = scop4j.solve();
    for (int i = 0; i < size; i++) {
      ArrayList<String> lineElements = new ArrayList<>();
      for (int j = 0; j < size; j++) {
        String value = solution.getSolution().get(x[i][j]);
        lineElements.add(String.format("%6s", value));
      }
      System.out.println(String.join(" ", lineElements));
    }

    System.out.println("hard: " + solution.getHardPenalty());
    System.out.println("soft: " + solution.getSoftPenalty());
    System.out.println(
        "cputime: " + solution.getCpuTime() + "/" + solution.getLastImprovedCpuTime());
    System.out.println(
        "iteration: " + solution.getIteration() + "/" + solution.getLastImprovedIteration());
    System.out.println("violated constraints: " + solution.getViolatedConstraints());
  }
  // Java CHECKSTYLE:ON LocalVariableName

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("MagicSquare SIZE");
      System.exit(1);
    }

    int size = Integer.valueOf(args[0]);
    new MagicSquare(size).solve();
  }
}
