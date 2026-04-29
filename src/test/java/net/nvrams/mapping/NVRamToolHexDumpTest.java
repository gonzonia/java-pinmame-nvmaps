package net.nvrams.mapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import net.nvrams.mapping.map.NVRamMap;
import net.nvrams.mapping.map.NVRamMapParser;
import net.nvrams.mapping.map.SparseMemory;
import net.nvrams.mapping.tools.NVRamToolHexDump;

/**
 * Take a rom and dump its hex characters, not really a test...
 */
public class NVRamToolHexDumpTest {

  @Test
  public void testDumpAlpok() throws IOException {
    doTestDump("alpok_l6");
  }

  @Test
  public void testDumpAfm() throws IOException {
    doTestDump("afm_113b");
  }

  private void doTestDump(String rom) throws IOException {
    File mainFolder = new File("nvrams");

    File entry = new File(mainFolder, rom + ".nv");

    byte[] bytes = Files.readAllBytes(entry.toPath());

    NVRamMapParser parser = new NVRamMapParser("resources/maps");
    NVRamMap mapJson = new NVRamMap();
    SparseMemory memory = parser.getMemory(mapJson, bytes);
    NVRamToolHexDump dump = new NVRamToolHexDump();
    String txt = dump.hexDump(mapJson, memory, Locale.ENGLISH);

    System.out.println(txt);
  }
}
