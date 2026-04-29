package net.nvrams.mapping.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nvrams.mapping.NVRamParser;
import net.nvrams.mapping.NVRamScore;

/**
 * Main class for parsing PinMAME .nv files using NVRAM maps.
 * This is a modified port in java of https://github.com/tomlogic/py-pinmame-nvmaps/blob/main/nvram_parser.py 
 * 
 * How to use:
 * - create NVRamParser
 * - use getMap(rom) to retrieve a NVRamMap definition that is the downloaded and parsed NVRAm Map
 * - use getMemory(mapJson, bytes) to retrieve a SparseMemory object that is the NV file parsed
 * - use tools like NVRamToolDump to dump the content of the NVRam
 * 
 * see NVRam Map structure
 * https://github.com/tomlogic/pinmame-nvram-maps/blob/main/README.md?plain=1
 */
@Service
public class NVRamMapParser implements NVRamParser {

  private final static Logger LOG = LoggerFactory.getLogger(NVRamMapParser.class);

  private String mapFolder;

  private Map<String, String> cacheMapForRom;
  private Map<String, String> cacheRomNames;

  private final Map<String, NVRamMap> cacheNVRamMap = new HashMap<>();
  private final Map<String, NVRamPlatform> cachePlatform = new HashMap<>();


  /**
   * A service that uses a local folder to access json maps
   */
  public NVRamMapParser(String mapFolder) {
    this.mapFolder = mapFolder;
  }

  public String getMapFolder() {
    return mapFolder;
  }


  //------------
  @Override
  public List<NVRamScore> parseNvRam(String rom, File nvRam, Locale locale, boolean parseAll) throws IOException {
    NVRamMap mapJson = getMap(rom);
    if (mapJson == null) {
      return Collections.emptyList();
    }

    byte[] data = Files.readAllBytes(nvRam.toPath());
    SparseMemory memory = getMemory(mapJson, data);

    List<NVRamScoreMapping> scoreDefs = mapJson.getHighScores();
    List<NVRamScore> scores = new ArrayList<>();
    if (scoreDefs != null) {
      // count number of scores, group by section title (label)
      Map<String, Long> countByTitle = scoreDefs.stream()
        .collect(Collectors.groupingBy(sc -> normalize(sc.formatLabel(false), locale), Collectors.counting()));
      // true if all scores use a different label
      boolean allOnes = scoreDefs.size() > 1 && countByTitle.values().stream().allMatch(v -> v == 1);

      int position = 1;
      String currentLabel = null;
      for (NVRamScoreMapping scoreDef : scoreDefs) {
        String lbl = normalize(scoreDef.formatLabel(false), locale);
        if (!StringUtils.equals(currentLabel, lbl)) {
          currentLabel = lbl;
          // When all labels are different, do not reset position
          if (!allOnes) {
            position = 1;
          }
        }
        
        if (parseAll || filter(lbl)) {
          NVRamScore sc = NvRamScoreDecoders.decodeScore(rom, scoreDef, currentLabel, memory);
          if (sc != null && (allOnes || countByTitle.get(currentLabel) > 1)) {
            sc.setPosition(position++);
          }
          if (sc != null && !containsScore(scores, sc)) {
            scores.add(sc);
          }
        }
      }
    }
    return scores;
  }

  private boolean containsScore(List<NVRamScore> scores, NVRamScore sc) {
    if (sc.getScore() == 0 || StringUtils.isEmpty(sc.getInitials())) {
      return false;
    }
    return scores.stream().anyMatch(score -> Objects.equals(score.getScore(), sc.getScore()) && StringUtils.equals(score.getInitials(), sc.getInitials()));
  }

  //------------
  @Override
  public List<NVRamScore> parseRaw(String rom, List<String> lines, Locale locale, boolean parseAll) throws IOException {
    NVRamMap mapJson = getMap(rom);
    if (mapJson == null) {
      return Collections.emptyList();
    }

    Iterator<String> linesIterator = lines.iterator();
    List<NVRamScore> scores = new ArrayList<>();
    parseScoresRaw(scores, mapJson.getHighScores(), linesIterator, locale, parseAll);
    if (parseAll) {
      parseScoresRaw(scores, mapJson.getModeChampions(), linesIterator, locale, parseAll);
    }
    return scores;
  }

