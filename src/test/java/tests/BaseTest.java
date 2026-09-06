package tests;

import framework.actions.PlaywrightActions;
import framework.assertions.SoftAssertions;
import framework.config.FrameworkConfig;
import framework.config.UserConfig;
import framework.context.TestCaseContext;
import framework.listeners.FrameworkListener;
import framework.logging.StepTracker;
import framework.report.AllureEnvironmentWriter;
import framework.report.DownloadManager;
import framework.report.ScreenshotManager;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.lang.reflect.Method;

/**
 * Base class of every test case.
 *
 * <p>It deliberately <strong>does not open any browser</strong>. Opening the browser is a step of
 * the test case itself ({@code PlaywrightActions.openBrowserAndNavigate(url, username, password)}),
 * which keeps that step visible in the Allure report and lets a test case decide which url and
 * which credentials it needs. The base class only:</p>
 * <ul>
 *     <li>prepares the context of the test case before each method: suite name, URS, test case id,
 *         screenshot folder and a freshly emptied download folder;</li>
 *     <li>closes every browser after each method.</li>
 * </ul>
 *
 * <p>Because everything is resolved from the {@code @TestCaseInfo} annotation when no TestNG xml is
 * involved, a test class can be started straight from the IDE with "Run" or "Debug".</p>
 */
@Listeners(FrameworkListener.class)
public abstract class BaseTest {

    /**
     * Prepares the test case: resets the per test case state, resolves the names and the folders,
     * and empties the download folder of the test case.
     *
     * @param context TestNG context of the running {@code <test>} tag, injected by TestNG
     * @param method  the test method that is about to run, injected by TestNG
     */
    @BeforeMethod(alwaysRun = true)
    public void prepareTestCase(final ITestContext context, final Method method) {
        StepTracker.clear();
        SoftAssertions.resetCurrent();
        DownloadManager.reset();
        ScreenshotManager.resetFailureTracking();
        TestCaseContext.start(context, method);
        AllureEnvironmentWriter.recordRunInfo();
    }

    /**
     * Closes every browser the test case opened and clears the per test case state.
     */
    @AfterMethod(alwaysRun = true)
    public void closeBrowserAfterTestCase() {
        try {
            PlaywrightActions.closeBrowser();
        } finally {
            TestCaseContext.clear();
            StepTracker.clear();
        }
    }

    /**
     * @return the user selected for this run with {@code -Duser=...}
     */
    protected UserConfig user() {
        return UserConfig.current();
    }

    /**
     * @return the entry point url declared by the active environment file
     */
    protected String url() {
        return FrameworkConfig.baseUrl();
    }

    /**
     * @return the soft verification collector of the current thread; the framework verifies it
     *         automatically when the test method ends
     */
    protected SoftAssertions softly() {
        return SoftAssertions.current();
    }

    /**
     * @return the metadata of the running test case: suite name, URS, test case id and folders
     */
    protected TestCaseContext testCase() {
        return TestCaseContext.current();
    }

    /**
     * Opens the browser on the url of the environment file and authenticates the HTTP basic
     * dialog with the user of the run. Shortcut for the very common first step of a test case.
     */
    protected void openApplication() {
        PlaywrightActions.openBrowserAndNavigate(url(), user().getUsername(), user().getPassword());
    }
}
