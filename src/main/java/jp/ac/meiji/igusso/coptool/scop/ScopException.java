package jp.ac.meiji.igusso.coptool.scop;

public class ScopException extends RuntimeException {
  public ScopException() {
    super();
  }

  public ScopException(String message) {
    super(message);
  }

  public ScopException(String message, Throwable cause) {
    super(message, cause);
  }

  public ScopException(Throwable cause) {
    super(cause);
  }
}
