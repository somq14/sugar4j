package jp.ac.meiji.igusso.model;

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
public final class PseudoBooleanConstraint
    extends AbstractConstraint implements Iterable<PseudoBooleanTerm> {
  @Getter private final List<PseudoBooleanTerm> terms;
  @Getter private final Comparator op;
  @Getter private final int rhs;

  private PseudoBooleanConstraint(@NonNull String name, @NonNull List<PseudoBooleanTerm> terms,
      @NonNull Comparator op, int rhs, int weight) {
    super(name, weight);

    this.terms = Collections.unmodifiableList(new ArrayList<>(terms));
    this.op = op;
    this.rhs = rhs;
  }

  public int size() {
    return terms.size();
  }

  public PseudoBooleanTerm get(int index) {
    return terms.get(index);
  }

  public int getLhsUpperBound() {
    int res = 0;
    for (PseudoBooleanTerm term : terms) {
      int coeff = term.getCoeff();
      if (coeff > 0) {
        res += coeff;
      }
    }
    return res;
  }

  public int getLhsLowerBound() {
    int res = 0;
    for (PseudoBooleanTerm term : terms) {
      int coeff = term.getCoeff();
      if (coeff < 0) {
        res += coeff;
      }
    }
    return res;
  }

  @Override
  public int getPenaltyUpperBound() {
    if (isHard()) {
      return 0;
    }

    int res = 0;
    switch (op) {
      case LE: {
        res = Math.max(getLhsUpperBound() - rhs, 0);
      } break;
      case LT: {
        res = Math.max(getLhsUpperBound() - rhs + 1, 0);
      } break;
      case GE: {
        res = Math.max(rhs - getLhsLowerBound(), 0);
      } break;
      case GT: {
        res = Math.max(rhs - getLhsLowerBound() + 1, 0);
      } break;
      case EQ: {
        int pena1 = Math.max(getLhsUpperBound() - rhs, 0);
        int pena2 = Math.max(rhs - getLhsLowerBound(), 0);
        res = Math.max(pena1, pena2);
      } break;
      default:
        throw new RuntimeException();
    }
    return res;
  }

  @Override
  public Iterator<PseudoBooleanTerm> iterator() {
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
    @Getter private final List<PseudoBooleanTerm> terms = new ArrayList<>();
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

    public Builder addTerm(PseudoBooleanTerm term) {
      terms.add(term);
      return this;
    }

    public Builder addTerm(int coeff, @NonNull Variable variable, int value) {
      return addTerm(PseudoBooleanTerm.of(coeff, variable, value));
    }

    public PseudoBooleanConstraint build() {
      return new PseudoBooleanConstraint(name, terms, op, rhs, weight);
    }
  }
}
