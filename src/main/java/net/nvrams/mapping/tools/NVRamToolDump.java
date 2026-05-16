package net.nvrams.mapping.tools;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import net.nvrams.mapping.map.ChecksumMapping;
import net.nvrams.mapping.map.NVRamMap;
import net.nvrams.mapping.map.NVRamMapping;
import net.nvrams.mapping.map.NVRamMappings;
import net.nvrams.mapping.map.NVRamScoreMapping;
import net.nvrams.mapping.map.SparseMemory;
import org.apache.commons.lang3.Strings;

public class NVRamToolDump {

  /**
   * A method to dump the totality of the nvram
   */
  public String dump(NVRamMap mapJson, SparseMemory memory, Locale locale, boolean verifyChecksums) throws IOException {
    Appendable bld = new StringBuilder(3000);
    printLine(bld, "Using " + mapJson.getMapPath() + " for " + mapJson.getRom() + ".nv");
    printLine(bld, "Dumping known entries for " + mapJson.getRom() + ".nv [" + mapJson.getRomName() + "]...");

    // audits and adjustments
    dumpMapOfMappings(bld, "audits", mapJson.getAudits(), memory, locale);
    dumpMapOfMappings(bld, "adjustments", mapJson.getAdjustments(), memory, locale);

    // game_state
    if (mapJson.getGameState() != null) {
      dumpMappings(bld, "Game State", mapJson.getGameState().getMappings(), memory, locale);
    }
    // dip_switches
    if (memory.getDipswData() != null) {
      dumpMappings(bld, "DIP Switches", mapJson.getDipSwitches(), memory, locale); 
    }
    dumpScores(bld, "high_scores", mapJson.getHighScores(), memory, locale);
    dumpScores(bld, "mode_champions", mapJson.getModeChampions(), memory, locale);

    NVRamMapping lp = mapJson.getLastPlayed();
    if (lp != null) {
      String played = lp.formatEntry(memory, locale);
      printLine(bld, "Last Played: " + played);
    }

    if (verifyChecksums) {
      for (ChecksumMapping checksum : mapJson.getChecksumEntries()) {
        String calc = checksum.formatValue(checksum.calculate(memory), locale);
        String stored = checksum.formatValue(checksum.getValue(memory), locale);
        if (!calc.equals(stored)) {
          printLine(bld, "checksum at 0x%X: %s != %s %s", locale,
              checksum.getStart(), calc, stored, checksum.getLabel());
        }
      }
    }
    return bld.toString();
  }

  //-----------------------------------------------

  /**
   * A method that dump scores, same model as pinhemi
   * 
   */
  public String dumpScores(NVRamMap mapJson, SparseMemory memory, Locale locale, boolean displayLabel) {
    StringBuilder bld = new StringBuilder(3000);

    List<NVRamScoreMapping> scores  = mapJson.getHighScores();
    int pos = 1;
    String currentLabel = null;
    for (NVRamScoreMapping score : scores) {
      String lbl = score.formatLabel(false);
      if (displayLabel && lbl != null && !Strings.CI.equals(currentLabel, lbl)) {
        if (pos++ > 1) {
          bld.append(System.lineSeparator());  
        }
        bld.append(lbl).append(System.lineSeparator());
        currentLabel = lbl;
      }

      bld.append(score.formatHighScore(memory, locale));
      bld.append(System.lineSeparator());
    }

    //mapJson.getModeChampions()

    return bld.toString();
  }


  //============================================================

  private void dumpMapOfMappings(Appendable bld, String section, Map<String, NVRamMappings> sectionMap,
        SparseMemory memory, Locale locale) throws IOException {
    if (sectionMap != null) {

      List<String> groups = new ArrayList<>(sectionMap.keySet());
      Collections.sort(groups);
      for (String group : groups) {
        if (group.startsWith("_")) continue;

        printGroupName(bld, group);

        NVRamMappings mapMap = sectionMap.get(group);
        List<String> keys = mapMap.keySet();
        Collections.sort(keys);
        for (String entryKey : keys) {
          if (entryKey.startsWith("_")) continue;
          NVRamMapping entry = mapMap.get(entryKey);
          dumpMapping(bld, memory, locale, entry, entryKey);
        }
      }
    }
  }

  private void dumpMappings(Appendable bld, String group, Map<String,NVRamMapping> sectionMap, 
      SparseMemory memory, Locale locale) throws IOException {
    if (sectionMap != null) {

      printGroupName(bld, group);

      for (String entryKey : sectionMap.keySet()) {
        if (entryKey.startsWith("_")) continue;
        NVRamMapping entry = sectionMap.get(entryKey);
        dumpMapping(bld, memory, locale, entry, null);
      }
    }
  }

  private void dumpMappings(Appendable bld, String group, List<NVRamMapping> mappings, 
      SparseMemory memory, Locale locale) throws IOException {
    if (mappings != null) {

      printGroupName(bld, group);

      for (NVRamMapping mapEntry : mappings) {
        dumpMapping(bld, memory, locale, mapEntry, null);
      }
    }
  }

  private void dumpScores(Appendable bld, String group, List<NVRamScoreMapping> scores, 
      SparseMemory memory, Locale locale) throws IOException {
    if (scores != null) {

      printGroupName(bld, group);

      for (NVRamScoreMapping score : scores) {
        String value = "";
        if (StringUtils.isNotEmpty(score.getLabel())) {
          value += score.getLabel() + ": ";
        }
        String initials = score.getInitials(memory);
        if (StringUtils.isNotEmpty(initials)) {
          value += initials + " ";
        }
        String val = score.formatValue(memory, locale);
        if (StringUtils.isNotEmpty(val)) {
          value += val;
        }

        printLine(bld, value.trim());
      }
    }
  }


  private void dumpMapping(Appendable bld, SparseMemory memory, Locale locale,
      NVRamMapping entry, String entryKey) throws IOException {
    String value = entry.formatEntry( memory, locale);
    String lbl = "";
    if (entryKey != null) {
      if (value == null) value = Objects.toString(entry.getDefaultVal());
      lbl = entry.formatLabel(entryKey, false);
    } 
    else {
      lbl = entry.formatLabel(null, false);
    }
    if (value != null) {
      printLine(bld, lbl + ": " + value);
    }
  }

  private void printGroupName(Appendable bld, String group) throws IOException {
    printLine(bld, "");
    printLine(bld, group);
    printLine(bld, "-".repeat(group.length()));
  }

  private void printLine(Appendable bld, String line) throws IOException {
    bld.append(line).append("\n");
  }

  private void printLine(Appendable bld, String format, Locale locale, Object... args) throws IOException {
    try (Formatter formatter = new Formatter(bld)) {
      formatter.format(locale, format, args);
    }
    catch (UnknownFormatConversionException ufce) {
      System.out.println("error in format " + ufce.getMessage());
    }
    bld.append("\n");
  }
}
