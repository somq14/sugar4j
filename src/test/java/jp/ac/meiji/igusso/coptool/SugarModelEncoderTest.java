package jp.ac.meiji.igusso.coptool;

import org.junit.Test;

import static java.util.Arrays.asList;
import static jp.ac.meiji.igusso.coptool.Comparator.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class SugarModelEncoderTest {
  @Test
  public void test() {
    Model model = new Model();

    Variable v1 = model.addVariable("v1", asList(1, 2, 3));
    Variable v2 = model.addVariable("v2", asList(1, 2, 3));
    Variable v3 = model.addVariable("v3", asList(1, 2, 3));

    LinearConstraint c1 = LinearConstraint.of("c1", EQ, 3, 1)
                             .addTerm(1, v1, 1)
                             .addTerm(2, v2, 2)
                             .addTerm(3, v3, 3)
                             .build();
    model.addConstraint(c1);

    LinearConstraint c2 = LinearConstraint.of("c2", LE, 3, 10)
                             .addTerm(1, v1, 1)
                             .addTerm(2, v2, 2)
                             .addTerm(3, v3, 3)
                             .build();
    model.addConstraint(c2);

    LinearConstraint c3 = LinearConstraint.of("c3", GT, 3, 100)
                             .addTerm(3, v1, 1)
                             .addTerm(2, v2, 2)
                             .addTerm(1, v3, 3)
                             .build();
    model.addConstraint(c3);


    for (String line : SugarModelEncoder.getInstance().encode(model)) {
      System.out.println(line);
    }
  }
}

