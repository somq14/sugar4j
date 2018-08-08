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

  public int getLhsUpperBound() {
    int res = 0;
    for (LinearTerm term : terms) {
      int coeff = term.getCoeff();
      Domain domain = term.getVariable().getDomain();
      res += Math.max(coeff * domain.getLowerBound(), coeff * domain.getUpperBound());
    }
    return res;
  }

  public int getLhsLowerBound() {
    int res = 0;
    for (LinearTerm term : terms) {
      int coeff = term.getCoeff();
      Domain domain = term.getVariable().getDomain();
      res += Math.min(coeff * domain.getLowerBound(), coeff * domain.getUpperBound());
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
