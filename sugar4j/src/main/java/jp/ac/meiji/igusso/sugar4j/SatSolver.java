package jp.ac.meiji.igusso.sugar4j;

import java.util.Collection;
import java.util.List;

/**
 * SATソルバを表現するインタフェース.
 */
public interface SatSolver extends AutoCloseable {
  public static final int SAT = 10;
  public static final int UNSAT = 20;
  public static final int INTERRUPTED = 0;

  /**
   * SATソルバの名前を返す.
   */
  public String getName();

  /**
   * SATソルバに節を追加する.
   * 節は非ゼロ整数の列で表現する.
   * 例えば, x_1 or x_2 or ~x_3 は { 1, 2, -3 } で表現する.
   */
  public void add(int... clause);

  /**
   * SATソルバに節を追加する.
   * 節は非ゼロ整数の列で表現する.
   * 例えば, x_1 or x_2 or ~x_3 は { 1, 2, -3 } で表現する.
   */
  public void add(Collection<Integer> clause);

  /**
   * SATソルバに変数への割り当てを仮定させる.
   * リテラルは非ゼロの整数で表現する.
   * 例えば, assume(-1)を呼び出すと, SATソルバはx_1 = 0の仮定の元, 探索を行う.
   * 追加した仮定は一度solveメソッドを呼び出すと, リセットされることに注意せよ.
   */
  public void assume(int literal);

  /**
   * SATソルバに現在の問題を解かせる.
   * リストの先頭要素は問題が充足可能かを整数で表す. (定数SAT, UNSATを参照せよ)
   * 続く要素は(存在すれば)充足割り当てを表し,
   * x_iに1を割り当てるときはリストの第i要素はiであり,
   * x_iに0を割り当てるときはリストの第i要素は-iである.
   */
  public List<Integer> solve();

  /**
   * SATソルバにタイムアウト付きで現在の問題を解かせる.
   * リストの先頭要素は問題が充足可能かを整数で表す. (定数SAT, UNSAT, INTERRUPTEDを参照せよ)
   * 続く要素は(存在すれば)充足割り当てを表し,
   * x_iに1を割り当てるときはリストの第i要素はiであり,
   * x_iに0を割り当てるときはリストの第i要素は-iである.
   * タイムアウトの時間の単位は秒である.
   * 0以下の値はタイムアウトなしを意味する.
   */
  public List<Integer> solve(long timeout);

  /**
   * このソルバの資源を開放する.
   */
  @Override public void close();
}
