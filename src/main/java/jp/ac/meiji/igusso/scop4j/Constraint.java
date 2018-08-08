package jp.ac.meiji.igusso.scop4j;

import java.util.regex.Pattern;

public interface Constraint extends Comparable<Constraint> {
  public static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

  public String getName();

  public boolean isHard();

  public boolean isSoft();

  public int getWeight();
}
