package jp.ac.meiji.igusso.sugar4j;

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

  public void addExpression(Expression expression);

  public void addExpressions(Collection<Expression> expressions);

  public void addConstraint(Expression expression);

  public void addConstraints(Collection<Expression> expressions);

  public void addAssumption(Expression boolVariable, boolean isPositive) throws SugarException;

  public void addAssumption(Expression intVariable, Comparator op, int value) throws SugarException;

  public void update() throws SugarException;

  public Solution solve() throws SugarException;

  public Solution solve(long timeout) throws SugarException;

  public int getSatClausesCount() throws SugarException;

  public int getSatVariablesCount() throws SugarException;
}
