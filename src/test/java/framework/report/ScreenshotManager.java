package framework.report;

import com.microsoft.playwright.Page;
import framework.config.FrameworkConfig;
import framework.config.FrameworkPaths;
import framework.context.TestCaseContext;
import framework.core.PageManager;
import framework.logging.Log;
import framework.utils.FileUtils;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Takes the screenshots of the framework and files them under
 * {@code screenshots/<suite name>/<testcase ID>}.
 *
 * <p>Every image is named {@code <URS>_<testcase ID>_<description>_SS<index>.png} where the index
 * is a three digit counter that restarts at {@code 001} for each test case. The same bytes are
 * attached to the Allure report, so the picture is visible both on disk and inside the report.</p>
 */
public final class ScreenshotManager {

    /** Description used by the screenshot taken automatically when a step fails. */
    public static final String FAILURE_DESCRIPTION = "Screenshot at the fail step";

    /**
     * Failures already photographed on this thread. A failure that bubbles up through several
     * nested keywords is the same object every time, so it only produces one picture, while two
     * different failures of the same test case (soft assertions) each produce their own.
     */
    private static final ThreadLocal<Set<Throwable>> ALREADY_CAPTURED =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private ScreenshotManager() {
    }

    /**
     * Takes a screenshot of the visible part of the active page.
     *
     * @param description short text describing what the picture shows, used in the file name and
     *                    as the attachment title in the report
     * @return the path of the saved image, or an empty optional when no browser is open
     */
    public static Optional<Path> capture(final String description) {
        return capture(description, false);
    }

    /**
     * Takes a screenshot of the whole page, scrolling included.
     *
     * @param description short text describing what the picture shows
     * @return the path of the saved image, or an empty optional when no browser is open
     */
    public static Optional<Path> captureFullPage(final String description) {
        return capture(description, true);
    }

    /**
     * Takes a screenshot of the active page.
     *
     * @param description short text describing what the picture shows
     * @param fullPage    {@code true} to capture the entire scrollable page
     * @return the path of the saved image, or an empty optional when no browser is open
     */
    public static Optional<Path> capture(final String description, final boolean fullPage) {
        if (!PageManager.isBrowserOpen()) {
            Log.warn("Screenshot '{}' skipped: no browser is open", description);
            return Optional.empty();
        }
        try {
            final TestCaseContext testCase = TestCaseContext.current();
            final Path directory = FrameworkPaths.ensureDirectory(testCase.getScreenshotDirectory());
            final Path file = directory.resolve(buildFileName(testCase, description));
            final Page page = PageManager.page();
            final byte[] image = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(fullPage)
                    .setPath(file));
            Allure.addAttachment(description, "image/png", new ByteArrayInputStream(image), ".png");
            Log.info("Screenshot saved: {}", file);
            return Optional.of(file);
        } catch (RuntimeException e) {
            Log.warn("Unable to take the screenshot '{}': {}", description, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Takes the screenshot the framework attaches to a failing step, named
     * "Screenshot at the fail step". Nothing happens when the browser is already gone, which is
     * exactly the case the requirement excludes.
     *
     * @param cause the failure being reported; the same failure is never photographed twice, pass
     *              {@code null} to force a new picture
     * @return the path of the saved image, or an empty optional when it was skipped
     */
    public static Optional<Path> captureFailure(final Throwable cause) {
        if (!FrameworkConfig.screenshotOnFailure() || !PageManager.isBrowserOpen()) {
            return Optional.empty();
        }
        if (cause != null && !ALREADY_CAPTURED.get().add(cause)) {
            return Optional.empty();
        }
        return capture(FAILURE_DESCRIPTION, true);
    }

    /**
     * Forgets the failures already photographed. Called between two test cases.
     */
    public static void resetFailureTracking() {
        ALREADY_CAPTURED.get().clear();
        ALREADY_CAPTURED.remove();
    }

    /**
     * Builds the file name of the next screenshot of a test case.
     *
     * @param testCase    the running test case
     * @param description description of the picture
     * @return {@code <URS>_<testcase ID>_<description>_SS<index>.png}
     */
    private static String buildFileName(final TestCaseContext testCase, final String description) {
        return String.format("%s_%s_SS%03d.png",
                testCase.screenshotPrefix(),
                FileUtils.sanitize(description),
                testCase.nextScreenshotIndex());
    }
}
