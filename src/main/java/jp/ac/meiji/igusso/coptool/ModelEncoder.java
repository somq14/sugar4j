package jp.ac.meiji.igusso.coptool;

import java.util.List;

public interface ModelEncoder {
  public List<String> encode(Variable variable);

  public List<String> encode(ConflictPointConstraint constraint);

  public List<String> encode(LinearConstraint constraint);

  public List<String> encode(PseudoBooleanConstraint constraint);

  public List<String> encode(AllDifferentConstraint constraint);

  public List<String> encode(Model model);
}

