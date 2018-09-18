package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Skill {
  @Getter private final String id;
  @Getter private final String label;

  public Skill(@NonNull String id, @NonNull String label) {
    this.id = id;
    this.label = label;
  }
}
