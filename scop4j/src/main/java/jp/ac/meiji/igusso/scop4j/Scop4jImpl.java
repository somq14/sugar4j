package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@ToString
@EqualsAndHashCode
final class Scop4jImpl implements Scop4j {
  private final Map<String, Variable> variableMap;
  private final List<Variable> variables;

  private final Map<String, Constraint> constraintMap;
  private final List<Constraint> constraints;

  @Getter private int verbose = 0;
  @Getter private int seed = new Random().nextInt(1000 * 1000);
  @Getter private int target = 0;
  @Getter private int timeout = 0;
  @Getter private Path logFile = null;

  Scop4jImpl() {
    this.variableMap = new TreeMap<String, Variable>();
    this.variables = new ArrayList<Variable>();
    this.constraintMap = new TreeMap<String, Constraint>();
    this.constraints = new ArrayList<Constraint>();
  }

  Scop4jImpl(Scop4j scop4j) {
    this.variableMap = new TreeMap<String, Variable>(scop4j.getVariableMap());
    this.variables = new ArrayList<Variable>(scop4j.getVariables());
    this.constraintMap = new TreeMap<String, Constraint>(scop4j.getConstraintMap());
    this.constraints = new ArrayList<Constraint>(scop4j.getConstraints());
    this.verbose = verbose;
    this.seed = seed;
    this.target = target;
    this.timeout = timeout;
    this.logFile = logFile;
  }

  @Override
  public int variableSize() {
    return variables.size();
  }

  @Override
  public List<Variable> getVariables() {
    return Collections.unmodifiableList(variables);
  }

  @Override
  public Variable getVariable(int index) {
    return variables.get(index);
  }

  @Override
  public Map<String, Variable> getVariableMap() {
    return Collections.unmodifiableMap(variableMap);
  }

  @Override
  public void addVariable(@NonNull Variable variable) {
    if (variableMap.containsKey(variable.getName())) {
      throw new IllegalArgumentException();
    }
    variables.add(variable);
    variableMap.put(variable.getName(), variable);
  }

  @Override
  public void addVariables(@NonNull Collection<Variable> variables) {
    for (Variable v : variables) {
      addVariable(v);
    }
  }

  @Override
  public int constraintSize() {
    return constraints.size();
  }

  @Override
  public List<Constraint> getConstraints() {
    return Collections.unmodifiableList(constraints);
  }

  @Override
  public Constraint getConstraint(int index) {
    return constraints.get(index);
  }

  @Override
  public Map<String, Constraint> getConstraintMap() {
    return Collections.unmodifiableMap(constraintMap);
  }

  @Override
  public void addConstraint(@NonNull Constraint constraint) {
    if (constraintMap.containsKey(constraint.getName())) {
      throw new IllegalArgumentException();
    }
    constraints.add(constraint);
    constraintMap.put(constraint.getName(), constraint);
  }

  @Override
  public void addConstraints(@NonNull Collection<Constraint> constraints) {
    for (Constraint constraint : constraints) {
      addConstraint(constraint);
    }
  }

  @Override
  public void setVerbose(int verbose) {
    if (verbose < 0 || verbose >= 4) {
      throw new IllegalArgumentException();
    }
    this.verbose = verbose;
  }

  @Override
  public void setSeed(int seed) {
    if (seed < 0) {
      throw new IllegalArgumentException();
    }
    this.seed = seed;
  }

  @Override
  public void setTarget(int target) {
    if (target < 0) {
      throw new IllegalArgumentException();
    }
    this.target = target;
  }

  @Override
  public void setTimeout(int timeout) {
    this.timeout = Math.max(timeout, 0);
  }

  @Override
  public void setLogFile(@NonNull Path logFile) {
    this.logFile = logFile;
  }

  @Override
  public void encode(@NonNull Writer writer) {
    try (ScopEncoder scopEncoder = new ScopEncoder(writer)) {
      scopEncoder.encode(this);
    }
  }

  private List<String> commandHelper() {
    List<String> args = new ArrayList<>();
    args.add("scop");
    if (verbose > 0) {
      args.add("-display");
      args.add(String.valueOf(verbose));
    }

    args.add("-seed");
    args.add(String.valueOf(seed));

    if (timeout > 0) {
      args.add("-time");
      args.add(String.valueOf(timeout));
    }

    if (target > 0) {
      args.add("-target");
      args.add(String.valueOf(target));
    }

    return args;
  }

  @Override
  public Solution solve() {
    Solution solution = null;
    try {
      // encode
      Path scopInputFile = Files.createTempFile(Scop4jImpl.class.getSimpleName(), ".scop");
      encode(new FileWriter(scopInputFile.toFile()));

      // start process
      ProcessBuilder scopBuilder = new ProcessBuilder(commandHelper());
      scopBuilder.redirectErrorStream(true);
      scopBuilder.redirectInput(scopInputFile.toFile());
      if (logFile == null) {
        logFile = Files.createTempFile(Scop4jImpl.class.getSimpleName(), ".log");
      }
      scopBuilder.redirectOutput(logFile.toFile());

      Process scopProcess = scopBuilder.start();
      int exitCode = scopProcess.waitFor();
      if (exitCode != 0) {
        throw new ScopException();
      }

      // decode
      try (SolutionDecoder decoder = new SolutionDecoder(new FileReader(logFile.toFile()))) {
        solution = decoder.decode(this);
      }
    } catch (Exception ex) {
      throw new ScopException(ex);
    }
    return solution;
  }
}
