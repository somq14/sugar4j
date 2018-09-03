package jp.ac.meiji.igusso.scheduling;

import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Cover;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.DaysOff;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Shift;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOffRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.ShiftOnRequests;
import static jp.ac.meiji.igusso.scheduling.SchedulingProblem.Staff;

import lombok.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SchedulingProblemParser {
  private static Pattern COMMENT_PATTERN = Pattern.compile("^#.*$");

  private BufferedReader br;

  private int line = 0;
  private String cur = "";

  private boolean created = false;
  private SchedulingProblem.SchedulingProblemBuilder problemBuilder = SchedulingProblem.builder();

  public SchedulingProblemParser(@NonNull Reader reader) {
    this.br = new BufferedReader(reader);
  }

  private void nextLine() {
    try {
      do {
        cur = br.readLine();
        if (cur == null) {
          return;
        }
        cur = cur.trim();
        line++;
      } while (COMMENT_PATTERN.matcher(cur).matches());
    } catch (IOException ex) {
      throw new SchedulingProblemException(ex);
    }
  }

  private void skipEmptyLines() {
    while (cur != null && cur.equals("")) {
      nextLine();
    }
  }

  private void parseSectionHorizon() {
    skipEmptyLines();
    if (!cur.equals("SECTION_HORIZON")) {
      throw new SchedulingProblemException("SECTION_HORIZON not found: " + line);
    }
    nextLine();

    problemBuilder.length(Integer.valueOf(cur));
    nextLine();
  }

  private void parseSectionShifts() {
    skipEmptyLines();
    if (!cur.equals("SECTION_SHIFTS")) {
      throw new SchedulingProblemException("SECTION_SHIFTS not found: " + line);
    }
    nextLine();

    Map<String, Shift> shifts = new HashMap<String, Shift>();
    while (cur != null && !cur.equals("")) {
      String[] csv = cur.split(",");
      String id = csv[0];
      int length = Integer.valueOf(csv[1]);

      List<String> notFollow =
          csv.length == 3 ? Arrays.asList(csv[2].split("\\|")) : Arrays.asList();

      shifts.put(id, new Shift(id, length, notFollow));

      nextLine();
    }

    problemBuilder.shifts(shifts);
  }

  private void parseSectionStaff() {
    skipEmptyLines();
    if (!cur.equals("SECTION_STAFF")) {
      throw new SchedulingProblemException("SECTION_STAFF not found: " + line);
    }
    nextLine();

    Map<String, Staff> staff = new HashMap<>();
    while (cur != null && !cur.equals("")) {
      String[] csv = cur.split(",");

      String id = csv[0];
      Map<String, Integer> maxShifts = new HashMap<>();
      for (String elem : csv[1].split("\\|")) {
        String[] csv2 = elem.split("=");
        maxShifts.put(csv2[0], Integer.valueOf(csv2[1]));
      }
      int maxTotalMinutes = Integer.valueOf(csv[2]);
      int minTotalMinutes = Integer.valueOf(csv[3]);
      int maxConsecutiveShifts = Integer.valueOf(csv[4]);
      int minConsecutiveShifts = Integer.valueOf(csv[5]);
      int minConsecutiveDayOff = Integer.valueOf(csv[6]);
      int maxWeekends = Integer.valueOf(csv[7]);

      staff.put(id,
          new Staff(id, maxShifts, maxTotalMinutes, minTotalMinutes, maxConsecutiveShifts,
              minConsecutiveShifts, minConsecutiveDayOff, maxWeekends));

      nextLine();
    }

    problemBuilder.staff(staff);
  }

  private void parseSectionDaysOff() {
    skipEmptyLines();
    if (!cur.equals("SECTION_DAYS_OFF")) {
      throw new SchedulingProblemException("SECTION_DAYS_OFF not found: " + line);
    }
    nextLine();

    Map<String, DaysOff> daysOff = new HashMap<>();
    while (cur != null && !cur.equals("")) {
      String[] csv = cur.split(",");
      String staffId = csv[0];
      List<Integer> dayIndexes = new ArrayList<>();
      for (int i = 1; i < csv.length; i++) {
        dayIndexes.add(Integer.valueOf(csv[i]));
      }

      daysOff.put(staffId, new DaysOff(staffId, dayIndexes));

      nextLine();
    }

    problemBuilder.daysOff(daysOff);
  }

  private void parseSectionShiftOnRequests() {
    skipEmptyLines();
    if (!cur.equals("SECTION_SHIFT_ON_REQUESTS")) {
      throw new SchedulingProblemException("SECTION_SHIFT_ON_REQUESTS not found: " + line);
    }
    nextLine();

    List<ShiftOnRequests> shiftOnRequests = new ArrayList<>();
    while (cur != null && !cur.equals("")) {
      String[] csv = cur.split(",");
      String staffId = csv[0];
      int day = Integer.valueOf(csv[1]);
      String shiftId = csv[2];
      int weight = Integer.valueOf(csv[3]);

      shiftOnRequests.add(new ShiftOnRequests(staffId, day, shiftId, weight));

      nextLine();
    }

    problemBuilder.shiftOnRequests(shiftOnRequests);
  }

  private void parseSectionShiftOffRequests() {
    skipEmptyLines();
    if (!cur.equals("SECTION_SHIFT_OFF_REQUESTS")) {
      throw new SchedulingProblemException("SECTION_SHIFT_OFF_REQUESTS not found: " + line);
    }
    nextLine();

    List<ShiftOffRequests> shiftOffRequests = new ArrayList<>();
    while (cur != null && !cur.equals("")) {
      String[] csv = cur.split(",");
      String staffId = csv[0];
      int day = Integer.valueOf(csv[1]);
      String shiftId = csv[2];
      int weight = Integer.valueOf(csv[3]);

      shiftOffRequests.add(new ShiftOffRequests(staffId, day, shiftId, weight));

      nextLine();
    }

    problemBuilder.shiftOffRequests(shiftOffRequests);
  }

  private void parseSectionCover() {
    skipEmptyLines();
    if (!cur.equals("SECTION_COVER")) {
      throw new SchedulingProblemException("SECTION_SHIFT_OFF_REQUESTS not found: " + line);
    }
    nextLine();

    List<Cover> cover = new ArrayList<>();
    while (cur != null && !cur.equals("")) {
      String[] csv = cur.split(",");
      int day = Integer.valueOf(csv[0]);
      String shiftId = csv[1];
      int requirement = Integer.valueOf(csv[2]);
      int weightUnder = Integer.valueOf(csv[3]);
      int weightOver = Integer.valueOf(csv[4]);

      cover.add(new Cover(day, shiftId, requirement, weightUnder, weightOver));

      nextLine();
    }

    problemBuilder.cover(cover);
  }

  public SchedulingProblem parse() throws SchedulingProblemException {
    if (created) {
      return problemBuilder.build();
    }

    try {
      parseSectionHorizon();
      parseSectionShifts();
      parseSectionStaff();
      parseSectionDaysOff();
      parseSectionShiftOnRequests();
      parseSectionShiftOffRequests();
      parseSectionCover();
    } catch (SchedulingProblemException sex) {
      throw sex;
    } catch (Exception ex) {
      throw new SchedulingProblemException(ex);
    }

    created = true;
    return problemBuilder.build();
  }
}
