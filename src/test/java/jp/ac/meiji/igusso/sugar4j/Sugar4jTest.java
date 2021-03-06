package jp.ac.meiji.igusso.sugar4j;

import static jp.kobe_u.sugar.expression.Expression.create;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import jp.kobe_u.sugar.expression.Expression;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class Sugar4jTest {
  Sugar4j sugar4j;
  Expression[] bv;
  Expression[] iv;

  @Before
  public void before() {
    sugar4j = Sugar4j.newInstance(IpasirSolver.newInstance("glueminisat"));

    bv = new Expression[3];
    for (int i = 0; i < bv.length; i++) {
      bv[i] = sugar4j.addBoolVariable("bv" + i);
    }

    iv = new Expression[3];
    iv[0] = sugar4j.addIntVariable("iv0", 5);
    iv[1] = sugar4j.addIntVariable("iv1", 5);
    iv[2] = sugar4j.addIntVariable("iv2", 5);
  }

  @After
  public void after() {
    try {
      sugar4j.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Test
  public void testAddBoolVariable() {
    sugar4j.addBoolVariable("var0");
    sugar4j.addBoolVariable("var1");
    sugar4j.addBoolVariable("var2");
  }

  @Test
  public void testAddIntVariable() {
    sugar4j.addIntVariable("var0", 10);
    sugar4j.addIntVariable("var1", 1, 3);
    sugar4j.addIntVariable("var2", Arrays.asList(2, 4, 6));
  }

  @Test
  public void testAddConstraint2() throws Exception {
    sugar4j.addConstraint(create(Expression.IFF, bv[0], create(Expression.XOR, bv[1], bv[2])));
    Solution solution = sugar4j.solve();

    assertThat(solution.isSat(), is(true));

    boolean v0 = solution.getBoolMap().get(bv[0]);
    boolean v1 = solution.getBoolMap().get(bv[1]);
    boolean v2 = solution.getBoolMap().get(bv[2]);
    assertThat(v0 == v1 ^ v2, is(true));
  }

  @Test
  public void testAddConstraint1() throws Exception {
    sugar4j.addConstraint(create(Expression.EQ, iv[0], create(Expression.ADD, iv[1], iv[2])));
    Solution solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));

    int v0 = solution.getIntMap().get(iv[0]);
    int v1 = solution.getIntMap().get(iv[1]);
    int v2 = solution.getIntMap().get(iv[2]);
    assertThat(v0 == v1 + v2, is(true));
  }

  @Test
  public void testIncremental() throws Exception {
    sugar4j.addConstraint(create(Expression.OR, bv[0], bv[1], bv[2]));
    sugar4j.addConstraint(create(Expression.OR, bv[0].not(), bv[1].not()));
    sugar4j.addConstraint(create(Expression.OR, bv[1].not(), bv[2].not()));
    sugar4j.addConstraint(create(Expression.OR, bv[2].not(), bv[0].not()));
    Solution solution = null;

    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));

    sugar4j.addConstraint(bv[0]);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));

    sugar4j.addConstraint(bv[1]);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(false));
  }

  @Test
  public void testBoolAssume() throws Exception {
    sugar4j.addConstraint(create(Expression.OR, bv[0], bv[1], bv[2]));
    sugar4j.addConstraint(create(Expression.OR, bv[0].not(), bv[1].not()));
    sugar4j.addConstraint(create(Expression.OR, bv[1].not(), bv[2].not()));
    sugar4j.addConstraint(create(Expression.OR, bv[1].not(), bv[0].not()));

    Solution solution = null;

    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));

    sugar4j.addAssumption(bv[0], true);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));
    assertThat(solution.getBoolMap().get(bv[0]), is(true));

    sugar4j.addAssumption(bv[0], false);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));
    assertThat(solution.getBoolMap().get(bv[0]), is(false));

    sugar4j.addAssumption(bv[0], false);
    sugar4j.addAssumption(bv[1], false);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));
    assertThat(solution.getBoolMap().get(bv[0]), is(false));
    assertThat(solution.getBoolMap().get(bv[1]), is(false));
    assertThat(solution.getBoolMap().get(bv[2]), is(true));

    sugar4j.addAssumption(bv[0], true);
    sugar4j.addAssumption(bv[1], true);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(false));
  }

  @Test
  public void testIntAssume1() throws Exception {
    sugar4j.addConstraint(create(Expression.LT, iv[0], iv[1]));
    sugar4j.addConstraint(create(Expression.LT, iv[1], iv[2]));

    Solution solution = null;

    int v0;
    int v1;
    int v2;

    solution = sugar4j.solve();
    v0 = solution.getIntMap().get(iv[0]);
    v1 = solution.getIntMap().get(iv[1]);
    v2 = solution.getIntMap().get(iv[2]);
    assertThat(solution.isSat(), is(true));
    assertThat(v0 < v1 && v1 < v2, is(true));

    sugar4j.addAssumption(iv[0], Expression.GE, 2);
    solution = sugar4j.solve();
    v0 = solution.getIntMap().get(iv[0]);
    v1 = solution.getIntMap().get(iv[1]);
    v2 = solution.getIntMap().get(iv[2]);
    assertThat(solution.isSat(), is(true));
    assertThat(v0, is(2));
    assertThat(v1, is(3));
    assertThat(v2, is(4));

    sugar4j.addAssumption(iv[0], Expression.GE, 3);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(false));
  }

  @Test
  public void testIntAssume2() throws Exception {
    sugar4j.addConstraint(create(Expression.EQ, iv[0], iv[1]));
    sugar4j.addConstraint(create(Expression.EQ, iv[1], iv[2]));

    sugar4j.addAssumption(iv[0], Expression.GE, 10);

    Solution solution = sugar4j.solve();
    assertThat(solution.isSat(), is(false));
  }

  @Test
  public void testIntAssume3() throws Exception {
    sugar4j.addConstraint(create(Expression.EQ, iv[0], iv[1]));
    sugar4j.addConstraint(create(Expression.EQ, iv[1], iv[2]));

    sugar4j.addAssumption(iv[0], Expression.LE, -1);

    Solution solution = sugar4j.solve();
    assertThat(solution.isSat(), is(false));
  }

  @Test
  public void testIntAssume4() throws Exception {
    sugar4j.addConstraint(create(Expression.EQ, iv[0], iv[1]));
    sugar4j.addConstraint(create(Expression.EQ, iv[1], iv[2]));

    Solution solution = null;

    sugar4j.addAssumption(iv[0], Expression.EQ, 1);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));
    assertThat(solution.getIntMap().get(iv[0]), is(1));

    sugar4j.addAssumption(iv[0], Expression.EQ, 2);
    solution = sugar4j.solve();
    assertThat(solution.isSat(), is(true));
    assertThat(solution.getIntMap().get(iv[0]), is(2));
  }

  @Test
  public void testEnumeration() throws Exception {
    sugar4j.addConstraint(create(Expression.EQ, iv[0], create(Expression.ADD, iv[1], iv[2])));
    sugar4j.addConstraint(create(Expression.LE, iv[1], iv[2]));

    Set<Set<Integer>> actual = new HashSet<>();
    while (true) {
      Solution solution = sugar4j.solve();
      if (!solution.isSat()) {
        break;
      }
      int v0 = solution.getIntMap().get(iv[0]);
      int v1 = solution.getIntMap().get(iv[1]);
      int v2 = solution.getIntMap().get(iv[2]);
      assertThat(v0 == v1 + v2, is(true));
      sugar4j.addConstraint(create(Expression.OR, create(Expression.NE, iv[0], create(v0)),
          create(Expression.NE, iv[1], create(v1)), create(Expression.NE, iv[2], create(v2))));

      Set<Integer> ans = new HashSet<>();
      ans.add(v0);
      ans.add(v1);
      ans.add(v2);
      actual.add(ans);
    }

    Set<Set<Integer>> expected = new HashSet<>();
    for (int v0 = 0; v0 < 5; v0++) {
      for (int v1 = 0; v1 < 5; v1++) {
        for (int v2 = 0; v2 < 5; v2++) {
          if (v0 == v1 + v2 && v1 <= v2) {
            Set<Integer> ans = new HashSet<>();
            ans.add(v0);
            ans.add(v1);
            ans.add(v2);
            expected.add(ans);
          }
        }
      }
    }
    assertThat(actual, is(expected));
  }

  // @Test
  public void testTimeout() throws Exception {
    Random rand = new Random();

    Expression[] v = new Expression[1000];
    for (int i = 0; i < v.length; i++) {
      v[i] = sugar4j.addBoolVariable("v" + i);
    }

    List<Expression> terms = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      terms.add(null);
    }

    int n = 1000 * 1000;
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < terms.size(); j++) {
        int lit = rand.nextInt(v.length);
        terms.set(j, rand.nextBoolean() ? v[lit] : v[lit].not());
      }
      sugar4j.addConstraint(create(Expression.OR, terms));
    }

    assertThat(sugar4j.solve(1).isTimeout(), is(true));
  }
}
