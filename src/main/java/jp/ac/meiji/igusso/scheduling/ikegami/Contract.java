package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode
public final class Contract {
  @Getter private String id;
  @Getter private String label;
  @Getter private List<PatternContract> patternContracts;

  public Contract(
      @NonNull String id, @NonNull String label, @NonNull List<PatternContract> patternContracts) {
    this.id = id;
    this.label = label;
    this.patternContracts = Collections.unmodifiableList(new ArrayList<>(patternContracts));
  }

  @Override
  public String toString() {
    String body =
        String.join(", ", "id=" + id, "label=" + label, "pattenContracts=" + patternContracts);
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
