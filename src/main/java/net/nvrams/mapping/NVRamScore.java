package net.nvrams.mapping;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NVRamScore {
  private final static Logger LOG = LoggerFactory.getLogger(NVRamScore.class);

  private String label;       // The label/title of the group

  private String initials = "";
  private Long score;
  private String scoreText;

  private int position;
  private String suffix;      // optional suffix for non high scores: 100M, 100Gb, 100 combos, 100 martians,... Include initial space if this is a unit
  private String rawScore;

  private final static Map<Locale, DecimalFormat> formats = new HashMap<>();


  public NVRamScore(String initials, String scoreText, int position, String title) {
    this.label = title;
    this.scoreText = scoreText;
    this.position = position;
    if (!StringUtils.isEmpty(initials)) {
      this.initials = initials.trim();
    }
  }

  public NVRamScore(String initials, Long score, int position, String title) {
    this.label = title;
    this.score = score;
    this.position = position;
    if (!StringUtils.isEmpty(initials)) {
      this.initials = initials.trim();
    }
  }

  public String _getPlayerInitials() {
    String initials = StringUtils.defaultIfBlank(this.initials, "???");
    while (initials.length() < 3) {
      initials += " ";
    }
    return initials;
  }

  public String getInitials() {
    return initials;
  }

  public void setInitials(String initials) {
    this.initials = initials;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public Long getScore() {
    return score;
  }

  public void setScore(Long score) {
    this.score = score;
  }

  public String getScoreText() {
    return scoreText;
  }

  public void setScoreText(String scoreText) {
    this.scoreText = scoreText;
  }

  public String getRawScore() {
    return rawScore;
  }

  public void setRawScore(String rawScore) {
    this.rawScore = rawScore;
  }
  
  public String getLabel() {
    return label;
  }

  public void setLabel(String title) {
    this.label = title;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  //---------------------------------------------

  public boolean hasInitials() {
    return !StringUtils.isEmpty(initials);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof NVRamScore)) {
      return false;
    }

    NVRamScore score = (NVRamScore) obj;
    return StringUtils.equals(score.getInitials(), this.getInitials())
        && (score.getPosition() == this.getPosition())
        && (score.getScore() != null ? score.getScore().equals(this.getScore()) : true)
        && (score.getScoreText() != null ? score.getScoreText().equals(this.getScoreText()) : true);
  }

  @Override
  public String toString() {
    return toRaw(Locale.getDefault());
  }

  public String toRaw(Locale loc) {
    String disp = (position > 0 ? "#" + position + " ": "") 
          + StringUtils.rightPad(initials, 3) 
          + "   " 
          + getFormattedScore(loc) 
          + (suffix != null? " " + suffix : "");
    return disp.trim();
  }

  private static final String _patternScore = "(#(\\d+)\\s)?([ ?/+\\-a-zA-Z0-9]{3,}\\s+)?(\\d\\d?\\d?(?:[.,\\u00a0\\u202f\\ufffd\\u00ff]?\\d\\d\\d)*(?:\\.\\d)?)((?:\\s\\d+)?[\\-\\sa-zA-Z]*)$";
  private static final Pattern patternScoreTitle = Pattern.compile("^" + _patternScore);

  public static NVRamScore fromRaw(String line, String title, Locale locale) {
    Matcher m = patternScoreTitle.matcher(line);
    if (m.find()) {
      String positionString = StringUtils.trim(m.group(2));
      int position = positionString != null ? Integer.parseInt(positionString) : -1;

      String initials = StringUtils.trim(m.group(3));

      String scoreString = StringUtils.trim(m.group(4));
      long scoreValue = -1;
      try {
         scoreValue = Long.parseLong(cleanScore(scoreString));
      }
      catch (NumberFormatException e) {
        LOG.error("Cannot parse {}: {}", scoreString, e.getMessage());
        return null;
      }

      NVRamScore sc = new NVRamScore(initials, scoreValue, position, title);
      sc.setRawScore(line);

      // do not trim and keep spaces at beginning if present
      String suffix = StringUtils.trim(m.group(5));
      if (StringUtils.isNotEmpty(suffix)) {
        sc.setSuffix(suffix);
      }
      return sc;
    }
    return null;
  }

  @JsonIgnore
  public String getFormattedScore() {
    return getFormattedScore(Locale.getDefault());
  }

  @JsonIgnore
  public String getFormattedScore(Locale loc) {
    if (score != null) {
      return getFormattedScore(score, loc);
    }
    else if (scoreText != null) {
      return scoreText;
    }
    return "";
  }

  public static String getFormattedScore(long score, Locale loc) {
    try {
      DecimalFormat decimalFormat = formats.get(loc);
      if (decimalFormat == null) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(loc);
        decimalFormat = new DecimalFormat("#.##", symbols);
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
        formats.put(loc, decimalFormat);
      }

      String formattedScore = decimalFormat.format(score);
      // for french locale replace non-breaking spaces with normal spaces
      // see https://bugs.openjdk.org/browse/JDK-8274768, french whitespace separators changed in Java17
      formattedScore = formattedScore.replace('\u00A0', ' ');
      formattedScore = formattedScore.replace('\u202F', ' ');
      formattedScore = formattedScore.replaceAll(" {2}", " ");
      return formattedScore;
    }
    catch (NumberFormatException e) {
      LOG.error("Failed to read number from '" + score + "': " + e.getMessage());
      return "0";
    }
  }

  protected static String cleanScore(String score) {
    return score
        .replace(".", "")
        .replace(",", "")
        .replace("\u00ff", "")
        .replace("\u00a0", "")
        .replace("\u202f", "")
        .replace("\ufffd", "")
        .replace(" ", "");
  }
}