package jp.kobe_u.sugar;

import java.io.PrintWriter;
import jp.kobe_u.sugar.csp.CSP;

public interface OutputInterface {
  void setCSP(CSP csp);

  void setOut(PrintWriter out);

  void setFormat(String format) throws SugarException;

  void output() throws SugarException;
}
