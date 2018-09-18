package jp.ac.meiji.igusso.scheduling.ikegami;

import static java.lang.String.format;
import static jp.kobe_u.sugar.expression.Expression.ADD;
import static jp.kobe_u.sugar.expression.Expression.AND;
import static jp.kobe_u.sugar.expression.Expression.EQ;
import static jp.kobe_u.sugar.expression.Expression.GE;
import static jp.kobe_u.sugar.expression.Expression.IF;
import static jp.kobe_u.sugar.expression.Expression.INT_DEFINITION;
import static jp.kobe_u.sugar.expression.Expression.LE;
import static jp.kobe_u.sugar.expression.Expression.MINIMIZE;
import static jp.kobe_u.sugar.expression.Expression.OBJECTIVE_DEFINITION;
import static jp.kobe_u.sugar.expression.Expression.ONE;
import static jp.kobe_u.sugar.expression.Expression.OR;
import static jp.kobe_u.sugar.expression.Expression.SUB;
import static jp.kobe_u.sugar.expression.Expression.ZERO;
import static jp.kobe_u.sugar.expression.Expression.create;

import jp.kobe_u.sugar.expression.Expression;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Java CHECKSTYLE:OFF LocalVariableName
// Java CHECKSTYLE:OFF MemberName
// Java CHECKSTYLE:OFF ParameterName
@ToString
@EqualsAndHashCode
public final class Sugar4jFormulator {
  private final Problem problem;

  // Parameters
  private int I;
  private int D;
  private int T;

  // Variables
  private Expression[][][] x;

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
  private List<Expression> objectiveDeclaration;

  // Penalty Info
  private List<Expression> penaltyVariables = new ArrayList<>();
  private Map<Expression, Integer> penaltyVariableWeight = new HashMap<>();
  private Map<Expression, Integer> penaltyVariableUpperBound = new HashMap<>();

  public Sugar4jFormulator(@NonNull Problem problem) {
    this.problem = problem;

    initializeParameters();
    generateVariables();
    generateConstraints();
  }

  private static Expression pos(Expression var) {
    return create(GE, var, ONE);
  }

  private static Expression neg(Expression var) {
    return create(LE, var, ZERO);
  }

  private void initializeParameters() {
    this.I = problem.getStaffs().size();
    this.D = problem.getLength();
    this.T = problem.getShifts().size() + 1;
  }

  private void generateVariables() {
    this.x = new Expression[I][D][T];
    for (int i = 0; i < I; i++) {
      for (int d = 0; d < D; d++) {
        for (int t = 0; t < T; t++) {
          x[i][d][t] = create(format("x_i%02d_d%02d_t%02d", i, d, t));
        }
      }
    }
  }

  private void generateConstraints() {
    this.variableDeclarations = generateVariableDeclarations();
    this.constraint1 = generateConstraint1();
    this.constraint2 = generateConstraint2();
    this.constraint3 = generateConstraint3();
    this.constraint4 = generateConstraint4();
    this.constraint5 = generateConstraint5();
    this.constraint6 = generateConstraint6();
    this.constraint7 = generateConstraint7();
    this.constraint8 = generateConstraint8();
    this.objectiveDeclaration = generateObjectiveDeclaration();
  }

