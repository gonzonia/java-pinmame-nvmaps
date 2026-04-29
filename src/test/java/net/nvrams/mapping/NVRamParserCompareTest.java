package net.nvrams.mapping;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.nvrams.mapping.map.NVRamMapParser;
import net.nvrams.mapping.pinemhi.NVRamPinemhiParser;
import net.nvrams.mapping.superhac.NVRamSuperhacParser;

/**
 * A test that compare scores parsed by pinemhi with scores form another parser
 */
class NVRamParserCompareTest {

  private NVRamParser pinemhi = new NVRamPinemhiParser("resources/pinemhi/");
  private NVRamParser mapParser = new NVRamMapParser("resources/maps");
  private NVRamParser superhacParser = new NVRamSuperhacParser("resources/superhac/roms.json");

  @Test
  public void compareNVsWithMapParser() throws Exception {
    doCompareNVs(mapParser, 
        new String[] {
          "bguns_la", "rs_l6", "sc_091", "strlt_l1", "sttng_l7", "trek_201",
          //needs updates in maps :
          "acd_170", "andretti", "barbwire", "kissc", "rescu911", "sfight2" , "smanve_100", "twd_156", "vlcno_ax"
        }, new String[] {
          "abv106", "bsv103", "freddy", "gladiatr", "tf_180"
        });
  }

  @Test
  public void compareNVsWithSuperhacParser() throws Exception {
    doCompareNVs( superhacParser, new String[] {
      "bop_l7", "dollyptb", "gi_l9", "nbaf_31", "pool_l7", "punchy", "rs_l6", "strlt_l1", "sttng_l7", 
      "stwr_a14", "tagteam", "tf_180", "trek_201", "wwfr_103"
    }, new String[] {});
  }

  @Test
  public void compareNV() throws Exception {

    NVRamParser parser = mapParser;
    String rom = "whirl_l3";
    boolean ignorePosition = false;
    boolean parseAll = false;


    /*
    FIXME NVRamMapParser These roms need to be fixed

      bop_l7, bop_l8, freddy, rs_l6, sc_091, strlt_l1, sttng_l7, tf_180, trek_201


    FIXME NVRamSUperhacParser These roms need to be fixed  
    strlt_l1
    ts_lx5
    viprsega

    FIXME NVRamSUperhacParser fix map
    dollyptb  => "decoder": "single_high_nibble_score_x10"
    jd_l7 => improve pinhemi, problem with REGULAR GAME / HIGH SCORE ON 2 lines
    mm_10    => decoder: bdc  pour TROLL CHAMPION et les autres
    monopoly => specific name_decoder so suffix is in initials when it should be separated, review design + map
    nbaf_31 => very complex ^^
    stingray => decalage de 1 dans offsets
    phnix_l1, scrpn_l1 => à revoir
    cv_20h => find a way to skip CANNON BALL CHAMPION
    fs_lx5 => find a way to skip TOP BOWLER
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
    doCompare(rom, entry, parser, parseAll, true, ignorePosition, loc);
  }

  //---------------------

  public void doCompareNVs(NVRamParser parser, String[] ignoreRoms, String[] ignorePositions) throws Exception {

    // used in manual testing to skip first roms, should be null normally 
    String firstRom = null;
    boolean parseAll = false;

    Locale loc = Locale.ENGLISH;
    List<String> errors = new ArrayList<>();
    File testFolder = new File("nvrams");
    for (File entry : testFolder.listFiles()) {
      String rom = entry.getName().replace(".nv", "").toLowerCase();

      if ((firstRom==null || firstRom.compareTo(rom) <= 0) && pinemhi.isSupportedRom(rom) && parser.isSupportedRom(rom)) {
        if (!doCompare(rom, entry, parser, parseAll, false, false, loc)) {
          if (!doCompare(rom, entry, parser, parseAll, false, true, loc)) {
            if (!ArrayUtils.contains(ignoreRoms, rom)) {
              errors.add(rom);
            }
          }
          else if (!ArrayUtils.contains(ignorePositions, rom)) {
            errors.add("<>" + rom);
          }
        }
      }
    }
    assertEquals(0, errors.size(), "roms in error: " + String.join(", ", errors));
  }

  private boolean doCompare(String rom, File entry, NVRamParser parser, boolean parseAll, boolean useAssert, boolean ignorePosition, Locale loc) throws IOException {
    System.out.println("Checking " + rom);

    List<NVRamScore> scoresPinemhi = pinemhi.parseNvRam(rom, entry, loc, parseAll);
    String rawPinemhi = String.join("\n", pinemhi.getRaw(rom, entry, loc));

    List<String> raw = parser.getRaw(rom, entry, loc);
    String rawMap = String.join("\n", raw);

    List<NVRamScore> scoresMap = parser.parseNvRam(rom, entry, loc, parseAll);
    if (!checkScores(rom, scoresPinemhi, rawPinemhi, scoresMap, rawMap, useAssert, ignorePosition)) {
      System.out.println(" => Error");
      System.out.println(" ---------------------------");
      return false;
    }

    // same with RawScoreParser
    List<NVRamScore> scoresMap2 = parser.parseRaw(rom, raw, loc, parseAll);
    if (!checkScores(rom, scoresPinemhi, rawPinemhi, scoresMap2, rawMap, useAssert, ignorePosition)) {
      System.out.println(" => Error");
      System.out.println(" ---------------------------");
      return false;
    }

    return true;
  }

  private boolean checkScores(String rom, List<NVRamScore> scoresPinemhi, String rawPinemhi, List<NVRamScore> scoresMap, String rawMap, boolean useAssert, boolean ignorePosition) {
    if (scoresPinemhi.size() == scoresMap.size()) {
      for (int i = 0; i < scoresPinemhi.size(); i++) {
        NVRamScore scorePinemhi = scoresPinemhi.get(i);
        NVRamScore scoreMap = scoresMap.get(i);
        if (!checkScore(rom, scoreMap, scorePinemhi, ignorePosition)) {
          System.out.println(rawPinemhi);
          System.out.println("-----------------");
          System.out.println(rawMap);
          if (useAssert) {
            assertEquals(scorePinemhi, scoreMap);
          }
          else {
            System.out.println("==> " + scorePinemhi + " != " + scoreMap);
            return false;
          }
        }
      }
    }
    else {
      System.out.println(rawPinemhi);
      System.out.println("-----------------");
      System.out.println(rawMap);
      if (useAssert) {
        assertEquals(scoresPinemhi.size(), scoresMap.size());
      }
      return false;
    }

    return true;
  }

  private boolean checkScore(String rom, NVRamScore score1, NVRamScore score2, boolean ignorePosition) {
    return StringUtils.equals(score1.getInitials(), score2.getInitials())
        && (ignorePosition ? true : score1.getPosition() == score2.getPosition())
        && (score1.getScore() != null ? score1.getScore().equals(score2.getScore()) : true)
        && (score1.getScoreText() != null ? score1.getScoreText().equals(score2.getScoreText()) : true);
  }
}
