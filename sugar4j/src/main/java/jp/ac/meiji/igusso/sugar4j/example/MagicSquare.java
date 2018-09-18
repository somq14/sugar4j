package jp.ac.meiji.igusso.sugar4j.example;

// Java CHECKSTYLE:OFF AvoidStarImport
import static jp.kobe_u.sugar.expression.Expression.*;
// Java CHECKSTYLE:ON AvoidStarImport

import jp.ac.meiji.igusso.sugar4j.IpasirSolver;
import jp.ac.meiji.igusso.sugar4j.IpasirSolver;
import jp.ac.meiji.igusso.sugar4j.SatSolver;
import jp.ac.meiji.igusso.sugar4j.Solution;
import jp.ac.meiji.igusso.sugar4j.Sugar4j;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.expression.Expression;

import java.util.ArrayList;
import java.util.List;

// Java CHECKSTYLE:OFF MemberName
public class MagicSquare {
  private final List<Expression> csp;
  private Expression[][] x;

  private final int size;

  public MagicSquare(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException();
    }
    this.size = size;
    this.csp = new ArrayList<>();
  }

  private void add(Expression... expressions) {
    csp.add(create(expressions));
  }

  private void add(Expression opr, List<Expression> terms) {
    csp.add(create(opr, terms));
  }

  public void print() {
    for (Expression exp : csp) {
      System.out.println(exp);
    }
  }

  public void solve() throws SugarException {
    SatSolver satSolver = IpasirSolver.newInstance("glueminisat");
    Sugar4j sugar4j = Sugar4j.newInstance(satSolver);

    sugar4j.addExpressions(csp);

    Solution sol = sugar4j.solve();
    if (!sol.isSat()) {
      System.out.println("Solution Is Not Found");
      return;
    }

    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        System.out.printf(" %4d", sol.getIntMap().get(x[i][j]));
      }
      System.out.println();
    }
  }

  public void encode() {
    final int sum = (size * size) * (size * size + 1) / 2 / size;

    // sengen
    x = new Expression[size][size];
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        x[i][j] = create(String.format("x_%02d_%02d", i, j));
        add(INT_DEFINITION, x[i][j], create(1), create(size * size));
      }
    }

    // alldifferent
    {
      List<Expression> terms = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
          terms.add(x[i][j]);
        }
      }
      add(ALLDIFFERENT, terms);
    }

    // yoko
    for (int i = 0; i < size; i++) {
      List<Expression> terms = new ArrayList<>();
      for (int j = 0; j < size; j++) {
        terms.add(x[i][j]);
      }
      add(EQ, create(ADD, terms), create(sum));
    }

    // tate
    for (int j = 0; j < size; j++) {
      List<Expression> terms = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        terms.add(x[i][j]);
      }
      add(EQ, create(ADD, terms), create(sum));
    }

    // naname1
    {
      List<Expression> terms = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        terms.add(x[i][i]);
      }
      add(EQ, create(ADD, terms), create(sum));
    }

    // naname2
    {
      List<Expression> terms = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        terms.add(x[i][size - 1 - i]);
      }
      add(EQ, create(ADD, terms), create(sum));
    }
  }

  public static void main(String[] args) throws Exception {
    int size = Integer.valueOf(args[0]);
    MagicSquare ms = new MagicSquare(size);
    ms.encode();
    ms.print();
    ms.solve();
  }
}
// Java CHECKSTYLE:ON MemberName
