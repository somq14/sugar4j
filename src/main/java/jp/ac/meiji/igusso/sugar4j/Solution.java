package jp.ac.meiji.igusso.sugar4j;

import jp.kobe_u.sugar.expression.Expression;

import java.util.Map;

/**
 * Sugar4jによるCSPの解を表現するインタフェース.
 */
public interface Solution {
  /**
   * 問題が充足可能かを返す.
   */
  public boolean isSat();

  /**
   * タイムアウトにより解が得られなかったかを返す.
   */
  public boolean isTimeout();

  /**
   * 充足可能のとき, 整数変数に対する値割り当てを表すマップを返す.
   */
  public Map<Expression, Integer> getIntMap();

  /**
   * 充足可能のとき, 論理変数に対する値割り当てを表すマップを返す.
   */
  public Map<Expression, Boolean> getBoolMap();
}
