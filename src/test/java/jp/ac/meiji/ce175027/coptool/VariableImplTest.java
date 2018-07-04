package jp.ac.meiji.ce175027.coptool;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class VariableImplTest {
  @Test
  public void testGetName() throws Exception {
    VariableImpl v = new VariableImpl("v1_2", asList(1, 2, 3));
    assertThat(v.getName(), is("v1_2"));
  }

  @Test
  public void testGetDomain() throws Exception {
    VariableImpl v = new VariableImpl("v1_2", asList(1, 2, 3));
    assertThat(v.getDomain(), is(asList(1, 2, 3)));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testModifyDomain() throws Exception {
    VariableImpl v = new VariableImpl("v1_2", asList(1, 2, 3));
    v.getDomain().add(2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWrongName() throws Exception {
    VariableImpl v = new VariableImpl("_v1_2", asList(1, 2, 3));
  }
}
