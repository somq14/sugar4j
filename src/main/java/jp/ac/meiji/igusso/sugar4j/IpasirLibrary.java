package jp.ac.meiji.igusso.sugar4j;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * インクリメンタルSAT APIのIPASIRを定義したインタフェース.
 */
// Java CHECKSTYLE:OFF JavadocParagraph
// Java CHECKSTYLE:OFF ParameterName
public interface IpasirLibrary extends Library {
  int SAT = 10;
  int UNSAT = 20;
  int INTERRUPTED = 0;

  /**
   * Return the name and the version of the incremental SAT
   * solving library.
   */
  String ipasir_signature();

  /**
   * Construct a new solver and return a pointer to it.
   * Use the returned pointer as the first parameter in each
   * of the following functions.
   * <p>
   * Required state: N/A
   * State after: INPUT
   */
  Pointer ipasir_init();

  /**
   * Release the solver, i.e., all its resoruces and
   * allocated memory (destructor). The solver pointer
   * cannot be used for any purposes after this call.
   * <p>
   * Required state: INPUT or SAT or UNSAT
   * State after: undefined
   */
  void ipasir_release(Pointer solver);

  /**
   * Add the given literal into the currently added clause
   * or finalize the clause with a 0.  Clauses added this way
   * cannot be removed. The addition of removable clauses
   * can be simulated using activation literals and assumptions.
   * <p>
   * Required state: INPUT or SAT or UNSAT
   * State after: INPUT
   * <p>
   * Literals are encoded as (non-zero) integers as in the
   * DIMACS formats.  They have to be smaller or equal to
   * INT_MAX and strictly larger than INT_MIN (to avoid
   * negation overflow).  This applies to all the literal
   * arguments in API functions.
   */
  void ipasir_add(Pointer solver, int lit_or_zero);

  /**
   * Add an assumption for the next SAT search (the next call
   * of ipasir_solve). After calling ipasir_solve all the
   * previously added assumptions are cleared.
   * <p>
   * Required state: INPUT or SAT or UNSAT
   * State after: INPUT
   */
  void ipasir_assume(Pointer solver, int lit);

  /**
   * Solve the formula with specified clauses under the specified assumptions.
   * If the formula is satisfiable the function returns 10 and the state of the solver is changed to
   * SAT. If the formula is unsatisfiable the function returns 20 and the state of the solver is
   * changed to UNSAT. If the search is interrupted (see ipasir_set_terminate) the function returns
   * 0 and the state of the solver remains INPUT. This function can be called in any defined state
   * of the solver.
   * <p>
   * Required state: INPUT or SAT or UNSAT
   * State after: INPUT or SAT or UNSAT
   */
  int ipasir_solve(Pointer solver);

  /**
   * Get the truth value of the given literal in the found satisfying
   * assignment. Return 'lit' if True, '-lit' if False, and 0 if not important.
   * This function can only be used if ipasir_solve has returned 10
   * and no 'ipasir_add' nor 'ipasir_assume' has been called
   * since then, i.e., the state of the solver is SAT.
   * <p>
   * Required state: SAT
   * State after: SAT
   */
  int ipasir_val(Pointer solver, int lit);

  /**
   * Check if the given assumption literal was used to prove the
   * unsatisfiability of the formula under the assumptions
   * used for the last SAT search. Return 1 if so, 0 otherwise.
   * This function can only be used if ipasir_solve has returned 20 and
   * no ipasir_add or ipasir_assume has been called since then, i.e.,
   * the state of the solver is UNSAT.
   * <p>
   * Required state: UNSAT
   * State after: UNSAT
   */
  int ipasir_failed(Pointer solver, int lit);

  /**
   * Set a callback function used to indicate a termination requirement to the
   * solver. The solver will periodically call this function and check its return
   * value during the search. The ipasir_set_terminate function can be called in any
   * state of the solver, the state remains unchanged after the call.
   * The callback function is of the form "int terminate(void * state)"
   * - it returns a non-zero value if the solver should terminate.
   * - the solver calls the callback function with the parameter "state"
   * having the value passed in the ipasir_set_terminate function (2nd parameter).
   * <p>
   * Required state: INPUT or SAT or UNSAT
   * State after: INPUT or SAT or UNSAT
   */
  void ipasir_set_terminate(Pointer solver, Pointer state, Callback terminate);

  interface IpasirCallback extends Callback {
    int callback(Pointer state);
  }
}
// Java CHECKSTYLE:ON JavadocParagraph
// Java CHECKSTYLE:ON ParameterName
