package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode
public class Cover {
  @Getter private int day;
  @Getter private int min;
  @Getter private int max;
  @Getter private Skill skill;
  @Getter private SkillGroup skillGroup;
  @Getter private Shift shift;
  @Getter private ShiftGroup shiftGroup;
  @Getter private String label;
  // TimePeriod is not supported
  // ShiftGroup is not supported
  // CoverResource is not supported

  public Cover(int day, int min, int max, Skill skill, SkillGroup skillGroup, Shift shift,
      ShiftGroup shiftGroup, String label) {
    this.day = day;
    this.min = min;
    this.max = max;
    this.skill = skill;
    this.skillGroup = skillGroup;
    this.shift = shift;
    this.shiftGroup = shiftGroup;
    this.label = label;
  }

  @Override
  public String toString() {
    String body = String.join(", ", "day=" + day, "min=" + min, "max=" + max,
        "skill=" + (skill == null ? null : skill.getId()),
        "skillGroup=" + (skillGroup == null ? null : skillGroup.getId()),
        "shift=" + (shift == null ? null : shift.getId()),
        "shiftGroup=" + (shiftGroup == null ? null : shiftGroup.getId()), "label=" + label);
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
  }
}
