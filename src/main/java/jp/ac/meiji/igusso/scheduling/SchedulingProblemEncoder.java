package jp.ac.meiji.igusso.scheduling;

import static java.lang.String.format;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Cover;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Shift;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOffRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOnRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Staff;

import jp.ac.meiji.igusso.coptool.Comparator;
import jp.ac.meiji.igusso.coptool.Constraint;
import jp.ac.meiji.igusso.coptool.LinearConstraint;
import jp.ac.meiji.igusso.coptool.Model;
import jp.ac.meiji.igusso.coptool.Variable;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Java CHECKSTYLE:OFF MemberName
// Java CHECKSTYLE:OFF AbbreviationAsWordInName
// Java CHECKSTYLE:OFF LocalVariableName
public final class SchedulingProblemEncoder {
  private final SchedulingProblem problem;
  private Model model;

  public SchedulingProblemEncoder(@NonNull SchedulingProblem problem) {
    this.problem = problem;
  }

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

  // Variables (lowercase)
  private Variable[][] x;
  private Variable[][] k;
  private Variable[][] y;
  private Variable[][] z;

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
  }

  private void generateVariables() {
    x = new Variable[I.length][D.length];
    for (int i : I) {
      for (int d : D) {
        String varName = String.format("x_i%02d_d%02d", i, d);
        x[i][d] = model.addVariable(varName, T.length);
      }
    }

    k = new Variable[I.length][W.length];
    for (int i : I) {
      for (int w : W) {
        String varName = String.format("k_i%02d_w%d", i, w);
        k[i][w] = model.addVariable(varName, 2);
      }
    }

    y = new Variable[D.length][T.length];
    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        String varName = String.format("y_d%02d_t%02d", d, t);
        y[d][t] = model.addVariable(varName, U[d][t] + 1);
      }
    }

    z = new Variable[D.length][T.length];
    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        String varName = String.format("z_d%02d_t%02d", d, t);
        z[d][t] = model.addVariable(varName, I.length - U[d][t] + 1);
      }
    }
  }

  /**
  /* C01:
   * 任意のスタッフi，任意の日dについて，スタッフiがd日目に勤めるシフトは1個以下である.
  /* (1人のスタッフが1日に2個以上のシフトを勤めることはない)
   */
  private List<Constraint> constraint1() {
    return Collections.unmodifiableList(new ArrayList<>());
  }

  /**
  /* C02:
   * 任意のシフトtに対して，その翌日に勤めてはならないシフトの集合R[t]が定められている.
  /* 任意のスタッフi，最終日を除く任意の日d，任意のシフトt1，任意のシフトt2 \in R[t1]に対して，
  /* [ スタッフiがd日目にシフトt1を勤め，更に(d + 1)日目にシフトt2を勤める ] ことは許されない.
   */
  private List<Constraint> constraint2() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int d = 0; d < D.length - 1; d++) {
        for (int t1 = 1; t1 < T.length; t1++) {
          for (int t2 : R[t1]) {
            String consName = format("C02_i%02d_d%02d_t%02d_r%02d", i, d, t1, t2);
            LinearConstraint cons = LinearConstraint.of(consName, Comparator.LE, 1)
                                        .addTerm(1, x[i][d], t1)
                                        .addTerm(1, x[i][d + 1], t2)
                                        .build();
            res.add(cons);
          }
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C03:
  /* 任意のスタッフi，任意のシフトtに対して，
  /* スタッフiがシフトtを勤めることのできる最大回数mmax[i, t]が定められている.
   */
  private List<Constraint> constraint3() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int t = 1; t < T.length; t++) {
        String consName = format("C03_i%02d_t%02d", i, t);
        LinearConstraint.Builder cons = LinearConstraint.of(consName, Comparator.LE, M_MAX[i][t]);
        for (int d : D) {
          cons.addTerm(1, x[i][d], t);
        }
        res.add(cons.build());
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C04:
   * 任意のスタッフiに対して， 総勤務時間の下限bmin[i]と上限bmax[i]が定められている.
   */
  private List<Constraint> constraint4() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      String consName1 = format("C04LB_i%02d", i);
      LinearConstraint.Builder cons1 = LinearConstraint.of(consName1, Comparator.GE, B_MIN[i]);
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          cons1.addTerm(L[t], x[i][d], t);
        }
      }
      res.add(cons1.build());

      String consName2 = format("C04UB_i%02d", i);
      LinearConstraint.Builder cons2 = LinearConstraint.of(consName2, Comparator.LE, B_MAX[i]);
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          cons2.addTerm(L[t], x[i][d], t);
        }
      }
      res.add(cons2.build());
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C05:
   * 任意のスタッフiに対して，連続で勤務する日数の上限cmax[i]が定められている.
  /* 任意の連続するcmax[i]日間に関して，休暇の回数が1日以上であればよい
   */
  private List<Constraint> constraint5() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int d = 0; d < D.length - C_MAX[i]; d++) {
        String consName = format("C05_i%02d_d%02d", i, d);
        LinearConstraint.Builder cons = LinearConstraint.of(consName, Comparator.GE, 1);
        for (int j = d; j <= d + C_MAX[i]; j++) {
          cons.addTerm(1, x[i][j], 0);
        }
        res.add(cons.build());
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C06:
   * 任意のスタッフiに対し，連続勤務日数の下限cmin[i]が定められている.
  /* (連続勤務日数とはある休暇から次の休暇までの勤務日数)
  /* すなわち，cmin[i]日未満の連続勤務は許されない
  /*
  /* s日の連続勤務が許されないという制約は次のように表現できる
  /* 任意の連続する(s + 2)日間について，
  /* [その初日と最終日が休暇であり，その間は毎日勤務する]ことは許されない
   */
  private List<Constraint> constraint6() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int s = 1; s < C_MIN[i]; s++) {
        // ここでs日の連続勤務を許さないという制約を生成
        for (int d = 0; d < D.length - (s + 1); d++) {
          String consName = format("C06_i%02d_s%02d_d%02d", i, s, d);
          LinearConstraint.Builder cons = LinearConstraint.of(consName, Comparator.GE, -1);

          cons.addTerm(-1, x[i][d], 0);
          for (int j = d + 1; j < d + s + 1; j++) {
            cons.addTerm(1, x[i][j], 0);
          }
          cons.addTerm(-1, x[i][d + s + 1], 0);

          res.add(cons.build());
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C07:
   * 任意のスタッフiに対し，連続休暇日数の下限omin[i]が定められている.
  /* (連続休暇日数とはある勤務から次の勤務までの休暇日数)
  /* すなわち，omin[i]日未満の連続休暇は許されない
  /* 制約はC06と同様に生成する
   */
  private List<Constraint> constraint7() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int s = 1; s < O_MIN[i]; s++) {
        // ここでs日の連続休暇を許さないという制約を生成
        for (int d = 0; d < D.length - (s + 1); d++) {
          String consName = format("C07_i%02d_s%02d_d%02d", i, s, d);
          LinearConstraint.Builder cons = LinearConstraint.of(consName, Comparator.GE, 1 - s);

          cons.addTerm(1, x[i][d], 0);
          for (int j = d + 1; j < d + s + 1; j++) {
            cons.addTerm(-1, x[i][j], 0);
          }
          cons.addTerm(1, x[i][d + s + 1], 0);

          res.add(cons.build());
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C08:
   * 任意のスタッフi，任意の週末 (土曜日と日曜日) wに対し,
   * 週末を休暇にしない回数の上限amax[i]が定められている.
  /* 週末を休暇にしない回数はamax[i]以下でなければならない
  /*
  /* 制約C08L, C08Rは，変数k[(i, w)]をスタッフiが週末wを休暇にしないとき1
  /* そうでないとき0にするための制約である
   */
  private List<Constraint> constraint8() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int w : W) {
        String consName1 = format("C08L_i%02d_w%d", i, w);
        LinearConstraint cons1 = LinearConstraint.of(consName1, Comparator.LE, 2)
                                     .addTerm(1, x[i][7 * w + 5], 0)
                                     .addTerm(1, x[i][7 * w + 6], 0)
                                     .addTerm(1, k[i][w], 1)
                                     .build();
        res.add(cons1);

        String consName2 = format("C08R_i%02d_w%d", i, w);
        LinearConstraint cons2 = LinearConstraint.of(consName2, Comparator.GE, 2)
                                     .addTerm(1, x[i][7 * w + 5], 0)
                                     .addTerm(1, x[i][7 * w + 6], 0)
                                     .addTerm(2, k[i][w], 1)
                                     .build();
        res.add(cons2);
      }
    }

    for (int i : I) {
      String consName = format("C08_i%02d", i);
      LinearConstraint.Builder cons = LinearConstraint.of(consName, Comparator.LE, A_MAX[i]);
      for (int w : W) {
        cons.addTerm(1, k[i][w], 1);
      }
      res.add(cons.build());
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C09:
   * 任意のスタッフiに対し，スタッフiが勤務不能な日の集合N[i]が定められている.
   */
  private List<Constraint> constraint9() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int d : N[i]) {
        String consName = format("C09_i%02d_d%02d", i, d);
        Constraint cons =
            LinearConstraint.of(consName, Comparator.EQ, 1).addTerm(1, x[i][d], 0).build();
        res.add(cons);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C10:
  /* 任意の日d，任意のシフトtに対して，
  /* d日目にシフトtの勤務をする適正スタッフ数u[(d, t)]が定められている.
  /*
  /* 適正スタッフ数u[(d, t)]に対する勤務をするスタッフ数の不足人数を変数y[(d, t)]
  /* 適正スタッフ数u[(d, t)]に対する勤務をするスタッフ数の超過人数を変数z[(d, t)]
  /* で表す
  /* 制約C10は変数に適切な値を割り当てる
   */
  private List<Constraint> constraint10() {
    List<Constraint> res = new ArrayList<>();

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        String consName = format("C10_d%02d_t%02d", d, t);
        LinearConstraint.Builder cons = LinearConstraint.of(consName, Comparator.EQ, U[d][t]);

        for (int i : I) {
          cons.addTerm(1, x[i][d], t);
        }

        for (int zz = 0; zz <= I.length - U[d][t]; zz++) {
          cons.addTerm(-zz, z[d][t], zz);
        }

        for (int yy = 0; yy <= U[d][t]; yy++) {
          cons.addTerm(yy, y[d][t], yy);
        }

        res.add(cons.build());
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C11:
  /* 任意のスタッフi，任意の日d，任意のシフトtに対し，
  /* スタッフiがd日目にシフトtで勤務しないときのペナルティq[(i, d, t)]が定められている.
   */
  private List<Constraint> constraint11() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (Q[i][d][t] == 0) {
            continue;
          }
          String consName = format("C11_i%02d_d%02d_t%02d", i, d, t);
          LinearConstraint cons = LinearConstraint.of(consName, Comparator.EQ, 1, Q[i][d][t])
                                      .addTerm(1, x[i][d], t)
                                      .build();
          res.add(cons);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C12: 任意のスタッフi，任意の日d，任意のシフトtに対し，
  /* スタッフiがd日目にシフトtで勤務するときのペナルティp[(i, d, t)]が定められている.
   */
  private List<Constraint> constraint12() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (P[i][d][t] == 0) {
            continue;
          }
          String consName = format("C12_i%02d_d%02d_t%02d", i, d, t);
          LinearConstraint cons = LinearConstraint.of(consName, Comparator.EQ, 0, P[i][d][t])
                                      .addTerm(1, x[i][d], t)
                                      .build();
          res.add(cons);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C13: 任意の日d，任意のシフトtに対し，適正スタッフ数u[(d, t)]が定められており，
  /* それから1人不足するごとに与えられるペナルティvmin[(d, t)]が定められている.
   */
  private List<Constraint> constraint13() {
    List<Constraint> res = new ArrayList<>();

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MIN[d][t] == 0) {
          continue;
        }
        String consName = format("C13_d%02d_t%02d", d, t);
        LinearConstraint.Builder cons =
            LinearConstraint.of(consName, Comparator.LE, 0, V_MIN[d][t]);

        for (int yy = 0; yy <= U[d][t]; yy++) {
          cons.addTerm(yy, y[d][t], yy);
        }

        res.add(cons.build());
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C14:
  /* 任意の日d，任意のシフトtに対し，適正スタッフ数u[(d, t)]が定められており，
  /* それを1人超過するごとに与えられるペナルティvmax[(d, t)]が定められている.
   */
  private List<Constraint> constraint14() {
    List<Constraint> res = new ArrayList<>();

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MAX[d][t] == 0) {
          continue;
        }
        String consName = format("C14_d%02d_t%02d", d, t);
        LinearConstraint.Builder cons =
            LinearConstraint.of(consName, Comparator.LE, 0, V_MAX[d][t]);

        for (int zz = 0; zz <= I.length - U[d][t]; zz++) {
          cons.addTerm(zz, z[d][t], zz);
        }

        res.add(cons.build());
      }
    }

    return Collections.unmodifiableList(res);
  }

  public Model encode() {
    this.model = new Model();

    initializeParameters();
    generateVariables();

    model.addConstraints(constraint1());
    model.addConstraints(constraint2());
    model.addConstraints(constraint3());
    model.addConstraints(constraint4());
    model.addConstraints(constraint5());
    model.addConstraints(constraint6());
    model.addConstraints(constraint7());
    model.addConstraints(constraint8());
    model.addConstraints(constraint9());
    model.addConstraints(constraint10());
    model.addConstraints(constraint11());
    model.addConstraints(constraint12());
    model.addConstraints(constraint13());
    model.addConstraints(constraint14());
    return model;
  }
}
// Java CHECKSTYLE:ON MemberName
// Java CHECKSTYLE:ON AbbreviationAsWordInName
// Java CHECKSTYLE:ON LocalVariableName
