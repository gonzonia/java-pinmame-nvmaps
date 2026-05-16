package net.nvrams.mapping.pinemhi.adapters;

import net.nvrams.mapping.RawScoreParser;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SinglePlayerScoreAdapter implements ScoreNvRamAdapter {
  private final static Logger LOG = LoggerFactory.getLogger(SinglePlayerScoreAdapter.class);

  private String name;
  private int scoreLine;

  public SinglePlayerScoreAdapter(String name, int scoreLine) {
    this.name = name;
    this.scoreLine = scoreLine;
  }

  public SinglePlayerScoreAdapter() {
  }

  @Override
  public boolean isApplicable(@NonNull String nvRam, @NonNull List<String> lines) {
    return nvRam.equals(name) || lines.size() == 2;
  }

  @Override
  public List<String> convert(@NonNull String nvRam, @NonNull List<String> lines) {
    List<String> converted = new ArrayList<>();
    try {
      converted.add("HIGHEST SCORE");
      StringBuilder builder = new StringBuilder();
      String score1 = lines.get(scoreLine);
      builder.append("#1");
      builder.append(" ");
      builder.append("???");
      builder.append("   ");
      builder.append(RawScoreParser.cleanScore(score1));
      converted.add(builder.toString());
    }
    catch (Exception e) {
      LOG.warn("Failed to parse {} lines with index {}: {}", lines.size(), scoreLine, e.getMessage());
    }
    return converted;
  }
}
