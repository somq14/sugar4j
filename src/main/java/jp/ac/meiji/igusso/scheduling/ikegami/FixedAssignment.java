package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode
public class FixedAssignment {
  private final Staff staff;
  private final Shift shift; // null means DayOff
  private final int day;

  public FixedAssignment(@NonNull Staff staff, Shift shift, int day) {
    this.staff = staff;
    this.shift = shift;
    this.day = day;
  }

  @Override
  public String toString() {
    String body = String.join(", ", "staff=" + staff.getId(),
        "shift=" + (shift == null ? "-" : shift.getId()), "day=" + day);
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
