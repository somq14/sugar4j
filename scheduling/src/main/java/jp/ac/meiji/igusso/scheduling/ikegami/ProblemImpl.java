package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString
@EqualsAndHashCode
final class ProblemImpl implements Problem {
  @Getter private long beginDate;
  @Getter private long endDate;
  @Getter private int length;
  @Getter private int overStaffingWeight;
  @Getter private int underStaffingWeight;
  @Getter private List<Staff> staffs;
  @Getter private List<Shift> shifts;
  @Getter private List<ShiftGroup> shiftGroups;
  @Getter private List<Skill> skills;
  @Getter private List<SkillGroup> skillGroups;
  @Getter private List<Contract> contracts;
  @Getter private List<Cover> covers;
  @Getter private List<DayOffRequest> dayOffRequests;
  @Getter private List<ShiftOffRequest> shiftOffRequests;
  @Getter private List<ShiftOnRequest> shiftOnRequests;
  @Getter private List<FixedAssignment> fixedAssignments;

  ProblemImpl(long beginDate, long endDate, int length, int overStaffingWeight,
      int underStaffingWeight, @NonNull List<Staff> staffs, @NonNull List<Shift> shifts,
      @NonNull List<ShiftGroup> shiftGroups, @NonNull List<Skill> skills,
      @NonNull List<SkillGroup> skillGroups, @NonNull List<Contract> contracts,
      @NonNull List<Cover> covers, @NonNull List<DayOffRequest> dayOffRequests,
      @NonNull List<ShiftOffRequest> shiftOffRequests,
      @NonNull List<ShiftOnRequest> shiftOnRequests,
      @NonNull List<FixedAssignment> fixedAssignments) {
    this.beginDate = beginDate;
    this.endDate = endDate;
    this.length = length;
    this.overStaffingWeight = overStaffingWeight;
    this.underStaffingWeight = underStaffingWeight;
    this.staffs = Collections.unmodifiableList(new ArrayList<>(staffs));
    this.shifts = Collections.unmodifiableList(new ArrayList<>(shifts));
    this.shiftGroups = Collections.unmodifiableList(new ArrayList<>(shiftGroups));
    this.skills = Collections.unmodifiableList(new ArrayList<>(skills));
    this.skillGroups = Collections.unmodifiableList(new ArrayList<>(skillGroups));
    this.contracts = Collections.unmodifiableList(new ArrayList<>(contracts));
    this.covers = Collections.unmodifiableList(new ArrayList<>(covers));
    this.dayOffRequests = Collections.unmodifiableList(new ArrayList<>(dayOffRequests));
    this.shiftOffRequests = Collections.unmodifiableList(new ArrayList<>(shiftOffRequests));
    this.shiftOnRequests = Collections.unmodifiableList(new ArrayList<>(shiftOnRequests));
    this.fixedAssignments = Collections.unmodifiableList(new ArrayList<>(fixedAssignments));
  }
}