  private void parseScoresRaw(List<NVRamScore> scores, List<NVRamScoreMapping> scoreDefs,
                              Iterator<String> linesIterator, Locale locale, boolean parseAll) throws IOException {
    if (scoreDefs != null) {

      String currentLabel = null;
      for (NVRamScoreMapping score : scoreDefs) {
        String lbl = normalize(score.formatLabel(false), locale);
        // new section
        if (!StringUtils.equals(currentLabel, lbl)) {
          if (scores.size() > 0) {
            // read blank line
            readLine(linesIterator, "");
          }
          // read label from lines and check this is same lbl
          readLine(linesIterator, lbl);
          currentLabel = lbl;
        }

        String line = readLine(linesIterator, null);
        if (parseAll || filter(lbl)) {
          // turn line in score
          NVRamScore sc = NVRamScore.fromRaw(line, currentLabel, locale);
          if (sc != null && !containsScore(scores, sc)) {
            scores.add(sc);
          }
        }
      }
    }
  }

  private String readLine(Iterator<String> linesIterator, String expected) throws IOException {
    if (linesIterator.hasNext()) {
      String line = linesIterator.next();
      // when expected is null, just read 
      if (expected != null && !StringUtils.equals(line, expected)) {
        throw new IOException("Wrong line '" + line + "'', expected '" + expected + "'");
      }
      return line;
    }
    // else, end reached
    if (expected != null) {
      throw new IOException("No more line to process, expected '" + expected + "'");
    }
    else {
      throw new IOException("No more line to process, expected at least one");
    }
  }

  //------------
  @Override
  public List<String> getRaw(String rom, File nvRam, Locale locale) throws IOException {
    NVRamMap mapJson = getMap(rom);
    if (mapJson == null) {
      return Collections.emptyList();
    }

    byte[] data = Files.readAllBytes(nvRam.toPath());
    SparseMemory memory = getMemory(mapJson, data);

    List<String> raw = new ArrayList<>();

    getScoresRaw(raw, rom, mapJson.getHighScores(), memory, locale, true);
    getScoresRaw(raw, rom, mapJson.getModeChampions(), memory, locale, false);

    return raw;
  }

  private void getScoresRaw(List<String> raw, String rom, List<NVRamScoreMapping> scoreDefs, SparseMemory memory, Locale locale, boolean addPosition) {
    if (scoreDefs != null) {
      // count number of scores, group by section title (label)
      Map<String, Long> countByTitle = scoreDefs.stream()
        .collect(Collectors.groupingBy(sc -> normalize(sc.formatLabel(false), locale), Collectors.counting()));
      // true if all scores use a different label
      boolean allOnes = scoreDefs.size() > 1 && countByTitle.values().stream().allMatch(v -> v == 1);

      int position = 1;
      String currentLabel = null;
      for (NVRamScoreMapping scoreDef : scoreDefs) {
        String lbl = normalize(scoreDef.formatLabel(false), locale);
        if (!StringUtils.equals(currentLabel, lbl)) {
          if (raw.size() > 0) {
            raw.add("");
          }
          raw.add(lbl);
          currentLabel = lbl;
          // When all labels are different, do not reset position
          if (!allOnes) {
            position = 1;
          }
        }

        NVRamScore sc = NvRamScoreDecoders.decodeScore(rom, scoreDef, currentLabel, memory);
        if (addPosition && (allOnes || countByTitle.get(currentLabel) > 1)) {
          sc.setPosition(position++);
        }
        raw.add(sc.toRaw(locale));
      }
    }
  }

  //----------------------------------------------------

  private static final Pattern patternIndex = Pattern.compile("[# ]\\d+");

  private String[] ignoredLabels = {
      "FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH", "SIXTH", "SEVENTH", "EIGHTH", "NINTH", "TENTH",
      "1ST", "2ND", "3RD", "4TH", "5TH", "6TH", "7TH", "8TH", "9TH", "10TH"
  };

  private String normalize(String lbl, Locale locale) {
    for (String ignoredLabel : ignoredLabels) {
      if (StringUtils.containsIgnoreCase(lbl, ignoredLabel)) {
        return "HIGHEST SCORES";
      }
    }
    // If label contains #1, remove this part
    lbl = patternIndex.matcher(lbl).replaceFirst("");
    lbl = lbl.trim().toUpperCase(locale);
    if (StringUtils.isEmpty(lbl)) {
      return "HIGHEST SCORES";
    }
    return lbl;
  }

