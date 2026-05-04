package net.nvrams.mapping;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public interface NVRamParser {

  boolean isSupportedRom(String rom);

  List<String> getRaw(String rom, File nvRam, Locale locale) throws IOException;

  List<NVRamScore> parseRaw(String rom, List<String> lines, Locale locale, boolean parseAll) throws IOException;

  List<NVRamScore> parseNvRam(String rom, File nvRam, Locale locale, boolean parseAll) throws IOException;

}
