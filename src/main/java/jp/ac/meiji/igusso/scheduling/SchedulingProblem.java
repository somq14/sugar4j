package jp.ac.meiji.igusso.scheduling;

import static java.lang.String.format;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.io.Reader;
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

  @Override
  public String toString() {
    List<String> res = new ArrayList<>();

    res.add(format("SECTION_HORIZON"));
    res.add("" + length);
    res.add("");

    res.add(format("SECTION_SHIFTS"));
    for (String key : shifts.keySet()) {
      res.add(shifts.get(key).toString());
    }
    res.add("");

    res.add(format("SECTION_STAFF"));
    for (String key : staff.keySet()) {
      res.add(staff.get(key).toString());
    }
    res.add("");

    res.add(format("SECTION_DAYS_OFF"));
    for (String key : daysOff.keySet()) {
      res.add(daysOff.get(key).toString());
    }
    res.add("");

    res.add(format("SECTION_SHIFT_ON_REQUESTS"));
    for (ShiftOnRequests elem : shiftOnRequests) {
      res.add(elem.toString());
    }
    res.add("");

    res.add(format("SECTION_SHIFT_OFF_REQUESTS"));
    for (ShiftOffRequests elem : shiftOffRequests) {
      res.add(elem.toString());
    }
    res.add("");

    res.add(format("SECTION_SHIFT_OFF_REQUESTS"));
    for (Cover elem : cover) {
      res.add(elem.toString());
    }
    res.add("");

    return String.join(System.getProperty("line.separator"), res);
  }

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

  public static SchedulingProblem parse(Reader reader) {
    return new SchedulingProblemParser(reader).parse();
  }
}
