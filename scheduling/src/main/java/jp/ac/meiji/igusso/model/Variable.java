package jp.ac.meiji.igusso.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

@ToString
@EqualsAndHashCode
public final class Variable implements Comparable<Variable> {
  public static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]\\w*$");

  @Getter private final String name;
  @Getter private final Domain domain;

  Variable(@NonNull String name, @NonNull Domain domain) {
    if (!Variable.NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("Invalid Variable Name: " + name);
    }

    this.name = name;
    this.domain = domain;
  }

  @Override
  public int compareTo(@NonNull Variable variable) {
    return name.compareTo(variable.getName());
  }

  public static Variable of(@NonNull String name, @NonNull Domain domain) {
    return new Variable(name, domain);
  }

  public static Variable of(@NonNull String name, int lowerBound, int upperBound) {
    return new Variable(name, Domain.of(lowerBound, upperBound));
  }

  public static Variable of(@NonNull String name, int size) {
    return new Variable(name, Domain.of(size));
  }
}
