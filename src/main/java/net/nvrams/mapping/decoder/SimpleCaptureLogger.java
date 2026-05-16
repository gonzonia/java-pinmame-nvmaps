package net.nvrams.mapping.decoder;

import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SimpleCaptureLogger extends SimpleLogger {

    private final StringBuilder writer = new StringBuilder();

    public SimpleCaptureLogger(Class<?> clazz, LEVEL minimumLevel) {
        super(clazz, minimumLevel);
    }

    @Override
    public void log(LEVEL level, String msg, Object[] args, Throwable t) {
        if (writer != null) {
            // Use native SLF4J formatting instead of String.replace
            String formattedMessage = (args != null && args.length > 0)
                    ? MessageFormatter.arrayFormat(msg, args).getMessage()
                    : msg;

            writer.append(formattedMessage).append("\n");

            if (t != null) {
                // Native Java stack trace extraction (No Apache Commons needed)
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                writer.append(sw.toString()).append("\n");
            }
        }
    }

    public void reset() {
        writer.setLength(0);
    }

    public String getText() {
        return writer.toString();
    }
}