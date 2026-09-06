package framework.report;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;
import framework.config.FrameworkPaths;
import framework.context.TestCaseContext;
import framework.logging.Log;
import framework.utils.FileUtils;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Routes every file the application downloads into {@code downloads/<suite name>/<testcase ID>}.
 *
 * <p>The folder is emptied when the test case starts (see {@code TestCaseContext}) so a test case
 * only ever sees the files it produced itself. The handler is installed on every page the framework
 * opens, popups included, therefore a download triggered from a second tab lands in the same
 * folder.</p>
 */
public final class DownloadManager {

    /** Files bigger than this are stored on disk but not attached to the Allure report. */
    private static final long MAX_ATTACHMENT_BYTES = 5L * 1024 * 1024;

    private static final Map<Download, Path> SAVED = new ConcurrentHashMap<>();

    private DownloadManager() {
    }

    /**
     * Installs the automatic download handler on a page.
     *
     * @param page the page to observe
     */
    public static void attachTo(final Page page) {
        page.onDownload(DownloadManager::save);
    }

    /**
     * Saves a download in the folder of the running test case. Calling it twice with the same
     * download is safe: the file is written only once.
     *
     * @param download the Playwright download event
     * @return the path of the saved file
     */
    public static Path save(final Download download) {
        return SAVED.computeIfAbsent(download, pending -> {
            final Path directory = FrameworkPaths.ensureDirectory(targetDirectory());
            final Path target = uniquePath(directory, FileUtils.sanitize(pending.suggestedFilename()));
            pending.saveAs(target);
            Log.info("Downloaded '{}' to {}", target.getFileName(), target);
            attachToReport(target);
            return target;
        });
    }

    /**
     * Lists what the running test case has downloaded so far.
     *
     * @return the files currently present in the download folder of the test case
     */
    public static List<Path> downloadedFiles() {
        final Path directory = targetDirectory();
        if (!Files.isDirectory(directory)) {
            return Collections.emptyList();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Forgets the downloads already handled. Called between two test cases.
     */
    public static void reset() {
        SAVED.clear();
    }

    private static Path targetDirectory() {
        return TestCaseContext.currentOrEmpty()
                .map(TestCaseContext::getDownloadDirectory)
                .orElseGet(() -> FrameworkPaths.downloadsRoot().resolve("unassigned"));
    }

    private static Path uniquePath(final Path directory, final String fileName) {
        Path candidate = directory.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }
        final int dot = fileName.lastIndexOf('.');
        final String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        final String extension = dot > 0 ? fileName.substring(dot) : "";
        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(base + "(" + counter++ + ")" + extension);
        }
        return candidate;
    }

    private static void attachToReport(final Path file) {
        try {
            if (Files.size(file) > MAX_ATTACHMENT_BYTES) {
                Allure.addAttachment("Downloaded file", "text/plain",
                        "Stored on disk (too large to attach): " + file, ".txt");
                return;
            }
            Allure.addAttachment(file.getFileName().toString(), "application/octet-stream",
                    new ByteArrayInputStream(Files.readAllBytes(file)), fileExtension(file));
        } catch (Exception e) {
            Log.warn("Unable to attach the downloaded file {} to the report: {}", file, e.getMessage());
        }
    }

    private static String fileExtension(final Path file) {
        final String name = file.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot) : "";
    }
}
