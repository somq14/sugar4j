package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class ShiftGroup {
  @Getter private final String id;
  @Getter private final List<Shift> shifts;

  public ShiftGroup(@NonNull String id, @NonNull List<Shift> shifts) {
    this.id = id;
    this.shifts = Collections.unmodifiableList(new ArrayList<>(shifts));
  }

  @Override
  public String toString() {
    String body = String.join(", ", "id=" + id,
        "shifts=" + shifts.stream().map(it -> it.getId()).collect(Collectors.toList()));
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
