package jp.ac.meiji.igusso.sugar4j;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

// Java CHECKSTYLE:OFF JavadocParagraph
// Java CHECKSTYLE:OFF ParameterName
public interface Ipasir4jLibrary extends Library {
  int SAT = 10;
  int UNSAT = 20;
  int INTERRUPTED = 0;

  Pointer ipasir4j_init(String solver_name);

  String ipasir4j_signature(Pointer solver);

  void ipasir4j_release(Pointer solver);

  void ipasir4j_add(Pointer solver, int lit_or_zero);

  void ipasir4j_assume(Pointer solver, int lit);

  int ipasir4j_solve(Pointer solver);

  int ipasir4j_val(Pointer solver, int lit);

  int ipasir4j_failed(Pointer solver, int lit);

  void ipasir4j_set_terminate(Pointer solver, Pointer state, Callback terminate);

  void ipasir4j_add_all(Pointer solver, int size, int[] lits);

  void ipasir4j_assume_all(Pointer solver, int size, int[] lits);

  void ipasir4j_val_all(Pointer solver, int size, int[] assign);
}
// Java CHECKSTYLE:ON JavadocParagraph
// Java CHECKSTYLE:ON ParameterName
