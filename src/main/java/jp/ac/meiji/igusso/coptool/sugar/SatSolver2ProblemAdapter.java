package jp.ac.meiji.igusso.coptool.sugar;

import jp.ac.meiji.igusso.coptool.sat.SatSolver;
import jp.kobe_u.sugar.encoder.Problem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
final class SatSolver2ProblemAdapter extends Problem {
  private SatSolver solver;

  SatSolver2ProblemAdapter(SatSolver solver) {
    this.solver = solver;
  }

  @Override
  public void clear() {
    if (variablesCount == 0 && clausesCount == 0 && fileSize == 0) {
      return;
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public void cancel() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void done() {
  }

  @Override
  public void addNormalizedClause(int[] clause) {
    solver.add(clause);
  }
}
