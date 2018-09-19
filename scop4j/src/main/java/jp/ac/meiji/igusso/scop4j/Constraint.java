package jp.ac.meiji.igusso.scop4j;

import java.util.regex.Pattern;

/**
 * 各種制約を表すインタフェース.
 */
public interface Constraint extends Comparable<Constraint> {
  public static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

  /**
   * 制約の名前を返す.
   */
  public String getName();

  /**
   * 制約がハード制約かを返す.
   */
  public boolean isHard();

  /**
   * 制約がソフト制約かを返す.
   */
  public boolean isSoft();

  /**
   * 制約ソフト制約であるときに，その重みを返す.
   */
  public int getWeight();
}
