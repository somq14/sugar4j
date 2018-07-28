package jp.ac.meiji.igusso.coptool.sugar;

import jp.ac.meiji.igusso.coptool.sat.SatSolver;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.expression.Expression;

import java.io.Closeable;
import java.util.Collection;

public interface Sugar4j extends Closeable {
  public static Sugar4j newInstance(SatSolver solver) {
    return new Sugar4jImpl(solver);
  }

  public Expression addBoolVariable(String name);

  public Expression addIntVariable(String name, int size);

  public Expression addIntVariable(String name, int lowerBound, int upperBound);

  public Expression addIntVariable(String name, Collection<Integer> domain);

  public void addConstraint(Expression expression);

  public void addConstraints(Collection<Expression> expressions);

  public void addAssumption(Expression boolVariable, boolean isPositive);

  public void addAssumption(Expression intVariable, Comparator op, int value);

  public Solution solve() throws SugarException;
}