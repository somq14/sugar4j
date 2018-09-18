package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class DayOffRequest {
  @Getter private final Staff staff;
  @Getter private final int day;
  @Getter private final int weight;

  public DayOffRequest(@NonNull Staff staff, int day, int weight) {
    this.staff = staff;
    this.day = day;
    this.weight = weight;
  }

  @Override
  public String toString() {
    String body = String.join(", ", "staff=" + staff.getId(), "day=" + day, "weight=" + weight);
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
