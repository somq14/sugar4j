package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * quadratic制約を表すクラス.
 */
@ToString
@EqualsAndHashCode
public final class QuadraticConstraint
    extends AbstractConstraint implements Iterable<QuadraticTerm> {
  @Getter private final List<QuadraticTerm> terms;
  @Getter private final Comparator op;
  @Getter private final int rhs;

  private QuadraticConstraint(@NonNull String name, int weight, @NonNull List<QuadraticTerm> terms,
      @NonNull Comparator op, @NonNull int rhs) {
    super(name, weight);

    this.terms = Collections.unmodifiableList(new ArrayList<>(terms));
    this.op = op;
    this.rhs = rhs;
  }

  public int size() {
    return terms.size();
  }

  public QuadraticTerm get(int index) {
    return terms.get(index);
  }

  @Override
  public Iterator<QuadraticTerm> iterator() {
    return terms.iterator();
  }

  public static Builder of(String name, Comparator op, int rhs) {
    return of(name, op, rhs, -1);
  }

  public static Builder of(String name, Comparator op, int rhs, int weight) {
    return new Builder(name, op, rhs, weight);
  }

  @EqualsAndHashCode
  @ToString
  public static final class Builder {
    @Getter @Setter private String name;
    @Getter @Setter private Comparator op;
    @Getter @Setter private int rhs;
    @Getter @Setter private int weight;
    @Getter @Setter private List<QuadraticTerm> terms = new ArrayList<>();

    private Builder(@NonNull String name, @NonNull Comparator op, int rhs, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException();
      }
      this.name = name;
      this.op = op;
      this.rhs = rhs;
      this.weight = Math.max(weight, -1);
    }

    public Builder addTerm(@NonNull QuadraticTerm term) {
      terms.add(term);
      return this;
    }

    public Builder addTerm(
        int coeff, Variable variable1, String value1, Variable variable2, String value2) {
      return addTerm(QuadraticTerm.of(coeff, variable1, value1, variable2, value2));
    }

    public Builder addTerm(
        int coeff, Variable variable1, Integer value1, Variable variable2, Integer value2) {
      return addTerm(QuadraticTerm.of(coeff, variable1, value1, variable2, value2));
    }

    public QuadraticConstraint build() {
      return new QuadraticConstraint(name, weight, terms, op, rhs);
    }
  }
}
