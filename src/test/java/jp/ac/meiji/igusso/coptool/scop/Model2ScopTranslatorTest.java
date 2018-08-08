package jp.ac.meiji.igusso.coptool.scop;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public final class Model2ScopTranslatorTest {
  Model2ScopTranslator translator;
  jp.ac.meiji.igusso.coptool.model.Variable[] v;

  @Before
  public void initialize() {
    translator = Model2ScopTranslator.newInstance();

    v = new jp.ac.meiji.igusso.coptool.model.Variable[3];
    for (int i = 0; i < v.length; i++) {
      v[i] = modelVariable("v" + i, 1, 3);
    }
  }

  private static jp.ac.meiji.igusso.coptool.model.Variable modelVariable(
      String name, int lowerBound, int upperBound) {
    return jp.ac.meiji.igusso.coptool.model.Variable.of(name, lowerBound, upperBound);
  }

  @Test
  public void testVariableTranslation() {
    Variable actual = translator.translate(modelVariable("v", 1, 3));
    assertThat(actual, is(Variable.of("v", 1, 3)));
  }

  @Test
  public void testAllDifferentConstraintTranslation() {
    jp.ac.meiji.igusso.coptool.model.AllDifferentConstraint constraint =
        jp.ac.meiji.igusso.coptool.model.AllDifferentConstraint.of("alldiff", 3)
            .addVariable(v[0])
            .addVariable(v[1])
            .addVariable(v[2])
            .build();

    Constraint actual = translator.translate(constraint);
    Constraint expected = AllDifferentConstraint.of("alldiff", 3)
                              .addVariable(translator.translate(v[0]))
                              .addVariable(translator.translate(v[1]))
                              .addVariable(translator.translate(v[2]))
                              .build();
    assertThat(actual, is(expected));
  }

  @Test
  public void testLinearConstraintTest() {
    jp.ac.meiji.igusso.coptool.model.LinearConstraint constraint =
        jp.ac.meiji.igusso.coptool.model.LinearConstraint
            .of("linear", jp.ac.meiji.igusso.coptool.model.Comparator.LE, 3, 100)
            .addTerm(3, v[0])
            .addTerm(2, v[1])
            .addTerm(1, v[2])
            .build();

    Constraint actual = translator.translate(constraint);
    Constraint expected = LinearConstraint.of("linear", Comparator.LE, 3, 100)
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
    jp.ac.meiji.igusso.coptool.model.PseudoBooleanConstraint constraint =
        jp.ac.meiji.igusso.coptool.model.PseudoBooleanConstraint
            .of("pb", jp.ac.meiji.igusso.coptool.model.Comparator.LE, 3, 100)
            .addTerm(6, v[0], 3)
            .addTerm(5, v[1], 2)
            .addTerm(4, v[2], 1)
            .build();

    Constraint actual = translator.translate(constraint);
    Constraint expected = LinearConstraint.of("pb", Comparator.LE, 3, 100)
                              .addTerm(6, translator.translate(v[0]), 3)
                              .addTerm(5, translator.translate(v[1]), 2)
                              .addTerm(4, translator.translate(v[2]), 1)
                              .build();
    assertThat(actual, is(expected));
  }

  @Test
  public void testConflictPointConstraint() {
    jp.ac.meiji.igusso.coptool.model.ConflictPointConstraint constraint =
        jp.ac.meiji.igusso.coptool.model.ConflictPointConstraint.of("conflict", 100)
            .addTerm(jp.ac.meiji.igusso.coptool.model.PredicateTerm.of(v[0], 3))
            .addTerm(jp.ac.meiji.igusso.coptool.model.PredicateTerm.of(v[1], 2))
            .addTerm(jp.ac.meiji.igusso.coptool.model.PredicateTerm.of(v[2], 1).not())
            .build();

    Constraint actual = translator.translate(constraint);
    Constraint expected = LinearConstraint.of("conflict", Comparator.LE, 1, 100)
                              .addTerm(1, translator.translate(v[0]), 3)
                              .addTerm(1, translator.translate(v[1]), 2)
                              .addTerm(-1, translator.translate(v[2]), 1)
                              .build();
    assertThat(actual, is(expected));
  }
}
