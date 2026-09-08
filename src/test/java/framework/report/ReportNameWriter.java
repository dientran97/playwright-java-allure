package framework.report;

import framework.logging.Log;
import framework.utils.FileUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Publishes the name the Allure report file must take: the test suite name declared by
 * {@code <suite name="...">}.
 *
 * <p>Maven cannot read that name itself, it lives inside the TestNG xml. So the framework writes it
 * to the properties file named by the {@code report.name.file} system property (exported by the
 * pom), and the build reads it back when it copies the report to
 * {@code reports/<suite name>.html}. When the file is missing - the suite never started, or the run
 * was not launched by Maven - the build falls back on the name of the xml file.</p>
 */
public final class ReportNameWriter {

    /** System property, set by the pom, holding the file to write the report name to. */
    public static final String TARGET_FILE_PROPERTY = "report.name.file";

    private static final String KEY = "report.name";

    private ReportNameWriter() {
    }

    /**
     * Writes the report name for the build to pick up. Does nothing when the run is not driven by
     * Maven, i.e. when {@code report.name.file} is not set.
     *
     * @param suiteName the {@code <suite name>} of the TestNG xml
     */
    public static void publish(final String suiteName) {
        final String target = System.getProperty(TARGET_FILE_PROPERTY);
        if (target == null || target.trim().isEmpty() || suiteName == null || suiteName.trim().isEmpty()) {
            return;
        }
        final Path file = Paths.get(target.trim());
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                // java.util.Properties format: only the backslash has to be escaped in a value
                writer.write(KEY + "=" + FileUtils.sanitize(suiteName).replace("\\", "\\\\"));
                writer.newLine();
            }
        } catch (IOException e) {
            Log.warn("Unable to publish the report name to {}: {}", file, e.getMessage());
        }
    }
}