  private boolean filter(String lbl) {
    return !StringUtils.containsIgnoreCase(lbl, "BUY-IN")
        && !StringUtils.containsIgnoreCase(lbl, "BUYIN");
  }

  //--------------------------
  private void initMap(NVRamMap mapJson) throws IOException {
    NVRamMetadata metadata = mapJson.getMetadata();
    if (metadata == null) {
      throw new IllegalArgumentException("Unsupported map file format -- update to v0.6 or later");
    }

		NVRamPlatform platform = loadPlatform(mapJson.getMetadata());
    mapJson.setNVRamPlatform(platform);

    // checksums
    createChecksum(mapJson, mapJson.getChecksum8(), false);
    createChecksum(mapJson, mapJson.getChecksum16(), true);
  }    

  public NVRamPlatform loadPlatform(NVRamMetadata metadata) throws IOException {
    String platformName = metadata.getPlatform();
    NVRamPlatform platform;
    if (platformName != null) {
      platform = cachePlatform.get(platformName);
      if (platform == null) {
        String platformUrl = "platforms/" + platformName + ".json";
        platform = getStream(platformUrl, in -> {
          ObjectMapper objectMapper = new ObjectMapper();
          return objectMapper.readValue(in, NVRamPlatform.class);
        });
      }
      cachePlatform.put(platformName, platform);
    } 
    else {
		  platform = new NVRamPlatform();
      platform.setName("auto-generated");
      platform.setCpu("unknown");
      platform.setEndian(metadata.getEndian());

      Integer size = null;
      if (StringUtils.isNotEmpty(metadata.getRamSize())) {
        size = BcdUtils.toInt(metadata.getRamSize());
      }
      NVRamRegion region = NVRamRegion.createDefault("undefined", size);
      platform.addLayout(region);
    }
    return platform;
  }
   
  private void createChecksum(NVRamMap mapJson, List<NVRamMapping> checksums, boolean is16) {
    if (checksums != null) {
      for (NVRamMapping c : checksums) {
        int start = c.getStart();
        int end;
        if (c.getEnd() != null) {
          end = c.getEnd();
        } else {
          end = start + c.getLength() - 1;
        }
        int grouping = ObjectUtils.defaultIfNull(c.getGroupings(), end - start + 1);
        while (start <= end) {
          int entryEnd = start + grouping - 1;
          mapJson.addChecksumEntry(new ChecksumMapping(start, entryEnd, c.getChecksum(),
              c.getLabel(), is16, mapJson.isBigEndian()));
          start = entryEnd + 1;
        }
      }
    }
  }

  //----------------------------------------

  public SparseMemory getMemory(NVRamMap mapJson, byte[] nvData) {
    NVRamRegion nvramMem = mapJson.getMemoryArea(null, "nvram");
    int base = nvramMem != null ? nvramMem.getAddress() : 0;
    int length = ObjectUtils.defaultIfNull(nvramMem != null ? nvramMem.getSize() : null, nvData.length);
    if (length > nvData.length) length = nvData.length;

    SparseMemory memory = new SparseMemory(mapJson);

    memory.updateMemory(base, Arrays.copyOf(nvData, length));
    if (length < nvData.length) {
      byte[] extra = new byte[nvData.length - length];
      System.arraycopy(nvData, length, extra, 0, extra.length);
      memory.setPinmameData(extra);
    }
    return memory;
  }

  public byte[] getDotNv(NVRamMap mapJson, SparseMemory memory) {
    NVRamRegion nvramArea = mapJson.getMemoryArea(null, "nvram");
    int address = BcdUtils.toInt(nvramArea.getAddress());
    SparseMemory.MemoryRegion nvramMem = memory.findRegion(address);

    byte[] dotNv = Arrays.copyOf(nvramMem.data, nvramMem.data.length);
    byte[] pinmameData = memory.getPinmameData();
    if (pinmameData != null) {
      byte[] combined = new byte[dotNv.length + pinmameData.length];
      System.arraycopy(dotNv, 0, combined, 0, dotNv.length);
      System.arraycopy(pinmameData, 0, combined, dotNv.length, pinmameData.length);
      return combined;
    }
    return dotNv;
  }

  public String lastPlayed(NVRamMap mapJson, SparseMemory memory, Locale locale) {
    NVRamMapping lp = mapJson.getLastPlayed();
    if (lp == null) return null;
    return lp.formatEntry(memory, locale);
  }

