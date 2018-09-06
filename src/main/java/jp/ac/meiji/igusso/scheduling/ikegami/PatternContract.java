package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode
@ToString
public final class PatternContract {
  @Getter private int beginDay;
  @Getter private int endDay;
  @Getter private int min;
  @Getter private int minWeight;
  @Getter private String minLabel;
  @Getter private int max;
  @Getter private int maxWeight;
  @Getter private String maxLabel;
  @Getter private List<Pattern> patterns;
  // weight function are not supported
  // Starts are not supported
  // StartExcludes are not supported

  public PatternContract(int beginDay, int endDay, int min, int minWeight, String minLabel, int max,
      int maxWeight, String maxLabel, @NonNull List<Pattern> patterns) {
    this.beginDay = beginDay;
    this.endDay = endDay;
    this.min = min;
    this.minWeight = minWeight;
    this.minLabel = minLabel;
    this.max = max;
    this.maxWeight = maxWeight;
    this.maxLabel = maxLabel;
    this.patterns = Collections.unmodifiableList(new ArrayList<>(patterns));
  }
}
