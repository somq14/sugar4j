package jp.ac.meiji.igusso.coptool.scop;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;


@ToString
@EqualsAndHashCode
public class Variable implements Comparable<Variable> {
  public static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]\\w*$");
  public static final Pattern VALUE_PATTERN = Pattern.compile("^\\w+$");

  @Getter private final String name;
  @Getter private final List<String> domain;

  private Variable(@NonNull String name, @NonNull List<String> domain) {
    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException();
    }
    Set<String> domainSet = new TreeSet<>();
    for (String value : domain) {
      if (!VALUE_PATTERN.matcher(value).matches()) {
        throw new IllegalArgumentException();
      }
      if (domainSet.contains(value)) {
        throw new IllegalArgumentException();
      }
      domainSet.add(value);
    }
    this.name = name;
    this.domain = domain;
  }

  public static Variable of(String name, List<String> domain) {
    return new Variable(name, Collections.unmodifiableList(new ArrayList<>(domain)));
  }

  public static Variable of(String name, String... domain) {
    return new Variable(name, Collections.unmodifiableList(Arrays.asList(domain)));
  }

  public static Variable of(String name, int lowerBound, int upperBound) {
    if (lowerBound > upperBound) {
      throw new IllegalArgumentException();
    }
    List<String> domain = new ArrayList<>();
    for (int v = lowerBound; v <= upperBound; v++) {
      domain.add(String.valueOf(v));
    }
    return new Variable(name, Collections.unmodifiableList(domain));
  }

  public static Variable of(String name, int size) {
    return of(name, 0, size - 1);
  }

  @Override
  public int compareTo(@NonNull Variable varaible) {
    return this.name.compareTo(varaible.name);
  }
}
