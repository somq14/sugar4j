package jp.ac.meiji.igusso.scheduling;

import java.io.FileReader;

import jp.ac.meiji.igusso.coptool.Model;
import jp.ac.meiji.igusso.coptool.ModelEncoder;
import jp.ac.meiji.igusso.coptool.ScopModelEncoder;

import org.junit.Test;

public class SchedulingProblemEncoderTest {
  // @Test
  public void test() throws Exception {
    SchedulingProblem sp = SchedulingProblem.parse(new FileReader("instances/Instance1.txt"));
    SchedulingProblemEncoder encoder = new SchedulingProblemEncoder(sp);
    Model model = encoder.encode();
    ModelEncoder modelEncoder = ScopModelEncoder.getInstance();

    for (String line : modelEncoder.encode(model)) {
      System.out.println(line);
    }
  }
}
