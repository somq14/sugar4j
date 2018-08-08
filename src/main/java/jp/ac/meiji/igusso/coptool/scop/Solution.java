package jp.ac.meiji.igusso.coptool.scop;

import java.util.Map;

public interface Solution {
  public int getHardPenalty();

  public int getSoftPenalty();

  public long getCpuTime();

  public long getLastImprovedCpuTime();

  public long getIteration();

  public long getLastImprovedIteration();

  public Map<Variable, String> getSolution();

  public Map<Constraint, Integer> getViolatedConstraints();
}
