package jp.ac.meiji.igusso.coptool.example;

import jp.ac.meiji.igusso.coptool.AllDifferentConstraint;
import jp.ac.meiji.igusso.coptool.Comparator;
import jp.ac.meiji.igusso.coptool.Model;
import jp.ac.meiji.igusso.coptool.ModelEncoder;
import jp.ac.meiji.igusso.coptool.PseudoBooleanConstraint;
import jp.ac.meiji.igusso.coptool.ScopModelEncoder;
import jp.ac.meiji.igusso.coptool.SugarModelEncoder;
import jp.ac.meiji.igusso.coptool.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MagicSquare {
  private final int siz;

  public MagicSquare(int siz) {
    if (siz <= 0) {
      throw new IllegalArgumentException();
    }
    this.siz = siz;
  }

  public Model generate() {
    Model model = new Model();

    int sum = (siz * siz) * ((siz * siz) + 1) / 2 / siz;

    List<Integer> domain = new ArrayList<>();
    for (int i = 1; i <= siz * siz; i++) {
      domain.add(i);
    }

    Variable[][] var = new Variable[siz][siz];
    for (int i = 0; i < siz; i++) {
      for (int j = 0; j < siz; j++) {
        String varName = String.format("v_%02d_%02d", i, j);
        var[i][j] = model.addVariable(varName, domain);
      }
    }

    AllDifferentConstraint.Builder diffCons = AllDifferentConstraint.of("diff");
    for (int i = 0; i < siz; i++) {
      for (int j = 0; j < siz; j++) {
        diffCons.addVariable(var[i][j]);
      }
    }
    model.addConstraint(diffCons.build());

    for (int i = 0; i < siz; i++) {
      String consName = String.format("row%d", i);
      PseudoBooleanConstraint.Builder cons =
          PseudoBooleanConstraint.of(consName, Comparator.EQ, sum);
      for (int j = 0; j < siz; j++) {
        for (int k = 1; k <= siz * siz; k++) {
          cons.addTerm(k, var[i][j], k);
        }
      }
      model.addConstraint(cons.build());
    }

    for (int j = 0; j < siz; j++) {
      String consName = String.format("col%d", j);
      PseudoBooleanConstraint.Builder cons =
          PseudoBooleanConstraint.of(consName, Comparator.EQ, sum);
      for (int i = 0; i < siz; i++) {
        for (int k = 1; k <= siz * siz; k++) {
          cons.addTerm(k, var[i][j], k);
        }
      }
      model.addConstraint(cons.build());
    }

    PseudoBooleanConstraint.Builder crossCons1 =
        PseudoBooleanConstraint.of("cross1", Comparator.EQ, sum);
    for (int i = 0; i < siz; i++) {
      for (int k = 1; k <= siz * siz; k++) {
        crossCons1.addTerm(k, var[i][i], k);
      }
    }
    model.addConstraint(crossCons1.build());

    PseudoBooleanConstraint.Builder crossCons2 =
        PseudoBooleanConstraint.of("cross2", Comparator.EQ, sum);
    for (int i = 0; i < siz; i++) {
      for (int k = 1; k <= siz * siz; k++) {
        crossCons2.addTerm(k, var[i][siz - 1 - i], k);
      }
    }
    model.addConstraint(crossCons2.build());

    return model;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("MagicSquare SIZE [sugar|scop]");
      System.exit(1);
    }

    int siz = Integer.valueOf(args[0]);

    MagicSquare magicSquare = new MagicSquare(siz);
    Model model = magicSquare.generate();

    ModelEncoder encoder = null;
    if (args.length >= 2 && "sugar".equals(args[1])) {
      encoder = SugarModelEncoder.getInstance();
    } else if (args.length >= 2 && "scop".equals(args[1])) {
      encoder = ScopModelEncoder.getInstance();
    }

    List<String> body = encoder.encode(model);
    for (String line : body) {
      System.out.println(line);
    }
  }
}
