package net.nvrams.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Copied from VpinStudio to test 
 */
public class RawScoreParser {
  private final static Logger LOG = LoggerFactory.getLogger(RawScoreParser.class);

  private List<String> titles;

  private List<String> skipTitlesCheckFor;

  public RawScoreParser(List<String> titles, List<String> romsSkipTitlesCheck) {
    this.titles = titles;
    this.skipTitlesCheckFor = romsSkipTitlesCheck;
  }

  public List<NVRamScore> getScores(String rom, List<String> lines, boolean parseAll) {
    if (lines == null || lines.isEmpty()) {
      return Collections.emptyList();
    }

    try {
      List<NVRamScore> scores = new ArrayList<>();

	    String currentTitle = null;
      String currentSuffix = null;
      NVRamScore currentScore = null;
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i).trim();
      	if (StringUtils.isEmpty(line)) {
          if (currentSuffix != null && currentScore != null) {
            currentScore.setSuffix(currentSuffix);
          }
        	// restart a possible new sequence
        	currentTitle = null;
          currentSuffix = null;
          currentScore = null;
        	if (parseAll || skipTitlesCheckFor.contains(rom) || scores.size() < 3) {
            continue;
          }
          else {
            // stop the parsing
            break;
          }
      	}

        if (currentTitle != null && isScoreLine(line)) {
          currentScore = createScore(currentTitle, line);
          if (currentScore != null) {
            scores.add(currentScore);
          }
        }
        else if (currentTitle != null && isTitleScoreLine(line)) {
          currentScore = createTitledScore(currentTitle, line);
          if (currentScore != null) {
            if (parseAll || skipTitlesCheckFor.contains(rom) || titles.contains(currentTitle)) {
              scores.add(currentScore);
            }
          }
        }
        else if (currentTitle != null && StringUtils.isNotEmpty(line)) {
          if (currentScore != null) {
            currentSuffix = " " + line;
          } else {
            currentTitle += " " + line;
          }
        }
        else if (StringUtils.isNotEmpty(line)) {
          currentTitle = line;
        }
      }
      if (currentSuffix != null && currentScore != null) {
        currentScore.setSuffix(currentSuffix);
      }
      // when there is only one score that has been ignored, return it
      if (scores.isEmpty() && currentScore != null) {
        scores.add(currentScore);
      }

      return scores;
    }
    catch (Exception e) {
      LOG.error("Score parsing failed: {}", e.getMessage(), e);
      throw e;
    }
  }

  //-------------------------

  private static final String _patternIndex = "(\\d+\\)|#\\d+|\\d+#|\\d+,|\\d+\\.:) +";
  private static final String _patternScore = "([ ?/+\\-a-zA-Z0-9\u0000]{3,}\\s+)?(?:[-|$]?\\s+)?(\\d\\d?\\d?(?:[.,?\u00a0\u202f\ufffd\u00ff]?\\d\\d\\d)*(?:\\.\\d)?)((?:\\s\\d+)?[\\-\\sa-zA-Z]*)$";

  private static final Pattern patternScoreLine = Pattern.compile("^" + _patternIndex + _patternScore);
  private static final Pattern patternScoreTitle = Pattern.compile("^" + _patternScore);


  public static boolean isTitleScoreLine(String line) {
    Matcher m = patternScoreTitle.matcher(line);
    return m.find();
  }

  public static boolean isScoreLine(String line) {
    Matcher m = patternScoreLine.matcher(line);
    return m.find();
  }

  /**
   * Parses score that are shown right behind a possible title.
   * These scores do not have a leading position number.
   */
  @Nullable
  protected NVRamScore createTitledScore(@Nullable String title, @NonNull String line) {
    Matcher m = patternScoreTitle.matcher(line);
    if (m.find()) {
      String initials = StringUtils.trim(m.group(1));
      if (initials == null) {
        initials = "";
      }

      String scoreString = StringUtils.trim(m.group(2));
      long scoreValue = toNumericScore(scoreString, false);
      if (scoreValue != -1) {
        NVRamScore sc = new NVRamScore(initials, scoreValue, -1, title);
        sc.setLabel(title);
        sc.setRawScore(line);

        // do not trim and keep spaces at beginning if present
        String suffix = StringUtils.trim(m.group(3));
        if (StringUtils.isNotEmpty(suffix)) {
          sc.setSuffix(suffix);
        }
        return sc;
      }
    }
    return null;
  }

  @Nullable
  public NVRamScore createScore(@Nullable String title, @NonNull String line) {
    String idx = StringUtils.substringBefore(line, " ");
    idx = idx.replace(")", "");
    idx = idx.replace(",", "");
    idx = idx.replace("#", "");
    idx = idx.replace(".:", "");
    int index = Integer.parseInt(idx);
    
    String line2 = StringUtils.substringAfter(line, " ");
    NVRamScore sc = createTitledScore(title, line2);
    sc.setPosition(index);
    sc.setRawScore(line);
    return sc;
  }

    protected long toNumericScore(@Nullable String score, boolean log) {
    if (StringUtils.isEmpty(score)) {
      if (log) {
        LOG.warn("Cannot parse empty numeric highscore, ignoring this segment");
      }
      return -1;
    }
    try {
      String cleanScore = cleanScore(score);
      return Long.parseLong(cleanScore);
    }
    catch (NumberFormatException e) {
      if (log) {
        LOG.warn("Failed to parse numeric highscore string '{}', ignoring this segment", score);
      }

      return -1;
    }
  }

  public static String cleanScore(String score) {
    return score
        .replace(".", "")
        .replace(",", "")
        .replace("?", "")
        .replace("\u00ff", "")
        .replace("\u00a0", "")
        .replace("\u202f", "")
        .replace("\ufffd", "")
        .replace(" ", "");
  }
}