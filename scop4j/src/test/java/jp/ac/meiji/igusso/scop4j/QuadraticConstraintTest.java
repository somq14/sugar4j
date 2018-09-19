package jp.ac.meiji.igusso.scop4j;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public final class QuadraticConstraintTest {
  Scop4j scop4j;

  int n;
  Variable[] v;

  @Before
  public void initialize() {
    this.scop4j = Scop4j.newInstance();

    this.n = 10;
    this.v = new Variable[n];
    for (int i = 0; i < n; i++) {
      v[i] = Variable.of("v" + i, 0, n - 1);
      scop4j.addVariable(v[i]);
    }
  }

  @Test
  public void test() {
    QuadraticConstraint.Builder cons = QuadraticConstraint.of("cons", Comparator.LE, 10, 100);
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        cons.addTerm(i + j, v[i], j, v[j], i);
      }
    }
    scop4j.addConstraint(cons.build());
  }
}
