package jp.ac.meiji.igusso.scheduling;

import static java.lang.String.format;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Cover;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Shift;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOffRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOnRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Staff;
import static jp.kobe_u.sugar.expression.Expression.ADD;
import static jp.kobe_u.sugar.expression.Expression.EQ;
import static jp.kobe_u.sugar.expression.Expression.GE;
import static jp.kobe_u.sugar.expression.Expression.INT_DEFINITION;
import static jp.kobe_u.sugar.expression.Expression.LE;
import static jp.kobe_u.sugar.expression.Expression.MUL;
import static jp.kobe_u.sugar.expression.Expression.NEG;
import static jp.kobe_u.sugar.expression.Expression.ONE;
import static jp.kobe_u.sugar.expression.Expression.OR;
import static jp.kobe_u.sugar.expression.Expression.ZERO;
import static jp.kobe_u.sugar.expression.Expression.create;

import jp.kobe_u.sugar.expression.Expression;
import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

// Java CHECKSTYLE:OFF MemberName
// Java CHECKSTYLE:OFF AbbreviationAsWordInName
// Java CHECKSTYLE:OFF LocalVariableName
public final class Sugar4jFormulator {
  private final SchedulingProblem problem;

  // Parameters (UPPER_SNAKE_CASE)
  private int H;
  private int[] I;
  private int[] D;
  private int[] W;
  private int[] T;
  private int[][] R;
  private int[][] N;
  private int[] L;
  private int[][] M_MAX;
  private int[] B_MIN;
  private int[] B_MAX;
  private int[] C_MIN;
  private int[] C_MAX;
  private int[] O_MIN;
  private int[] A_MAX;
  private int[][][] Q;
  private int[][][] P;
  private int[][] U;
  private int[][] V_MIN;
  private int[][] V_MAX;

  private List<Integer> SHIFT_LENGTH_SET;

  // Variables (lowercase)
  /** x[i][d][t] : スタッフiがd日目にシフトtに従事するかを表す. */
  private Expression[][][] x;
  /** xt[i][d] : スタッフiのd日目の勤務時間を表す. */
  private Expression[][] xt;
  /** k[i][w] : スタッフiが週末wを休暇にしないとき1, そうでないとき0. */
  private Expression[][] k;

  public Sugar4jFormulator(@NonNull SchedulingProblem problem) {
    this.problem = problem;

    initializeParameters();
    initializeVariables();
  }

