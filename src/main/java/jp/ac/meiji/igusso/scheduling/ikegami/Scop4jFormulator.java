package jp.ac.meiji.igusso.scheduling.ikegami;

import static java.lang.String.format;
import static jp.ac.meiji.igusso.scop4j.Comparator.EQ;
import static jp.ac.meiji.igusso.scop4j.Comparator.LE;
import static jp.ac.meiji.igusso.scop4j.Comparator.GE;

import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import jp.ac.meiji.igusso.scop4j.Variable;
import jp.ac.meiji.igusso.scop4j.Constraint;
import jp.ac.meiji.igusso.scop4j.LinearConstraint;
import jp.ac.meiji.igusso.scop4j.Term;
import jp.ac.meiji.igusso.scop4j.Comparator;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Calendar;

@ToString
@EqualsAndHashCode
public final class Scop4jFormulator {
  private final Problem problem;

  // Parameters
  private int I;
  private int D;
  private int T;

  // Variables
  private Variable[][] x;

  // Generated Variables
  private List<Variable> variables;
  private List<Variable> tempVariables = new ArrayList<>();

  // Generated Constraints
  private List<Constraint> constraint1;
  private List<Constraint> constraint2;
  private List<Constraint> constraint3;
  private List<Constraint> constraint4;
  private List<Constraint> constraint5;
  private List<Constraint> constraint6;
  private List<Constraint> constraint7;
  private List<Constraint> constraint8;
  private List<Constraint> tempConstraints = new ArrayList<>();

  public Scop4jFormulator(@NonNull Problem problem) {
    this.problem = problem;

    initializeParameters();
    this.variables = generateVariables();
    generateConstraints();
  }

  private void initializeParameters() {
    this.I = problem.getStaffs().size();
    this.D = problem.getLength();
    this.T = problem.getShifts().size() + 1;
  }

