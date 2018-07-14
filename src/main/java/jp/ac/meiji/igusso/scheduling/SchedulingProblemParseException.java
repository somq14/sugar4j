package jp.ac.meiji.igusso.scheduling;

public class SchedulingProblemParseException extends RuntimeException {
  public SchedulingProblemParseException() {
    super();
  }

  public SchedulingProblemParseException(String message) {
    super(message);
  }

  public SchedulingProblemParseException(String message, Throwable cause) {
    super(message, cause);
  }

  public SchedulingProblemParseException(Throwable cause) {
    super(cause);
  }
}
