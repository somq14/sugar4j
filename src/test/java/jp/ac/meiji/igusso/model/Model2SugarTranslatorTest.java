package jp.ac.meiji.igusso.model;

import static jp.kobe_u.sugar.expression.Expression.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import jp.kobe_u.sugar.expression.Expression;
import jp.kobe_u.sugar.expression.Parser;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

public final class Model2SugarTranslatorTest {
  Model2SugarTranslator translator;
  Variable[] x;

  @Before
  public void initialize() {
    translator = Model2SugarTranslator.newInstance();

    x = new Variable[3];
    for (int i = 0; i < x.length; i++) {
      x[i] = Variable.of("x" + i, 1, 3);
      translator.translate(x[i]);
    }
  }

  @Test
  public void testVariableTranslation() {
    translator = Model2SugarTranslator.newInstance();

    List<Expression> actual = translator.translate(Variable.of("x", 1, 2));
    List<Expression> expected =
        Arrays.asList(create(INT_DEFINITION, create("x"), create(create(1), create(2))),
            create(INT_DEFINITION, create("_x__1"), create(0), create(1)),
            create(IMP, create(GE, create("_x__1"), ONE), create(LE, create("x"), create(1))),
            create(IMP, create(GE, create("_x__1"), ONE), create(GE, create("x"), create(1))),
            create(IMP,
                create(AND, create(LE, create("x"), create(1)), create(GE, create("x"), create(1))),
                create(GE, create("_x__1"), ONE)),
            create(INT_DEFINITION, create("_x__2"), create(0), create(1)),
            create(IMP, create(GE, create("_x__2"), ONE), create(LE, create("x"), create(2))),
            create(IMP, create(GE, create("_x__2"), ONE), create(GE, create("x"), create(2))),
            create(IMP,
                create(AND, create(LE, create("x"), create(2)), create(GE, create("x"), create(2))),
                create(GE, create("_x__2"), ONE)));
    assertThat(actual, is(expected));
  }

  @Test
  public void testAllDifferentConstraint() {
    AllDifferentConstraint cons = AllDifferentConstraint.of("cons")
                                      .addVariable(x[0])
                                      .addVariable(x[1])
                                      .addVariable(x[2])
                                      .build();
    List<Expression> actual = translator.translate(cons);
    List<Expression> expected =
        Arrays.asList(create(ALLDIFFERENT, create("x0"), create("x1"), create("x2")));
    assertThat(actual, is(expected));
  }

  @Test
  public void testHardConflictPointConstraint() {
    ConflictPointConstraint cons = ConflictPointConstraint.of("cons")
                                       .addTerm(x[0], 1, true)
                                       .addTerm(x[1], 2, true)
                                       .addTerm(x[2], 3, false)
                                       .build();
    List<Expression> actual = translator.translate(cons);
    List<Expression> expected = Arrays.asList(create(OR, create(LE, create("_x0__1"), ZERO),
        create(LE, create("_x1__2"), ZERO), create(GE, create("_x2__3"), ONE)));
    assertThat(actual, is(expected));
  }

  @Test
  public void testSoftConflictPointConstraint() {
    ConflictPointConstraint cons = ConflictPointConstraint.of("cons", 100)
                                       .addTerm(x[0], 1, true)
                                       .addTerm(x[1], 2, true)
                                       .addTerm(x[2], 3, false)
                                       .build();
    List<Expression> actual = translator.translate(cons);
    List<Expression> expected =
        Arrays.asList(create(INT_DEFINITION, create("_P__cons"), create(0), create(1)),
            create(OR, create(LE, create("_x0__1"), ZERO), create(LE, create("_x1__2"), ZERO),
                create(GE, create("_x2__3"), ONE), create(GE, create("_P__cons"), ONE)));
    assertThat(actual, is(expected));
  }

  @Test
  public void testHardLinearConstraint() {
    LinearConstraint cons = LinearConstraint.of("cons", Comparator.LT, 8)
                                .addTerm(1, x[0])
                                .addTerm(-2, x[1])
                                .addTerm(3, x[2])
                                .build();
    List<Expression> actual = translator.translate(cons);
    List<Expression> expected = Arrays.asList(create(LT,
        create(ADD, create(MUL, create(1), create("x0")), create(MUL, create(-2), create("x1")),
            create(MUL, create(3), create("x2"))),
        create(8)));
    assertThat(actual, is(expected));
  }

