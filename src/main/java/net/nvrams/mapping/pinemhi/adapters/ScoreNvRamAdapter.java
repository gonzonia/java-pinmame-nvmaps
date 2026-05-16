package net.nvrams.mapping.pinemhi.adapters;

import java.util.List;

import org.jspecify.annotations.NonNull;

public interface ScoreNvRamAdapter {

  boolean isApplicable(@NonNull String nvRam, @NonNull List<String> lines);

  List<String> convert(@NonNull String nvRam, @NonNull List<String> lines);
}
