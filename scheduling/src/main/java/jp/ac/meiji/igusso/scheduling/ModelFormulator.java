package jp.ac.meiji.igusso.scheduling;

import static java.lang.String.format;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Cover;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Shift;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOffRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOnRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Staff;

import jp.ac.meiji.igusso.model.Comparator;
import jp.ac.meiji.igusso.model.ConflictPointConstraint;
import jp.ac.meiji.igusso.model.Constraint;
import jp.ac.meiji.igusso.model.Domain;
import jp.ac.meiji.igusso.model.LinearConstraint;
import jp.ac.meiji.igusso.model.Model;
import jp.ac.meiji.igusso.model.PseudoBooleanConstraint;
import jp.ac.meiji.igusso.model.Variable;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Java CHECKSTYLE:OFF MemberName
// Java CHECKSTYLE:OFF AbbreviationAsWordInName
// Java CHECKSTYLE:OFF LocalVariableName
public final class ModelFormulator {
  private final SchedulingProblem problem;
  private Model model;

  public ModelFormulator(@NonNull SchedulingProblem problem) {
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
  /** x[i][d] : スタッフiがd日目にするシフトを表す. (0は休暇) */
  private Variable[][] x;
  /** k[i][w] : スタッフiが週末wを休暇にしないとき1, そうでないとき0. */
  private Variable[][] k;
  /** xt[i][d] : スタッフiのd日目の勤務時間を表す. */
  private Variable[][] xt;

  private void initializeParameters() {
    SchedulingProblemParameter parameter = new SchedulingProblemParameter(problem);
    this.H = parameter.getH();
    this.I = parameter.getI();
    this.D = parameter.getD();
    this.W = parameter.getW();
    this.T = parameter.getT();
    this.R = parameter.getR();
    this.N = parameter.getN();
    this.L = parameter.getL();
    this.M_MAX = parameter.getMmax();
    this.B_MIN = parameter.getBmin();
    this.B_MAX = parameter.getBmax();
    this.C_MIN = parameter.getCmin();
    this.C_MAX = parameter.getCmax();
    this.O_MIN = parameter.getOmin();
    this.A_MAX = parameter.getAmax();
    this.Q = parameter.getQ();
    this.P = parameter.getP();
    this.U = parameter.getU();
    this.V_MIN = parameter.getVmin();
    this.V_MAX = parameter.getVmax();
  }

  private void generateVariables() {
    x = new Variable[I.length][D.length];
    for (int i : I) {
      for (int d : D) {
        String varName = String.format("x_i%02d_d%02d", i, d);
        x[i][d] = Variable.of(varName, T.length);
        model.addVariable(x[i][d]);
      }
    }

    k = new Variable[I.length][W.length];
    for (int i : I) {
      for (int w : W) {
        String varName = String.format("k_i%02d_w%d", i, w);
        k[i][w] = Variable.of(varName, 2);
        model.addVariable(k[i][w]);
      }
    }

    Set<Integer> shiftLengthSet = new HashSet<>();
    shiftLengthSet.add(0);
    for (int t = 1; t < T.length; t++) {
      shiftLengthSet.add(L[t]);
    }

    xt = new Variable[I.length][D.length];
    for (int i : I) {
      for (int d : D) {
        String varName = String.format("xt_i%02d_d%02d", i, d);
        xt[i][d] = Variable.of(varName, Domain.of(shiftLengthSet));
        model.addVariable(xt[i][d]);
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
            Constraint cons = ConflictPointConstraint.of(consName)
                                  .addTerm(x[i][d], t1)
                                  .addTerm(x[i][d + 1], t2)
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
        if (M_MAX[i][t] == H) {
          continue;
        }

        String consName = format("C03_i%02d_t%02d", i, t);
        PseudoBooleanConstraint.Builder cons =
            PseudoBooleanConstraint.of(consName, Comparator.LE, M_MAX[i][t]);
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
   * スタッフiがd日目に勤務する時間 xt[i][d] の総和を取ることで, 総勤務時間が求める.
   */
  private List<Constraint> constraint4() {
    List<Constraint> res = new ArrayList<>();

    Set<Integer> shiftLengthSet = new HashSet<>();
    shiftLengthSet.add(0);
    for (int t = 1; t < T.length; t++) {
      shiftLengthSet.add(L[t]);
    }

    for (int i : I) {
      for (int d : D) {
        String consName = format("C04_i%02d_d%02d", i, d);
        PseudoBooleanConstraint.Builder cons =
            PseudoBooleanConstraint.of(consName, Comparator.EQ, 0);
        for (int length : shiftLengthSet) {
          if (length == 0) {
            continue;
          }
          cons.addTerm(-length, xt[i][d], length);
        }
        for (int t = 1; t < T.length; t++) {
          cons.addTerm(L[t], x[i][d], t);
        }
        res.add(cons.build());
      }
    }

    for (int i : I) {
      String consName1 = format("C04LB_i%02d", i);
      LinearConstraint.Builder cons1 = LinearConstraint.of(consName1, Comparator.GE, B_MIN[i]);
      for (int d : D) {
        cons1.addTerm(1, xt[i][d]);
      }
      res.add(cons1.build());

      String consName2 = format("C04UB_i%02d", i);
      LinearConstraint.Builder cons2 = LinearConstraint.of(consName2, Comparator.LE, B_MAX[i]);
      for (int d : D) {
        cons2.addTerm(1, xt[i][d]);
      }
      res.add(cons2.build());
    }

    return Collections.unmodifiableList(res);
  }

  /**
  /* C05:
   * 任意のスタッフiに対して，連続で勤務する日数の上限cmax[i]が定められている.
  /* 任意の連続するcmax[i] + 1日間に関して，休暇の回数が1日以上であればよい
   */
  private List<Constraint> constraint5() {
    List<Constraint> res = new ArrayList<>();

    for (int i : I) {
      for (int d = 0; d < D.length - C_MAX[i]; d++) {
        String consName = format("C05_i%02d_d%02d", i, d);
        ConflictPointConstraint.Builder cons = ConflictPointConstraint.of(consName);
        for (int j = d; j <= d + C_MAX[i]; j++) {
          cons.addTerm(x[i][j], 0, false);
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
          ConflictPointConstraint.Builder cons = ConflictPointConstraint.of(consName);

          cons.addTerm(x[i][d], 0, true);
          for (int j = d + 1; j < d + s + 1; j++) {
            cons.addTerm(x[i][j], 0, false);
          }
          cons.addTerm(x[i][d + s + 1], 0, true);

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
          ConflictPointConstraint.Builder cons = ConflictPointConstraint.of(consName);

          cons.addTerm(x[i][d], 0, false);
          for (int j = d + 1; j < d + s + 1; j++) {
            cons.addTerm(x[i][j], 0, true);
          }
          cons.addTerm(x[i][d + s + 1], 0, false);

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
        String consName1 = format("C08A_i%02d_w%d", i, w);
        ConflictPointConstraint cons1 = ConflictPointConstraint.of(consName1)
                                            .addTerm(x[i][7 * w + 5], 0, true)
                                            .addTerm(x[i][7 * w + 6], 0, true)
                                            .addTerm(k[i][w], 1, true)
                                            .build();
        res.add(cons1);

        String consName2 = format("C08B_i%02d_w%d", i, w);
        ConflictPointConstraint cons2 = ConflictPointConstraint.of(consName2)
                                            .addTerm(x[i][7 * w + 5], 0, true)
                                            .addTerm(x[i][7 * w + 6], 0, false)
                                            .addTerm(k[i][w], 0, true)
                                            .build();
        res.add(cons2);

        String consName3 = format("C08C_i%02d_w%d", i, w);
        ConflictPointConstraint cons3 = ConflictPointConstraint.of(consName3)
                                            .addTerm(x[i][7 * w + 5], 0, false)
                                            .addTerm(x[i][7 * w + 6], 0, true)
                                            .addTerm(k[i][w], 0, true)
                                            .build();
        res.add(cons3);

        String consName4 = format("C08D_i%02d_w%d", i, w);
        ConflictPointConstraint cons4 = ConflictPointConstraint.of(consName4)
                                            .addTerm(x[i][7 * w + 5], 0, false)
                                            .addTerm(x[i][7 * w + 6], 0, false)
                                            .addTerm(k[i][w], 0, true)
                                            .build();
        res.add(cons4);
      }

      String consName = format("C08_i%02d", i);
      LinearConstraint.Builder cons = LinearConstraint.of(consName, Comparator.LE, A_MAX[i]);
      for (int w : W) {
        cons.addTerm(1, k[i][w]);
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
        Constraint cons = ConflictPointConstraint.of(consName).addTerm(x[i][d], 0, false).build();
        res.add(cons);
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
          Constraint cons =
              ConflictPointConstraint.of(consName, Q[i][d][t]).addTerm(x[i][d], t, false).build();
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
          Constraint cons =
              ConflictPointConstraint.of(consName, P[i][d][t]).addTerm(x[i][d], t, true).build();
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
        PseudoBooleanConstraint.Builder cons =
            PseudoBooleanConstraint.of(consName, Comparator.GE, U[d][t], V_MIN[d][t]);

        for (int i : I) {
          cons.addTerm(1, x[i][d], t);
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
        PseudoBooleanConstraint.Builder cons =
            PseudoBooleanConstraint.of(consName, Comparator.LE, U[d][t], V_MAX[d][t]);

        for (int i : I) {
          cons.addTerm(1, x[i][d], t);
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
