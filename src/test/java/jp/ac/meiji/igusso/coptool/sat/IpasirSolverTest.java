package jp.ac.meiji.igusso.coptool.sat;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.List;
import java.util.Arrays;

public class IpasirSolverTest {
  SatSolver solver;

  @Before
  public void before() {
    solver = IpasirSolver.newInstance("glueminisat");
  }

  @After
  public void after() {
    solver.close();
  }

  @Test
  public void testName() {
    String actual = solver.getName();
    String expected = "glueminisat2.2.5";
    assertThat(actual, is(expected));
  }

  @Test
  public void testSat() {
    solver.add(1, 2, 3);
    solver.add(-1, -2);
    solver.add(-2, -3);
    solver.add(-3, -1);
    solver.add(1);

    List<Integer> actual = solver.solve();
    List<Integer> expected = Arrays.asList(SatSolver.SAT, 1, -2, -3);

    assertThat(actual, is(expected));
  }

  @Test
  public void testUnsat() {
    solver.add(1, 2, 3);
    solver.add(-1, -2);
    solver.add(-2, -3);
    solver.add(-3, -1);
    solver.add(1);
    solver.add(2);

    List<Integer> actual = solver.solve();
    List<Integer> expected = Arrays.asList(SatSolver.UNSAT);

    assertThat(actual, is(expected));
  }

  @Test
  public void testAssume() {
    solver.add(1, 2, 3);
    solver.add(-1, -2);
    solver.add(-2, -3);
    solver.add(-3, -1);
    solver.assume(1);

    List<Integer> actual;
    List<Integer> expected;

    solver.assume(1);
    actual = solver.solve();
    expected = Arrays.asList(SatSolver.SAT, 1, -2, -3);
    assertThat(actual, is(expected));

    solver.assume(2);
    actual = solver.solve();
    expected = Arrays.asList(SatSolver.SAT, -1, 2, -3);
    assertThat(actual, is(expected));

    solver.assume(3);
    actual = solver.solve();
    expected = Arrays.asList(SatSolver.SAT, -1, -2, 3);
    assertThat(actual, is(expected));

    solver.assume(1);
    solver.assume(2);
    actual = solver.solve();
    expected = Arrays.asList(SatSolver.UNSAT);
    assertThat(actual, is(expected));
  }

  @Test
  public void testIncremental() {
    solver.add(1, 2, 3);
    solver.add(-1, -2);
    solver.add(-2, -3);
    solver.add(-3, -1);

    int actual;
    int expected;

    actual = solver.solve().get(0);
    expected = SatSolver.SAT;
    assertThat(actual, is(expected));

    solver.add(-1);
    actual = solver.solve().get(0);
    expected = SatSolver.SAT;
    assertThat(actual, is(expected));

    solver.add(-2);
    actual = solver.solve().get(0);
    expected = SatSolver.SAT;
    assertThat(actual, is(expected));

    solver.add(-3);
    actual = solver.solve().get(0);
    expected = SatSolver.UNSAT;
    assertThat(actual, is(expected));
  }
}
