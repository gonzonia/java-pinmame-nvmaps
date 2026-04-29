package net.nvrams.mapping;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class BaseParserTest {

  protected static final int STATUS_NOT_RUN = -1;
  protected static final int STATUS_SUCCESS = 0;
  protected static final int STATUS_FAILED = 1;
  protected static final int STATUS_NEW = 2;


  protected List<String> doTestAllFiles(NVRamParser parser, String firstRom, List<String> ignoreList) throws Exception {
    File testFolder = new File("nvrams");

    File[] files = testFolder.listFiles((dir, name) -> name.endsWith(".nv"));
    int count = 0;
    List<String> failedList = new ArrayList<>();
    List<String> createdList = new ArrayList<>();
    for (File entry : files) {
      //if (!entry.getName().equals("tmac_a24.nv")) {
      //  continue;
      //}
      if (ignoreList.contains(entry.getName()) || firstRom != null && entry.getName().compareTo(firstRom) < 0) {
        continue;
      }

      int status = doTestOneFile(parser, entry);

      if (status == STATUS_FAILED) {
        failedList.add(entry.getName());
      }
      else if (status == STATUS_NEW) {
        createdList.add(entry.getName());
      }
      count++;
    }

    System.out.println("Tested " + count + " entries, " + failedList.size() + " failed, " + createdList.size() + " new list files created.");
    for (String item : failedList) {
      System.out.println("  '" + item + "' failed.");
    }
    for (String item : createdList) {
      System.out.println("  '" + item + "' created.");
    }

    return failedList;
  }

  protected int doTestOneFile(NVRamParser parser, String filename) throws Exception {
    File entry = new File("nvrams", filename);
    return doTestOneFile(parser, entry);
  }

  protected int doTestOneFile(NVRamParser parser, File entry) throws Exception {
    int status = STATUS_NOT_RUN;
    String rom = entry.getName().replace(".nv", "");

    if (!parser.isSupportedRom(rom)) {
      return status;
    }

    System.out.println("Testing " + rom);

    Locale loc = Locale.GERMANY;

    List<String> raw = parser.getRaw(rom, entry, loc);
    assertFalse(raw.isEmpty(), "Empty raw  for nvram " + entry.getAbsolutePath());

    List<NVRamScore> parse = parser.parseRaw(rom, raw, loc, false);
    assertFalse(parse.isEmpty(), "Found empty highscore for nvram " + entry.getAbsolutePath());

    int maxSize = -1;
    for (NVRamScore score : parse) {
      maxSize = Math.max(maxSize, score.getFormattedScore(loc).length());
    }

    StringBuilder scoreList = new StringBuilder();
    int position = 1;
    for (NVRamScore score : parse) {
      String disp = "#" + position + " " +  (parse.size() > 9 && position < 10 ? " " : "")
        + score._getPlayerInitials() + "   " + StringUtils.leftPad(score.getFormattedScore(loc), maxSize);
      scoreList.append(disp + System.lineSeparator());
      position++;
    }

    File listFile = new File("nvramslist", entry.getName() + ".list");
    if (listFile.exists()) {
      // compare with test output
      String fileContents = Files.readString(listFile.toPath(), StandardCharsets.UTF_8);
      if (!fileContents.equals(scoreList.toString())) {
        status = STATUS_FAILED;
        System.out.println(fileContents);
        System.out.println("---");
        System.out.println(scoreList.toString());
      } else {
        status = STATUS_SUCCESS;
      }
    }
    else {
      // create for next test
      listFile.createNewFile();
      try (FileWriter writer = new FileWriter(listFile, StandardCharsets.UTF_8)) {
        writer.write(scoreList.toString());
      }
      status = STATUS_NEW;
    }

    System.out.println("Parsed " + parse.size() + " score entries.");
    System.out.println("*******************************************************************************************");
    return status;
  }
}
