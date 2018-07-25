package jp.ac.meiji.igusso.coptool.scop;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Scop4j {
  public static Scop4j newInstance() {
    return new Scop4jImpl();
  }

  public static Scop4j newInstance(Scop4j scop4j) {
    return new Scop4jImpl(scop4j);
  }

  public int variableSize();

  public List<Variable> getVariables();

  public Variable getVariable(int index);

  public Map<String, Variable> getVariableMap();

  public int constraintSize();

  public List<Constraint> getConstraints();

  public Constraint getConstraint(int index);

  public Map<String, Constraint> getConstraintMap();

  public void addVariable(Variable variable);

  public void addVariables(Collection<Variable> variables);

  public void addConstraint(Constraint constraint);

  public void addConstraints(Collection<Constraint> constraints);

  public void setVerbose(int verbose);

  public int getVerbose();

  public void setSeed(int seed);

  public int getSeed();

  public void setTarget(int target);

  public int getTarget();

  public void setTimeout(int timeout);

  public int getTimeout();

  public void setLogFile(Path logFile);

  public Path getLogFile();

  public void encode(Writer writer);

  public Solution solve();
}
