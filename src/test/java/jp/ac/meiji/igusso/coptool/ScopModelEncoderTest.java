package jp.ac.meiji.igusso.coptool;

import static java.util.Arrays.asList;
import static jp.ac.meiji.igusso.coptool.Comparator.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;
import org.junit.Before;

import java.util.List;

public class ScopModelEncoderTest {
  ScopModelEncoder encoder;
  Model model;
  Variable[] v;

  @Before
  public void initialize() {
    encoder = ScopModelEncoder.getInstance();
    model = new Model();

    v = new Variable[10];
    for (int i = 0; i < v.length; i++) {
      String varName = String.format("v%02d", i);
      v[i] = model.addVariable(varName, 0, 3);
    }
  }

  @Test
  public void testConflictPointConstraintEncode1() {
    Constraint cons = ConflictPointConstraint.of("cons").addTerm(v[0], 1).build();
    model.addConstraint(cons);

    List<String> body = encoder.encode(model);
    String actual = body.get(body.size() - 1);
    assertThat("cons: weight=inf type=linear  1(v00, 1) <= 0", is(actual));
  }

  @Test
  public void testConflictPointConstraintEncode2() {
    Constraint cons = ConflictPointConstraint.of("cons").addTerm(v[0], 1, false).build();
    model.addConstraint(cons);

    List<String> body = encoder.encode(model);
    String actual = body.get(body.size() - 1);
    assertThat("cons: weight=inf type=linear  -1(v00, 1) <= -1", is(actual));
  }

  @Test
  public void testConflictPointConstraintEncode3() {
    Constraint cons = ConflictPointConstraint.of("cons")
                          .addTerm(v[0], 3)
                          .addTerm(v[1], 2, true)
                          .addTerm(v[2], 1, false)
                          .build();
    model.addConstraint(cons);

    List<String> body = encoder.encode(model);
    String actual = body.get(body.size() - 1);
    assertThat("cons: weight=inf type=linear  1(v00, 3) 1(v01, 2) -1(v02, 1) <= 1", is(actual));
  }

  @Test
  public void testLinearConstraintEncode() {
    Constraint cons = LinearConstraint.of("cons", Comparator.LE, 12)
                          .addTerm(1, v[0])
                          .addTerm(2, v[1])
                          .addTerm(-3, v[2])
                          .build();
    model.addConstraint(cons);
    List<String> body = encoder.encode(model);
    // for (String line : body) {
    //   System.out.println(line);
    // }
  }

  // @Test
  public void test() {
    PseudoBooleanConstraint c1 = PseudoBooleanConstraint.of("c1", LE, 3)
                                     .addTerm(1, v[0], 1)
                                     .addTerm(2, v[1], 2)
                                     .addTerm(3, v[2], 3)
                                     .build();
    model.addConstraint(c1);

    PseudoBooleanConstraint c2 = PseudoBooleanConstraint.of("c2", GE, 3, 1)
                                     .addTerm(3, v[0], 1)
                                     .addTerm(2, v[1], 2)
                                     .addTerm(1, v[2], 3)
                                     .build();
    model.addConstraint(c2);

    for (String line : encoder.encode(model)) {
      System.out.println(line);
    }
  }
}
