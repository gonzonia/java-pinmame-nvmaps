package net.nvrams.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 */
public class RawScoreParserTest {

  @Test
  public void testParsing() {
    doTest("#1", "JOE            250.000.000", 1, "JOE", 250000000);
    doTest("#3", "IND        30.000.000", 3, "IND", 30000000);
    doTest("#3", "I N        30.000.000", 3, "I N", 30000000);
    doTest("#10", "MT             110.000.000", 10, "MT", 110000000);

    doTest("1)", "RA        161.000", 1, "RA", 161000);
    doTest("2)", "P G     1.610.000", 2, "P G", 1610000);
    doTest("3)", "X      16.100.000", 3, "X", 16100000);
    doTest("4)", "TEX   161.000.000", 4, "TEX", 161000000);

    doTest("1)", "TEX 16", 1, "TEX", 16);
    doTest("1#", "DAD   267", 1, "DAD", 267);

    doTest("1)", "4.000.000", 1, "", 4000000);

    doTest("#1", "???   1.000.000", 1, "???", 1000000);

    //doTest(adapter, "2)", "P G", 2, "P G", 0);
    //doTest(adapter, "3#", "TEX", 3, "TEX", 0);
  }

  private void doTest(String posInput, String input, int rank, String initials, long score) {
    String[] seps = { ".", ",", "\ufffd", ""};
    for (String sep : seps) {
      String line = (posInput + " " + input).replace(".", sep);

      assertTrue(RawScoreParser.isScoreLine(line));
      NVRamScore s = RawScoreParser.createScore("TEST", line);
      assertEquals(initials, s.getInitials());
      assertEquals(score, s.getScore());
      assertEquals(rank, s.getPosition());
    }

    for (String sep : seps) {
      String line = (input).replace(".", sep);
      assertTrue(RawScoreParser.isTitleScoreLine(line));
      NVRamScore s = RawScoreParser.createTitledScore("TEST", line);
      assertEquals(initials, s.getInitials());
      assertEquals(score, s.getScore());
    }
  }
}
