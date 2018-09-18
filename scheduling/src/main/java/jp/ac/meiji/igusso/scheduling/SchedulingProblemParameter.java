package jp.ac.meiji.igusso.scheduling;

import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Cover;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Shift;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOffRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOnRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Staff;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Java CHECKSTYLE:OFF MemberName
// Java CHECKSTYLE:OFF AbbreviationAsWordInName
// Java CHECKSTYLE:OFF LocalVariableName
public final class SchedulingProblemParameter {
  private final SchedulingProblem problem;

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

  public SchedulingProblemParameter(@NonNull SchedulingProblem problem) {
    this.problem = problem;
    initializeParameters();
  }

  public int getH() {
    return H;
  }

  public int[] getI() {
    return I.clone();
  }

  public int[] getD() {
    return D.clone();
  }

  public int[] getW() {
    return W.clone();
  }

  public int[] getT() {
    return T.clone();
  }

  public int[][] getR() {
    return R.clone();
  }

  public int[][] getN() {
    return N.clone();
  }

  public int[] getL() {
    return L.clone();
  }

  public int[][] getMmax() {
    return M_MAX.clone();
  }

  public int[] getBmin() {
    return B_MIN.clone();
  }

  public int[] getBmax() {
    return B_MAX.clone();
  }

  public int[] getCmin() {
    return C_MIN.clone();
  }

  public int[] getCmax() {
    return C_MAX.clone();
  }

  public int[] getOmin() {
    return O_MIN.clone();
  }

  public int[] getAmax() {
    return A_MAX.clone();
  }

  public int[][][] getQ() {
    return Q.clone();
  }

  public int[][][] getP() {
    return P.clone();
  }

  public int[][] getU() {
    return U.clone();
  }

  public int[][] getVmin() {
    return V_MIN.clone();
  }

  public int[][] getVmax() {
    return V_MAX.clone();
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
}
// Java CHECKSTYLE:ON MemberName
// Java CHECKSTYLE:ON AbbreviationAsWordInName
// Java CHECKSTYLE:ON LocalVariableName
