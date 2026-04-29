package net.nvrams.mapping.tools;

import net.nvrams.mapping.map.NVRamMapParser;
import net.nvrams.mapping.pinemhi.NVRamPinemhiParser;
import net.nvrams.mapping.superhac.NVRamSuperhacParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;

/**
 * A Tool that compares NVRam support between pinemHi, superhac and the Nvram-Map project
 */
public class NVRamParserCompareTool {

  public static void main(String[] args) throws IOException {

    File mameFolder = new File("C:/Visual Pinball/VPinMAME");

    Map<String, String> roms = VPXUtil.getRomnames(mameFolder);
    Map<String, String> clones = VPXUtil.getClones(mameFolder);

    NVRamMapParser parser = new NVRamMapParser("resources/maps");
    NVRamPinemhiParser pinemhi = new NVRamPinemhiParser("resources/pinemhi/");
    NVRamSuperhacParser superhac = new NVRamSuperhacParser("resources/superhac/roms.json");

    File nvramsFolder = new File("nvrams");

    File[] testFolders = new File[] { 
      new File("C:/temp/_NVRAMS/Matt"),
      new File("C:/temp/_NVRAMS/ed209"),
      new File("C:/temp/_NVRAMS/YabbaDabbaDoo"),
      new File("C:/temp/_NVRAMS/gonzonia"),
      new File("C:/temp/_NVRAMS/GerhardK"),
      new File("C:/temp/_NVRAMS/Buffdriver"),
      new File("C:/temp/_NVRAMS/BostonBuckeye"),
      new File("C:/temp/_NVRAMS/FuFu"),
      new File("C:/temp/_NVRAMS/Blap"),
      new File("C:/Github/py-pinmame-nvmaps/test/nvram"),
      new File("C:/Visual Pinball/VPinMAME/nvram"),         // OLE
      new File("C:/Github/vpin-studio/testsystem/vPinball/VisualPinball/VPinMAME/nvram/"),
      new File("C:/Github/vpin-studio/resources/nvrams")    // resetted nvrams
    };

    try (PrintWriter w = new PrintWriter("allroms.csv")) {
      w.println("\"rom\",\"Name\",\"clone of\",\"pinemHi\",\"tomslogic\",\"superhac\",\"nvrams\"");
      for (String s : roms.keySet()) {
        String cloneOf = clones.get(s);
        boolean supportByPinemhi = pinemhi.isSupportedRom(s);
        boolean supportByNVRams = parser.isSupportedRom(s);
        boolean supportBySuperhac = superhac.isSupportedRom(s);

        boolean cloneByNVRams = parser.isSupportedRom(cloneOf);
        boolean cloneBySuperhac = superhac.isSupportedRom(cloneOf);

        File nvram = new File(nvramsFolder, s + ".nv");
        File nvramClone = new File(nvramsFolder, cloneOf + ".nv");
        boolean nvramExists = nvram.exists();
        boolean nvramCloneExists = nvramClone.exists();

        if (!supportByNVRams && !supportBySuperhac && !cloneByNVRams && !cloneBySuperhac && !nvramExists && !nvramCloneExists) {
          w.print(s + ",");
          w.print(roms.get(s) + ",");

          w.print((cloneOf != null? cloneOf: "") + ",");

          w.print((supportByPinemhi ? "x": "") + ",");
          w.print((supportByNVRams ? "x": "") + ",");
          w.print((supportBySuperhac ? "x": "") + ",");

          if (nvramExists) {
            w.println("OK");
          } else {
            boolean copied = false;
            for (File folder : testFolders) {
              File entry = new File(folder, s + ".nv");
              if (entry.exists()) {
                Files.copy(entry.toPath(), nvram.toPath());
                w.println("COPIED");
                copied = true;
                break;
              }
            }
            if (!copied) {
              w.println("KO!!");
            }
          }
        }
      }
    }
  }
}
