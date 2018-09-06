package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class ShiftOnRequest {
  @Getter private final Staff staff;
  @Getter private final Shift shift;
  @Getter private final ShiftGroup shiftGroup;
  @Getter private final int day;
  @Getter private final int weight;

  public ShiftOnRequest(
      @NonNull Staff staff, Shift shift, ShiftGroup shiftGroup, int day, int weight) {
    this.staff = staff;
    this.shift = shift;
    this.shiftGroup = shiftGroup;
    this.day = day;
    this.weight = weight;
  }

  @Override
  public String toString() {
    String body = String.join(", ", "staff=" + staff.getId(),
        "shift=" + (shift == null ? null : shift.getId()),
        "shiftGroup=" + (shiftGroup == null ? null : shiftGroup.getId()), "day=" + day,
        "weight=" + weight);
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
