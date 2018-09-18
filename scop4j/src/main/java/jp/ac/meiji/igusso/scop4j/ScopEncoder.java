package jp.ac.meiji.igusso.scop4j;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.io.Closeable;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

@ToString
@EqualsAndHashCode
public class ScopEncoder implements Closeable {
  private PrintWriter writer;

  public ScopEncoder(Writer writer) {
    this.writer = new PrintWriter(writer);
  }

  private static String encodeWeight(int weight) {
    return weight < 0 ? "inf" : String.valueOf(weight);
  }

  private static String encodeComparator(@NonNull Comparator op) {
    switch (op) {
      case LE:
        return "<=";
      case GE:
        return ">=";
      case EQ:
        return "=";
      default:
        throw new RuntimeException();
    }
  }

  public void encode(@NonNull Variable variable) {
    writer.printf(
        "variable %s in {%s}%n", variable.getName(), String.join(", ", variable.getDomain()));
  }

  public void encode(@NonNull LinearConstraint constraint) {
    List<String> terms = new ArrayList<>();

    int ind = 0;
    for (Term term : constraint) {
      if (ind % 5 == 0) {
        terms.add(String.format("%n    "));
      }
      terms.add(String.format(
          "%d(%s, %s)", term.getCoeff(), term.getVariable().getName(), term.getValue()));
      ind++;
    }

    writer.printf("%s: weight = %s type = linear %s %s %d%n", constraint.getName(),
        encodeWeight(constraint.getWeight()), String.join(" ", terms),
        encodeComparator(constraint.getOp()), constraint.getRhs());
  }

  public void encode(@NonNull QuadraticConstraint constraint) {
    throw new UnsupportedOperationException();
  }

  public void encode(@NonNull AllDifferentConstraint constraint) {
    List<String> vars = new ArrayList<>();
    for (Variable variable : constraint) {
      vars.add(variable.getName());
    }

    writer.printf("%s: weight = %s type = alldiff %s ;%n", constraint.getName(),
        encodeWeight(constraint.getWeight()), String.join(" ", vars));
  }

  public void encode(Scop4j scop4j) {
    for (Variable variable : scop4j.getVariables()) {
      encode(variable);
    }

    for (Constraint constraint : scop4j.getConstraints()) {
      if (constraint instanceof LinearConstraint) {
        encode((LinearConstraint) constraint);
      } else if (constraint instanceof QuadraticConstraint) {
        encode((QuadraticConstraint) constraint);
      } else if (constraint instanceof AllDifferentConstraint) {
        encode((AllDifferentConstraint) constraint);
      } else {
        throw new RuntimeException();
      }
    }
  }

  @Override
  public void close() {
    writer.close();
  }
}
