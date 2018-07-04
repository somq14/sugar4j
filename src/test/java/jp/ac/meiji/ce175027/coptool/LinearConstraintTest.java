package jp.ac.meiji.ce175027.coptool;

import org.junit.Test;

import static jp.ac.meiji.ce175027.coptool.Comparator.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class LinearConstraintTest {
  @Test
  public void test1() {
    LinearConstraint c = LinearConstraint.of("c1_2", LE, 32)
      .addTerm(5, new VariableImpl("v1", asList(1, 2, 3)), 1)
      .addTerm(7, new VariableImpl("v2", asList(1, 2, 3)), 2)
      .addTerm(11, new VariableImpl("v3", asList(1, 2, 3)), 3)
      .build();
  }

  @Test
  public void test2() {
    LinearConstraint c = LinearConstraint.of("c1_2", LE, 32, 100)
      .addTerm(5, new VariableImpl("v1", asList(1, 2, 3)), 1)
      .addTerm(7, new VariableImpl("v2", asList(1, 2, 3)), 2)
      .addTerm(11, new VariableImpl("v3", asList(1, 2, 3)), 3)
      .build();
  }
}
