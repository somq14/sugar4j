package jp.ac.meiji.igusso.model;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public final class Model2ScopTranslatorTest {
  Model2ScopTranslator translator;
  Variable[] v;

  @Before
  public void initialize() {
    translator = Model2ScopTranslator.newInstance();

    v = new Variable[3];
    for (int i = 0; i < v.length; i++) {
      v[i] = modelVariable("v" + i, 1, 3);
    }
  }

  private static Variable modelVariable(String name, int lowerBound, int upperBound) {
    return Variable.of(name, lowerBound, upperBound);
  }

  @Test
  public void testVariableTranslation() {
    jp.ac.meiji.igusso.scop4j.Variable actual = translator.translate(modelVariable("v", 1, 3));
    assertThat(actual, is(jp.ac.meiji.igusso.scop4j.Variable.of("v", 1, 3)));
  }

  @Test
  public void testAllDifferentConstraintTranslation() {
    AllDifferentConstraint constraint = AllDifferentConstraint.of("alldiff", 3)
                                            .addVariable(v[0])
                                            .addVariable(v[1])
                                            .addVariable(v[2])
                                            .build();

    jp.ac.meiji.igusso.scop4j.Constraint actual = translator.translate(constraint);
    jp.ac.meiji.igusso.scop4j.Constraint expected =
        jp.ac.meiji.igusso.scop4j.AllDifferentConstraint.of("alldiff", 3)
            .addVariable(translator.translate(v[0]))
            .addVariable(translator.translate(v[1]))
            .addVariable(translator.translate(v[2]))
            .build();
    assertThat(actual, is(expected));
  }

  @Test
  public void testLinearConstraintTest() {
    LinearConstraint constraint = LinearConstraint.of("linear", Comparator.LE, 3, 100)
                                      .addTerm(3, v[0])
                                      .addTerm(2, v[1])
                                      .addTerm(1, v[2])
                                      .build();

    jp.ac.meiji.igusso.scop4j.Constraint actual = translator.translate(constraint);
    jp.ac.meiji.igusso.scop4j.Constraint expected =
        jp.ac.meiji.igusso.scop4j.LinearConstraint
            .of("linear", jp.ac.meiji.igusso.scop4j.Comparator.LE, 3, 100)
            .addTerm(3, translator.translate(v[0]), 1)
            .addTerm(6, translator.translate(v[0]), 2)
            .addTerm(9, translator.translate(v[0]), 3)
            .addTerm(2, translator.translate(v[1]), 1)
            .addTerm(4, translator.translate(v[1]), 2)
            .addTerm(6, translator.translate(v[1]), 3)
            .addTerm(1, translator.translate(v[2]), 1)
            .addTerm(2, translator.translate(v[2]), 2)
            .addTerm(3, translator.translate(v[2]), 3)
            .build();
    assertThat(actual, is(expected));
  }

  @Test
  public void testPseudoBooleanConstraint() {
    PseudoBooleanConstraint constraint = PseudoBooleanConstraint.of("pb", Comparator.LE, 3, 100)
                                             .addTerm(6, v[0], 3)
                                             .addTerm(5, v[1], 2)
                                             .addTerm(4, v[2], 1)
                                             .build();

    jp.ac.meiji.igusso.scop4j.Constraint actual = translator.translate(constraint);
    jp.ac.meiji.igusso.scop4j.Constraint expected =
        jp.ac.meiji.igusso.scop4j.LinearConstraint
            .of("pb", jp.ac.meiji.igusso.scop4j.Comparator.LE, 3, 100)
            .addTerm(6, translator.translate(v[0]), 3)
            .addTerm(5, translator.translate(v[1]), 2)
            .addTerm(4, translator.translate(v[2]), 1)
            .build();
    assertThat(actual, is(expected));
  }

  @Test
  public void testConflictPointConstraint() {
    ConflictPointConstraint constraint = ConflictPointConstraint.of("conflict", 100)
                                             .addTerm(PredicateTerm.of(v[0], 3))
                                             .addTerm(PredicateTerm.of(v[1], 2))
                                             .addTerm(PredicateTerm.of(v[2], 1).not())
                                             .build();

    jp.ac.meiji.igusso.scop4j.Constraint actual = translator.translate(constraint);
    jp.ac.meiji.igusso.scop4j.Constraint expected =
        jp.ac.meiji.igusso.scop4j.LinearConstraint
            .of("conflict", jp.ac.meiji.igusso.scop4j.Comparator.LE, 1, 100)
            .addTerm(1, translator.translate(v[0]), 3)
            .addTerm(1, translator.translate(v[1]), 2)
            .addTerm(-1, translator.translate(v[2]), 1)
            .build();
    assertThat(actual, is(expected));
  }
}
