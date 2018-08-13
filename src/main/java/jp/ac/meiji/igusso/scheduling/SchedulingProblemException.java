package jp.ac.meiji.igusso.scheduling;

public class SchedulingProblemException extends RuntimeException {
  public SchedulingProblemException() {
    super();
  }

  public SchedulingProblemException(String message) {
    super(message);
  }

  public SchedulingProblemException(String message, Throwable cause) {
    super(message, cause);
  }

  public SchedulingProblemException(Throwable cause) {
    super(cause);
  }
}
