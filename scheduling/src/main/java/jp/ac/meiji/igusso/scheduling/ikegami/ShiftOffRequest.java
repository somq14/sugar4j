package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class ShiftOffRequest {
  @Getter private final Staff staff;
  @Getter private final Shift shift;
  @Getter private final int day;
  @Getter private final int weight;

  public ShiftOffRequest(@NonNull Staff staff, @NonNull Shift shift, int day, int weight) {
    this.staff = staff;
    this.shift = shift;
    this.day = day;
    this.weight = weight;
  }

  @Override
  public String toString() {
    String body = String.join(
        ", ", "staff=" + staff.getId(), "shift=" + shift.getId(), "day=" + day, "weight=" + weight);
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
