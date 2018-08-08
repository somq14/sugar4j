package jp.ac.meiji.igusso.scheduling;

import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Cover;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.DaysOff;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Shift;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOffRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOnRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Staff;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EqualsAndHashCode
@ToString
public final class SchedulingProblemUtil {
  private SchedulingProblemUtil() {}

  public static SchedulingProblem trim(@NonNull SchedulingProblem problem, int week) {
    if (problem.getLength() <= week * 7) {
      return problem;
    }

    final double rate = 1.0 * (week * 7) / problem.getLength();

    SchedulingProblem.SchedulingProblemBuilder builder = SchedulingProblem.builder();
    builder.length(week * 7);

    builder.shifts(problem.getShifts());

    Map<String, Staff> trimedStaff = new HashMap<>();
    for (Staff staff : problem.getStaff().values()) {
      Map<String, Integer> trimedMaxShifts = new HashMap<>();
      for (String shiftId : staff.getMaxShifts().keySet()) {
        int trimedDays = (int) Math.round(staff.getMaxShifts().get(shiftId) * rate);
        trimedMaxShifts.put(shiftId, trimedDays);
      }

      int trimedMaxTotalMinutes = (int) Math.round(1.25 * staff.getMaxTotalMinutes() * rate);
      int trimedMinTotalMinutes = (int) Math.round(0.75 * staff.getMinTotalMinutes() * rate);
      int trimedMaxWeekends = (int) Math.round(staff.getMaxWeekends() * rate);

      trimedStaff.put(staff.getId(),
          new Staff(staff.getId(), trimedMaxShifts, trimedMaxTotalMinutes, trimedMinTotalMinutes,
              staff.getMaxConsecutiveShifts(), staff.getMinConsecutiveShifts(),
              staff.getMinConsecutiveDayOff(), trimedMaxWeekends));
    }
    builder.staff(trimedStaff);

    Map<String, DaysOff> trimedDaysOff = new HashMap<>();
    for (DaysOff daysOff : problem.getDaysOff().values()) {
      trimedDaysOff.put(daysOff.getStaffId(), new DaysOff(daysOff.getStaffId(), new ArrayList<>()));
    }
    builder.daysOff(trimedDaysOff);

    List<ShiftOnRequests> trimedShiftOnRequests = new ArrayList<>();
    for (ShiftOnRequests req : problem.getShiftOnRequests()) {
      if (req.getDay() < 7 * week) {
        trimedShiftOnRequests.add(req);
      }
    }
    builder.shiftOnRequests(trimedShiftOnRequests);

    List<ShiftOffRequests> trimedShiftOffRequests = new ArrayList<>();
    for (ShiftOffRequests req : problem.getShiftOffRequests()) {
      if (req.getDay() < 7 * week) {
        trimedShiftOffRequests.add(req);
      }
    }

    for (DaysOff daysOff : problem.getDaysOff().values()) {
      List<Integer> days = new ArrayList<>();
      for (Integer day : daysOff.getDayIndexes()) {
        if (day >= week * 7) {
          continue;
        }

        for (String shiftId : problem.getShifts().keySet()) {
          trimedShiftOffRequests.add(new ShiftOffRequests(daysOff.getStaffId(), day, shiftId, 50));
        }
      }
    }
    builder.shiftOffRequests(trimedShiftOffRequests);

    List<Cover> trimedCover = problem.getCover()
                                  .stream()
                                  .filter(it -> it.getDay() < 7 * week)
                                  .collect(Collectors.toList());
    builder.cover(trimedCover);

    return builder.build();
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Command     : Main [InstanceFile] [Week]");
      System.err.println("Description : Trim Instance File");
      return;
    }

    SchedulingProblem problem = SchedulingProblem.parse(new java.io.FileReader(args[0]));
    SchedulingProblem trimedProblem = trim(problem, Integer.valueOf(args[1]));
    trimedProblem.encode(new java.io.OutputStreamWriter(System.out));
  }
}
