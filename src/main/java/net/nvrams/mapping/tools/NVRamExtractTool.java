package net.nvrams.mapping.tools;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;

public class NVRamExtractTool {

  private static Robot robot;
  static {
    try {
      robot = new Robot();
    }
    catch (Exception e) {
      System.out.println("Failed to create robot: " + e.getMessage());
    }
  }


  public static final void main(String[] args) {
    NVRamExtractTool tool = new NVRamExtractTool();

    String table = "Amazon Hunt (Gottlieb 1983)";
    String rom = "amazonh";
    String altrom = "amazonh";

    try {
      //tool.runAll(table, rom);
      tool.run1(table, rom, altrom);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unused")
  private void runAll(String table, String rom) throws Exception {
    try (FileReader r = new FileReader("c:/temp/_NVRAMS/roms.txt");
          BufferedReader br = new BufferedReader(r)) {
      String line = null;
      while ((line = br.readLine()) != null) {
        run1(table, rom, line);
      }
    }
  }

  private void run1(String table, String rom, String altrom) throws Exception {
    File gameFile = new File("C:\\Visual Pinball\\tables\\" + table +  ".vpx");
    File vbsFile = new File(gameFile.getParentFile(), FilenameUtils.getBaseName(gameFile.getName()) + ".vbs");
    String script;
    if (vbsFile.exists()) {
      script = Files.readString(vbsFile.toPath());
    }
    else {
      script = VPXUtil.exportVBS(gameFile, true);
    }

    String script2 = script.replace(rom, altrom);
    Files.writeString(vbsFile.toPath(), script2);

    launchGame(gameFile);

    // set script back
    Files.writeString(vbsFile.toPath(), script);
  }


  private void launchGame(File gameFile) throws Exception {
    File vpxExe = new File("C:\\Visual Pinball\\VPinballX64.exe");
    List<String> strings = new ArrayList<>();
    strings.add(vpxExe.getAbsolutePath());
    strings.add("-Minimized");
    strings.add("-Play");
      
    strings.add("\"" + gameFile.getAbsolutePath() + "\"");

    Process process = new ProcessBuilder(strings).start();
    process.waitFor(30, TimeUnit.SECONDS);
   
    robot.keyPress(KeyEvent.VK_ESCAPE);
    Thread.sleep(100);
    robot.keyRelease(KeyEvent.VK_ESCAPE);
  }

}
