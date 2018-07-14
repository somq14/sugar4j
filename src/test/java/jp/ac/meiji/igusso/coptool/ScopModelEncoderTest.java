package jp.ac.meiji.igusso.coptool;

import org.junit.Test;

import static java.util.Arrays.asList;
import static jp.ac.meiji.igusso.coptool.Comparator.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ScopModelEncoderTest {
  // @Test
  public void test() {
    Model model = new Model();

    Variable v1 = model.addVariable("v1", asList(1, 2, 3));
    Variable v2 = model.addVariable("v2", asList(1, 2, 3));
    Variable v3 = model.addVariable("v3", asList(1, 2, 3));

    LinearConstraint c1 = LinearConstraint.of("c1", LE, 3)
                             .addTerm(1, v1, 1)
                             .addTerm(2, v2, 2)
                             .addTerm(3, v3, 3)
                             .build();
    model.addConstraint(c1);

    LinearConstraint c2 = LinearConstraint.of("c2", GE, 3, 1)
                             .addTerm(3, v1, 1)
                             .addTerm(2, v2, 2)
                             .addTerm(1, v3, 3)
                             .build();
    model.addConstraint(c2);


    for (String line : ScopModelEncoder.getInstance().encode(model)) {
      System.out.println(line);
    }
  }
}