  private void initializeParameters() {
    List<Staff> iList = new ArrayList<>();
    for (String key : problem.getStaff().keySet()) {
      iList.add(problem.getStaff().get(key));
    }

    Map<String, Integer> iMap = new HashMap<>();
    for (int i = 0; i < iList.size(); i++) {
      iMap.put(iList.get(i).getId(), i);
    }

    List<Shift> tList = new ArrayList<>();
    for (String key : problem.getShifts().keySet()) {
      tList.add(problem.getShifts().get(key));
    }

    Map<String, Integer> tMap = new HashMap<>();
    for (int t = 1; t <= tList.size(); t++) {
      tMap.put(tList.get(t - 1).getId(), t);
    }

    // initialize parameters
    H = problem.getLength();

    I = new int[problem.getStaff().size()];
    for (int i = 0; i < I.length; i++) {
      I[i] = i;
    }

    D = new int[H];
    for (int d = 0; d < D.length; d++) {
      D[d] = d;
    }

    W = new int[H / 7];
    for (int w = 0; w < H / 7; w++) {
      W[w] = w;
    }

    T = new int[problem.getShifts().size() + 1];
    for (int t = 0; t < T.length; t++) {
      T[t] = t;
    }

    R = new int[T.length][];
    R[0] = null;
    for (int t = 1; t < T.length; t++) {
      List<String> notFollow = tList.get(t - 1).getNotFollow();

      R[t] = new int[notFollow.size()];
      for (int j = 0; j < notFollow.size(); j++) {
        R[t][j] = tMap.get(notFollow.get(j));
      }
    }

    N = new int[I.length][];
    for (int i : I) {
      List<Integer> daysOff = problem.getDaysOff().get(iList.get(i).getId()).getDayIndexes();

      N[i] = new int[daysOff.size()];
      for (int j = 0; j < daysOff.size(); j++) {
        N[i][j] = daysOff.get(j);
      }
    }

    L = new int[T.length];
    for (int t = 1; t < T.length; t++) {
      L[t] = tList.get(t - 1).getLength();
    }

    M_MAX = new int[I.length][T.length];
    for (int i : I) {
      for (int t = 1; t < T.length; t++) {
        M_MAX[i][t] = iList.get(i).getMaxShifts().get(tList.get(t - 1).getId());
      }
    }

    B_MIN = new int[I.length];
    B_MAX = new int[I.length];
    C_MIN = new int[I.length];
    C_MAX = new int[I.length];
    O_MIN = new int[I.length];
    A_MAX = new int[I.length];
    for (int i : I) {
      B_MIN[i] = iList.get(i).getMinTotalMinutes();
      B_MAX[i] = iList.get(i).getMaxTotalMinutes();
      C_MIN[i] = iList.get(i).getMinConsecutiveShifts();
      C_MAX[i] = iList.get(i).getMaxConsecutiveShifts();
      O_MIN[i] = iList.get(i).getMinConsecutiveDayOff();
      A_MAX[i] = iList.get(i).getMaxWeekends();
    }

    Q = new int[I.length][D.length][T.length];
    for (ShiftOnRequests sor : problem.getShiftOnRequests()) {
      int i = iMap.get(sor.getStaffId());
      int d = sor.getDay();
      int t = tMap.get(sor.getShiftId());
      Q[i][d][t] = sor.getWeight();
    }

    P = new int[I.length][D.length][T.length];
    for (ShiftOffRequests sor : problem.getShiftOffRequests()) {
      int i = iMap.get(sor.getStaffId());
      int d = sor.getDay();
      int t = tMap.get(sor.getShiftId());
      P[i][d][t] = sor.getWeight();
    }

    U = new int[D.length][T.length];
    V_MIN = new int[D.length][T.length];
    V_MAX = new int[D.length][T.length];
    for (Cover cvr : problem.getCover()) {
      int d = cvr.getDay();
      int t = tMap.get(cvr.getShiftId());
      U[d][t] = cvr.getRequirement();
      V_MIN[d][t] = cvr.getWeightUnder();
      V_MAX[d][t] = cvr.getWeightOver();
    }

    SortedSet<Integer> shiftLengthSet = new TreeSet<>();
    shiftLengthSet.add(0);
    for (int t = 1; t < T.length; t++) {
      shiftLengthSet.add(L[t]);
    }
    SHIFT_LENGTH_SET = Collections.unmodifiableList(new ArrayList<>(shiftLengthSet));
  }

  private void initializeVariables() {
    x = new Expression[I.length][D.length][T.length];
    for (int i : I) {
      for (int d : D) {
        for (int t : T) {
          x[i][d][t] = create(format("x_i%02d_d%02d_t%02d", i, d, t));
        }
      }
    }

    xt = new Expression[I.length][D.length];
    for (int i : I) {
      for (int d : D) {
        xt[i][d] = create(format("xt_i%02d_d%02d", i, d));
      }
    }

    k = new Expression[I.length][W.length];
    for (int i : I) {
      for (int w : W) {
        k[i][w] = create(format("k_i%02d_w%02d", i, w));
      }
    }
  }

