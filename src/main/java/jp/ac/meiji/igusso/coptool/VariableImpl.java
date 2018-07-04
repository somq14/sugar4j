package jp.ac.meiji.igusso.coptool;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
final class VariableImpl implements Variable {
  String name;
  List<Integer> domain;

  VariableImpl(@NonNull String name, @NonNull List<Integer> domain) {
    if (!Variable.NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("Invalid Variable Name: " + name);
    }
    this.name = name;
    this.domain = domain;
  }
}
