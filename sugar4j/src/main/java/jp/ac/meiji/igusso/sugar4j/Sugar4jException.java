package jp.ac.meiji.igusso.sugar4j;

public class Sugar4jException extends RuntimeException {
  public Sugar4jException() {
    super();
  }

  public Sugar4jException(String message) {
    super(message);
  }

  public Sugar4jException(String message, Throwable cause) {
    super(message, cause);
  }

  public Sugar4jException(Throwable cause) {
    super(cause);
  }
}
