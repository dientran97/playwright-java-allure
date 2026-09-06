package framework.report;

import framework.config.FrameworkConfig;
import framework.config.FrameworkPaths;
import framework.config.UserConfig;
import framework.core.BrowserSession;
import framework.logging.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes {@code allure-results/environment.properties}, the file the Allure report renders in its
 * "Environment" widget on the overview page.
 *
 * <p>It carries what the report must show about the run: the browser that was used, its version and
 * the host the suite was executed on, plus the environment, the user and the entry point url.</p>
 */
public final class AllureEnvironmentWriter {

    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private AllureEnvironmentWriter() {
    }

    /**
     * Records the static part of the run (environment, user, host, os, java) and writes the file.
     * Safe to call several times.
     */
    public static synchronized void recordRunInfo() {
        VALUES.put("Environment", FrameworkConfig.environment());
        VALUES.put("Base.URL", safe(FrameworkConfig::baseUrl));
        VALUES.put("Test.User", FrameworkConfig.userName());
        VALUES.put("Test.User.Login", safe(() -> UserConfig.current().getUsername()));
        VALUES.put("Execution.Host", executionHost());
        VALUES.put("Execution.OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        VALUES.put("Java.Version", System.getProperty("java.version"));
        VALUES.put("Suite.Xml", System.getProperty("suiteXmlFolder", "-")
                + "/" + System.getProperty("suiteXmlFile", "-"));
        VALUES.putIfAbsent("Browser", FrameworkConfig.browser());
        VALUES.putIfAbsent("Browser.Version", "not started yet");
        VALUES.put("Headless", String.valueOf(FrameworkConfig.headless()));
        write();
    }

    /**
     * Records the browser that has just been started, including the version reported by Playwright.
     *
     * @param session the session that was just opened
     */
    public static synchronized void recordBrowser(final BrowserSession session) {
        VALUES.put("Browser", session.getBrowserType());
        VALUES.put("Browser.Version", session.getBrowserVersion());
        VALUES.put("Headless", String.valueOf(FrameworkConfig.headless()));
        write();
    }

    /**
     * @return the host name the suite runs on, falling back on the {@code HOSTNAME} variable
     */
    public static String executionHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            final String fromEnvironment = System.getenv("HOSTNAME");
            return fromEnvironment == null ? "unknown" : fromEnvironment;
        }
    }

    /**
     * @return an unmodifiable copy of what has been recorded so far
     */
    public static synchronized Map<String, String> values() {
        return new LinkedHashMap<>(VALUES);
    }

    private static void write() {
        final Path file = FrameworkPaths.ensureDirectory(FrameworkPaths.allureResults())
                .resolve("environment.properties");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (final Map.Entry<String, String> entry : VALUES.entrySet()) {
                writer.write(entry.getKey() + "=" + escape(entry.getValue()));
                writer.newLine();
            }
        } catch (IOException e) {
            Log.warn("Unable to write {}: {}", file, e.getMessage());
        }
    }

    private static String escape(final String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\n", " ").replace(":", "\\:");
    }

    private static String safe(final ThrowingSupplier supplier) {
        try {
            final String value = supplier.get();
            return value == null ? "-" : value;
        } catch (RuntimeException e) {
            return "-";
        }
    }

    /**
     * Supplier that is allowed to blow up, used for the optional entries of the widget.
     */
    @FunctionalInterface
    private interface ThrowingSupplier {
        /**
         * @return the value to publish
         */
        String get();
    }
}
