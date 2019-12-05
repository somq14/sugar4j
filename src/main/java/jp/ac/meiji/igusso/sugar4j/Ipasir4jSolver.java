package jp.ac.meiji.igusso.sugar4j;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Ipasir4jSolver implements SatSolver {
  private Ipasir4jLibrary ipasir4j;
  private Pointer solver;
  private int maxLiteral = 0;

  private Ipasir4jSolver(@NonNull String solverName) {
    this.ipasir4j = Native.loadLibrary("ipasir4j", Ipasir4jLibrary.class);
    this.solver = ipasir4j.ipasir4j_init("lib" + solverName + ".so"); // FIXME
    if (solver == null) {
      throw new IllegalArgumentException("Solver Not Found : " + solverName);
    }
    ipasir4j.ipasir4j_set_terminate(solver, null, null);
  }

  public static Ipasir4jSolver newInstance(@NonNull String solverName) {
    return new Ipasir4jSolver(solverName);
  }

  /*
   * Buffer
   */
  private static final int BUFFER_SIZE = 4 * 1024 * 1024;
  private int[] bufferToAdd = new int[BUFFER_SIZE];
  private int bufferToAddPointer = 0;
  private int[] bufferToAssume = new int[BUFFER_SIZE];
  private int bufferToAssumePointer = 0;

  private void flushBuffer() {
    ipasir4j.ipasir4j_add_all(solver, bufferToAddPointer, bufferToAdd);
    bufferToAddPointer = 0;

    ipasir4j.ipasir4j_assume_all(solver, bufferToAssumePointer, bufferToAssume);
    bufferToAssumePointer = 0;
  }

  private void addBuffer(int literal) {
    if (bufferToAddPointer >= BUFFER_SIZE) {
      flushBuffer();
    }
    bufferToAdd[bufferToAddPointer++] = literal;
  }

  private void assumeBuffer(int literal) {
    if (bufferToAssumePointer >= BUFFER_SIZE) {
      flushBuffer();
    }
    bufferToAssume[bufferToAssumePointer++] = literal;
  }

  /*
   * Implementation Of SatSolver Interface
   */
  @Override
  public String getName() {
    return ipasir4j.ipasir4j_signature(solver);
  }

  @Override
  public void add(int... clause) {
    for (int literal : clause) {
      if (literal == 0) {
        throw new IllegalArgumentException("Literal Must Not Be Zero");
      }

      maxLiteral = Math.max(maxLiteral, Math.abs(literal));
      addBuffer(literal);
    }
    addBuffer(0);
  }

  @Override
  public void add(Collection<Integer> clause) {
    for (int literal : clause) {
      if (literal == 0) {
        throw new IllegalArgumentException("Literal Must Not Be Zero");
      }

      maxLiteral = Math.max(maxLiteral, Math.abs(literal));
      addBuffer(literal);
    }
    addBuffer(0);
  }

  @Override
  public void assume(int literal) {
    if (literal == 0) {
      throw new IllegalArgumentException("Literal Must Not Be Zero");
    }
    assumeBuffer(literal);
  }

  @Override
  public List<Integer> solve() {
    flushBuffer();

    List<Integer> res = new ArrayList<>();
    res.add(ipasir4j.ipasir4j_solve(solver));

    if (res.get(0) == IpasirLibrary.SAT) {
      int[] assign = new int[maxLiteral];
      ipasir4j.ipasir4j_val_all(solver, maxLiteral, assign);
      for (int literal = 1; literal <= maxLiteral; literal++) {
        res.add(assign[literal - 1]);
      }
    }

    return res;
  }

  private static class TimeoutCallback implements IpasirLibrary.IpasirCallback {
    private final long period;
    private final long beginTime;

    TimeoutCallback(long period) {
      this.period = period;
      this.beginTime = System.currentTimeMillis();
    }

    @Override
    public int callback(Pointer state) {
      long currTime = System.currentTimeMillis();
      return (currTime - beginTime) >= period * 1000L ? 1 : 0;
    }
  }

  @Override
  public List<Integer> solve(long timeout) {
    if (timeout <= 0) {
      return solve();
    }

    ipasir4j.ipasir4j_set_terminate(solver, null, new TimeoutCallback(timeout));
    List<Integer> res = solve();
    ipasir4j.ipasir4j_set_terminate(solver, null, null);
    return res;
  }

  @Override
  public void close() {
    if (ipasir4j != null) {
      ipasir4j.ipasir4j_set_terminate(solver, null, null);
      ipasir4j.ipasir4j_release(solver);
    }
    ipasir4j = null;
    solver = null;
  }
}
