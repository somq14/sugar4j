package jp.ac.meiji.igusso.scheduling;

import static java.lang.String.format;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder
public final class SchedulingProblem {
  private int length;
  private Map<String, Shift> shifts;
  private Map<String, Staff> staff;
  private Map<String, DaysOff> daysOff;
  private List<ShiftOnRequests> shiftOnRequests;
  private List<ShiftOffRequests> shiftOffRequests;
  private List<Cover> cover;

  private SchedulingProblem(int length, @NonNull Map<String, Shift> shifts,
      @NonNull Map<String, Staff> staff, @NonNull Map<String, DaysOff> daysOff,
      @NonNull List<ShiftOnRequests> shiftOnRequests,
      @NonNull List<ShiftOffRequests> shiftOffRequests, @NonNull List<Cover> cover) {
    if (length < 0) {
      throw new IllegalStateException("" + length);
    }

    this.length = length;
    this.shifts = Collections.unmodifiableMap(new HashMap<String, Shift>(shifts));
    this.staff = Collections.unmodifiableMap(new HashMap<String, Staff>(staff));
    this.daysOff = Collections.unmodifiableMap(new HashMap<String, DaysOff>(daysOff));
    this.shiftOnRequests =
        Collections.unmodifiableList(new ArrayList<ShiftOnRequests>(shiftOnRequests));
    this.shiftOffRequests =
        Collections.unmodifiableList(new ArrayList<ShiftOffRequests>(shiftOffRequests));
    this.cover = Collections.unmodifiableList(new ArrayList<Cover>(cover));
  }

  @Value
  public static final class Shift {
    private String id;
    private int length;
    private List<String> notFollow;

    public Shift(String id, int length, @NonNull List<String> notFollow) {
      this.id = id;
      this.length = length;
      this.notFollow = Collections.unmodifiableList(new ArrayList<>(notFollow));
    }
  }

  @Value
  public static final class Staff {
    private String id;
    private Map<String, Integer> maxShifts;
    private int maxTotalMinutes;
    private int minTotalMinutes;
    private int maxConsecutiveShifts;
    private int minConsecutiveShifts;
    private int minConsecutiveDayOff;
    private int maxWeekends;

    public Staff(String id, @NonNull Map<String, Integer> maxShifts, int maxTotalMinutes,
        int minTotalMinutes, int maxConsecutiveShifts, int minConsecutiveShifts,
        int minConsecutiveDayOff, int maxWeekends) {
      this.id = id;
      this.maxShifts = Collections.unmodifiableMap(new HashMap<String, Integer>(maxShifts));
      this.maxTotalMinutes = maxTotalMinutes;
      this.minTotalMinutes = minTotalMinutes;
      this.maxConsecutiveShifts = maxConsecutiveShifts;
      this.minConsecutiveShifts = minConsecutiveShifts;
      this.minConsecutiveDayOff = minConsecutiveDayOff;
      this.maxWeekends = maxWeekends;
    }
  }

  @Value
  public static final class DaysOff {
    private String staffId;
    private List<Integer> dayIndexes;

    public DaysOff(@NonNull String staffId, @NonNull List<Integer> dayIndexes) {
      this.staffId = staffId;
      this.dayIndexes = Collections.unmodifiableList(new ArrayList<Integer>(dayIndexes));
    }
  }

  @Value
  public static final class ShiftOnRequests {
    private String staffId;
    private int day;
    private String shiftId;
    private int weight;
  }

  @Value
  public static final class ShiftOffRequests {
    private String staffId;
    private int day;
    private String shiftId;
    private int weight;
  }

  @Value
  public static final class Cover {
    private int day;
    private String shiftId;
    private int requirement;
    private int weightUnder;
    private int weightOver;
  }

