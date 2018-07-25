package jp.ac.meiji.igusso.coptool.model;

import org.junit.Test;

import static jp.ac.meiji.igusso.coptool.model.Comparator.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ConflictPointConstraintTest {
  @Test
  public void test1() {
    ConflictPointConstraint c = ConflictPointConstraint.of("c1")
                                    .addTerm(new VariableImpl("v1", asList(1, 2, 3)), 1)
                                    .addTerm(new VariableImpl("v2", asList(1, 2, 3)), 2, true)
                                    .addTerm(new VariableImpl("v3", asList(1, 2, 3)), 3, false)
                                    .build();
  }
}
