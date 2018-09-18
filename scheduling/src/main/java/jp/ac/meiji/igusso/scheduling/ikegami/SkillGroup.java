package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class SkillGroup {
  @Getter private final String id;
  @Getter private final List<Skill> skills;

  public SkillGroup(@NonNull String id, @NonNull List<Skill> skills) {
    this.id = id;
    this.skills = Collections.unmodifiableList(new ArrayList<>(skills));
  }

  @Override
  public String toString() {
    String body = String.join(", ", "id=" + id,
        "skills=" + skills.stream().map(it -> it.getId()).collect(Collectors.toList()));
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
