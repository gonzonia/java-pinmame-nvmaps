package net.nvrams.mapping.superhac;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.nvrams.mapping.NVRamScore;
import net.nvrams.mapping.common.TextDecoders;

public class NVRamEntry implements NVRamScoreDefinition {

  private Integer rank;
  @JsonProperty("rank")
  private void unpackRank(Object _rank) {
    if (_rank instanceof Integer) {
      this.rank = (Integer) _rank;
    }
    else if (_rank instanceof String) {
      this.rank = Integer.valueOf(((String) _rank).replace("#", ""));
    }
  }

  @JsonProperty("title")
  private String title;

  @JsonProperty("label")
  private String label;

  @JsonProperty("score_decoder")
  private String scoreDecoder;

  @JsonProperty("entry_decoder")
  private String entryDecoder;

  @JsonProperty("name_decoder")
  private String nameDecoder;

  @JsonProperty("value_prefix")
  private String valuePrefix;

  @JsonProperty("value_suffix")
  private String valueSuffix;

  @JsonProperty("text")
  private String text;

  @JsonProperty("value_format")
  private String valueFormat;

  @JsonProperty("name_offsets")
  private List<Integer> nameOffsets;

  @JsonProperty("score_offsets")
  private List<Integer> scoreOffsets;

  @JsonProperty("data_offsets")
  private List<Integer> dataOffsets;

  @JsonProperty("extra_offsets")
  private Map<String, Integer> extraOffsets;

  @JsonProperty("digit_offset")
  private Integer digitOffset;

  @JsonProperty("skip_score_values")
  private List<Long> skipScoreValues;

  @JsonProperty("base_year")
  private Integer baseYear;

  //-----------------------------------------

  @Override
  public NVRamScore getScore(Iterator<String> lines, String globalTitle, Locale locale) {

    int position = rank != null ? rank.intValue() : -1;
    String ttle = Objects.toString(title, globalTitle);

    // No score_offsets and no entry_decoder: plain name entry
    if (scoreOffsets == null && entryDecoder == null) {
      return nameOffsets != null ? new NVRamScore(NVRamMap.readLine(lines, null), (Long) null, position, ttle) : null;
    }

    if (entryDecoder != null) {
      /*String line =*/ NVRamMap.readLine(lines, null);
      // don't know how to decode specific lines
      return null;
    }

    // Standard score entry
    String line = NVRamMap.readLine(lines, null);
    NVRamScore sc = NVRamScore.fromRaw(line, ttle, locale);
    if (sc != null) {
      sc.setSuffix(getValueSuffix());
    }
    //parsed.valuePrefix = (String) entry.get("value_prefix");
    //parsed.valueSuffix = (String) entry.get("value_suffix");
    //parsed.valueFormat = (String) entry.get("value_format");
    return sc;
  }

  @Override
  public NVRamScore getScore(byte[] data, String globalTitle, Locale locale, boolean oneBased, Integer zeroByte, Integer zeroIfGte) {

    String initials = "";
    String suffix = null;
    if (nameOffsets != null) {
      List<Integer> byteVals = ByteDecoders.readOffsets(data, nameOffsets, oneBased, false);
      initials = EntryDecoders._decodeInitials(byteVals, nameDecoder);
      initials = TextDecoders.cleanText(initials);

      suffix = EntryDecoders.decodeSuffix(byteVals, nameDecoder);
    }

    int position = rank != null ? rank.intValue() : -1;
    String ttle = Objects.toString(title, globalTitle);

    // No score_offsets and no entry_decoder: plain name entry
    if (scoreOffsets == null && entryDecoder == null) {
      return StringUtils.isNotEmpty(initials) ? new NVRamScore(initials, (Long) null, position, ttle) : null;
    }

    if (entryDecoder != null) {
      NVRamScore sc = EntryDecoders.dispatchEntryDecoder(data, this, initials, position, ttle, oneBased, zeroByte, zeroIfGte);
      return !TextDecoders.cleanText(initials).isEmpty() || StringUtils.isNotEmpty(sc.getScoreText()) ? sc : null;
    }

    // Standard score entry
    List<Integer> scoreBytes = ByteDecoders.readOffsets(data, scoreOffsets, oneBased, false);
    long decodedScore = ScoreDecoders.decodeScoreBytes(scoreBytes, Objects.toString(scoreDecoder, "bcd"), this, zeroByte, zeroIfGte);

    if (skipScoreValues != null && skipScoreValues.contains(decodedScore)) {
      // Ignore the value but keep the line
      return new NVRamScore(initials, (Long) null, position, ttle);
    }

    NVRamScore sc = new NVRamScore(initials, decodedScore, position, ttle);
    suffix = Objects.toString(suffix, getValueSuffix());
    sc.setSuffix(suffix);
    //parsed.valuePrefix = (String) entry.get("value_prefix");
    //parsed.valueSuffix = (String) entry.get("value_suffix");
    //parsed.valueFormat = (String) entry.get("value_format");
    return sc;
  }

  public int getExtraOffset(String key) {
    return extraOffsets != null && extraOffsets.containsKey(key) ? extraOffsets.get(key) : 0;
  }

  //-----------------------------------------

  public Integer getRank() {
    return rank;
  }

  public void setRank(Integer rank) {
    this.rank = rank;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public List<Integer> getNameOffsets() {
    return nameOffsets;
  }

  public void setNameOffsets(List<Integer> nameOffsets) {
    this.nameOffsets = nameOffsets;
  }

  public List<Integer> getScoreOffsets() {
    return scoreOffsets;
  }

  public void setScoreOffsets(List<Integer> scoreOffsets) {
    this.scoreOffsets = scoreOffsets;
  }

  public String getScoreDecoder() {
    return scoreDecoder;
  }

  public void setScoreDecoder(String scoreDecoder) {
    this.scoreDecoder = scoreDecoder;
  }

  public String getEntryDecoder() {
    return entryDecoder;
  }

  public void setEntryDecoder(String entryDecoder) {
    this.entryDecoder = entryDecoder;
  }

  public String getNameDecoder() {
    return nameDecoder;
  }

  public void setNameDecoder(String nameDecoder) {
    this.nameDecoder = nameDecoder;
  }

  public String getValuePrefix() {
    return valuePrefix;
  }

  public void setValuePrefix(String valuePrefix) {
    this.valuePrefix = valuePrefix;
  }

  public String getValueSuffix() {
    return valueSuffix;
  }

  public void setValueSuffix(String valueSuffix) {
    this.valueSuffix = valueSuffix;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getValueFormat() {
    return valueFormat;
  }

  public void setValueFormat(String valueFormat) {
    this.valueFormat = valueFormat;
  }

  public List<Integer> getDataOffsets() {
    return dataOffsets;
  }

  public void setDataOffsets(List<Integer> dataOffsets) {
    this.dataOffsets = dataOffsets;
  }

  public Map<String, Integer> getExtraOffsets() {
    return extraOffsets;
  }

  public void setExtraOffsets(Map<String, Integer> extraOffsets) {
    this.extraOffsets = extraOffsets;
  }

  public Integer getDigitOffset() {
    return digitOffset;
  }

  public void setDigitOffset(Integer digitOffset) {
    this.digitOffset = digitOffset;
  }

  public Integer getBaseYear() {
    return baseYear;
  }

  public void setBaseYear(Integer baseYear) {
    this.baseYear = baseYear;
  }

  public List<Long> getSkipScoreValues() {
    return skipScoreValues;
  }

  public void setSkipScoreValues(List<Long> skipScoreValues) {
    this.skipScoreValues = skipScoreValues;
  }
}
