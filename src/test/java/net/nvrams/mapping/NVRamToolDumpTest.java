package net.nvrams.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nvrams.mapping.map.NVRamMap;
import net.nvrams.mapping.map.NVRamMapParser;
import net.nvrams.mapping.map.NVRamMapping;
import net.nvrams.mapping.map.SparseMemory;
import net.nvrams.mapping.tools.NVRamToolDump;

/**
 * Uses test data from https://github.com/tomlogic/py-pinmame-nvmaps
 * This project uses a snapshot version of https://github.com/tomlogic/pinmame-nvram-maps/
 */
public class NVRamToolDumpTest {

  public static final String TEST_ROOT = "https://github.com/tomlogic/py-pinmame-nvmaps/raw/refs/heads/main/test/";

  private NVRamMapParser parser = new NVRamMapParser("resources/maps");

  /** TODO commented as a bit long... */
  //@Test
  public void testAllDump() throws IOException {
    File indexJson = new File(parser.getMapFolder(), "index.json");

    // optional ROM, to start with, leave null for all
    String romStart = null; //"t2_l8";

    try (FileInputStream in = new FileInputStream(indexJson)) {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> values = mapper.readValue(in, new TypeReference<>() {});
      for (String key : values.keySet()) {
        if (romStart == null || key.compareTo(romStart) >= 0) {
          checkRom(parser, key, false);
        }
      }
    }
  }

  @Test
  public void testOneDump() throws IOException {
    checkRom(parser, "t2_l8", true);
  }

  private void checkRom(NVRamMapParser parser, String rom, boolean runAssert) throws IOException {
    System.out.println("checking " + rom + "...");
    parseNVRam(parser, rom, (mapJson, memory) -> {
      String result = TEST_ROOT + "expected/" + rom + ".nv.txt";
      try {
        parser.download(result, res -> {

          NVRamToolDump tool = new NVRamToolDump();
          String dump = tool.dump(mapJson, memory, Locale.ENGLISH, true);

          String expected = IOUtils.toString(res, StandardCharsets.UTF_8);
          // check files
          if (runAssert) {
            Assertions.assertEquals(expected, dump);
          }
          else {
            if (!expected.equals(dump)) {
              System.out.println("=> file are different for " + rom);
            }
          }

          return null;
        });
      }
      catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    });
  }

  //-------------------------------------------

  @Test
  public void testDumpPlayerCount() throws IOException {
    String rom = "bcats_l5";

    parseNVRam(parser, rom, (mapJson, memory) -> {
        NVRamMapping m = mapJson.getGameState().getPlayerCount();
        String e = m.formatEntry(memory, Locale.ENGLISH);
        Assertions.assertEquals("0", e);
    });
  }

  private void parseNVRam(NVRamMapParser parser, String rom, BiConsumer<NVRamMap, SparseMemory> consumer) throws IOException {
    String testnv = TEST_ROOT + "nvram/" + rom + ".nv";
    parser.download(testnv, in -> {
      byte[] bytes = IOUtils.toByteArray(in);
      NVRamMap mapJson = parser.getMap(rom);
      SparseMemory memory = parser.getMemory(mapJson, bytes);
      consumer.accept(mapJson, memory);
      return null;
    });
  }

  //-------------------------------------------

  @Test
  public void testDumpAlpok() throws IOException {
    doTestDump("alpok_l6", "1.000.000\n");
  }

  /** @fixme check how to limit nb of scores as buy-in scores are added at the moment */
  @Test
  public void testDumpAfm() throws IOException {
    doTestDump("afm_113b", "SLL   7.500.000.000\n" +
            "BRE   7.000.000.000\n" +
            "LFS   6.500.000.000\n" +
            "RCF   6.000.000.000\n" +
            "DTW   5.500.000.000\n" +
            "DWF   5.000.000.000\n" +
            "ASR   4.500.000.000\n" +
            "BCM   4.000.000.000\n" +
            "MOO   3.500.000.000\n");
  }


  private void doTestDump(String rom, String expected) throws IOException {
    File mainFolder = new File("nvrams");
    
    File entry = new File(mainFolder, rom + ".nv");
    byte[] bytes = Files.readAllBytes(entry.toPath());

    NVRamMap mapJson = parser.getMap(rom);
    SparseMemory memory = parser.getMemory(mapJson, bytes);

    NVRamToolDump dump = new NVRamToolDump();
    //String txt = dump.dump(mapJson, memory, Locale.ENGLISH, true);

    String rawscores = dump.dumpScores(mapJson, memory, Locale.GERMANY, false);

    Assertions.assertEquals(expected.replace("\n", System.lineSeparator()), rawscores);
  }
}
