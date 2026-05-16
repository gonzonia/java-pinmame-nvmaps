package net.nvrams.mapping.pinemhi.adapters;

import org.jspecify.annotations.NonNull;

import net.nvrams.mapping.RawScoreParser;

import java.util.ArrayList;
import java.util.List;

public class MultiBlockAdapter implements ScoreNvRamAdapter {

  private String name;
  private int totalScores;

  public MultiBlockAdapter(String name, int totalScores) {
    this.name = name;
    this.totalScores = totalScores;
  }

  @Override
  public boolean isApplicable(@NonNull String nvRam, @NonNull List<String> lines) {
    return nvRam.equals(name);
  }

  @NonNull
  public List<String> convert(@NonNull String nvRam, @NonNull List<String> lines) {
    List<String> builder = new ArrayList<>();

    int index = 1;
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (RawScoreParser.isScoreLine(line)) {
        line = line.substring(1);
        line = index + line;
        builder.add(line);
        index++;
      }

      if (builder.size() == totalScores) {
        break;
      }
    }

    return builder;
  }
}