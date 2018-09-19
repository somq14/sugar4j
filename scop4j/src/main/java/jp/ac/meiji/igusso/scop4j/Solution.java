package jp.ac.meiji.igusso.scop4j;

import java.util.Map;

/**
 * Scopにより得られた解を表す.
 */
public interface Solution {
  /**
   * ハード制約を違反した量を返す.
   */
  public int getHardPenalty();

  /**
   * ソフト制約を違反した量とその重みの積の総和を返す.
   */
  public int getSoftPenalty();

  /**
   * Scopが使用したCPU時間をミリ秒単位で返す.
   */
  public long getCpuTime();

  /**
   * 最後に解が改善したときにScopが使用していたCPU時間をミリ秒単位で返す.
   */
  public long getLastImprovedCpuTime();

  /**
   * Scopが行ったイテレーション回数を返す.
   */
  public long getIteration();

  /**
   * 最後に解が改善したときにScopが行っていたイテレーション回数を返す.
   */
  public long getLastImprovedIteration();

  /**
   * 問題の各変数に対する割り当て値を表すマップを返す.
   */
  public Map<Variable, String> getSolution();

  /**
   * 問題の各制約に対する違反量を表すマップを返す.
   * 違反のない制約はマップに登録されていない.
   */
  public Map<Constraint, Integer> getViolatedConstraints();
}
