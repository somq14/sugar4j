package jp.ac.meiji.igusso.coptool.sat;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@ToString
@EqualsAndHashCode
public final class IpasirSolver implements SatSolver {
  private IpasirLibrary ipasir;
  private Pointer solver;
  private int maxLiteral = 0;

  private IpasirSolver(@NonNull String solverName) {
    this.ipasir = Native.loadLibrary(solverName, IpasirLibrary.class);
    this.solver = ipasir.ipasir_init();
    ipasir.ipasir_set_terminate(solver, null, null);
  }

  public static IpasirSolver newInstance(@NonNull String solverName) {
    return new IpasirSolver(solverName);
  }

  public String getName() {
    return ipasir.ipasir_signature();
  }

  public void add(@NonNull int... clause) {
    for (int literal : clause) {
      if (literal == 0) {
        throw new IllegalArgumentException("Literal Must Not Be Zero");
      }

      maxLiteral = Math.max(maxLiteral, Math.abs(literal));
      ipasir.ipasir_add(solver, literal);
    }
    ipasir.ipasir_add(solver, 0);
  }

  public void add(@NonNull Collection<Integer> clause) {
    for (int literal : clause) {
      if (literal == 0) {
        throw new IllegalArgumentException("Literal Must Not Be Zero");
      }

      maxLiteral = Math.max(maxLiteral, Math.abs(literal));
      ipasir.ipasir_add(solver, literal);
    }
    ipasir.ipasir_add(solver, 0);
  }

  public void assume(int literal) {
    if (literal == 0) {
      throw new IllegalArgumentException("Literal Must Not Be Zero");
    }
    ipasir.ipasir_assume(solver, literal);
  }

  public List<Integer> solve() {
    List<Integer> res = new ArrayList<>();
    res.add(ipasir.ipasir_solve(solver));

    if (res.get(0) == IpasirLibrary.SAT) {
      for (int literal = 1; literal <= maxLiteral; literal++) {
        res.add(ipasir.ipasir_val(solver, literal));
      }
    }

    return res;
  }

  @Override
  public void close() {
    if (ipasir != null) {
      ipasir.ipasir_release(solver);
    }
    ipasir = null;
    solver = null;
  }
}
