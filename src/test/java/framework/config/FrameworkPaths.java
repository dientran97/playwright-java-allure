package framework.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Single place where every output folder of the framework is resolved.
 *
 * <p>All folders are rooted at the project directory so that a run started from Maven and a run
 * started from the IDE write to exactly the same place.</p>
 */
public final class FrameworkPaths {

    private FrameworkPaths() {
    }

    /**
     * Root folder of the project.
     *
     * @return the value of the {@code project.base.dir} system property (exported by Maven) or,
     *         when absent, the current working directory
     */
    public static Path projectRoot() {
        final String fromMaven = System.getProperty("project.base.dir");
        return Paths.get(isBlank(fromMaven) ? System.getProperty("user.dir", ".") : fromMaven)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Folder where the single file Allure reports are published.
     *
     * @return {@code <project>/reports} unless overridden by {@code -Dreports.dir}
     */
    public static Path reportsRoot() {
        return resolve("reports.dir", "reports");
    }

    /**
     * Folder where screenshots are stored.
     *
     * @return {@code <project>/screenshots} unless overridden by {@code -Dscreenshots.dir}
     */
    public static Path screenshotsRoot() {
        return resolve("screenshots.dir", "screenshots");
    }

    /**
     * Folder where downloaded files are stored.
     *
     * @return {@code <project>/downloads} unless overridden by {@code -Ddownloads.dir}
     */
    public static Path downloadsRoot() {
        return resolve("downloads.dir", "downloads");
    }

    /**
     * Folder where the raw Allure results (json/attachments) are written.
     *
     * @return {@code target/allure-results} unless overridden by {@code -Dallure.results.directory}
     */
    public static Path allureResults() {
        return resolve("allure.results.directory", "target/allure-results");
    }

    /**
     * Creates a directory (and its parents) when it does not exist yet.
     *
     * @param directory directory to create
     * @return the same directory, guaranteed to exist
     */
    public static Path ensureDirectory(final Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create directory " + directory, e);
        }
        return directory;
    }

    private static Path resolve(final String systemProperty, final String defaultRelativePath) {
        final String configured = System.getProperty(systemProperty);
        if (isBlank(configured)) {
            return projectRoot().resolve(defaultRelativePath);
        }
        final Path path = Paths.get(configured);
        return path.isAbsolute() ? path.normalize() : projectRoot().resolve(path).normalize();
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}
