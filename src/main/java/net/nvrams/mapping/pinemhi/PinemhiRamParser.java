package net.nvrams.mapping.pinemhi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.nvrams.mapping.RawScoreParser;
import net.nvrams.mapping.RawScoreParserConf;
import net.nvrams.mapping.NVRamParser;
import net.nvrams.mapping.NVRamScore;
import net.nvrams.mapping.pinemhi.adapters.AlteringLinesWithoutPosAdapter;
import net.nvrams.mapping.pinemhi.adapters.Anonymous5PlayerScoreAdapter;
import net.nvrams.mapping.pinemhi.adapters.FourColumnScoreAdapter;
import net.nvrams.mapping.pinemhi.adapters.ScoreNvRamAdapter;
import net.nvrams.mapping.pinemhi.adapters.SkipFirstListScoreAdapter;

public class PinemhiRamParser implements NVRamParser {
  private final static Logger LOG = LoggerFactory.getLogger(PinemhiRamParser.class);

  private File pinemhiFolder;
  private File vpPathAdjusted = null;
  private File fpPathAdjusted = null;
  private Set<String> supportedNvRams = new HashSet<>();

  private final RawScoreParser rawScoreParser;

  private final List<ScoreNvRamAdapter> adapters = new ArrayList<>();

  public PinemhiRamParser(String pinhemiFolder) {
    this(pinhemiFolder, RawScoreParserConf.createParser());
  }

  public PinemhiRamParser(String pinhemiFolder, List<String> titles, List<String> romsSkipTitlesCheck) {
    this(pinhemiFolder, new RawScoreParser(titles, romsSkipTitlesCheck));
  }

  private PinemhiRamParser(String pinemhiFolder, RawScoreParser rawScoreParser) {
    this.rawScoreParser = rawScoreParser;
    this.pinemhiFolder = new File(pinemhiFolder);

    //adapters.add(new SinglePlayerScoreAdapter("algar_l1.nv", 1));
    //adapters.add(new SinglePlayerScoreAdapter("alienstr.nv", 1));
    //adapters.add(new SinglePlayerScoreAdapter("alpok_b6.nv", 1));
    adapters.add(new FourColumnScoreAdapter("monopoly.nv"));
    adapters.add(new SkipFirstListScoreAdapter("godzilla.nv"));
    //adapters.add(new NewLineAfterFirstScoreAdapter("kiko_a10.nv"));
    adapters.add(new Anonymous5PlayerScoreAdapter("punchy.nv"));
    //adapters.add(new FixTitleScoreAdapter("rs_l6.nv", "TODAY'S HIGHEST SCORES", "ALL TIME HIGHEST SCORES"));
    //adapters.add(new SinglePlayerScoreAdapter());
    //adapters.add(new MultiBlockAdapter("pool_l7.nv", 8));
    adapters.add(new AlteringLinesWithoutPosAdapter("wrldtou2.nv", 5));

    //force the same folder structure as for the Studio Server
    File commandFile = new File(pinemhiFolder, "PINemHi.exe");
    List<String> commands = Arrays.asList("cmd.exe", "/c", commandFile.getName(), "-lr");

    try {
      List<String> roms = execute(commands, commandFile.getParentFile());
      this.supportedNvRams.addAll(roms);
    }
    catch (IOException ioe) {
      throw new RuntimeException("Cannot load pinemhi supported roms");
    }
  }

  //@Override
  public Set<String> getSupportedRoms() {
    return supportedNvRams;
  }

  @Override
  public boolean isSupportedRom(String rom) {
    return supportedNvRams.contains(rom);
  }

  @Nullable
  @Override
  public List<NVRamScore> parseNvRam(String rom, @NonNull File nvRam, Locale locale, boolean parseAll) throws IOException {
    List<String> lines = getRaw(rom, nvRam, locale);
    return parseRaw(rom, lines, locale, parseAll);
  }


  @Override
  public List<NVRamScore> parseRaw(String rom, List<String> lines, Locale locale, boolean parseAll) throws IOException {
    List<NVRamScore> scores = rawScoreParser.getScores(rom, lines, parseAll);

    // E.g. Transformers has a separate highscore list for Autobots and Decepticons, combines all scores into one list
    if (StringUtils.equals(rom, "tf_180")) {
      // keep only first 10 items
      if (scores.size() > 10) {
        scores = scores.subList(0, 10);
      }
      scores.sort((a, b) -> Long.compare(b.getScore(), a.getScore()));
      int i = 1;
      for (NVRamScore score : scores) {
        score.setPosition(i++);
      }
    }

    return filterDuplicates(scores);
  }


