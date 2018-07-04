package jp.ac.meiji.igusso.coptool;

import java.util.regex.Pattern;

public interface Constraint {
  public static Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

  public String getName();

  public int getWeight();

  public boolean feasible(Model model);
}
