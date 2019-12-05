package jp.ac.meiji.igusso.sugar4j;

import java.util.Collection;
import jp.kobe_u.sugar.expression.Expression;

/**
 * 制約充足ソルバSugarを利用するためのインタフェース.
 */
public interface Sugar4j extends AutoCloseable {
  /**
   * バックエンドに使用するSATソルバを与えて, Sugar4jオブジェクトを生成する.
   */
  static Sugar4j newInstance(SatSolver solver) {
    return new Sugar4jImpl(solver);
  }

  /**
   * 論理変数の宣言を追加する.
   */
  Expression addBoolVariable(String name);

  /**
   * 整数変数の宣言を追加する.
   * 整数の定義域は0以上size未満である.
   */
  Expression addIntVariable(String name, int size);

  /**
   * 整数変数の宣言を追加する.
   * 整数の定義域はlowerBound以上upperBound以下である.
   */
  Expression addIntVariable(String name, int lowerBound, int upperBound);

  /**
   * 整数変数の宣言を追加する.
   */
  Expression addIntVariable(String name, Collection<Integer> domain);

  /**
   * 制約 (変数宣言などでもよい) を追加する.
   */
  void addExpression(Expression expression);

  /**
   * 制約 (変数宣言などでもよい) を全て追加する.
   */
  void addExpressions(Collection<Expression> expressions);

  /**
   * addExpressionと等価.
   */
  void addConstraint(Expression expression);

  /**
   * addExpressionsと等価.
   */
  void addConstraints(Collection<Expression> expressions);

  /**
   * 論理変数への割り当てを仮定する.
   * boolVariableへの割り当てをisPositiveに固定する.
   * 追加した仮定は一度solveメソッドを呼び出すとリセットされることに注意せよ.
   */
  void addAssumption(Expression boolVariable, boolean isPositive);

  /**
   * 整数変数への割り当てを仮定する.
   * intVariableへ割り当てられる値は, valueとのopによる比較が成立するように仮定される.
   * opはExpression.EQ, Expression.LE, Expression.GE, Expression.LT, Expression.GE
   * のいずれかでなければならない.
   * 追加した仮定は一度solveメソッドを呼び出すとリセットされることに注意せよ.
   */
  void addAssumption(Expression intVariable, Expression op, int value);

  /**
   * 遅延されているSAT符号化を実行する.
   */
  void update();

  /**
   * このオブジェクトが表す制約充足問題を解く.
   */
  Solution solve();

  /**
   * このオブジェクトが表す制約充足問題をタイムアウト付きで解く.
   * 時間の単位は秒である.
   * 0以下の値のとき, タイムアウトなしを表す.
   */
  Solution solve(long timeout);

  /**
   * SAT符号化により生成されたSAT節の個数を返す.
   */
  int getSatClausesCount();

  /**
   * SAT符号化により生成されたSAT変数の個数を返す.
   */
  int getSatVariablesCount();
}
