package net.nvrams.mapping.superhac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.nvrams.mapping.NVRamScore;

public class NVRamMap implements NVRamScoreDefinition {

  @JsonProperty("decoder")
  private String decoder;

  @JsonProperty("one_based")
  private boolean oneBased = true;

  @JsonProperty("reverse_digits")
  private boolean reverseDigits = false;

  @JsonProperty("scoretype")
  private String scoretype;

  @JsonProperty("zero_byte")
  private Integer zeroByte;

  @JsonProperty("zero_if_gte")
  private Integer zeroIfGte;

  @JsonProperty("offsets")
  private List<Integer> offsets;

  @JsonProperty("digit_offset")
  private Integer digitOffset = 0;

  @JsonProperty("entries")
  private List<NVRamEntry> entries;

  @JsonProperty("sections")
  private List<NVRamSection> sections;

  @JsonProperty("settings")
  private Map<String, ?> settings;

  //--------------------------------------------------

  public List<NVRamScore> parseScores(byte[] data, Locale locale, boolean parseAll) throws IOException {
    List<NVRamScore> scores = new ArrayList<>();
    iterate((scoreEntry, title, sectionEnabled) -> {
        NVRamScore sc = scoreEntry.getScore(data, title, locale, oneBased, zeroByte, zeroIfGte);
        if (sc != null && (parseAll || sc.getScore() != null) && sectionEnabled) {
           if (!containsScore(scores, sc)) {
            scores.add(sc);
            return 1;
           }
        }
        return 0;
      }, parseAll);
    return scores;
  }

  public List<NVRamScore> parseRaw(List<String> lines, Locale locale, boolean parseAll) throws IOException {
    Iterator<String> linesIterator = lines.iterator();
    List<NVRamScore> scores = new ArrayList<>();
    String[] currentTitle = { null };
    iterate((scoreEntry, title, sectionEnabled) -> {
        if (!StringUtils.equals(currentTitle[0], title)) {
          if (currentTitle[0] != null) {
            // read blank line
            readLine(linesIterator, "");
          }
          if (StringUtils.isNotEmpty(title)) {
            readLine(linesIterator, title);
          }
          currentTitle[0] = title;
        }
        // read the score
        NVRamScore sc = scoreEntry.getScore(linesIterator, title, locale);
        if (sc != null && (parseAll || sc.getScore() != null) && sectionEnabled) {
           if (!containsScore(scores, sc)) {
            scores.add(sc);
            return 1;
           }
        }
        return 0;
      }, 
      parseAll);
    return scores;
  }


  public List<String> getRaw(byte[] data, Locale locale) throws IOException {
    List<String> raw = new ArrayList<>();
    String[] currentTitle = { null };
    iterate((scoreEntry, title, sectionEnabled) -> {
        if (!StringUtils.equals(currentTitle[0], title)) {
          if (currentTitle[0] != null) {
            raw.add("");
          }
          if (StringUtils.isNotEmpty(title)) {
            raw.add(title);
          }
          currentTitle[0] = title;
        }

        NVRamScore sc = scoreEntry.getScore(data, title, locale, oneBased, zeroByte, zeroIfGte);
        if (sc != null && sectionEnabled) {
          raw.add(sc.toRaw(locale));
          return 1;
        }
        return 0;
      }, true);
    return raw;
  }

  /**
   * Iterate on map and process <NVRamEntry, title>
   */
  private void iterate(NVRamScoreDefinitionProcessor scores, boolean parseAll) throws IOException {

    Map<String, Object> _settings = new HashMap<>();
    if (settings != null) {
      _settings.putAll(settings);
    }
    _settings.put("buyins", parseAll);
    _settings.put("additional", parseAll);

    int nbScores = 0;
    switch (decoder) {
      case "leaderboard_bcd":
        for (NVRamEntry entry : entries) {
          nbScores += scores.process(entry, StringUtils.defaultString(entry.getTitle(), "HIGHEST SCORES"), true);
        }
        break;
      case "mixed_leaderboard":
        // FIXME should not be needed but today some sections are redundant
        mergeSections(sections);
        for (NVRamSection section : sections) {
          boolean sectionEnabled = section.isEnabled(_settings);
          // stop when a first section is filtered, except if it is at beginning
          if (!sectionEnabled && nbScores > 0) {
            break;
          }
          if (nbScores > 3 && !parseAll) {
            break;
          }
          for (NVRamEntry entry : section.getEntries()) {
            nbScores += scores.process(entry, section.getTitle(), sectionEnabled);
          }
        }
        break;
      default:
        // other decoders are single score cases
        nbScores += scores.process(this, "HIGHEST SCORE", true);
    }
  }

  @FunctionalInterface
  public static interface NVRamScoreDefinitionProcessor {
    public int process(NVRamScoreDefinition scoreDefinition, String title, boolean sectionEnabled) throws IOException;
  }

