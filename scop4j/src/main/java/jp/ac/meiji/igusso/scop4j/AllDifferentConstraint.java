package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@ToString
@EqualsAndHashCode
public final class AllDifferentConstraint extends AbstractConstraint implements Iterable<Variable> {
  @Getter private final List<Variable> variables;

  private AllDifferentConstraint(
      @NonNull String name, int weight, @NonNull List<Variable> variables) {
    super(name, weight);
    this.variables = Collections.unmodifiableList(new ArrayList<>(variables));
  }

  public int size() {
    return variables.size();
  }

  public Variable get(int index) {
    return variables.get(index);
  }

  @Override
  public Iterator<Variable> iterator() {
    return variables.iterator();
  }

  public static Builder of(@NonNull String name) {
    return of(name, -1);
  }

  public static Builder of(@NonNull String name, int weight) {
    return new Builder(name, weight);
  }

  public static final class Builder {
    @Getter @Setter private String name;
    @Getter @Setter private int weight;
    @Getter @Setter private List<Variable> variables = new ArrayList<>();

    private Builder(@NonNull String name, int weight) {
      if (!Constraint.NAME_PATTERN.matcher(name).matches()) {
        throw new IllegalArgumentException();
      }
      this.name = name;
      this.weight = Math.max(weight, -1);
    }

    public Builder addVariable(Variable variable) {
      variables.add(variable);
      return this;
    }

    public Builder addVariables(Variable... variables) {
      for (Variable variable : variables) {
        addVariable(variable);
      }
      return this;
    }

    public Builder addVariables(Collection<Variable> variables) {
      for (Variable variable : variables) {
        addVariable(variable);
      }
      return this;
    }

    public AllDifferentConstraint build() {
      return new AllDifferentConstraint(name, weight, variables);
    }
  }
}
