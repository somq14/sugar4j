package jp.ac.meiji.igusso.coptool.model;

import org.junit.Test;
import org.junit.Before;

import static jp.ac.meiji.igusso.coptool.model.Comparator.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class LinearConstraintTest {
  Model model;
  Variable[] v;

  @Before
  public void initialize() {
    model = new Model();

    v = new Variable[4];
    for (int i = 0; i < v.length; i++) {
      String varName = String.format("v%02d", i);
      v[i] = model.addVariable(varName, 0, 3);
    }
  }

  @Test
  public void test1() {
    LinearConstraint c = LinearConstraint.of("c1_2", LE, 16)
                             .addTerm(5, v[0])
                             .addTerm(7, v[1])
                             .addTerm(11, v[2])
                             .build();
  }

  @Test
  public void test2() {
    LinearConstraint c = LinearConstraint.of("c1_2", LE, 16, 100)
                             .addTerm(5, v[0])
                             .addTerm(7, v[1])
                             .addTerm(11, v[2])
                             .build();
  }
}
