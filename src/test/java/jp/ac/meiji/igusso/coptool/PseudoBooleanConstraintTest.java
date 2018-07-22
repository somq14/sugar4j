package jp.ac.meiji.igusso.coptool;

import org.junit.Test;

import static jp.ac.meiji.igusso.coptool.Comparator.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class PseudoBooleanConstraintTest {
  @Test
  public void test1() {
    PseudoBooleanConstraint c = PseudoBooleanConstraint.of("c1_2", LE, 32)
                             .addTerm(5, new VariableImpl("v1", asList(1, 2, 3)), 1)
                             .addTerm(7, new VariableImpl("v2", asList(1, 2, 3)), 2)
                             .addTerm(11, new VariableImpl("v3", asList(1, 2, 3)), 3)
                             .build();
  }

  @Test
  public void test2() {
    PseudoBooleanConstraint c = PseudoBooleanConstraint.of("c1_2", LE, 32, 100)
                             .addTerm(5, new VariableImpl("v1", asList(1, 2, 3)), 1)
                             .addTerm(7, new VariableImpl("v2", asList(1, 2, 3)), 2)
                             .addTerm(11, new VariableImpl("v3", asList(1, 2, 3)), 3)
                             .build();
  }
}