  public List<Expression> generateVariables() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t : T) {
          res.add(create(INT_DEFINITION, x[i][d][t], ZERO, ONE));
        }
      }
    }

    for (int i : I) {
      for (int d : D) {
        List<Expression> terms = new ArrayList<>();
        for (int l : SHIFT_LENGTH_SET) {
          terms.add(create(l));
        }
        res.add(create(INT_DEFINITION, xt[i][d], create(terms)));
      }
    }

    for (int i : I) {
      for (int w : W) {
        res.add(create(INT_DEFINITION, k[i][w], ZERO, ONE));
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C01:
   * 任意のスタッフi，任意の日dについて，スタッフiがd日目に勤めるシフトは1個以下である.
   * (1人のスタッフが1日に2個以上のシフトを勤めることはない)
   */
  public List<Expression> generateConstraint1() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t1 = 0; t1 < T.length; t1++) {
          for (int t2 = t1 + 1; t2 < T.length; t2++) {
            Expression cons =
                create(OR, create(LE, x[i][d][t1], ZERO), create(LE, x[i][d][t2], ZERO));
            cons.setComment(format("C01UB_i%02d_d%02d_t%02d_t%02d", i, d, t1, t2));
            res.add(cons);
          }
        }

        List<Expression> terms = new ArrayList<>();
        for (int t = 0; t < T.length; t++) {
          terms.add(create(GE, x[i][d][t], ONE));
        }
        Expression cons = create(OR, terms);
        cons.setComment(format("C01LB_i%02d_d%02d", i, d));
        res.add(cons);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C02:
   * 任意のシフトtに対して，その翌日に勤めてはならないシフトの集合R[t]が定められている.
   * 任意のスタッフi，最終日を除く任意の日d，任意のシフトt1，任意のシフトt2 \in R[t1]に対して，
   * [ スタッフiがd日目にシフトt1を勤め，更に(d + 1)日目にシフトt2を勤める ] ことは許されない.
   */
  public List<Expression> generateConstraint2() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d = 0; d < D.length - 1; d++) {
        for (int t1 = 1; t1 < T.length; t1++) {
          for (int t2 : R[t1]) {
            Expression cons =
                create(OR, create(LE, x[i][d][t1], ZERO), create(LE, x[i][d + 1][t2], ZERO));
            cons.setComment(format("C02_i%02d_d%02d_t%02d_r%02d", i, d, t1, t2));
            res.add(cons);
          }
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C03:
   * 任意のスタッフi，任意のシフトtに対して，
   * スタッフiがシフトtを勤めることのできる最大回数M_MAX[i][t]が定められている.
   */
  public List<Expression> generateConstraint3() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int t = 1; t < T.length; t++) {
        if (M_MAX[i][t] >= H) {
          continue;
        }

        List<Expression> terms = new ArrayList<>();
        for (int d : D) {
          terms.add(x[i][d][t]);
        }

        Expression cons = create(LE, create(ADD, terms), create(M_MAX[i][t]));
        cons.setComment(format("C03_i%02d_t%02d", i, t));
        res.add(cons);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C04:
   * 任意のスタッフiに対して， 総勤務時間の下限B_MIN[i]と上限B_MAX[i]が定められている.
   */
  public List<Expression> generateConstraint4() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        List<Expression> terms = new ArrayList<>();
        for (int t = 1; t < T.length; t++) {
          terms.add(create(MUL, create(L[t]), x[i][d][t]));
        }
        Expression cons = create(EQ, xt[i][d], create(ADD, terms));
        cons.setComment(format("BIND_TO_xt_i%02d_d%02d", i, d));
        res.add(cons);
      }
    }

    for (int i : I) {
      List<Expression> terms = new ArrayList<>();
      for (int d : D) {
        terms.add(xt[i][d]);
      }

      Expression consLb = create(GE, create(ADD, terms), create(B_MIN[i]));
      consLb.setComment(format("C04LB_i%02d", i));
      res.add(consLb);

      Expression consUb = create(LE, create(ADD, terms), create(B_MAX[i]));
      consUb.setComment(format("C04UB_i%02d", i));
      res.add(consUb);
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C05:
   * 任意のスタッフiに対して，連続で勤務する日数の上限C_MAX[i]が定められている.
   * 任意の連続するC_MAX[i] + 1日間に関して，休暇の回数が1日以上であればよい
   */
  public List<Expression> generateConstraint5() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d = 0; d < D.length - C_MAX[i]; d++) {
        List<Expression> terms = new ArrayList<>();
        for (int j = d; j <= d + C_MAX[i]; j++) {
          terms.add(create(GE, x[i][j][0], ONE));
        }

        Expression cons = create(OR, terms);
        cons.setComment(format("C05_i%02d_d%02d", i, d));
        res.add(cons);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C06:
   * 任意のスタッフiに対し，連続勤務日数の下限C_MIN[i]が定められている.
   * (連続勤務日数とはある休暇から次の休暇までの勤務日数)
   * すなわち，C_MIN[i]日未満の連続勤務は許されない
   *
   * s日の連続勤務が許されないという制約は次のように表現できる
   * 任意の連続する(s + 2)日間について，
   * [その初日と最終日が休暇であり，その間は毎日勤務する]ことは許されない
   */
  public List<Expression> generateConstraint6() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int s = 1; s < C_MIN[i]; s++) {
        // ここでs日の連続勤務を許さないという制約を生成
        for (int d = 0; d < D.length - (s + 1); d++) {
          List<Expression> terms = new ArrayList<>();
          terms.add(create(LE, x[i][d][0], ZERO));
          for (int j = d + 1; j < d + s + 1; j++) {
            terms.add(create(GE, x[i][j][0], ONE));
          }
          terms.add(create(LE, x[i][d + s + 1][0], ZERO));

          Expression cons = create(OR, terms);
          cons.setComment(format("C06_i%02d_s%02d_d%02d", i, s, d));
          res.add(cons);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C07:
   * 任意のスタッフiに対し，連続休暇日数の下限O_MIN[i]が定められている.
   * (連続休暇日数とはある勤務から次の勤務までの休暇日数)
   * すなわち，O_MIN[i]日未満の連続休暇は許されない
   * 制約はC06と同様に生成する
   */
  public List<Expression> generateConstraint7() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int s = 1; s < O_MIN[i]; s++) {
        // ここでs日の連続休暇を許さないという制約を生成
        for (int d = 0; d < D.length - (s + 1); d++) {
          List<Expression> terms = new ArrayList<>();
          terms.add(create(GE, x[i][d][0], ONE));
          for (int j = d + 1; j < d + s + 1; j++) {
            terms.add(create(LE, x[i][j][0], ZERO));
          }
          terms.add(create(GE, x[i][d + s + 1][0], ONE));

          Expression cons = create(OR, terms);
          cons.setComment(format("C07_i%02d_s%02d_d%02d", i, s, d));
          res.add(cons);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C08:
   * 任意のスタッフi，任意の週末 (土曜日と日曜日) wに対し,
   * 週末を休暇にしない回数の上限A_MAX[i]が定められている.
   * 週末を休暇にしない回数はA_MAX[i]以下でなければならない
   *
   * 制約C08L, C08Rは，変数k[i][w]をスタッフiが週末wを休暇にしないとき1
   * そうでないとき0にするための制約である
   */
  public List<Expression> generateConstraint8() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int w : W) {
        {
          Expression cons = create(OR, create(GE, x[i][7 * w + 5][0], ONE),
              create(GE, x[i][7 * w + 6][0], ONE), create(GE, k[i][w], ONE));
          cons.setComment(format("C08A_i%02d_w%d", i, w));
          res.add(cons);
        }
        {
          Expression cons = create(OR, create(GE, x[i][7 * w + 5][0], ONE),
              create(LE, x[i][7 * w + 6][0], ZERO), create(GE, k[i][w], ONE));
          cons.setComment(format("C08B_i%02d_w%d", i, w));
          res.add(cons);
        }
        {
          Expression cons = create(OR, create(LE, x[i][7 * w + 5][0], ZERO),
              create(GE, x[i][7 * w + 6][0], ONE), create(GE, k[i][w], ONE));
          cons.setComment(format("C08C_i%02d_w%d", i, w));
          res.add(cons);
        }
        {
          Expression cons = create(OR, create(LE, x[i][7 * w + 5][0], ZERO),
              create(LE, x[i][7 * w + 6][0], ZERO), create(LE, k[i][w], ZERO));
          cons.setComment(format("C08D_i%02d_w%d", i, w));
          res.add(cons);
        }
      }

      List<Expression> terms = new ArrayList<>();
      for (int w : W) {
        terms.add(k[i][w]);
      }

      Expression cons = create(LE, create(ADD, terms), create(A_MAX[i]));
      cons.setComment(format("C08_i%02d", i));
      res.add(cons);
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C09:
   * 任意のスタッフiに対し，スタッフiが勤務不能な日の集合N[i]が定められている.
   */
  public List<Expression> generateConstraint9() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d : N[i]) {
        Expression cons = create(EQ, x[i][d][0], ONE);
        cons.setComment(format("C09_i%02d_d%02d", i, d));
        res.add(cons);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C11:
   * 任意のスタッフi，任意の日d，任意のシフトtに対し，
   * スタッフiがd日目にシフトtで勤務しないときのペナルティQ[i][d][t]が定められている.
   */
  public List<Expression> generateConstraint11() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (Q[i][d][t] == 0) {
            continue;
          }

          Expression penalty = create(format("P_C11_i%02d_d%02d_t%02d", i, d, t));
          res.add(create(INT_DEFINITION, penalty, ZERO, ONE));

          Expression cons = create(OR, create(GE, x[i][d][t], ONE), create(GE, penalty, ONE));
          cons.setComment(format("C11_i%02d_d%02d_t%02d", i, d, t));
          res.add(cons);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C12: 任意のスタッフi，任意の日d，任意のシフトtに対し，
   * スタッフiがd日目にシフトtで勤務するときのペナルティP[i][d][t]が定められている.
   */
  public List<Expression> generateConstraint12() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (P[i][d][t] == 0) {
            continue;
          }

          Expression penalty = create(format("P_C12_i%02d_d%02d_t%02d", i, d, t));
          res.add(create(INT_DEFINITION, penalty, ZERO, ONE));

          Expression cons = create(OR, create(LE, x[i][d][t], ZERO), create(GE, penalty, ONE));
          cons.setComment(format("C12_i%02d_d%02d_t%02d", i, d, t));
          res.add(cons);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C13: 任意の日d，任意のシフトtに対し，適正スタッフ数U[d][t]が定められており，
   * それから1人不足するごとに与えられるペナルティV_MIN[d][t]が定められている.
   */
  public List<Expression> generateConstraint13() {
    List<Expression> res = new ArrayList<>();

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MIN[d][t] == 0) {
          continue;
        }

        Expression penalty = create(format("P_C13_d%02d_t%02d", d, t));
        res.add(create(INT_DEFINITION, penalty, ZERO, create(U[d][t])));

        List<Expression> terms = new ArrayList<>();
        for (int i : I) {
          terms.add(x[i][d][t]);
        }
        terms.add(penalty);

        Expression cons = create(GE, create(ADD, terms), create(U[d][t]));
        cons.setComment(format("C13_d%02d_t%02d", d, t));
        res.add(cons);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C14:
   * 任意の日d，任意のシフトtに対し，適正スタッフ数U[d][t]が定められており，
   * それを1人超過するごとに与えられるペナルティV_MAX[d][t]が定められている.
   */
  public List<Expression> generateConstraint14() {
    List<Expression> res = new ArrayList<>();

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MIN[d][t] == 0) {
          continue;
        }

        Expression penalty = create(format("P_C14_d%02d_t%02d", d, t));
        res.add(create(INT_DEFINITION, penalty, ZERO, create(I.length - U[d][t])));

        List<Expression> terms = new ArrayList<>();
        for (int i : I) {
          terms.add(x[i][d][t]);
        }
        terms.add(create(NEG, penalty));

        Expression cons = create(LE, create(ADD, terms), create(U[d][t]));
        cons.setComment(format("C14_d%02d_t%02d", d, t));
        res.add(cons);
      }
    }

    return Collections.unmodifiableList(res);
  }

  public List<Expression> generateAllConstraints() {
    List<Expression> res = new ArrayList<>();
    res.addAll(generateConstraint1());
    res.addAll(generateConstraint2());
    res.addAll(generateConstraint3());
    res.addAll(generateConstraint4());
    res.addAll(generateConstraint5());
    res.addAll(generateConstraint6());
    res.addAll(generateConstraint7());
    res.addAll(generateConstraint8());
    res.addAll(generateConstraint9());
    res.addAll(generateConstraint11());
    res.addAll(generateConstraint12());
    res.addAll(generateConstraint13());
    res.addAll(generateConstraint14());
    return Collections.unmodifiableList(res);
  }

  @Value
  private static class PenaltyVariable {
    String name;
    int weight;
    int upperBound;
  }

  private List<PenaltyVariable> getPenaltyVariables() {
    List<PenaltyVariable> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (Q[i][d][t] == 0) {
            continue;
          }
          String varName = format("P_C11_i%02d_d%02d_t%02d", i, d, t);
          res.add(new PenaltyVariable(varName, Q[i][d][t], 1));
        }
      }
    }

    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (P[i][d][t] == 0) {
            continue;
          }

          String varName = format("P_C12_i%02d_d%02d_t%02d", i, d, t);
          res.add(new PenaltyVariable(varName, P[i][d][t], 1));
        }
      }
    }

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MIN[d][t] == 0) {
          continue;
        }

        String varName = format("P_C13_d%02d_t%02d", d, t);
        res.add(new PenaltyVariable(varName, V_MIN[d][t], U[d][t]));
      }
    }

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MAX[d][t] == 0) {
          continue;
        }

        String varName = format("P_C14_d%02d_t%02d", d, t);
        res.add(new PenaltyVariable(varName, V_MAX[d][t], I.length - U[d][t]));
      }
    }

    return Collections.unmodifiableList(res);
  }

  private List<Expression> generateObjective(
      String objName, List<PenaltyVariable> penaltyVariables) {
    List<Expression> res = new ArrayList<>();

    int maxPena = 0;
    for (PenaltyVariable v : penaltyVariables) {
      maxPena += v.getWeight() * v.getUpperBound();
    }

    Expression obj = create(objName);
    res.add(create(INT_DEFINITION, obj, ZERO, create(maxPena)));
    res.add(create(Expression.OBJECTIVE_DEFINITION, Expression.MINIMIZE, obj));

    Map<Integer, List<PenaltyVariable>> group = new HashMap<>();
    for (PenaltyVariable v : penaltyVariables) {
      if (!group.containsKey(v.getWeight())) {
        group.put(v.getWeight(), new ArrayList<>());
      }
      group.get(v.getWeight()).add(v);
    }

    for (int weight : group.keySet()) {
      List<PenaltyVariable> vars = group.get(weight);

      int sum = 0;
      for (PenaltyVariable v : vars) {
        sum += v.getUpperBound();
      }

      Expression pena = create(format("%s_w%03d", objName, weight));
      res.add(create(INT_DEFINITION, pena, ZERO, create(sum)));

      List<Expression> varExp = new ArrayList<>();
      for (PenaltyVariable v : vars) {
        varExp.add(create(v.getName()));
      }

      res.add(create(Expression.EQ, pena, create(ADD, varExp)));
    }

    List<Expression> varExp = new ArrayList<>();
    for (int weight : group.keySet()) {
      varExp.add(create(MUL, create(weight), create(format("%s_w%03d", objName, weight))));
    }
    res.add(create(Expression.EQ, obj, create(ADD, varExp)));

    return Collections.unmodifiableList(res);
  }

  public List<Expression> generateObjective() {
    return generateObjective("OBJ", getPenaltyVariables());
  }

  public static void main(String[] args) throws Exception {
    Sugar4jFormulator formulator =
        new Sugar4jFormulator(SchedulingProblem.parse(new java.io.FileReader(args[0])));

    for (Expression e : formulator.generateVariables()) {
      System.out.print(e);
      if (e.getComment() != null) {
        System.out.print(" ; " + e.getComment());
      }
      System.out.println();
    }

    for (Expression e : formulator.generateAllConstraints()) {
      System.out.print(e);
      if (e.getComment() != null) {
        System.out.print(" ; " + e.getComment());
      }
      System.out.println();
    }

    for (Expression e : formulator.generateObjective()) {
      System.out.print(e);
      if (e.getComment() != null) {
        System.out.print(" ; " + e.getComment());
      }
      System.out.println();
    }
  }
}
// Java CHECKSTYLE:ON MemberName
// Java CHECKSTYLE:ON AbbreviationAsWordInName
// Java CHECKSTYLE:ON LocalVariableName
