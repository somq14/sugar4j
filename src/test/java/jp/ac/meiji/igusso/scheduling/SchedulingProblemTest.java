package jp.ac.meiji.igusso.scheduling;

import java.io.FileReader;

import org.junit.Test;

public class SchedulingProblemTest {
  // @Test
  public void test() throws Exception {
    for (int i = 1; i <= 24; i++) {
      SchedulingProblem sp = SchedulingProblem.parse(new FileReader(String.format("instances/Instance%d.txt", i)));
      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println(sp);
    }
  }
}
