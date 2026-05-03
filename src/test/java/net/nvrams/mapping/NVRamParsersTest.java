package net.nvrams.mapping;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.nvrams.mapping.map.NVRamMapParser;
import net.nvrams.mapping.superhac.NVRamSuperhacParser;

/**
 * A test of NVRamParsers
 * It takes a NVRam, parse scores directly, generate raw, parse scores from raw and compare
 * 
 */
class NVRamParsersTest {

  private NVRamParser mapParser = new NVRamMapParser("resources/maps");
  private NVRamParser superhacParser = new NVRamSuperhacParser("resources/superhac/roms.json");

  // not needed to test pinemhi parser as parseNvRam() is equivalent to getRaw() + parseRaw()
  //private NVRamParser pinemhiParser = new NVRamPinemhiParser("resources/pinemhi/");

  @Test
  public void compareNVsWithSuperhacParser() throws Exception {
    doCompareNVs(superhacParser, new String[] {
      "gi_l9"
    });
  }

  @Test
  public void compareNVsWithMapParser() throws Exception {
    doCompareNVs(mapParser, new String[] {
      "andretti", "barbwire", "rescu911"
    });
  }

  private void doCompareNVs(NVRamParser parser, String[] ignoreRoms) throws Exception {

    Locale loc = Locale.ENGLISH;

    List<String> errors = new ArrayList<>();
    List<String> ignoredRomsInError = new ArrayList<>();
    // used in manual testing to skip first roms, should be null normally 
    String firstRom = null;
    boolean parseAll = false;

    File testFolder = new File("nvrams");
    for (File entry : testFolder.listFiles()) {
      String rom = entry.getName().replace(".nv", "").toLowerCase();

      if ((firstRom==null || firstRom.compareTo(rom) <= 0) && parser.isSupportedRom(rom)) {

        List<NVRamScore> scores1 = parser.parseNvRam(rom, entry, loc, parseAll);

        List<String> raw = parser.getRaw(rom, entry, loc);
        List<NVRamScore> scores2 = parser.parseRaw(rom, raw, loc, parseAll);

        if (!checkScores(rom, raw, scores1, scores2, false)) {
          if (ArrayUtils.contains(ignoreRoms, rom)) {
            ignoredRomsInError.add(rom);
          }
          else {
            errors.add(rom);
          }
        }
      }
    }
    System.out.println("the following ignored roms are still in error : "+ String.join(", ", ignoredRomsInError));
    assertEquals(0, errors.size(), "roms in error: " + String.join(", ", errors));
  }

  @Test
  public void compareNV() throws Exception {

    NVRamParser parser =  mapParser;

    String rom = "gi_l9";
    boolean parseAll = false;
    /*

    //----------------------------

    SUPERHAC PARSER ISSUES

    gi_l9 => decalage ?
    gnr_300
    jupk_513
    lw3_208

    jd_17

    sttng_l7 => buyin flag qui desactive les autres scores
    stwr_a14 => ne prend que les 4 premiers scores car des sections différentes, utilise 1 section et des labels ? 
    trek_201 => decalage offset sur names
    trn_174h => decalage sur names + scores
    twd_160h => decalage sur names + scores
    tf_180 => complex, autobot and decepticon conflict
    twst_405 => decalage
    viprsega => decalage
    viper => score sur 9 vs 6 sur pinemhi
    waterwld, wcsoccer, whirl_l3, wipeout => errro when parsing scores, possiblement un decalage de +1 sur name offsets and score offsets

    => group HIGH SCORE as one section :
    acd_170h, avr_200,  avs_170, bbh_170, bdk_294,fs_lx5 
    */

    File testFolder = new File("nvrams");
    File entry = new File(testFolder, rom + ".nv");

    Locale loc = Locale.ENGLISH;

    // Get scores from the map and nv directly (except pinemhi, does not use raw string)
    List<NVRamScore> scores1 = parser.parseNvRam(rom, entry, loc, parseAll);

    // generate the raw version of scores
    List<String> raw = parser.getRaw(rom, entry, loc);
    System.out.println(String.join("\n", raw));

    // parse raw with the parser using the map
    List<NVRamScore> scores2 = parser.parseRaw(rom, raw, loc, parseAll);
    checkScores(rom, raw, scores1, scores2, true);
  }

  private boolean checkScores(String rom, List<String> raw, List<NVRamScore> parsedScores, List<NVRamScore> scores, boolean useAssert) {
    System.out.println("Checking "+ rom);

    if (parsedScores.size() == scores.size()) {
      for (int i = 0; i < parsedScores.size(); i++) {
        NVRamScore parsedScore = parsedScores.get(i);
        NVRamScore score = scores.get(i);
        if (score==null) {
          System.out.println(String.join("\n", raw));
          if (useAssert) {
            assertNotNull(score);
          }
          else {
            System.out.println("==> score is null");
            return false;
          }
        }
        else if (!score.equals(parsedScore)) {
          System.out.println(String.join("\n", raw));
          if (useAssert) {
            assertEquals(parsedScore, score);
          }
          else {
            System.out.println("==> " + parsedScore + " != " + score);
            return false;
          }
        }
      }
    }
    else {
      System.out.println("Checking "+ rom);
      System.out.println(String.join("\n", raw));
      if (useAssert) {
        assertEquals(parsedScores.size(), scores.size());
      }
      System.out.println("==> " + parsedScores.size() + " != " + scores.size());
      return false;
    }

    return true;
  }
}
