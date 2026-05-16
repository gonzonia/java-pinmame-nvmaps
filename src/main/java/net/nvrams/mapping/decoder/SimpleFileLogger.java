package net.nvrams.mapping.decoder;

import org.slf4j.helpers.MessageFormatter;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class SimpleFileLogger extends SimpleLogger implements Closeable {

    private PrintWriter writer;

    public SimpleFileLogger(Class<?> clazz, LEVEL minimumLevel, File file) {
        super(clazz, minimumLevel);
        try {
            // Ensure parent directory exists before attempting to create the file
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            // Modern NIO.2 File I/O with UTF-8 and Append mode
            BufferedWriter bw = Files.newBufferedWriter(
                    file.toPath(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            this.writer = new PrintWriter(bw, true); // true = auto-flush
        } catch (IOException e) {
            System.err.println("SimpleFileLogger failed to initialize log file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    @Override
    public void log(LEVEL level, String msg, Object[] args, Throwable t) {
        if (writer != null) {
            // Use native SLF4J formatting instead of String.replace
            String formattedMessage = (args != null && args.length > 0)
                    ? MessageFormatter.arrayFormat(msg, args).getMessage()
                    : msg;

            writer.println(formattedMessage);

            if (t != null) {
                t.printStackTrace(writer);
            }
        }
    }

    @Override
    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}