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
public final class ConflictPointConstraint
    extends AbstractConstraint implements Iterable<PredicateTerm> {
  @Getter private final List<PredicateTerm> terms;

  private ConflictPointConstraint(String name, List<PredicateTerm> terms, int weight) {
    super(name, weight);
    this.terms = Collections.unmodifiableList(new ArrayList<>(terms));
  }

  public int size() {
    return terms.size();
  }

  public PredicateTerm get(int index) {
    return terms.get(index);
  }

  @Override
  public int getPenaltyUpperBound() {
    return isHard() ? 0 : 1;
  }

  @Override
  public Iterator<PredicateTerm> iterator() {
    return terms.iterator();
  }

  public static Builder of(@NonNull String name) {
    return new Builder(name, -1);
  }

  public static Builder of(@NonNull String name, int weight) {
    return new Builder(name, weight);
  }

  @ToString
  @EqualsAndHashCode
  public static class Builder {
    @Getter private String name;
    @Getter private final List<PredicateTerm> terms = new ArrayList<>();
    @Getter private int weight;

    private Builder(@NonNull String name, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException("Invalid Constraint Name: " + name);
      }

      this.name = name;
      this.weight = Math.max(-1, weight);
    }

    public Builder addTerm(@NonNull PredicateTerm term) {
      terms.add(term);
      return this;
    }

    public Builder addTerm(@NonNull Variable variable, int value, boolean positive) {
      return addTerm(PredicateTerm.of(variable, value, positive));
    }

    public Builder addTerm(@NonNull Variable variable, int value) {
      return addTerm(variable, value, true);
    }

    public ConflictPointConstraint build() {
      return new ConflictPointConstraint(name, terms, weight);
    }
  }
}
