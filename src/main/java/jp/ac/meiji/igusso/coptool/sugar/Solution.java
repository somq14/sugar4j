package jp.ac.meiji.igusso.coptool.sugar;

import jp.kobe_u.sugar.expression.Expression;

import java.util.Map;

public interface Solution {
  public boolean isSat();

  public boolean isTimeout();

  public Map<Expression, Integer> getIntMap();

  public Map<Expression, Boolean> getBoolMap();
}
