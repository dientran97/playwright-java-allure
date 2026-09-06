package framework.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Small file system helpers shared by the screenshot and download managers.
 */
public final class FileUtils {

    private static final String ILLEGAL_CHARACTERS = "[\\\\/:*?\"<>|\\r\\n\\t]";

    private FileUtils() {
    }

    /**
     * Makes a value safe to use as a file or folder name on every operating system.
     *
     * @param value raw value, i.e. a suite name or a screenshot description
     * @return the value without the characters forbidden by Windows, trimmed, never empty
     */
    public static String sanitize(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unnamed";
        }
        final String cleaned = value.trim()
                .replaceAll(ILLEGAL_CHARACTERS, "-")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? "unnamed" : cleaned;
    }

    /**
     * Deletes every file and sub folder of a directory but keeps the directory itself.
     * The directory is created when it does not exist.
     *
     * @param directory directory to empty
     * @return the same directory, guaranteed to exist and to be empty
     */
    public static Path cleanDirectory(final Path directory) {
        try {
            if (Files.exists(directory)) {
                try (Stream<Path> children = Files.walk(directory)) {
                    children.sorted(Comparator.reverseOrder())
                            .filter(path -> !path.equals(directory))
                            .forEach(FileUtils::deleteQuietly);
                }
            }
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to clean directory " + directory, e);
        }
        return directory;
    }

    /**
     * Deletes a single path, ignoring any error.
     *
     * @param path file or empty directory to delete
     */
    public static void deleteQuietly(final Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort only, a locked file must never fail a test case
        }
    }

    /**
     * Reads a file into memory, typically to attach it to the Allure report.
     *
     * @param path file to read
     * @return the file content, or an empty array when the file cannot be read
     */
    public static byte[] readAllBytesQuietly(final Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