  private boolean containsScore(List<NVRamScore> scores, NVRamScore sc) {
    if (sc.getScore() == 0 || StringUtils.isEmpty(sc.getInitials())) {
      return false;
    }
    return scores.stream().anyMatch(score -> Objects.equals(score.getScore(), sc.getScore()) && StringUtils.equals(score.getInitials(), sc.getInitials()));
  }

  /**
   * FIXME should not be needed but today some section are redundant
   */
  private void mergeSections(List<NVRamSection> sections) {
    NVRamSection currentSection = null;
    for (Iterator<NVRamSection> iter = sections.iterator(); iter.hasNext();) {
      NVRamSection section = iter.next();
      if (currentSection == null || !StringUtils.equals(currentSection.getTitle(), section.getTitle())) {
        currentSection = section;
      }
      else {
        iter.remove();
        currentSection.getEntries().addAll(section.getEntries());
        System.out.println("!!! CONSIDER MERGING SECTION " + section.getTitle());
      }
    }
  }

  public static String readLine(Iterator<String> linesIterator, String expected) {
    if (linesIterator.hasNext()) {
      String line = linesIterator.next();
      // when expected is null, just read 
      if (expected != null && !StringUtils.equals(line, expected)) {
        throw new RuntimeException("Wrong line '" + line + "'', expected '" + expected + "'");
      }
      return line;
    }
    // else, end reached
    if (expected != null) {
      throw new RuntimeException("No more line to process, expected '" + expected + "'");
    }
    else {
      throw new RuntimeException("No more line to process, expected at least one");
    }
  }

  //--------------------------------------------------
  // Implementation of NVRamScoreDefinition

  @Override
  public NVRamScore getScore(Iterator<String> lines, String title, Locale locale) {
    String line = readLine(lines, null);
    return NVRamScore.fromRaw(line, title, locale);
  }

  @Override
  public NVRamScore getScore(byte[] data, String title, Locale locale, boolean oneBased, Integer zeroByte, Integer zeroIfGte) {
    long scoreValue = 0;
    switch (decoder) {
      case "single_bcd_score":
        scoreValue = ScoreDecoders.decodeSingleBcdScore(ByteDecoders.readOffsets(data, offsets, oneBased, reverseDigits));
        break;
      case "single_bcd_score_x10":
        scoreValue = 10 * ScoreDecoders.decodeSingleBcdScore(ByteDecoders.readOffsets(data, offsets, oneBased, reverseDigits));
        break;
      case "single_digit_score":
        scoreValue = ScoreDecoders.decodeSingleDigitScore(ByteDecoders.readOffsets(data, offsets, oneBased, reverseDigits), digitOffset, zeroByte);
        break;
      case "single_digit_score_x10":
        scoreValue = 10 * ScoreDecoders.decodeSingleDigitScore(ByteDecoders.readOffsets(data, offsets, oneBased, reverseDigits), digitOffset, zeroByte);
        break;
      case "single_high_nibble_score":
        scoreValue = ScoreDecoders.decodeSingleHighNibbleScore(ByteDecoders.readOffsets(data, offsets, oneBased, reverseDigits), zeroByte, zeroIfGte);
        break;
      case "single_high_nibble_score_x10":
        scoreValue = 10 * ScoreDecoders.decodeSingleHighNibbleScore(ByteDecoders.readOffsets(data, offsets, oneBased, reverseDigits), zeroByte, zeroIfGte);
        break;
      default:
        throw new IllegalArgumentException("No decoder registered or decoder unknown: " + decoder);
    }
    return new NVRamScore(null,  scoreValue, -1, title);
  }

  //--------------------------------------------------

  public String getDecoder() {
    return decoder;
  }

  public void setDecoder(String decoder) {
    this.decoder = decoder;
  }

  public boolean isOneBased() {
    return oneBased;
  }

  public void setOneBased(boolean oneBased) {
    this.oneBased = oneBased;
  }

  public boolean isReverseDigits() {
    return reverseDigits;
  }

  public void setReverseDigits(boolean reverseDigits) {
    this.reverseDigits = reverseDigits;
  }

  public String getScoretype() {
    return scoretype;
  }

  public void setScoretype(String scoretype) {
    this.scoretype = scoretype;
  }

  public List<NVRamEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<NVRamEntry> entries) {
    this.entries = entries;
  }

  public List<NVRamSection> getSections() {
    return sections;
  }

  public void setSections(List<NVRamSection> sections) {
    this.sections = sections;
  }

  public Integer getZeroByte() {
    return zeroByte;
  }

  public void setZeroByte(Integer zeroByte) {
    this.zeroByte = zeroByte;
  }

  public List<Integer> getOffsets() {
    return offsets;
  }

  public void setOffsets(List<Integer> offsets) {
    this.offsets = offsets;
  }

  public Integer getDigitOffset() {
    return digitOffset;
  }

  public void setDigitOffset(Integer digitOffset) {
    this.digitOffset = digitOffset;
  }
}
