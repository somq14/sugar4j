package jp.ac.meiji.igusso.scheduling.ikegami;

import java.util.List;

public interface Problem {
  public long getBeginDate();

  public long getEndDate();

  public int getLength();

  public int getOverStaffingWeight();

  public int getUnderStaffingWeight();

  public List<Staff> getStaffs();

  public List<Shift> getShifts();

  public List<ShiftGroup> getShiftGroups();

  public List<Skill> getSkills();

  public List<SkillGroup> getSkillGroups();

  public List<Contract> getContracts();

  public List<Cover> getCovers();

  public List<DayOffRequest> getDayOffRequests();

  public List<ShiftOffRequest> getShiftOffRequests();

  public List<ShiftOnRequest> getShiftOnRequests();

  public List<FixedAssignment> getFixedAssignments();
}
