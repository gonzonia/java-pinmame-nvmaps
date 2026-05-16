package net.nvrams.mapping.map;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.nvrams.mapping.NVRamScore;

/**
 * Object representing a single entry from a nvram mapping file.
 */
public class NVRamScoreMapping extends NVRamObject {

  // A label describing this descriptor. 
  @JsonProperty("label")
  private String label;

  // An optional, abbreviated label for use when space is limited (like in a game launcher on a DMD). 
  @JsonProperty("short_label")
  private String shortLabel;

  @JsonProperty("initials")
  private NVRamMapping initials;
  @JsonProperty("score")
  private NVRamMapping score;
  @JsonProperty("timestamp")
  private NVRamMapping timestamp;
  @JsonProperty("counter")
  private NVRamMapping counter;
  @JsonProperty("nth time")
  private NVRamMapping nthtime;

  //---------------------------------------- Getters only for JSONProperties

  public String getLabel() {
    return label;
  }

  public String getShortLabel() {
    return shortLabel;
  }

  public NVRamMapping getInitials() {
    return initials;
  }

  public NVRamMapping getScore() {
    return score;
  }

  public NVRamMapping getTimestamp() {
    return timestamp;
  }

  //------------------------------------------------

  public List<Integer> offsets() {

    List<Integer> o = new ArrayList<>();
    if (initials != null) {
      o.addAll(initials.offsets());
    }
    if (score != null) {
      o.addAll(score.offsets());
    }
    if (timestamp != null) {
      o.addAll(timestamp.offsets());
    }
    return o;
  }

  public String formatHighScore(SparseMemory memory, Locale locale) {
    NVRamScore sc = NvRamScoreDecoders.decodeScore(memory.getRom(), this, null, memory);
    return sc.toRaw(locale);
  }

  public NVRamScore toScore(String title, SparseMemory memory) {
    String initials = getInitials(memory);
    Long value = null;
    String suffix = null;
    if (score != null) {
      value = score.getValue(memory);
      suffix = score.getSuffix();
    }

    //TODO manage timestamp
    // if (scoreMapping.getTimestamp() != null) {
    //   timestamp = scoreMapping.getTimestamp().getTimestampValue(memory);...
    // }

    NVRamScore sc = new NVRamScore(initials, value, -1, title);
    sc.setSuffix(suffix);
    return sc;
  }
  
  public String formatInitials(SparseMemory memory, Locale locale) {
    String lbl = initials != null? initials.formatEntry(memory, locale) : null;
    if (StringUtils.isBlank(lbl)) {
      return "???";
    }
    return StringUtils.rightPad(lbl.trim(), 3);
  }

  public String formatLabel(boolean useShortLabel) {
    String lbl = Objects.toString(label, "?");
    if (lbl.startsWith("_")) lbl = null;
    if (useShortLabel) {
      if (shortLabel != null) lbl = shortLabel;
    }
    return lbl;
  }

  public String formatValue(SparseMemory memory, Locale locale) {
    return score != null? score.formatEntry(memory, locale) : null;
  }

  
  public Long getValue(SparseMemory memory) {
    return score != null? score.getValue(memory) : null;
  }

  public String getInitials(SparseMemory memory) {
    return initials != null? initials.getTextValue(memory) : null;
  }

  public void reset(long value) {
    //TODO implement here
  }
}
