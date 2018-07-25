package jp.ac.meiji.igusso.coptool.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@ToString
@EqualsAndHashCode
public final class LinearConstraint extends AbstractConstraint implements Iterable<LinearTerm> {
  @Getter private final List<LinearTerm> terms;
  @Getter private final Comparator op;
  @Getter private final int rhs;

  private LinearConstraint(
      String name, List<LinearTerm> terms, Comparator op, int rhs, int weight) {
    super(name, weight);

    this.terms = Collections.unmodifiableList(new ArrayList<>(terms));
    this.op = op;
    this.rhs = rhs;
  }

  public int size() {
    return terms.size();
  }

  public LinearTerm get(int index) {
    return terms.get(index);
  }

  @Override
  public Iterator<LinearTerm> iterator() {
    return terms.iterator();
  }

  public static Builder of(@NonNull String name, @NonNull Comparator op, int rhs) {
    return new Builder(name, op, rhs, -1);
  }

  public static Builder of(@NonNull String name, @NonNull Comparator op, int rhs, int weight) {
    return new Builder(name, op, rhs, weight);
  }

  @ToString
  @EqualsAndHashCode
  public static class Builder {
    @Getter private String name;
    @Getter private final List<LinearTerm> terms = new ArrayList<>();
    @Getter private Comparator op;
    @Getter private int rhs;
    @Getter private int weight;

    private Builder(@NonNull String name, @NonNull Comparator op, int rhs, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException("Invalid Constraint Name: " + name);
      }

      this.name = name;
      this.op = op;
      this.rhs = rhs;
      this.weight = Math.max(-1, weight);
    }

    public Builder addTerm(@NonNull LinearTerm term) {
      terms.add(term);
      return this;
    }

    public Builder addTerm(int coeff, @NonNull Variable variable) {
      return addTerm(LinearTerm.of(coeff, variable));
    }

    public LinearConstraint build() {
      return new LinearConstraint(name, terms, op, rhs, weight);
    }
  }
}