  @Test
  public void testSoftLinearConstraint1() {
    LinearConstraint cons = LinearConstraint.of("cons", Comparator.LE, 8, 100)
                                .addTerm(1, x[0])
                                .addTerm(-2, x[1])
                                .addTerm(3, x[2])
                                .build();
    List<Expression> actual = translator.translate(cons);
    List<Expression> expected = Arrays.asList(
        create(INT_DEFINITION, create("_P__cons"), create(0), create(2)),
        create(LE,
            create(ADD, create(MUL, create(1), create("x0")), create(MUL, create(-2), create("x1")),
                create(MUL, create(3), create("x2")), create(NEG, create("_P__cons"))),
            create(8)));
    assertThat(actual, is(expected));
  }

  @Test
  public void testSoftLinearConstraint2() throws Exception {
    LinearConstraint cons = LinearConstraint.of("cons", Comparator.EQ, 8, 100)
                                .addTerm(1, x[0])
                                .addTerm(-2, x[1])
                                .addTerm(3, x[2])
                                .build();
    List<Expression> actual = translator.translate(cons);

    List<Expression> expected = new Parser(
        new BufferedReader(new StringReader("(int _P__cons 0 10)"
            + "(int _P1__cons 0 10)"
            + "(int _P2__cons 0 2)"
            + "(eq _P__cons (add _P1__cons _P2__cons))"
            + "(eq (add (mul 1 x0) (mul -2 x1) (mul 3 x2) _P1__cons (neg _P2__cons)) 8)")))
                                    .parse();
    assertThat(actual, is(expected));
  }

  @Test
  public void testHardPseudoBooleanConstraint() {
    PseudoBooleanConstraint cons = PseudoBooleanConstraint.of("cons", Comparator.GE, 8)
                                       .addTerm(1, x[0], 1)
                                       .addTerm(-2, x[1], 1)
                                       .addTerm(3, x[2], 1)
                                       .build();
    List<Expression> actual = translator.translate(cons);
    List<Expression> expected = Arrays.asList(create(GE,
        create(ADD, create(MUL, create(1), create("_x0__1")),
            create(MUL, create(-2), create("_x1__1")), create(MUL, create(3), create("_x2__1"))),
        create(8)));
    assertThat(actual, is(expected));
  }

  @Test
  public void testSoftPseudoBooleanConstraint() {
    PseudoBooleanConstraint cons = PseudoBooleanConstraint.of("cons", Comparator.GT, 8, 100)
                                       .addTerm(1, x[0], 1)
                                       .addTerm(-2, x[1], 1)
                                       .addTerm(3, x[2], 1)
                                       .build();
    List<Expression> actual = translator.translate(cons);
    List<Expression> expected =
        Arrays.asList(create(INT_DEFINITION, create("_P__cons"), create(0), create(11)),
            create(GT,
                create(ADD, create(MUL, create(1), create("_x0__1")),
                    create(MUL, create(-2), create("_x1__1")),
                    create(MUL, create(3), create("_x2__1")), create("_P__cons")),
                create(8)));
    assertThat(actual, is(expected));
  }

  @Test
  public void testObjective() throws Exception {
    ConflictPointConstraint cons1 = ConflictPointConstraint.of("cons1", 10)
                                        .addTerm(x[0], 1, true)
                                        .addTerm(x[1], 2, true)
                                        .addTerm(x[2], 3, false)
                                        .build();
    translator.translate(cons1);
    LinearConstraint cons2 = LinearConstraint.of("cons2", Comparator.LE, 8, 20)
                                 .addTerm(1, x[0])
                                 .addTerm(-2, x[1])
                                 .addTerm(3, x[2])
                                 .build();
    translator.translate(cons2);
    PseudoBooleanConstraint cons3 = PseudoBooleanConstraint.of("cons3", Comparator.GT, 8, 20)
                                        .addTerm(1, x[0], 1)
                                        .addTerm(-2, x[1], 1)
                                        .addTerm(3, x[2], 1)
                                        .build();
    translator.translate(cons3);

    List<Expression> actual = translator.translateObjective();
    List<Expression> expected = new Parser(new BufferedReader(new StringReader("(int _P 0 270)"
                                               + "(objective minimize _P)"
                                               + "(int _P020 0 13)"
                                               + "(eq _P020 (add _P__cons2 _P__cons3))"
                                               + "(int _P010 0 1)"
                                               + "(eq _P010 (add _P__cons1))"
                                               + "(eq _P (add (mul 20 _P020) (mul 10 _P010)))")))
                                    .parse();

    assertThat(actual, is(expected));
  }
}
