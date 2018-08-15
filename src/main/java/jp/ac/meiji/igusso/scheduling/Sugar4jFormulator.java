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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  // Generated Expressions
  private List<Expression> variableDeclarations;
  private List<Expression> constraint1;
  private List<Expression> constraint2;
  private List<Expression> constraint3;
  private List<Expression> constraint4;
  private List<Expression> constraint5;
  private List<Expression> constraint6;
  private List<Expression> constraint7;
  private List<Expression> constraint8;
  private List<Expression> constraint9;
  private List<Expression> constraint11;
  private List<Expression> constraint12;
  private List<Expression> constraint13;
  private List<Expression> constraint14;

  // Generated Variables
  private List<Expression> variables;
  private List<Expression> penaltyVariables;

  // Penalty Variables Info
  private Map<Expression, Integer> penaltyVariableWeight;
  private Map<Expression, Integer> penaltyVariableUpperBound;
  private Map<Integer, List<Expression>> softConstraints;

  public Sugar4jFormulator(@NonNull SchedulingProblem problem) {
    this.problem = problem;

    initializeParameters();
    this.variables = initializeVariables();
    this.variableDeclarations = generateVariableDeclarations();

    this.penaltyVariables = new ArrayList<>();
    this.penaltyVariableWeight = new HashMap<>();
    this.penaltyVariableUpperBound = new HashMap<>();
    this.softConstraints = new HashMap<>();

    generateConstraints();

    this.penaltyVariables = Collections.unmodifiableList(penaltyVariables);
    this.penaltyVariableWeight = Collections.unmodifiableMap(penaltyVariableWeight);
    this.penaltyVariableUpperBound = Collections.unmodifiableMap(penaltyVariableUpperBound);
    this.softConstraints = Collections.unmodifiableMap(softConstraints);
  }

  /*
   * PRIVATE
   */
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

    Set<Integer> shiftLengthSet = new HashSet<>();
    for (int l : L) {
      shiftLengthSet.add(l);
    }
    SHIFT_LENGTH_SET = new ArrayList<>(shiftLengthSet);
    Collections.sort(SHIFT_LENGTH_SET);
    SHIFT_LENGTH_SET = Collections.unmodifiableList(SHIFT_LENGTH_SET);
  }

  private List<Expression> initializeVariables() {
    List<Expression> res = new ArrayList<>();

    x = new Expression[I.length][D.length][T.length];
    for (int i : I) {
      for (int d : D) {
        for (int t : T) {
          x[i][d][t] = create(format("x_i%02d_d%02d_t%02d", i, d, t));
          res.add(x[i][d][t]);
        }
      }
    }

    xt = new Expression[I.length][D.length];
    for (int i : I) {
      for (int d : D) {
        xt[i][d] = create(format("xt_i%02d_d%02d", i, d));
        res.add(xt[i][d]);
      }
    }

    k = new Expression[I.length][W.length];
    for (int i : I) {
      for (int w : W) {
        k[i][w] = create(format("k_i%02d_w%02d", i, w));
        res.add(k[i][w]);
      }
    }

    return Collections.unmodifiableList(res);
  }

  private List<Expression> generateVariableDeclarations() {
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

  private void generateConstraints() {
    this.constraint1 = generateConstraint1();
    this.constraint2 = generateConstraint2();
    this.constraint3 = generateConstraint3();
    this.constraint4 = generateConstraint4();
    this.constraint5 = generateConstraint5();
    this.constraint6 = generateConstraint6();
    this.constraint7 = generateConstraint7();
    this.constraint8 = generateConstraint8();
    this.constraint9 = generateConstraint9();

    this.constraint11 = generateConstraint11();
    this.constraint12 = generateConstraint12();
    this.constraint13 = generateConstraint13();
    this.constraint14 = generateConstraint14();
  }

  private void registerSoftConstraint(@NonNull Expression cons, int weight) {
    if (!softConstraints.containsKey(weight)) {
      softConstraints.put(weight, new ArrayList<>());
    }
    softConstraints.get(weight).add(cons);
  }

  /**
   * C01:
   * 任意のスタッフi，任意の日dについて，スタッフiがd日目に勤めるシフトは1個以下である.
   * (1人のスタッフが1日に2個以上のシフトを勤めることはない)
   */
  private List<Expression> generateConstraint1() {
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
  private List<Expression> generateConstraint2() {
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
  private List<Expression> generateConstraint3() {
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
  private List<Expression> generateConstraint4() {
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
  private List<Expression> generateConstraint5() {
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
  private List<Expression> generateConstraint6() {
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
  private List<Expression> generateConstraint7() {
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
  private List<Expression> generateConstraint8() {
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
  private List<Expression> generateConstraint9() {
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
  private List<Expression> generateConstraint11() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (Q[i][d][t] == 0) {
            continue;
          }

          Expression penalty = create(format("P_C11_i%02d_d%02d_t%02d", i, d, t));
          res.add(create(INT_DEFINITION, penalty, ZERO, ONE));

          penaltyVariables.add(penalty);
          penaltyVariableWeight.put(penalty, Q[i][d][t]);
          penaltyVariableUpperBound.put(penalty, 1);

          Expression cons = create(OR, create(GE, x[i][d][t], ONE), create(GE, penalty, ONE));
          cons.setComment(format("C11_i%02d_d%02d_t%02d", i, d, t));
          res.add(cons);
          registerSoftConstraint(cons, Q[i][d][t]);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C12: 任意のスタッフi，任意の日d，任意のシフトtに対し，
   * スタッフiがd日目にシフトtで勤務するときのペナルティP[i][d][t]が定められている.
   */
  private List<Expression> generateConstraint12() {
    List<Expression> res = new ArrayList<>();

    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (P[i][d][t] == 0) {
            continue;
          }

          Expression penalty = create(format("P_C12_i%02d_d%02d_t%02d", i, d, t));
          res.add(create(INT_DEFINITION, penalty, ZERO, ONE));

          penaltyVariables.add(penalty);
          penaltyVariableWeight.put(penalty, P[i][d][t]);
          penaltyVariableUpperBound.put(penalty, 1);

          Expression cons = create(OR, create(LE, x[i][d][t], ZERO), create(GE, penalty, ONE));
          cons.setComment(format("C12_i%02d_d%02d_t%02d", i, d, t));
          res.add(cons);
          registerSoftConstraint(cons, P[i][d][t]);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C13: 任意の日d，任意のシフトtに対し，適正スタッフ数U[d][t]が定められており，
   * それから1人不足するごとに与えられるペナルティV_MIN[d][t]が定められている.
   */
  private List<Expression> generateConstraint13() {
    List<Expression> res = new ArrayList<>();

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MIN[d][t] == 0) {
          continue;
        }

        Expression penalty = create(format("P_C13_d%02d_t%02d", d, t));
        res.add(create(INT_DEFINITION, penalty, ZERO, create(U[d][t])));

        penaltyVariables.add(penalty);
        penaltyVariableWeight.put(penalty, V_MIN[d][t]);
        penaltyVariableUpperBound.put(penalty, U[d][t]);

        List<Expression> terms = new ArrayList<>();
        for (int i : I) {
          terms.add(x[i][d][t]);
        }
        terms.add(penalty);

        Expression cons = create(GE, create(ADD, terms), create(U[d][t]));
        cons.setComment(format("C13_d%02d_t%02d", d, t));
        res.add(cons);
        registerSoftConstraint(cons, V_MIN[d][t]);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C14:
   * 任意の日d，任意のシフトtに対し，適正スタッフ数U[d][t]が定められており，
   * それを1人超過するごとに与えられるペナルティV_MAX[d][t]が定められている.
   */
  private List<Expression> generateConstraint14() {
    List<Expression> res = new ArrayList<>();

    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MIN[d][t] == 0) {
          continue;
        }

        Expression penalty = create(format("P_C14_d%02d_t%02d", d, t));
        res.add(create(INT_DEFINITION, penalty, ZERO, create(I.length - U[d][t])));

        penaltyVariables.add(penalty);
        penaltyVariableWeight.put(penalty, V_MAX[d][t]);
        penaltyVariableUpperBound.put(penalty, I.length - U[d][t]);

        List<Expression> terms = new ArrayList<>();
        for (int i : I) {
          terms.add(x[i][d][t]);
        }
        terms.add(create(NEG, penalty));

        Expression cons = create(LE, create(ADD, terms), create(U[d][t]));
        cons.setComment(format("C14_d%02d_t%02d", d, t));
        res.add(cons);
        registerSoftConstraint(cons, V_MAX[d][t]);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /*
   * PUBLIC
   */

  public List<Expression> getVariables() {
    return penaltyVariables;
  }

  public List<Expression> getPenaltyVariables() {
    return penaltyVariables;
  }

  public Map<Expression, Integer> getPenaltyVariableWeight() {
    return penaltyVariableWeight;
  }

  public Map<Expression, Integer> getPenaltyVariableUpperBound() {
    return penaltyVariableUpperBound;
  }

  public List<Expression> getVariableDeclarations() {
    return variableDeclarations;
  }

  public List<Expression> getConstraint1() {
    return constraint1;
  }

  public List<Expression> getConstraint2() {
    return constraint2;
  }

  public List<Expression> getConstraint3() {
    return constraint3;
  }

  public List<Expression> getConstraint4() {
    return constraint4;
  }

  public List<Expression> getConstraint5() {
    return constraint5;
  }

  public List<Expression> getConstraint6() {
    return constraint6;
  }

  public List<Expression> getConstraint7() {
    return constraint7;
  }

  public List<Expression> getConstraint8() {
    return constraint8;
  }

  public List<Expression> getConstraint9() {
    return constraint9;
  }

  public List<Expression> getConstraint11() {
    return constraint11;
  }

  public List<Expression> getConstraint12() {
    return constraint12;
  }

  public List<Expression> getConstraint13() {
    return constraint13;
  }

  public List<Expression> getConstraint14() {
    return constraint14;
  }

  public List<Expression> getHardConstraints() {
    List<Expression> res = new ArrayList<>();
    res.addAll(getConstraint1());
    res.addAll(getConstraint2());
    res.addAll(getConstraint3());
    res.addAll(getConstraint4());
    res.addAll(getConstraint5());
    res.addAll(getConstraint6());
    res.addAll(getConstraint7());
    res.addAll(getConstraint8());
    res.addAll(getConstraint9());
    return Collections.unmodifiableList(res);
  }

  public List<Expression> getSoftConstraints() {
    List<Expression> res = new ArrayList<>();
    res.addAll(getConstraint11());
    res.addAll(getConstraint12());
    res.addAll(getConstraint13());
    res.addAll(getConstraint14());
    return Collections.unmodifiableList(res);
  }

  public List<Expression> getAllConstraints() {
    List<Expression> res = new ArrayList<>();
    res.addAll(getConstraint1());
    res.addAll(getConstraint2());
    res.addAll(getConstraint3());
    res.addAll(getConstraint4());
    res.addAll(getConstraint5());
    res.addAll(getConstraint6());
    res.addAll(getConstraint7());
    res.addAll(getConstraint8());
    res.addAll(getConstraint9());
    res.addAll(getConstraint11());
    res.addAll(getConstraint12());
    res.addAll(getConstraint13());
    res.addAll(getConstraint14());
    return Collections.unmodifiableList(res);
  }

  public List<Expression> getHeavyConstraints() {
    int maxWeight = 0;
    for (int weight : softConstraints.keySet()) {
      maxWeight = Math.max(maxWeight, weight);
    }

    return Collections.unmodifiableList(softConstraints.get(maxWeight));
  }

  public List<Expression> getLightConstraints() {
    List<Expression> res = new ArrayList<>();

    int maxWeight = 0;
    for (int weight : softConstraints.keySet()) {
      maxWeight = Math.max(maxWeight, weight);
    }

    for (int weight : softConstraints.keySet()) {
      if (weight == maxWeight) {
        continue;
      }
      res.addAll(softConstraints.get(weight));
    }

    return Collections.unmodifiableList(res);
  }

  private Map<Integer, List<Expression>> classifyByWeight(List<Expression> penaltyVariables) {
    Map<Integer, List<Expression>> res = new HashMap<>();

    for (Expression v : penaltyVariables) {
      int weight = penaltyVariableWeight.get(v);
      if (!res.containsKey(weight)) {
        res.put(weight, new ArrayList<>());
      }
      res.get(weight).add(v);
    }

    return res;
  }

  private List<Expression> generateObjective(String objName, List<Expression> penaltyVariables) {
    if (penaltyVariables.isEmpty()) {
      return Arrays.asList();
    }

    List<Expression> res = new ArrayList<>();

    int maxPenalty = 0;
    for (Expression v : penaltyVariables) {
      maxPenalty += penaltyVariableWeight.get(v) * penaltyVariableUpperBound.get(v);
    }

    Expression obj = create(objName);
    res.add(create(INT_DEFINITION, obj, ZERO, create(maxPenalty)));
    res.add(create(Expression.OBJECTIVE_DEFINITION, Expression.MINIMIZE, obj));

    Map<Integer, List<Expression>> group = classifyByWeight(penaltyVariables);
    for (int weight : group.keySet()) {
      int sum = 0;
      for (Expression v : group.get(weight)) {
        sum += penaltyVariableUpperBound.get(v);
      }

      Expression pena = create(format("%s_w%03d", objName, weight));
      res.add(create(INT_DEFINITION, pena, ZERO, create(sum)));

      List<Expression> terms = new ArrayList<>();
      for (Expression v : group.get(weight)) {
        terms.add(v);
      }

      res.add(create(Expression.EQ, pena, create(ADD, terms)));
    }

    List<Expression> terms = new ArrayList<>();
    for (int weight : group.keySet()) {
      terms.add(create(MUL, create(weight), create(format("%s_w%03d", objName, weight))));
    }
    res.add(create(Expression.EQ, obj, create(ADD, terms)));

    return Collections.unmodifiableList(res);
  }

  public List<Expression> generateObjective() {
    return generateObjective("OBJ", getPenaltyVariables());
  }

  public List<Expression> generateHeavyObjective(@NonNull String objName) {
    Map<Integer, List<Expression>> group = classifyByWeight(penaltyVariables);

    int maxWeight = 0;
    for (int weight : group.keySet()) {
      maxWeight = Math.max(maxWeight, weight);
    }

    List<Expression> heavyPeanaltyVariables = new ArrayList<>();
    for (Expression v : penaltyVariables) {
      if (penaltyVariableWeight.get(v) == maxWeight) {
        heavyPeanaltyVariables.add(v);
      }
    }

    int maxPenalty = 0;
    for (Expression v : heavyPeanaltyVariables) {
      maxPenalty += penaltyVariableUpperBound.get(v);
    }

    List<Expression> res = new ArrayList<>();
    Expression obj = create(objName);
    res.add(create(INT_DEFINITION, obj, ZERO, create(maxPenalty)));

    List<Expression> terms = new ArrayList<>();
    for (Expression v : heavyPeanaltyVariables) {
      terms.add(v);
    }
    res.add(create(EQ, obj, create(ADD, terms)));
    return Collections.unmodifiableList(res);
  }

  public List<Expression> generateLightObjective(@NonNull String objName) {
    int maxWeight = 0;
    for (Expression v : penaltyVariables) {
      maxWeight = Math.max(maxWeight, penaltyVariableWeight.get(v));
    }

    List<Expression> lightPeanaltyVariables = new ArrayList<>();
    for (Expression v : penaltyVariables) {
      if (penaltyVariableWeight.get(v) != maxWeight) {
        lightPeanaltyVariables.add(v);
      }
    }
    return generateObjective(objName, lightPeanaltyVariables);
  }

  public int evaluateSolution(@NonNull Map<Expression, Integer> solution) {
    for (int i : I) {
      for (int d : D) {
        for (int t : T) {
          if (!solution.containsKey(x[i][d][t])) {
            throw new IllegalArgumentException("Invalid Solution: " + solution);
          }
        }
      }
    }

    int penalty = 0;

    // C11
    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (Q[i][d][t] == 0) {
            continue;
          }

          if (solution.get(x[i][d][t]) == 0) {
            penalty += Q[i][d][t];
          }
        }
      }
    }

    // C12
    for (int i : I) {
      for (int d : D) {
        for (int t = 1; t < T.length; t++) {
          if (P[i][d][t] == 0) {
            continue;
          }

          if (solution.get(x[i][d][t]) == 1) {
            penalty += P[i][d][t];
          }
        }
      }
    }

    // C13
    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MIN[d][t] == 0) {
          continue;
        }

        int sum = 0;
        for (int i : I) {
          sum += solution.get(x[i][d][t]);
        }

        if (sum < U[d][t]) {
          penalty += V_MIN[d][t] * (U[d][t] - sum);
        }
      }
    }

    // C14
    for (int d : D) {
      for (int t = 1; t < T.length; t++) {
        if (V_MAX[d][t] == 0) {
          continue;
        }

        int sum = 0;
        for (int i : I) {
          sum += solution.get(x[i][d][t]);
        }

        if (sum > U[d][t]) {
          penalty += V_MAX[d][t] * (sum - U[d][t]);
        }
      }
    }

    return penalty;
  }

  public static void main(String[] args) throws Exception {
    Sugar4jFormulator formulator =
        new Sugar4jFormulator(SchedulingProblem.parse(new java.io.FileReader(args[0])));

    for (Expression e : formulator.getVariableDeclarations()) {
      System.out.print(e);
      if (e.getComment() != null) {
        System.out.print(" ; " + e.getComment());
      }
      System.out.println();
    }

    for (Expression e : formulator.getAllConstraints()) {
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

    for (Expression e : formulator.generateHeavyObjective("_HEAVY")) {
      System.out.print(e);
      if (e.getComment() != null) {
        System.out.print(" ; " + e.getComment());
      }
      System.out.println();
    }

    for (Expression e : formulator.generateLightObjective("_LIGHT")) {
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
