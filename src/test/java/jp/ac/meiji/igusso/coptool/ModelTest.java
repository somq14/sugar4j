package jp.ac.meiji.igusso.coptool;

import org.junit.Test;

import static jp.ac.meiji.igusso.coptool.Comparator.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ModelTest {
  @Test
  public void test1() {
    Model model = new Model();
    Variable var1 = model.addVariable("v1", asList(1, 2, 3));
    Variable var2 = model.addVariable("v2", asList(1, 2, 3));
    Variable var3 = model.addVariable("v3", asList(1, 2, 3));
    Constraint cons = PseudoBooleanConstraint.of("c1", LE, 12)
                          .addTerm(1, var1, 1)
                          .addTerm(2, var2, 2)
                          .addTerm(3, var3, 3)
                          .build();
    model.addConstraint(cons);
  }
}
