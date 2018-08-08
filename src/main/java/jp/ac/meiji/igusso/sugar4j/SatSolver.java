package jp.ac.meiji.igusso.sugar4j;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;

public interface SatSolver extends Closeable {
  public static final int SAT = 10;
  public static final int UNSAT = 20;
  public static final int INTERRUPTED = 0;

  public String getName();

  public void add(int... clause);

  public void add(Collection<Integer> clause);

  public void assume(int literal);

  public List<Integer> solve();

  public List<Integer> solve(long timeout);

  @Override public void close();
}
