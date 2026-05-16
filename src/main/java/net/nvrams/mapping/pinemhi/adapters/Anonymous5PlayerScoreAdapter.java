package net.nvrams.mapping.pinemhi.adapters;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Anonymous5PlayerScoreAdapter implements ScoreNvRamAdapter {

  private String name;

  public Anonymous5PlayerScoreAdapter(String name) {
    this.name = name;
  }

  @Override
  public boolean isApplicable(@NonNull String nvRam, @NonNull List<String> lines) {
    return nvRam.equals(name);
  }

  @Override
  public List<String> convert(@NonNull String nvRam, @NonNull List<String> lines) {
    int index = 0;
    int pos = 1;
    List<String> converted = new ArrayList<>();
    for (String line : new ArrayList<>(lines)) {
      if (index == 1 || index > 3) {
        if (line.length() > 12) {
          String score = line.substring(12).trim();
          converted.add(pos + ") ??? " + score);
          pos++;
        }
      }
      index++;
    }
    return converted;
  }
}