  public int nvramBaseAddress(NVRamMap mapJson) {
    NVRamPlatform platform = mapJson.getRamPlatform();
    List<NVRamRegion> layout = platform.getMemoryLayout();
    for (NVRamRegion region : layout) {
      if ("nvram".equals(region.getType())) {
        return BcdUtils.toInt(region.getAddress());
      }
    }
    return 0;
  }
  //============================================ Downloaders ======

  @Override
  public boolean isSupportedRom(String rom) {
    try {
      ensureCacheMapForRom();
      return cacheMapForRom.containsKey(rom);
    }
    catch (IOException ioe) {
      LOG.error("Cannot get supported roms: {}", ioe.getMessage());
      return false;
    }
  }

  //@Override
  public Set<String> getSupportedRoms() {
    try {
      ensureCacheMapForRom();
      return cacheMapForRom.keySet();
    }
    catch (IOException ioe) {
      LOG.error("Cannot get supported NVRams: {}", ioe.getMessage());
      return Collections.emptySet();
    }
  }

  private void ensureCacheMapForRom() throws IOException {
    if (cacheMapForRom == null) {
      String indexUrl = "index.json";
      LOG.info("Load cache of rom map from {}", indexUrl);

      cacheMapForRom  = getStream(indexUrl, in -> {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(in, new TypeReference<Map<String, String>>() {});
      });
      LOG.info("Rom Cache loaded with {} roms", cacheMapForRom.size());
    }
  }

  public String mapPathForRom(String rom) throws IOException {
    ensureCacheMapForRom();
    return cacheMapForRom.get(rom);
  }

  /**
   * A replacement method to getMap() that take a local map File and returns the NVramMap
   */
  public NVRamMap getLocalMap(File map, String rom) throws IOException {
    if (map.exists()) {
      ObjectMapper mapper = new ObjectMapper();
      try (FileInputStream in = new FileInputStream(map)) {
        NVRamMap mapJson = mapper.readValue(in, NVRamMap.class);

        // initiate mappings
        if (mapJson != null) {
          initMap(mapJson);

          String romname = romName(rom);
          mapJson.setRom(rom, romname);
          mapJson.setMapPath(map.getAbsolutePath());
          return mapJson;
        }
      }
    }
    // else not found
    return null;
  }

  /**
   * Download the map of the given rom
   */
 public NVRamMap getMap(@NonNull String rom) throws IOException {
    NVRamMap mapJson = cacheNVRamMap.get(rom);
    if (mapJson != null) {
      return mapJson;
    }
    String mapPath = mapPathForRom(rom);
    if (mapPath != null) {
      mapJson= getMapFromPath(mapPath);
      String romname = romName(rom);
      mapJson.setRom(rom, romname);
      mapJson.setMapPath(mapPath);

      cacheNVRamMap.put(rom, mapJson);
      return mapJson;
    } 
    else {
      LOG.warn("Couldn't find a map for rom {}", rom);
      return null;
    }
  }

  public NVRamMap getMapFromPath(String mapPath) throws IOException {
    NVRamMap mapJson =  getStream(mapPath, in -> {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(in, NVRamMap.class);
    });

    // initiate mappings
    if (mapJson != null) {
      initMap(mapJson);
    }

    return mapJson;
}

	/**
	 * Take a rom and return its name from romnames.json
	 */
  public String romName(String rom) {
    if (cacheRomNames == null) {
      try {
        String romnames = "romnames.json";
        cacheRomNames = getStream(romnames, in -> {
          ObjectMapper mapper = new ObjectMapper();
          return mapper.readValue(in, new TypeReference<Map<String, String>>() {});
        });
      }
      catch (IOException ioe) {
        return rom;
      }
    }
    return cacheRomNames.getOrDefault(rom, "(Unknown ROM " + rom + ")");
  }

  private <T> T getStream(String u, ProcessStream<T> consumer) throws IOException {
    try (FileInputStream in = new FileInputStream(new File(mapFolder, u))) {
      return consumer.process(in);
    }
  }

  public <T> T download(String u, ProcessStream<T> consumer) throws IOException {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(u);
      connection = (HttpURLConnection) url.openConnection();
      int code = connection.getResponseCode();
      if (code == 200) {
        InputStream in = url.openStream();
        return consumer.process(in);
      }
      return null;
    }
    finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @FunctionalInterface
  public interface ProcessStream<T> {
    T process(InputStream in) throws IOException;
  }
}
