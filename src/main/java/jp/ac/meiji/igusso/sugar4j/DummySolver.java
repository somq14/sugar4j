package jp.ac.meiji.igusso.sugar4j;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@ToString
@EqualsAndHashCode
public final class DummySolver implements SatSolver {
  private static final DummySolver singleton = new DummySolver();

  private DummySolver() {
  }

  public static DummySolver getInstance() {
    return singleton;
  }

  @Override
  public String getName() {
    return toString();
  }

  @Override
  public void add(@NonNull int... clause) {
  }

  @Override
  public void add(@NonNull Collection<Integer> clause) {
  }

  @Override
  public void assume(int literal) {
  }

  @Override
  public List<Integer> solve() {
    return Arrays.asList();
  }

  @Override
  public List<Integer> solve(long timeout) {
    return solve();
  }

  @Override
  public void close() {
  }
}

