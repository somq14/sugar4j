package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode
public class PatternAtom {
  public enum Type { SHIFT, GROUP, NOT_SHIFT, NOT_GROUP, ANY, ANY_SHIFT, DAY_OFF }

  @Getter private Type type;
  @Getter private Shift shift;
  @Getter private ShiftGroup shiftGroup;

  public PatternAtom(@NonNull Type type, Shift shift, ShiftGroup shiftGroup) {
    this.type = type;
    this.shift = shift;
    this.shiftGroup = shiftGroup;
  }

  public PatternAtom(@NonNull Type type, ShiftGroup shiftGroup) {
    this(type, null, shiftGroup);
  }

  public PatternAtom(@NonNull Type type, Shift shift) {
    this(type, shift, null);
  }

  public PatternAtom(@NonNull Type type) {
    this(type, null, null);
  }

  @Override
  public String toString() {
    /*
    String body =
        String.join(", ", "type=" + type, "shift=" + (shift == null ? null : shift.getId()),
            "shiftGroup=" + (shiftGroup == null ? null : shiftGroup.getId()));
    StringBuilder res = new StringBuilder();
    res.append(getClass().getSimpleName()).append('(').append(body).append(')');
    return res.toString();
    */
    switch (type) {
      case SHIFT:
        return shift.getId();
      case GROUP:
        return shiftGroup.getId();
      case NOT_SHIFT:
        return "!" + shift.getId();
      case NOT_GROUP:
        return "!" + shiftGroup.getId();
      case ANY:
        return "*";
      case ANY_SHIFT:
        return "$";
      case DAY_OFF:
        return "-";
      default:
        throw new IllegalStateException();
    }
  }
}
