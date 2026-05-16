package net.nvrams.mapping.tools;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;

public class VPXUtil {
  private final static Logger LOG = LoggerFactory.getLogger(VPXUtil.class);
  private final static String VPX_TOOL_EXE = "vpxtool.exe";

  public static String exportVBS(@NonNull File vpxFile, boolean keepVbsFile) throws Exception {
    String error = null;
    try {
      File vbsFile = new File(vpxFile.getParentFile(), FilenameUtils.getBaseName(vpxFile.getName()) + ".vbs");
      if (vbsFile.exists()) {
        vbsFile.delete();
      }
      String vpxFilePath = "\"" + vpxFile.getAbsolutePath() + "\"";
      List<String> cmds = Arrays.asList("cmd.exe", "/c", VPX_TOOL_EXE, "extractvbs", vpxFilePath);
      LOG.info("VBS Export CMD: {}", String.join(" ", cmds));

      execute(cmds, new File("tools"));

      String script = org.apache.commons.io.FileUtils.readFileToString(vbsFile, Charset.defaultCharset());
      if (!keepVbsFile && !vbsFile.delete()) {
        LOG.error("Failed to delete VBS export file {}", vbsFile.getAbsolutePath());
      }
      return script;
    }
    catch (Exception e) {
      LOG.error("Exporting VBS failed for {}: {} - {}", vpxFile.getAbsolutePath(), error, e.getMessage(), e);
      throw new Exception("Exporting VBS failed for \"" + vpxFile.getAbsolutePath() + "\": " + error);
    }
  }

  public static Map<String, String> getRomnames(@NonNull File mameFolder) {
    return listroms(mameFolder, "-listfull");
  }

  public static Map<String, String> getClones(@NonNull File mameFolder) {
    return listroms(mameFolder, "-listclones");
  }

  private static Map<String, String> listroms(@NonNull File mameFolder, String option) {
    TreeMap<String, String> roms = new TreeMap<>();
    try {
      File mameExe = new File(mameFolder, "PinMAME.exe");
      if (mameExe.exists()) {
        List<String> cmds = Arrays.asList("cmd.exe", "/c", mameExe.getName(), option);
        LOG.info("Executing ROM validation: {}", String.join(" ", cmds));

        List<String> result = execute(cmds, mameFolder);
        for (String s : result) {
          int pos1 = s.indexOf('"');
          int pos2 = s.indexOf(' ');
          int pos = pos1 < 0 ? pos2 : pos2 < 0 ? pos1 : Math.min(pos1, pos2);
          if (pos > 0) {
            roms.put(s.substring(0, pos).trim(), s.substring(pos).trim());
          }
        }
      }
    }
    catch (Exception e) {
      LOG.error("Cannot list roms: {}", e.getMessage(), e);
    }
    return roms;
  }
  
  private static List<String> execute(List<String> commands, File dir) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(commands)
      .directory(dir)
      .redirectErrorStream(true);
    Process process = pb.start();

    InputStream stdOut = process.getInputStream();

    List<String> out = new ArrayList<>();
    try (BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(stdOut))) {
      String line;
      while ((line = stdOutReader.readLine()) != null) {
        out.add(line);
      }
    }
    process.waitFor();
    return out;
  }
}