  private List<Expression> generateVariableDeclarations() {
    List<Expression> res = new ArrayList<>();

    for (int i = 0; i < I; i++) {
      for (int d = 0; d < D; d++) {
        for (int t = 0; t < T; t++) {
          res.add(create(INT_DEFINITION, x[i][d][t], ZERO, ONE));
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C01: あるスタッフは，ある日において，ただひとつのシフトに割り当てられる.
   */
  private List<Expression> generateConstraint1() {
    List<Expression> res = new ArrayList<>();

    for (int i = 0; i < I; i++) {
      for (int d = 0; d < D; d++) {
        for (int t1 = 0; t1 < T; t1++) {
          for (int t2 = t1 + 1; t2 < T; t2++) {
            Expression cons = create(OR, neg(x[i][d][t1]), neg(x[i][d][t2]));
            cons.setComment(format("C01_i%02d_d%02d_t%02d_t%02d", i, d, t1, t2));
            res.add(cons);
          }
        }

        List<Expression> terms = new ArrayList<>();
        for (int t = 0; t < T; t++) {
          terms.add(pos(x[i][d][t]));
        }
        Expression cons = create(OR, terms);
        cons.setComment(format("C01_i%02d_d%02d", i, d));
        res.add(cons);
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C02: 自動割当が無効のシフトには，固定シフト割当がない限り，割り当てない.
   * (AutoAllocate)
   */
  private List<Expression> generateConstraint2() {
    List<Expression> res = new ArrayList<>();

    boolean[][][] fix = new boolean[I][D][T];
    for (FixedAssignment assign : problem.getFixedAssignments()) {
      int i = problem.getStaffs().indexOf(assign.getStaff());
      int d = assign.getDay();
      int t = assign.isDayOff() ? 0 : problem.getShifts().indexOf(assign.getShift()) + 1;
      fix[i][d][t] = true;
    }

    for (int t = 1; t < T; t++) {
      if (problem.getShifts().get(t - 1).isAutoAllocate()) {
        continue;
      }
      for (int i = 0; i < I; i++) {
        for (int d = 0; d < D; d++) {
          if (fix[i][d][t]) {
            continue;
          }
          Expression cons = neg(x[i][d][t]);
          cons.setComment(format("C02_i%02d_d%02d_t%02d", i, d, t));
          res.add(cons);
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C03: あるスタッフに，ある日において，あるシフトを割り当てることが決められていることがある.
   * (FixedAssignments)
   */
  private List<Expression> generateConstraint3() {
    List<Expression> res = new ArrayList<>();

    for (FixedAssignment assign : problem.getFixedAssignments()) {
      int i = problem.getStaffs().indexOf(assign.getStaff());
      int d = assign.getDay();
      int t = assign.isDayOff() ? 0 : problem.getShifts().indexOf(assign.getShift()) + 1;

      Expression cons = pos(x[i][d][t]);
      cons.setComment(format("C03_i%02d_d%02d_t%02d", i, d, t));
      res.add(cons);
    }

    return Collections.unmodifiableList(res);
  }

  private int getDayOfWeek(int day) {
    long date = problem.getBeginDate() + day * (24L * 3600L * 1000L);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(date);
    switch (calendar.get(Calendar.DAY_OF_WEEK)) {
      case Calendar.SUNDAY:
        return 0;
      case Calendar.MONDAY:
        return 1;
      case Calendar.TUESDAY:
        return 2;
      case Calendar.WEDNESDAY:
        return 3;
      case Calendar.THURSDAY:
        return 4;
      case Calendar.FRIDAY:
        return 5;
      case Calendar.SATURDAY:
        return 6;
      default:
        throw new IllegalStateException();
    }
  }

  private List<Expression> generateMatches(int i, int beginDay, int endDay, Pattern pat) {
    List<Expression> res = new ArrayList<>();

    int patLen = pat.getAtoms().size();
    for (int d = beginDay; d <= endDay - patLen + 1; d++) {
      if (pat.hasStartDay() && d != pat.getStartDay()) {
        continue;
      }
      if (pat.hasStartDayOfWeek() && getDayOfWeek(d) != pat.getStartDayOfWeek()) {
        continue;
      }
      List<Expression> patTerms = new ArrayList<>();
      for (int off = 0; off < patLen; off++) {
        PatternAtom atom = pat.getAtoms().get(off);
        switch (atom.getType()) {
          case SHIFT: {
            int t = problem.getShifts().indexOf(atom.getShift()) + 1;
            patTerms.add(pos(x[i][d + off][t]));
          } break;
          case GROUP: {
            List<Expression> terms = new ArrayList<>();
            for (Shift s : atom.getShiftGroup().getShifts()) {
              int t = problem.getShifts().indexOf(s) + 1;
              terms.add(pos(x[i][d + off][t]));
            }
            patTerms.add(create(OR, terms));
          } break;
          case NOT_SHIFT: {
            int t = problem.getShifts().indexOf(atom.getShift()) + 1;
            patTerms.add(neg(x[i][d + off][t]));
          } break;
          case NOT_GROUP: {
            List<Expression> terms = new ArrayList<>();
            for (Shift s : atom.getShiftGroup().getShifts()) {
              int t = problem.getShifts().indexOf(s) + 1;
              terms.add(neg(x[i][d + off][t]));
            }
            patTerms.add(create(AND, terms));
          } break;
          case ANY: {
          } break;
          case ANY_SHIFT: {
            patTerms.add(neg(x[i][d + off][0]));
          } break;
          case DAY_OFF: {
            patTerms.add(pos(x[i][d + off][0]));
          } break;
          default:
            throw new RuntimeException();
        }
      }
      res.add(create(IF, create(AND, patTerms), ONE, ZERO));
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C04: あるスタッフにはその働き方に制約が設けられている.
   * (Contracts)
   */
  private List<Expression> generateConstraint4() {
    List<Expression> res = new ArrayList<>();

    for (int i = 0; i < I; i++) {
      int contCount = 0;
      for (Contract cont : problem.getStaffs().get(i).getContracts()) {
        for (PatternContract patCont : cont.getPatternContracts()) {
          List<Expression> terms = new ArrayList<>();
          for (Pattern pat : patCont.getPatterns()) {
            terms.addAll(generateMatches(i, patCont.getBeginDay(), patCont.getEndDay(), pat));
          }

          if (patCont.hasLowerBound()) {
            Expression cons = create(GE, create(ADD, terms), create(patCont.getMin()));
            cons.setComment(format("C04LB_i%02d_%03d", i, contCount));
            res.add(cons);
          }
          if (patCont.hasUpperBound()) {
            Expression cons = create(LE, create(ADD, terms), create(patCont.getMax()));
            cons.setComment(format("C04UB_i%02d_%03d", i, contCount));
            res.add(cons);
          }
          contCount++;
        }
      }
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C05: DayOffRequests.
   */
  private List<Expression> generateConstraint5() {
    List<Expression> res = new ArrayList<>();

    for (DayOffRequest req : problem.getDayOffRequests()) {
      int i = problem.getStaffs().indexOf(req.getStaff());
      int d = req.getDay();
      Expression cons = pos(x[i][d][0]);
      cons.setComment(format("C05_i%02d_d%02d", i, d));
      res.add(cons);
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C06: ShiftOffRequests.
   */
  private List<Expression> generateConstraint6() {
    List<Expression> res = new ArrayList<>();

    for (ShiftOffRequest req : problem.getShiftOffRequests()) {
      int i = problem.getStaffs().indexOf(req.getStaff());
      int d = req.getDay();
      int t = problem.getShifts().indexOf(req.getShift()) + 1;
      Expression cons = neg(x[i][d][t]);
      cons.setComment(format("C06_i%02d_d%02d_t%02d", i, d, t));
      res.add(cons);
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C07: ShiftOnRequests.
   */
  private List<Expression> generateConstraint7() {
    List<Expression> res = new ArrayList<>();

    for (ShiftOnRequest req : problem.getShiftOnRequests()) {
      int i = problem.getStaffs().indexOf(req.getStaff());
      int d = req.getDay();
      int t = problem.getShifts().indexOf(req.getShift()) + 1;
      Expression cons = pos(x[i][d][t]);
      cons.setComment(format("C07_i%02d_d%02d_t%02d", i, d, t));
      res.add(cons);
    }

    return Collections.unmodifiableList(res);
  }

  private static <T> boolean hasIntersection(Collection<T> c1, Collection<T> c2) {
    for (T e1 : c1) {
      if (c2.contains(e1)) {
        return true;
      }
    }
    return false;
  }

  /**
   * C08: 必要人数のソフト制約.
   * (Cover)
   */
  private List<Expression> generateConstraint8() {
    List<Expression> res = new ArrayList<>();

    int coverCount = 0;
    for (Cover cover : problem.getCovers()) {
      int d = cover.getDay();
      List<Shift> targetShifts = new ArrayList<>();
      if (cover.getShift() != null) {
        targetShifts.add(cover.getShift());
      }
      if (cover.getShiftGroup() != null) {
        targetShifts.addAll(cover.getShiftGroup().getShifts());
      }
      if (cover.getShift() == null && cover.getShiftGroup() == null) {
        targetShifts.addAll(problem.getShifts());
      }

      List<Skill> targetSkills = new ArrayList<>();
      if (cover.getSkill() != null) {
        targetSkills.add(cover.getSkill());
      }
      if (cover.getSkillGroup() != null) {
        targetSkills.addAll(cover.getSkillGroup().getSkills());
      }
      if (cover.getSkill() == null && cover.getSkillGroup() == null) {
        targetSkills.addAll(problem.getSkills());
      }

      List<Staff> targetStaffs = new ArrayList<>();
      for (Staff staff : problem.getStaffs()) {
        if (hasIntersection(staff.getSkills(), targetSkills)) {
          targetStaffs.add(staff);
        }
      }

      List<Expression> terms = new ArrayList<>();
      for (Staff staff : targetStaffs) {
        int i = problem.getStaffs().indexOf(staff);
        for (Shift shift : targetShifts) {
          int t = problem.getShifts().indexOf(shift) + 1;
          terms.add(x[i][d][t]);
        }
      }

      if (cover.hasLowerBound()) {
        Expression p = create(format("_P1_d%02d_%03d", d, coverCount));
        final int pMax = Math.max(0, cover.getMin());
        final int pWeight = problem.getUnderStaffingWeight();
        res.add(create(INT_DEFINITION, p, ZERO, create(pMax)));

        penaltyVariables.add(p);
        penaltyVariableUpperBound.put(p, pMax);
        penaltyVariableWeight.put(p, pWeight);

        Expression cons = create(GE, create(ADD, create(ADD, terms), p), create(cover.getMin()));
        cons.setComment(format("C08LB_d%02d_%03d", d, coverCount));
        res.add(cons);
      }

      if (cover.hasUpperBound()) {
        Expression p = create(format("_P2_d%02d_%03d", d, coverCount));
        final int pMax = Math.max(0, targetStaffs.size() - cover.getMax());
        final int pWeight = problem.getOverStaffingWeight();
        res.add(create(INT_DEFINITION, p, ZERO, create(pMax)));

        penaltyVariables.add(p);
        penaltyVariableUpperBound.put(p, pMax);
        penaltyVariableWeight.put(p, pWeight);

        Expression cons = create(LE, create(SUB, create(ADD, terms), p), create(cover.getMax()));
        cons.setComment(format("C08UB_d%02d_%03d", d, coverCount));
        res.add(cons);
      }
      coverCount++;
    }

    return Collections.unmodifiableList(res);
  }

  private List<Expression> generateObjectiveDeclaration() {
    List<Expression> res = new ArrayList<>();

    int maxPenalty = 0;
    for (Expression p : penaltyVariables) {
      maxPenalty += penaltyVariableWeight.get(p) * penaltyVariableUpperBound.get(p);
    }

    Expression obj = create("OBJ");
    res.add(create(INT_DEFINITION, obj, ZERO, create(maxPenalty)));

    List<Expression> terms = new ArrayList<>();
    for (Expression p : penaltyVariables) {
      terms.add(p);
    }
    res.add(create(EQ, obj, create(ADD, terms)));

    res.add(create(OBJECTIVE_DEFINITION, MINIMIZE, obj));

    return Collections.unmodifiableList(res);
  }

  /*
   * PUBLIC
   */
  public List<Expression> getAllExpressions() {
    List<Expression> res = new ArrayList<>();
    res.addAll(variableDeclarations);
    res.addAll(constraint1);
    res.addAll(constraint2);
    res.addAll(constraint3);
    res.addAll(constraint4);
    res.addAll(constraint5);
    res.addAll(constraint6);
    res.addAll(constraint7);
    res.addAll(constraint8);
    res.addAll(objectiveDeclaration);
    return Collections.unmodifiableList(res);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Expression e : getAllExpressions()) {
      sb.append(e);
      if (e.getComment() != null) {
        sb.append(" ; ").append(e.getComment());
      }
      sb.append(format("%n"));
    }
    return sb.toString();
  }

  public static void main(String[] args) throws Exception {
    Problem problem = Problem.of(new java.io.File(args[0]));
    Sugar4jFormulator formulator = new Sugar4jFormulator(problem);
    System.out.println(formulator);
  }
}
// Java CHECKSTYLE:ON LocalVariableName
// Java CHECKSTYLE:ON MemberName
// Java CHECKSTYLE:ON ParameterName
