package jp.ac.meiji.igusso.scop4j;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ScopソルバへのJavaインタフェースを提供するオブジェクト.
 */
public interface Scop4j {
  /**
   * Scop4jオブジェクトを生成して, 返す.
   */
  public static Scop4j newInstance() {
    return new Scop4jImpl();
  }

  /**
   * Scop4jオブジェクトを複製して, 返す.
   */
  public static Scop4j newInstance(Scop4j scop4j) {
    return new Scop4jImpl(scop4j);
  }

  /**
   * 追加済みの変数の個数を返す.
   */
  public int variableSize();

  /**
   * 追加済みの変数を返す.
   */
  public List<Variable> getVariables();

  /**
   * index番目の変数を返す.
   */
  public Variable getVariable(int index);

  /**
   * 追加済みの変数の名前とそのVariableオブジェクトを対応付けるマップを返す.
   */
  public Map<String, Variable> getVariableMap();

  /**
   * 追加済みの制約の個数を返す.
   */
  public int constraintSize();

  /**
   * 追加済みの制約を返す.
   */
  public List<Constraint> getConstraints();

  /**
   * index番目の制約を返す.
   */
  public Constraint getConstraint(int index);

  /**
   * 追加済みの制約の名前とそのConstraintオブジェクトを対応付けるマップを返す.
   */
  public Map<String, Constraint> getConstraintMap();

  /**
   * 変数を追加する.
   */
  public void addVariable(Variable variable);

  /**
   * 変数を全て追加する.
   */
  public void addVariables(Collection<Variable> variables);

  /**
   * 制約を追加する.
   */
  public void addConstraint(Constraint constraint);

  /**
   * 制約を全て追加する.
   */
  public void addConstraints(Collection<Constraint> constraints);

  /**
   * Scopのログの詳細度を設定する.
   * 値は0から3で, 大きいほど詳細である.
   * デフォルト値は0である.
   */
  public void setVerbose(int verbose);

  /**
   * Scopのログの詳細度を取得する.
   */
  public int getVerbose();

  /**
   * Scopの乱数シード値を設定する.
   * デフォルトでは乱数による値である.
   */
  public void setSeed(int seed);

  /**
   * Scopの乱数シード値を取得する.
   */
  public int getSeed();

  /**
   * Scopの解の目標値を設定する.
   */
  public void setTarget(int target);

  /**
   * Scopの解の目標値を取得する.
   */
  public int getTarget();

  /**
   * Scopのタイムアウトを設定する.
   * 時間の単位は秒である.
   * デフォルトではタイムアウトなし(これを0で表す)である.
   */
  public void setTimeout(int timeout);

  /**
   * Scopのタイムアウトを取得する.
   */
  public int getTimeout();

  /**
   * Scopのログの出力先を設定する.
   * デフォルトでは, nullである.
   * nullの状態で, solveメソッドを呼び出すと,
   * システム標準の一時ディレクトリに適当なファイルが作成され, 出力先に設定される.
   */
  public void setLogFile(Path logFile);

  /**
   * Scopのログの出力先を取得する.
   */
  public Path getLogFile();

  /**
   * このオブジェクトが表す問題をScopファイルに入力可能なファイルとして出力する.
   */
  public void encode(Writer writer);

  /**
   * このオブジェクトが表す問題をScopを起動し, 解く.
   */
  public Solution solve();
}