  public void encode(@NonNull Writer writer) {
    PrintWriter out = new PrintWriter(writer);

    out.println("# This is a comment. Comments start with #");
    out.println("SECTION_HORIZON");
    out.println("# All instances start on a Monday");
    out.println("# The horizon length in days:");
    out.println(getLength());
    out.println();

    out.println("SECTION_SHIFTS");
    out.println("# ShiftID, Length in mins, Shifts which cannot follow this shift | separated");
    for (Shift shift : getShifts().values()) {
      String id = shift.getId().toString();
      String length = String.valueOf(shift.getLength());
      String notFollow = String.join("|", shift.getNotFollow());
      out.println(String.join(",", id, length, notFollow));
    }
    out.println();

    out.println("SECTION_STAFF");
    out.println("# ID, MaxShifts, MaxTotalMinutes, MinTotalMinutes,"
        + " MaxConsecutiveShifts, MinConsecutiveShifts, MinConsecutiveDaysOff, MaxWeekends");
    for (Staff staff : getStaff().values()) {
      String id = staff.getId();

      List<String> maxShiftTerms = new ArrayList<>();
      for (String shift : staff.getMaxShifts().keySet()) {
        maxShiftTerms.add(format("%s=%d", shift, staff.getMaxShifts().get(shift)));
      }
      String maxShifts = String.join("|", maxShiftTerms);

      String maxTotalMinutes = String.valueOf(staff.getMaxTotalMinutes());
      String minTotalMinutes = String.valueOf(staff.getMinTotalMinutes());
      String maxConsecutiveShifts = String.valueOf(staff.getMaxConsecutiveShifts());
      String minConsecutiveShifts = String.valueOf(staff.getMinConsecutiveShifts());
      String minConsecutiveDayOff = String.valueOf(staff.getMinConsecutiveDayOff());
      String maxWeekends = String.valueOf(staff.getMaxWeekends());
      out.println(String.join(",", id, maxShifts, maxTotalMinutes, minTotalMinutes,
          maxConsecutiveShifts, minConsecutiveShifts, minConsecutiveDayOff, maxWeekends));
    }
    out.println();

    out.println("SECTION_DAYS_OFF");
    out.println("# EmployeeID, DayIndexes (start at zero)");
    for (DaysOff daysOff : getDaysOff().values()) {
      List<String> items = new ArrayList<>();
      items.add(daysOff.getStaffId());
      for (Integer day : daysOff.getDayIndexes()) {
        items.add(String.valueOf(day));
      }
      out.println(String.join(",", items));
    }
    out.println();

    out.println("SECTION_SHIFT_ON_REQUESTS");
    out.println("# EmployeeID, Day, ShiftID, Weight");
    for (ShiftOnRequests req : getShiftOnRequests()) {
      String staffId = req.getStaffId();
      String day = String.valueOf(req.getDay());
      String shiftId = req.getShiftId();
      String weight = String.valueOf(req.getWeight());
      out.println(String.join(",", staffId, day, shiftId, weight));
    }
    out.println();

    out.println("SECTION_SHIFT_OFF_REQUESTS");
    out.println("# EmployeeID, Day, ShiftID, Weight");
    for (ShiftOffRequests req : getShiftOffRequests()) {
      String staffId = req.getStaffId();
      String day = String.valueOf(req.getDay());
      String shiftId = req.getShiftId();
      String weight = String.valueOf(req.getWeight());
      out.println(String.join(",", staffId, day, shiftId, weight));
    }
    out.println();

    out.println("SECTION_COVER");
    out.println("# Day, ShiftID, Requirement, Weight for under, Weight for over");
    for (Cover cover : getCover()) {
      String day = String.valueOf(cover.getDay());
      String shiftId = cover.getShiftId();
      String requirement = String.valueOf(cover.getRequirement());
      String weightUnder = String.valueOf(cover.getWeightUnder());
      String weightOver = String.valueOf(cover.getWeightOver());
      out.println(String.join(",", day, shiftId, requirement, weightUnder, weightOver));
    }
    out.println();

    out.flush();
    out.close();
  }

  public static SchedulingProblem parse(Reader reader) {
    return new SchedulingProblemParser(reader).parse();
  }

  @Override
  public String toString() {
    StringWriter writer = new StringWriter();
    encode(writer);
    return writer.toString();
  }
}
