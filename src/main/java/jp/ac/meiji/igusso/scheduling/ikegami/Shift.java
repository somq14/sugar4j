package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Shift {
  @Getter private final String id;
  @Getter private final String label;
  @Getter private final String color;
  @Getter private final String name;
  @Getter private final long beginTime;
  @Getter private final long endTime;
  @Getter private final boolean autoAllocate;
  // TimeUnits will be ignored
  // TimePeriods will be ignored
  // Resources will be ignored

  public Shift(@NonNull String id, @NonNull String label, @NonNull String color,
      @NonNull String name, long beginTime, long endTime, boolean autoAllocate) {
    this.id = id;
    this.label = label;
    this.color = color;
    this.name = name;
    this.beginTime = beginTime;
    this.endTime = endTime;
    this.autoAllocate = autoAllocate;
  }
}
