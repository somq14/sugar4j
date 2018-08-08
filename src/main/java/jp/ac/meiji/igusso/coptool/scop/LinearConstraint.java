package jp.ac.meiji.igusso.coptool.scop;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@ToString
@EqualsAndHashCode
public final class LinearConstraint extends AbstractConstraint implements Iterable<Term> {
  @Getter private final List<Term> terms;
  @Getter private final Comparator op;
  @Getter private final int rhs;

  private LinearConstraint(@NonNull String name, int weight, @NonNull List<Term> terms,
      @NonNull Comparator op, int rhs) {
    super(name, weight);
    this.terms = Collections.unmodifiableList(new ArrayList<>(terms));
    this.op = op;
    this.rhs = rhs;
  }

  public int size() {
    return terms.size();
  }

  public Term get(int index) {
    return terms.get(index);
  }

  @Override
  public Iterator<Term> iterator() {
    return terms.iterator();
  }

  public static Builder of(@NonNull String name, @NonNull Comparator op, int rhs) {
    return of(name, op, rhs, -1);
  }

  public static Builder of(@NonNull String name, @NonNull Comparator op, int rhs, int weight) {
    return new Builder(name, op, rhs, weight);
  }

  public static final class Builder {
    @Getter @Setter private String name;
    @Getter @Setter private Comparator op;
    @Getter @Setter private int rhs;
    @Getter @Setter private int weight;
    @Getter @Setter private List<Term> terms = new ArrayList<>();

    private Builder(@NonNull String name, @NonNull Comparator op, int rhs, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException();
      }
      this.name = name;
      this.op = op;
      this.rhs = rhs;
      this.weight = Math.max(weight, -1);
    }

    public Builder addTerm(@NonNull Term term) {
      terms.add(term);
      return this;
    }

    public Builder addTerm(int coeff, Variable variable, String value) {
      return addTerm(Term.of(coeff, variable, value));
    }

    public Builder addTerm(int coeff, Variable variable, Integer value) {
      return addTerm(Term.of(coeff, variable, value));
    }

    public LinearConstraint build() {
      return new LinearConstraint(name, weight, terms, op, rhs);
    }
  }
}
