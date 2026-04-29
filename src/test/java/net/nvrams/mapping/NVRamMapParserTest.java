package net.nvrams.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import net.nvrams.mapping.map.NVRamMapParser;

/**
 */
public class NVRamMapParserTest extends BaseParserTest {

  private NVRamMapParser parser = new NVRamMapParser("resources/maps");

  @Test
  public void testGetScores() throws IOException {
    String rom = "bcats_l5";
    File nvram = new File("nvrams", rom + ".nv");

    List<NVRamScore> scores = parser.parseNvRam(rom, nvram, Locale.ENGLISH, false);
    assertEquals(4, scores.size());
    for (NVRamScore sc : scores) {
      System.out.println(sc.toString());
    }
  }

  //-------------------------------------------------------
  // Errors :
  // barbwire, initials are wrongly parsed
  // rapidfir, no highscore defined in map
  // sfight2, review the maps
  // strlt_l1, only one highscores, that is correct when pinemhi generate 4 lines
  private final static List<String> ignoreList = Arrays.asList("barbwire.nv", 
      "acd_170.nv",         // offset in names
      "bguns_la.nv", 
      "rapidfir.nv", 
      "rescu911.nv", 
      "sfight2.nv", 
      "strlt_l1.nv",
      "twd_156.nv",          //=> need remapping of initials
      "vlcno_ax.nv"          // only one score in pinemhi, several in maps
    );

//, , vlcno_ax.nv

  @Test
  public void testAllFiles() throws Exception {
    String firstRom = null;

    List<String> failedList = doTestAllFiles(parser, firstRom, ignoreList);
    assertEquals(0, failedList.size(), "NVRam failed: " + failedList);
  }

  @Test
  public void testOneFile() throws Exception {
    String filename = "bop_l8.nv";

    int status = doTestOneFile(parser, filename);
    assertEquals(STATUS_SUCCESS, status);
  }

}
