package jp.ac.meiji.igusso.coptool.sugar;

import static jp.kobe_u.sugar.expression.Expression.create;

import jp.ac.meiji.igusso.coptool.sat.SatSolver;
import jp.kobe_u.sugar.SugarException;
import jp.kobe_u.sugar.converter.Converter;
import jp.kobe_u.sugar.converter.Simplifier;
import jp.kobe_u.sugar.csp.BooleanVariable;
import jp.kobe_u.sugar.csp.CSP;
import jp.kobe_u.sugar.csp.IntegerDomain;
import jp.kobe_u.sugar.csp.IntegerVariable;
import jp.kobe_u.sugar.encoder.Encoder;
import jp.kobe_u.sugar.encoder.FileProblem;
import jp.kobe_u.sugar.encoder.FileProblem;
import jp.kobe_u.sugar.expression.Expression;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@ToString
@EqualsAndHashCode
final class Sugar4jImpl implements Sugar4j {
  private final SatSolver solver;

  private final Converter converter;
  private final CSP csp;
  private final Encoder encoder;

  Sugar4jImpl(@NonNull SatSolver solver) {
    /* こうすると何故か動く */
    Converter.INCREMENTAL_PROPAGATION = false;

    this.solver = solver;

    this.csp = new CSP();
    csp.commit();

    this.converter = new Converter(csp);
    this.encoder = new Encoder(csp);
    this.encoder.problem = new SatSolver2ProblemAdapter(solver);
  }

  private void update() throws SugarException {
    try {
      csp.propagate();
      Simplifier simplifier = new Simplifier(csp);
      simplifier.simplify();
      encoder.encodeDelta();

      csp.commit();
      encoder.commit();
    } catch (IOException ex1) {
      throw new SugarException("IOException occurred", ex1);
    } catch (SugarException ex2) {
      throw ex2;
    }
  }

  @Override
  public Expression addBoolVariable(String name) {
    addConstraint(create(Expression.BOOL_DEFINITION, create(name)));
    return create(name);
  }

  @Override
  public Expression addIntVariable(String name, int size) {
    addConstraint(
        create(Expression.INT_DEFINITION, create(name), Expression.ZERO, create(size - 1)));
    return create(name);
  }

  @Override
  public Expression addIntVariable(String name, int lowerBound, int upperBound) {
    addConstraint(
        create(Expression.INT_DEFINITION, create(name), create(lowerBound), create(upperBound)));
    return Expression.create(name);
  }

  @Override
  public Expression addIntVariable(String name, Collection<Integer> domain) {
    SortedSet<Integer> domainSet = new TreeSet<>();
    for (int value : domain) {
      domainSet.add(value);
    }
    List<Expression> domainList = new ArrayList<>();
    for (int value : domainSet) {
      domainList.add(create(value));
    }

    addConstraint(create(Expression.INT_DEFINITION, create(name), create(domainList)));
    return create(name);
  }

  @Override
  public void addExpression(Expression expression) {
    try {
      converter.convert(expression);
    } catch (SugarException ex) {
      throw new IllegalArgumentException(ex.getMessage() + " : Caused By: " + expression, ex);
    }
  }

  @Override
  public void addExpressions(Collection<Expression> expressions) {
    for (Expression expression : expressions) {
      addExpression(expression);
    }
  }

  @Override
  public void addConstraint(Expression expression) {
    addExpression(expression);
  }

  @Override
  public void addConstraints(Collection<Expression> expressions) {
    for (Expression expression : expressions) {
      addExpression(expression);
    }
  }

  @Override
  public void addAssumption(Expression boolVariable, boolean isPositive) throws SugarException {
    update();

    BooleanVariable variable = csp.getBooleanVariable(boolVariable.stringValue());
    solver.assume(isPositive ? variable.getCode() : -variable.getCode());
  }

  @Override
  public void addAssumption(Expression intVariable, Comparator op, int value)
      throws SugarException {
    if (op == Comparator.LT) {
      addAssumption(intVariable, Comparator.LE, value - 1);
      return;
    }
    if (op == Comparator.GT) {
      addAssumption(intVariable, Comparator.GE, value + 1);
      return;
    }
    if (op == Comparator.EQ) {
      addAssumption(intVariable, Comparator.LE, value);
      addAssumption(intVariable, Comparator.GE, value);
      return;
    }

    update();

    IntegerVariable variable = csp.getIntegerVariable(intVariable.stringValue());

    int baseCode = variable.getCode();
    if (op == Comparator.LE) {
      if (value < variable.getDomain().getLowerBound()) {
        // unsat
        solver.assume(baseCode);
        solver.assume(-baseCode);
        return;
      }

      try {
        Iterator<Integer> it = variable.getDomain().values();
        for (int offset = 0; offset < variable.getDomain().size() - 1; offset++) {
          if (it.next() >= value) {
            solver.assume(baseCode + offset);
          }
        }
      } catch (SugarException ex) {
        ex.printStackTrace();
        throw new IllegalStateException();
      }
    } else if (op == Comparator.GE) {
      if (value > variable.getDomain().getUpperBound()) {
        // unsat
        solver.assume(baseCode);
        solver.assume(-baseCode);
        return;
      }

      try {
        Iterator<Integer> it = variable.getDomain().values();
        for (int offset = 0; offset < variable.getDomain().size() - 1; offset++) {
          if (it.next() < value) {
            solver.assume(-(baseCode + offset));
          }
        }
      } catch (SugarException ex) {
        ex.printStackTrace();
        throw new IllegalStateException();
      }
    } else {
      throw new RuntimeException();
    }
  }

  @ToString
  @EqualsAndHashCode
  private static class SolutionImpl implements Solution {
    @Getter private final boolean sat;
    @Getter private final Map<Expression, Boolean> boolMap;
    @Getter private final Map<Expression, Integer> intMap;

    SolutionImpl(boolean sat, Map<Expression, Boolean> boolMap, Map<Expression, Integer> intMap) {
      this.sat = sat;
      this.boolMap = Collections.unmodifiableMap(boolMap);
      this.intMap = Collections.unmodifiableMap(intMap);
    }
  }

  @Override
  public Solution solve() throws SugarException {
    update();

    List<Integer> solution = solver.solve();
    if (solution.get(0) == SatSolver.UNSAT) {
      return new SolutionImpl(false, new HashMap<>(), new HashMap<>());
    }

    Map<Expression, Integer> intMap = new HashMap<>();
    for (IntegerVariable variable : csp.getIntegerVariables()) {
      IntegerDomain domain = variable.getDomain();
      if (variable.getCode() >= solution.size()) {
        intMap.put(create(variable.getName()), variable.getDomain().getLowerBound());
        continue;
      }

      int offset = 0;
      for (int i = 0; i < domain.size() - 1; i++) {
        int literal = solution.get(variable.getCode() + i);
        if (literal >= 0) {
          break;
        }
        offset++;
      }

      Iterator<Integer> it = domain.values();
      for (int i = 0; i < offset; i++) {
        it.next();
      }
      intMap.put(create(variable.getName()), it.next());
    }

    Map<Expression, Boolean> boolMap = new HashMap<>();
    for (BooleanVariable variable : csp.getBooleanVariables()) {
      if (variable.getCode() >= solution.size()) {
        boolMap.put(create(variable.getName()), true);
        continue;
      }
      boolMap.put(create(variable.getName()), solution.get(variable.getCode()) > 0);
    }

    return new SolutionImpl(true, boolMap, intMap);
  }

  @Override
  public void close() {
    solver.close();
  }
}
