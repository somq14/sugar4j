package jp.ac.meiji.igusso.coptool.sat;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;

public interface SatSolver extends Closeable {
  public static final int SAT = 10;
  public static final int UNSAT = 20;

  public String getName();

  public void add(int... clause);

  public void add(Collection<Integer> clause);

  public void assume(int literal);

  public List<Integer> solve();

  @Override public void close();
}
