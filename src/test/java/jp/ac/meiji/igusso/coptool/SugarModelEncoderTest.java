package jp.ac.meiji.igusso.coptool;

import static java.util.Arrays.asList;
import static jp.ac.meiji.igusso.coptool.Comparator.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;
import org.junit.Before;

import java.util.List;

public class SugarModelEncoderTest {
  SugarModelEncoder encoder;
  Model model;
  Variable[] v;

  @Before
  public void initialize() {
    encoder = SugarModelEncoder.getInstance();
    model = new Model();

    v = new Variable[3];
    for (int i = 0; i < v.length; i++) {
      String varName = String.format("v%02d", i);
      v[i] = model.addVariable(varName, 0, 3);
    }
  }

  @Test
  public void testConflictPointConstraintEncode1() {
    ConflictPointConstraint cons = ConflictPointConstraint.of("cons").addTerm(v[0], 1).build();
    model.addConstraint(cons);
    List<String> body = encoder.encode(cons);
    String actual = body.get(body.size() - 1);
    assertThat("(or (not v00__1)) ; cons", is(actual));
  }

  @Test
  public void testConflictPointConstraintEncode2() {
    ConflictPointConstraint cons =
        ConflictPointConstraint.of("cons").addTerm(v[0], 1, false).build();
    model.addConstraint(cons);

    List<String> body = encoder.encode(cons);
    String actual = body.get(body.size() - 1);
    assertThat("(or v00__1) ; cons", is(actual));
  }

  @Test
  public void testConflictPointConstraintEncode3() {
    ConflictPointConstraint cons = ConflictPointConstraint.of("cons")
                                       .addTerm(v[0], 3)
                                       .addTerm(v[1], 2, true)
                                       .addTerm(v[2], 1, false)
                                       .build();
    model.addConstraint(cons);

    List<String> body = encoder.encode(cons);
    String actual = body.get(body.size() - 1);
    assertThat("(or (not v00__3) (not v01__2) v02__1) ; cons", is(actual));
  }

  @Test
  public void testSoftConflictPointConstraintEncode3() {
    ConflictPointConstraint cons = ConflictPointConstraint.of("cons", 3)
                                       .addTerm(v[0], 1, true)
                                       .addTerm(v[1], 0, false)
                                       .build();
    model.addConstraint(cons);

    List<String> body = encoder.encode(model);
    // for (String line : body) {
    //   System.out.println(line);
    // }
  }

  // @Test
  public void test() {
    Model model = new Model();

    Variable v1 = model.addVariable("v1", asList(1, 2, 3));
    Variable v2 = model.addVariable("v2", asList(1, 2, 3));
    Variable v3 = model.addVariable("v3", asList(1, 2, 3));

    PseudoBooleanConstraint c1 = PseudoBooleanConstraint.of("c1", EQ, 3, 1)
                                     .addTerm(1, v1, 1)
                                     .addTerm(2, v2, 2)
                                     .addTerm(3, v3, 3)
                                     .build();
    model.addConstraint(c1);

    PseudoBooleanConstraint c2 = PseudoBooleanConstraint.of("c2", LE, 3, 10)
                                     .addTerm(1, v1, 1)
                                     .addTerm(2, v2, 2)
                                     .addTerm(3, v3, 3)
                                     .build();
    model.addConstraint(c2);

    PseudoBooleanConstraint c3 = PseudoBooleanConstraint.of("c3", GT, 3, 100)
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
