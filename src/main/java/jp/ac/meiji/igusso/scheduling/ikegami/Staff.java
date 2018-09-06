package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class Staff {
  @Getter private final String id;
  @Getter private final String name;
  @Getter private final List<Skill> skills;
  @Getter private final List<Contract> contracts;
  // CoverResources tag is not supported

  public Staff(@NonNull String id, @NonNull String name, @NonNull List<Skill> skills,
      @NonNull List<Contract> contracts) {
    this.id = id;
    this.name = name;
    this.skills = Collections.unmodifiableList(new ArrayList<>(skills));
    this.contracts = Collections.unmodifiableList(new ArrayList<>(contracts));
  }

  @Override
  public String toString() {
    String body = String.join(
        ", ", "id=" + id,
        "skills=" + skills.stream().map(it -> it.getId()).collect(Collectors.toList()));
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
