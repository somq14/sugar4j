package jp.ac.meiji.igusso.coptool.model;

import java.util.List;
import java.util.regex.Pattern;

public interface Variable {
  public static Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

  public String getName();

  public List<Integer> getDomain();
}
