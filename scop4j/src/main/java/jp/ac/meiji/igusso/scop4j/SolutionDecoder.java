package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scopの出力ログから情報を取得する機能のあるクラス.
 */
@ToString
@EqualsAndHashCode
public final class SolutionDecoder implements Closeable {
  // # penalty = 0/607 (hard/soft)
  private static final Pattern PENALTY_LINE_PATTERN =
      Pattern.compile("^# penalty = ([\\d]+)/([\\d]+) \\(hard/soft\\)$");
  // # cpu time = 0.05/7.61(s)
  private static final Pattern CPUTIME_LINE_PATTERN =
      Pattern.compile("^# cpu time = ([\\d\\.]+)/([\\d\\.]+)\\(s\\)$");
  // # iteration = 6612/2831424
  private static final Pattern ITERATION_LINE_PATTERN =
      Pattern.compile("^# iteration = ([\\d]+)/([\\d]+)$");
  private static final Pattern BEST_SOLUTION_HEADER_PATTERN =
      Pattern.compile("^\\[best solution\\]$");
  private static final Pattern VIOLATED_CONSTRAINTS_HEADER_PATTERN =
      Pattern.compile("^\\[Violated constraints\\]$");
  // X01_02: AB
  private static final Pattern VARIABLE_MAP_PATTERN = Pattern.compile("^([a-zA-Z]\\w*): (\\w+)$");

  private BufferedReader reader;
  private int hardPenalty;
  private int softPenalty;
  private long cpuTime;
  private long lastImprovedCpuTime;
  private long iteration;
  private long lastImprovedIteration;
  private Map<Variable, String> solution = new TreeMap<>();
  private Map<Constraint, Integer> violatedConstraints = new TreeMap<>();

  public SolutionDecoder(@NonNull Reader reader) {
    this.reader = new BufferedReader(reader);
  }

  private void decodePenalty() throws IOException {
    String line = null;
    Matcher matcher = null;
    do {
      line = reader.readLine();
      if (line == null) {
        throw new ScopException("penalty not found");
      }
      matcher = PENALTY_LINE_PATTERN.matcher(line);
    } while (!matcher.matches());

    this.hardPenalty = Integer.valueOf(matcher.group(1));
    this.softPenalty = Integer.valueOf(matcher.group(2));
  }

  private void decodeCpuTime() throws IOException {
    String line = null;
    Matcher matcher = null;
    do {
      line = reader.readLine();
      if (line == null) {
        throw new ScopException("cputime not found");
      }
      matcher = CPUTIME_LINE_PATTERN.matcher(line);
    } while (!matcher.matches());

    this.lastImprovedCpuTime = Math.round(1000 * Double.valueOf(matcher.group(1)));
    this.cpuTime = Math.round(1000 * Double.valueOf(matcher.group(2)));
  }

  private void decodeIteration() throws IOException {
    String line = null;
    Matcher matcher = null;
    do {
      line = reader.readLine();
      if (line == null) {
        throw new ScopException("iteration not found");
      }
      matcher = ITERATION_LINE_PATTERN.matcher(line);
    } while (!matcher.matches());

    this.lastImprovedIteration = Long.valueOf(matcher.group(1));
    this.iteration = Long.valueOf(matcher.group(2));
  }

  private void decodeBestSolution(Scop4j scop4j) throws IOException {
    String line = null;
    Matcher matcher = null;
    do {
      line = reader.readLine();
      if (line == null) {
        throw new ScopException("best solution header not found");
      }
      matcher = BEST_SOLUTION_HEADER_PATTERN.matcher(line);
    } while (!matcher.matches());

    do {
      line = reader.readLine();
      if (line == null) {
        break;
      }

      matcher = VARIABLE_MAP_PATTERN.matcher(line);
      if (!matcher.matches()) {
        break;
      }

      String variableName = matcher.group(1);
      String value = matcher.group(2);
      this.solution.put(scop4j.getVariableMap().get(variableName), value);
    } while (true);
  }

  private void decodeViolatedConstraints(Scop4j scop4j) throws IOException {
    String line = null;
    Matcher matcher = null;
    do {
      line = reader.readLine();
      if (line == null) {
        throw new ScopException("violated constraints header not found");
      }
      matcher = VIOLATED_CONSTRAINTS_HEADER_PATTERN.matcher(line);
    } while (!matcher.matches());

    do {
      line = reader.readLine();
      if (line == null) {
        break;
      }

      matcher = VARIABLE_MAP_PATTERN.matcher(line);
      if (!matcher.matches()) {
        break;
      }

      String constraintName = matcher.group(1);
      Integer amount = Integer.valueOf(matcher.group(2));
      this.violatedConstraints.put(scop4j.getConstraintMap().get(constraintName), amount);
    } while (true);
  }

  public Solution decode(Scop4j scop4j) {
    try {
      decodePenalty();
      decodeCpuTime();
      decodeIteration();
      decodeBestSolution(scop4j);
      decodeViolatedConstraints(scop4j);
    } catch (Exception ex) {
      throw new ScopException(ex);
    }
    return new SolutionImpl(hardPenalty, softPenalty, cpuTime, lastImprovedCpuTime, iteration,
        lastImprovedIteration, solution, violatedConstraints);
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  @ToString
  @EqualsAndHashCode
  private static class SolutionImpl implements Solution {
    @Getter private final int hardPenalty;
    @Getter private final int softPenalty;
    @Getter private final long cpuTime;
    @Getter private final long lastImprovedCpuTime;
    @Getter private final long iteration;
    @Getter private final long lastImprovedIteration;
    @Getter private final Map<Variable, String> solution;
    @Getter private final Map<Constraint, Integer> violatedConstraints;

    private SolutionImpl(int hardPenalty, int softPenalty, long cpuTime, long lastImprovedCpuTime,
        long iteration, long lastImprovedIteration, Map<Variable, String> solution,
        Map<Constraint, Integer> violatedConstraints) {
      this.hardPenalty = hardPenalty;
      this.softPenalty = softPenalty;
      this.cpuTime = cpuTime;
      this.lastImprovedCpuTime = lastImprovedCpuTime;
      this.iteration = iteration;
      this.lastImprovedIteration = lastImprovedIteration;
      this.solution = Collections.unmodifiableMap(solution);
      this.violatedConstraints = Collections.unmodifiableMap(violatedConstraints);
    }
  }
}
