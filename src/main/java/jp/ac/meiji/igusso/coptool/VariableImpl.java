package jp.ac.meiji.igusso.coptool;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@ToString
@EqualsAndHashCode
final class VariableImpl implements Variable {
  private final String name;
  private final List<Integer> domain;

  VariableImpl(@NonNull String name, @NonNull List<Integer> domain) {
    if (!Variable.NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("Invalid Variable Name: " + name);
    }
    if (domain.size() != new HashSet<>(domain).size()) {
      throw new IllegalArgumentException("Duplicated Values In Domain: " + domain);
    }

    this.name = name;
    this.domain = Collections.unmodifiableList(new ArrayList<>(domain));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Integer> getDomain() {
    return domain;
  }
}
