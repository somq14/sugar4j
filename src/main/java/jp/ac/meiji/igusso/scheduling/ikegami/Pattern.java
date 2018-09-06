package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode
@ToString
public class Pattern {
  @Getter private int startDay;
  @Getter private int startDayOfWeek;
  @Getter private List<PatternAtom> atoms;

  public Pattern(int startDay, int startDayOfWeek, List<PatternAtom> atoms) {
    this.startDay = startDay;
    this.startDayOfWeek = startDayOfWeek;
    this.atoms = Collections.unmodifiableList(new ArrayList<>(atoms));
  }
}