  private List<Variable> generateVariables() {
    x = new Variable[I][D];

    List<String> domain = new ArrayList<>();
    for (int t = 0; t < T; t++) {
      domain.add(String.valueOf(t));
    }

    List<Variable> res = new ArrayList<>();
    for (int i = 0; i < I; i++) {
      for (int d = 0; d < D; d++) {
        x[i][d] = Variable.of(format("x_i%02d_d%02d", i, d), domain);
        res.add(x[i][d]);
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
  }

  /**
   * C01: あるスタッフは，ある日において，ただひとつのシフトに割り当てられる.
   */
  private List<Constraint> generateConstraint1() {
    List<Constraint> res = new ArrayList<>();
    return Collections.unmodifiableList(res);
  }

  /**
   * C02: 自動割当が無効のシフトには，固定シフト割当がない限り，割り当てない.
   * (AutoAllocate)
   */
  private List<Constraint> generateConstraint2() {
    List<Constraint> res = new ArrayList<>();

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
          String consName = format("C02_i%02d_d%02d_t%02d", i, d, t);
          Constraint cons = LinearConstraint.of(consName, EQ, 0).addTerm(1, x[i][d], t).build();
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
  private List<Constraint> generateConstraint3() {
    List<Constraint> res = new ArrayList<>();

    for (FixedAssignment assign : problem.getFixedAssignments()) {
      int i = problem.getStaffs().indexOf(assign.getStaff());
      int d = assign.getDay();
      int t = assign.isDayOff() ? 0 : problem.getShifts().indexOf(assign.getShift()) + 1;

      String consName = format("C03_i%02d_d%02d_t%02d", i, d, t);
      Constraint cons = LinearConstraint.of(consName, EQ, 1).addTerm(1, x[i][d], t).build();
      res.add(cons);
    }

    return Collections.unmodifiableList(res);
  }

  private Variable andConstraint(int n, List<Term> terms, int lhs) {
    int tempId = tempVariables.size();
    Variable tempVar = Variable.of(format("temp%04d", tempId), 0, 1);
    tempVariables.add(tempVar);

    LinearConstraint.Builder consUb =
        LinearConstraint.of(format("AND_UB_%04d", tempId), LE, n - 1 - lhs);
    for (Term t : terms) {
      consUb.addTerm(t);
    }
    consUb.addTerm(Term.of(-n, tempVar, 1));
    tempConstraints.add(consUb.build());

    LinearConstraint.Builder consLb =
        LinearConstraint.of(format("AND_LB_%04d", tempId), GE, n - lhs);
    for (Term t : terms) {
      consLb.addTerm(t);
    }
    consLb.addTerm(Term.of(n, tempVar, 0));
    tempConstraints.add(consLb.build());

    return tempVar;
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

  private List<Term> generateMatches(int i, int beginDay, int endDay, Pattern pat) {
    List<Term> res = new ArrayList<>();

    int patLen = pat.getAtoms().size();
    for (int d = beginDay; d <= endDay - patLen + 1; d++) {
      if (pat.hasStartDay() && d != pat.getStartDay()) {
        continue;
      }
      if (pat.hasStartDayOfWeek() && getDayOfWeek(d) != pat.getStartDayOfWeek()) {
        continue;
      }

      List<Term> patTerms = new ArrayList<>();
      int lhs = 0;
      for (int off = 0; off < patLen; off++) {
        PatternAtom atom = pat.getAtoms().get(off);
        switch (atom.getType()) {
          case SHIFT: {
            int t = problem.getShifts().indexOf(atom.getShift()) + 1;
            patTerms.add(Term.of(1, x[i][d + off], t));
          } break;
          case GROUP: {
            for (Shift shift : atom.getShiftGroup().getShifts()) {
              int t = problem.getShifts().indexOf(shift) + 1;
              patTerms.add(Term.of(1, x[i][d + off], t));
            }
          } break;
          case NOT_SHIFT: {
            int t = problem.getShifts().indexOf(atom.getShift()) + 1;
            patTerms.add(Term.of(-1, x[i][d + off], t));
            lhs++;
          } break;
          case NOT_GROUP: {
            for (Shift shift : atom.getShiftGroup().getShifts()) {
              int t = problem.getShifts().indexOf(shift) + 1;
              patTerms.add(Term.of(-1, x[i][d + off], t));
            }
            lhs += atom.getShiftGroup().getShifts().size();
          } break;
          case ANY: {
            lhs++;
          } break;
          case ANY_SHIFT: {
            patTerms.add(Term.of(-1, x[i][d + off], 0));
            lhs++;
          } break;
          case DAY_OFF: {
            patTerms.add(Term.of(1, x[i][d + off], 0));
          } break;
          default:
            throw new RuntimeException();
        }
      }
      res.add(Term.of(1, andConstraint(patLen, patTerms, lhs), 1));
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C04: あるスタッフにはその働き方に制約が設けられている.
   * (Contracts)
   */
  private List<Constraint> generateConstraint4() {
    List<Constraint> res = new ArrayList<>();

    for (int i = 0; i < I; i++) {
      int contCount = 0;
      for (Contract cont : problem.getStaffs().get(i).getContracts()) {
        for (PatternContract patCont : cont.getPatternContracts()) {
          List<Term> terms = new ArrayList<>();
          for (Pattern pat : patCont.getPatterns()) {
            terms.addAll(generateMatches(i, patCont.getBeginDay(), patCont.getEndDay(), pat));
          }

          if (patCont.hasLowerBound()) {
            String consName = format("C04LB_i%02d_%03d", i, contCount);
            LinearConstraint.Builder cons = LinearConstraint.of(consName, GE, patCont.getMin());
            for (Term term : terms) {
              cons.addTerm(term);
            }
            res.add(cons.build());
          }

          if (patCont.hasUpperBound()) {
            String consName = format("C04UB_i%02d_%03d", i, contCount);
            LinearConstraint.Builder cons = LinearConstraint.of(consName, LE, patCont.getMax());
            for (Term term : terms) {
              cons.addTerm(term);
            }
            res.add(cons.build());
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
  private List<Constraint> generateConstraint5() {
    List<Constraint> res = new ArrayList<>();

    for (DayOffRequest req : problem.getDayOffRequests()) {
      int i = problem.getStaffs().indexOf(req.getStaff());
      int d = req.getDay();

      String consName = format("C05_i%02d_d%02d", i, d);
      Constraint cons = LinearConstraint.of(consName, EQ, 1).addTerm(1, x[i][d], 0).build();
      res.add(cons);
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C06: ShiftOffRequests.
   */
  private List<Constraint> generateConstraint6() {
    List<Constraint> res = new ArrayList<>();

    int id = 0;
    for (ShiftOffRequest req : problem.getShiftOffRequests()) {
      int i = problem.getStaffs().indexOf(req.getStaff());
      int d = req.getDay();
      int t = problem.getShifts().indexOf(req.getShift()) + 1;

      String consName = format("C06_i%02d_d%02d_t%02d_%03d", i, d, t, id++);
      Constraint cons = LinearConstraint.of(consName, EQ, 0).addTerm(1, x[i][d], t).build();
      res.add(cons);
    }

    return Collections.unmodifiableList(res);
  }

  /**
   * C07: ShiftOnRequests.
   */
  private List<Constraint> generateConstraint7() {
    List<Constraint> res = new ArrayList<>();

    for (ShiftOnRequest req : problem.getShiftOnRequests()) {
      int i = problem.getStaffs().indexOf(req.getStaff());
      int d = req.getDay();
      int t = problem.getShifts().indexOf(req.getShift()) + 1;

      String consName = format("C07_i%02d_d%02d_t%02d", i, d, t);
      Constraint cons = LinearConstraint.of(consName, EQ, 1).addTerm(1, x[i][d], t).build();
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
  private List<Constraint> generateConstraint8() {
    List<Constraint> res = new ArrayList<>();

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

      if (cover.hasLowerBound()) {
        String consName = format("C08LB_d%02d_%03d", d, coverCount);
        LinearConstraint.Builder cons =
            LinearConstraint.of(consName, GE, cover.getMin(), problem.getUnderStaffingWeight());
        for (Staff staff : targetStaffs) {
          int i = problem.getStaffs().indexOf(staff);
          for (Shift shift : targetShifts) {
            int t = problem.getShifts().indexOf(shift) + 1;
            cons.addTerm(1, x[i][d], t);
          }
        }
        res.add(cons.build());
      }

      if (cover.hasUpperBound()) {
        String consName = format("C08UB_d%02d_%03d", d, coverCount);
        LinearConstraint.Builder cons =
            LinearConstraint.of(consName, LE, cover.getMax(), problem.getOverStaffingWeight());
        for (Staff staff : targetStaffs) {
          int i = problem.getStaffs().indexOf(staff);
          for (Shift shift : targetShifts) {
            int t = problem.getShifts().indexOf(shift) + 1;
            cons.addTerm(1, x[i][d], t);
          }
        }
        res.add(cons.build());
      }

      coverCount++;
    }

    return Collections.unmodifiableList(res);
  }

  /*
   * PUBLIC
   */
  public List<Variable> getVariables() {
    List<Variable> res = new ArrayList<>();
    res.addAll(variables);
    res.addAll(tempVariables);
    return Collections.unmodifiableList(res);
  }

  public List<Constraint> getAllConstraints() {
    List<Constraint> res = new ArrayList<>();
    res.addAll(constraint1);
    res.addAll(constraint2);
    res.addAll(constraint3);
    res.addAll(constraint4);
    res.addAll(constraint5);
    res.addAll(constraint6);
    res.addAll(constraint7);
    res.addAll(constraint8);
    res.addAll(tempConstraints);
    return Collections.unmodifiableList(res);
  }

  public static void main(String[] args) throws Exception {
    Problem problem = Problem.of(new java.io.File(args[0]));
    Scop4jFormulator formulator = new Scop4jFormulator(problem);

    jp.ac.meiji.igusso.scop4j.Scop4j scop4j = jp.ac.meiji.igusso.scop4j.Scop4j.newInstance();
    scop4j.addVariables(formulator.getVariables());
    scop4j.addConstraints(formulator.getAllConstraints());
    scop4j.setTimeout(600);

    jp.ac.meiji.igusso.scop4j.Solution sol = scop4j.solve();

    List<String> logBody = java.nio.file.Files.readAllLines(
        scop4j.getLogFile(), java.nio.charset.Charset.defaultCharset());
    for (String line : logBody) {
      System.out.println(line);
    }

    for (Variable variable : formulator.getVariables()) {
      System.out.printf("%s = %s%n", variable.getName(), sol.getSolution().get(variable));
    }
    System.out.printf("Penalty = %d%n", sol.getSoftPenalty());
    System.out.printf("Cpu Time = %d [ms]%n", sol.getCpuTime());
    System.out.printf("Cpu Time (Last Improved) = %d [ms]%n", sol.getLastImprovedCpuTime());
  }
}
