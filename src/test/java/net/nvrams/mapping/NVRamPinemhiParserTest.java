package net.nvrams.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import net.nvrams.mapping.pinemhi.NVRamPinemhiParser;

/**
 */
public class NVRamPinemhiParserTest extends BaseParserTest {

  private NVRamPinemhiParser parser = new NVRamPinemhiParser("resources/pinemhi/");

  @Test
  public void testPinemhiExtract() throws IOException {
    String rom = "rs_l6";
    File nvram = new File("nvrams", rom + ".nv");

    List<String> lines = parser.executePINemHi(nvram);
    assertEquals(14, lines.size());
  }


  @Test
  public void testPinemhiScores() throws IOException {
    String rom = "bop_l7";
    File nvram = new File("nvrams", rom + ".nv");

    List<NVRamScore> scores = parser.parseNvRam(rom, nvram, Locale.ENGLISH, false);
    assertEquals(4, scores.size());
  }

  //-------------------------------------------------------

  private final static List<String> ignoreList = Arrays.asList(
      "afm_f20.nv",        // fail in pinemhi parsing
      "afm_f32.nv",       // fail in pinemhi parsing
      "bguns_la.nv",      // error in initials of HS#3
      "mtl_180hc.nv",     // fail in pinemhi parsing
      "kissc.nv",         // cf discussion with Tom, no reason to have a scale x10
      "rs_l6.nv",         // remove FixTitleScoreAdapter ?
      "sc_091.nv", 
      "sttng_l7.nv",      // see https://pinside.com/pinball/forum/topic/what-is-the-q-continuum-sttng
      "smanve_100.nv", 
      "trek_201.nv"          // score is truncated by pinemhi !
    );

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