  @Override
  public List<String> getRaw(String rom, @NonNull File ramFile, Locale locale) throws IOException {
    File originalRamFile = ramFile;
    String ramFileName = ramFile.getCanonicalFile().getName().toLowerCase();
    String ramName = FilenameUtils.getBaseName(ramFileName).toLowerCase();
    if (ramFileName.contains(" ") && ramName.endsWith(".nv")) {
      LOG.info("Stripping NV offset from nvram file \"{}\" to check if supported.", ramFileName);
      ramName = ramFileName.substring(0, ramFileName.indexOf(" "));

      //rename the original nvram file so that we can parse with the original name
      originalRamFile = new File(ramFile.getParentFile(), ramName + ".nv");
    }

    List<String> lines = executePINemHi(originalRamFile);

    for (ScoreNvRamAdapter adapter : adapters) {
      if (adapter.isApplicable(ramFileName, lines)) {
        LOG.info("Converted score using {}", adapter.getClass().getSimpleName());
        return adapter.convert(ramFileName, lines);
      }
    }
    return lines;
  }

  protected List<NVRamScore> filterDuplicates(List<NVRamScore> scores) {
    List<NVRamScore> scoreList = new ArrayList<>();
    for (NVRamScore s : scores) {
      if (s.getScore() != 0 && StringUtils.isNotEmpty(s.getInitials())) {
        if (scoreList.stream().anyMatch(score -> Objects.equals(score.getScore(), s.getScore()) && StringUtils.equals(score.getInitials(), s.getInitials()))) {
          continue;
        }
      }
      scoreList.add(s);
    }
    return scoreList;
  }


  @Nullable
  public List<String> executePINemHi(@NonNull File originalRamFile) throws IOException {
    File commandFile = new File(pinemhiFolder, "PINemHi.exe");
    String ramName = originalRamFile.getName().toLowerCase();

    // make sure nvram can be found
    if (originalRamFile.getName().toLowerCase().endsWith(".nv")) {
      adjustVPPathForEmulator(originalRamFile.getParentFile(), true);
    }
    else if (originalRamFile.getName().toLowerCase().endsWith(".fpram")) {
      adjustFPPathForEmulator(originalRamFile.getParentFile(), true);
    }

    List<String> commands = Arrays.asList("cmd.exe", "/c", commandFile.getName(), ramName);
    return execute(commands, commandFile.getParentFile());
  }

  private List<String> execute(List<String> commands, File dir) throws IOException {
    List<String> lines = new ArrayList<>();
    try {
      ProcessBuilder pb = new ProcessBuilder(commands)
          .directory(dir)
          .redirectErrorStream(true);
      Process process = pb.start();

      InputStream stdOut = process.getInputStream();

      try (BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(stdOut))) {
        String line;
        while ((line = stdOutReader.readLine()) != null) {
          lines.add(line);
        }
      }
      process.waitFor();
    }
    catch (InterruptedException ie) {
      LOG.error("Process interrupted", ie);
    }
    return lines;
  }

  private void adjustVPPathForEmulator(File nvRamFolder, boolean forcePath) {
    if (vpPathAdjusted != null && vpPathAdjusted.equals(nvRamFolder)) {
      return;
    }

    adjustPath(nvRamFolder, "VP", forcePath);
    vpPathAdjusted = nvRamFolder;
  }

  private void adjustFPPathForEmulator(File fpRamFolder, boolean forcePath) {
    if (fpPathAdjusted != null && fpPathAdjusted.equals(fpRamFolder)) {
      return;
    }
    adjustPath(fpRamFolder, "FP", forcePath);
    fpPathAdjusted = fpRamFolder;
  }

  private void adjustPath(File ramFolder, String key, boolean forcePath) {
    if (ramFolder.exists()) {
      try {
        File pinemhiIni = new File(pinemhiFolder, "pinemhi.ini");
        INIConfiguration iniConfiguration = loadIni(pinemhiIni);
        String emuPathString = (String) iniConfiguration.getSection("paths").getProperty(key);
        File emuPath = new File(emuPathString);

        if (forcePath || !emuPath.exists() || !emuPathString.endsWith("/")) {
          emuPath = new File(ramFolder.getAbsolutePath());
          iniConfiguration.getSection("paths").setProperty(key, emuPath.getAbsolutePath().replaceAll("\\\\", "/") + "/");

          saveIni(pinemhiIni, iniConfiguration);
          LOG.info("Changed {} path to {}", key, emuPath.getAbsolutePath());
        }
      }
      catch (Exception e) {
        LOG.error("Failed to update {} path in pinemhi.ini: {}", key, e.getMessage(), e);
      }
    }
  }


  private static void saveIni(File ini, INIConfiguration iniConfiguration) throws IOException, ConfigurationException {
    try (FileWriter fileWriter = new FileWriter(ini)) {
      iniConfiguration.write(fileWriter);
    }
  }

  private static INIConfiguration loadIni(File ini) throws IOException, ConfigurationException {
    INIConfiguration iniConfiguration = new INIConfiguration();
    iniConfiguration.setCommentLeadingCharsUsedInInput(";");
    iniConfiguration.setSeparatorUsedInOutput("=");
    iniConfiguration.setSeparatorUsedInInput("=");

    try (FileReader fileReader = new FileReader(ini)) {
      iniConfiguration.read(fileReader);
    }
    return iniConfiguration;
  }
}