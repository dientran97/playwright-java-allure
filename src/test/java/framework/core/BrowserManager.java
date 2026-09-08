package framework.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import framework.config.FrameworkConfig;
import framework.config.FrameworkPaths;
import framework.context.TestCaseContext;
import framework.logging.Log;
import framework.report.AllureEnvironmentWriter;
import framework.utils.FileUtils;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Starts and stops the browsers.
 *
 * <p>Nothing here is called from {@code BaseTest}: a test case decides itself when the browser must
 * be opened, through {@code PlaywrightActions.openBrowserAndNavigate(...)}. The teardown of
 * {@code BaseTest} only closes what is still open.</p>
 */
public final class BrowserManager {

    private BrowserManager() {
    }

    /**
     * Launches a browser and its context, and registers it in {@link PageManager}.
     *
     * @param alias    name under which the browser can be switched back to later,
     *                 {@link PageManager#DEFAULT_ALIAS} for the main one
     * @param username user for the HTTP basic authentication dialog, {@code null} to skip it
     * @param password password for the HTTP basic authentication dialog, {@code null} to skip it
     * @return the session that became active
     */
    public static BrowserSession open(final String alias, final String username, final String password) {
        final String browserName = FrameworkConfig.browser();
        final boolean headless = FrameworkConfig.headless();

        final Playwright playwright = Playwright.create();
        final BrowserType browserType = browserTypeOf(playwright, browserName);
        final Browser browser = launch(browserType, browserName, headless);

        final Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(FrameworkConfig.viewportWidth(), FrameworkConfig.viewportHeight())
                .setIgnoreHTTPSErrors(FrameworkConfig.ignoreHttpsErrors())
                .setAcceptDownloads(true);
        if (username != null && !username.isEmpty()) {
            // HTTP basic authentication, applied to every request of the context
            contextOptions.setHttpCredentials(username, password == null ? "" : password);
        }
        final BrowserContext context = browser.newContext(contextOptions);
        context.setDefaultTimeout(FrameworkConfig.defaultTimeoutMs());
        context.setDefaultNavigationTimeout(FrameworkConfig.defaultTimeoutMs());

        final BrowserSession session = new BrowserSession(alias, browserName, playwright, browser, context);
        PageManager.register(session);

        if (FrameworkConfig.tracingEnabled()) {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true).setSnapshots(true).setSources(false));
        }

        final Page page = context.newPage();
        session.setActivePage(page);
        PageManager.prepare(page);

        Log.info("Started {} {} (headless={}, alias={})", browserName, session.getBrowserVersion(),
                headless, alias);
        AllureEnvironmentWriter.recordBrowser(session);
        return session;
    }

    /**
     * Launches the browser, turning "this browser is not installed" into an error that says what to
     * do about it.
     *
     * @param browserType the Playwright engine to start
     * @param browserName the browser asked for
     * @param headless    whether the window must stay hidden
     * @return the running browser
     */
    private static Browser launch(final BrowserType browserType, final String browserName,
                                  final boolean headless) {
        try {
            return browserType.launch(launchOptions(browserName, headless));
        } catch (RuntimeException e) {
            final String channel = channelFor(browserName);
            if (channel == null || FrameworkConfig.browserExecutablePath() != null) {
                throw e;
            }
            throw new FrameworkException("Unable to start '" + browserName + "'. It runs the "
                    + channel + " browser installed on this machine, which was not found. Install "
                    + "it, or point browser.executable.path at its binary, or use -Dbrowser=chromium "
                    + "to run the Chromium build bundled with Playwright. Original error: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Stops the trace of a session, when tracing is enabled, and attaches it to the report.
     *
     * @param session the session about to be closed
     */
    public static void stopTracing(final BrowserSession session) {
        if (!FrameworkConfig.tracingEnabled()) {
            return;
        }
        try {
            final String fileName = TestCaseContext.currentOrEmpty()
                    .map(testCase -> FileUtils.sanitize(testCase.getTestCaseId()) + "_" + session.getAlias())
                    .orElse("trace_" + session.getAlias());
            final Path trace = FrameworkPaths
                    .ensureDirectory(FrameworkPaths.projectRoot().resolve("target/traces"))
                    .resolve(fileName + ".zip");
            session.getContext().tracing().stop(new Tracing.StopOptions().setPath(trace));
            Allure.addAttachment(fileName + " trace", "application/zip",
                    new ByteArrayInputStream(FileUtils.readAllBytesQuietly(trace)), ".zip");
        } catch (RuntimeException e) {
            Log.warn("Unable to store the Playwright trace: {}", e.getMessage());
        }
    }

    /**
     * Closes every browser opened by the current thread.
     */
    public static void closeAll() {
        PageManager.sessions().forEach(BrowserManager::stopTracing);
        PageManager.closeAll();
    }

    private static BrowserType.LaunchOptions launchOptions(final String browserName, final boolean headless) {
        final BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setDownloadsPath(downloadsPath());

        // An explicit binary wins over the channel: Playwright refuses to be given both.
        final String executable = FrameworkConfig.browserExecutablePath();
        if (executable != null) {
            options.setExecutablePath(Path.of(executable));
        } else {
            final String channel = channelFor(browserName);
            if (channel != null) {
                options.setChannel(channel);
            }
        }
        final int slowMo = FrameworkConfig.getInt("slowmo", 0);
        if (slowMo > 0) {
            options.setSlowMo(slowMo);
        }
        final String extraArgs = FrameworkConfig.get("browser.args", "");
        if (!extraArgs.trim().isEmpty()) {
            options.setArgs(Arrays.asList(extraArgs.trim().split("\\s+")));
        } else if (!headless && isChromium(browserName)) {
            options.setArgs(Collections.singletonList("--start-maximized"));
        }
        return options;
    }

    private static Path downloadsPath() {
        return FrameworkPaths.ensureDirectory(TestCaseContext.currentOrEmpty()
                .map(TestCaseContext::getDownloadDirectory)
                .orElseGet(() -> FrameworkPaths.downloadsRoot().resolve("unassigned")));
    }

    private static BrowserType browserTypeOf(final Playwright playwright, final String browserName) {
        switch (browserName) {
            case "firefox":
            case "ff":
                return playwright.firefox();
            case "webkit":
            case "safari":
                return playwright.webkit();
            case "chrome":
            case "chromium":
            case "edge":
            case "msedge":
                return playwright.chromium();
            default:
                throw new IllegalArgumentException("Unsupported browser '" + browserName
                        + "'. Supported values: " + supportedBrowsers());
        }
    }

    /**
     * Chooses the Playwright channel, i.e. the real browser installed on the machine, for a browser
     * name.
     *
     * <p>{@code chrome} drives Google Chrome and {@code edge} drives Microsoft Edge; only
     * {@code chromium} runs the Chromium build bundled with Playwright. {@code browser.channel} in
     * the environment file overrides the mapping, for a beta or dev channel for instance.</p>
     *
     * @param browserName the browser asked for
     * @return the channel to launch, or {@code null} to use the build bundled with Playwright
     */
    private static String channelFor(final String browserName) {
        final String configured = FrameworkConfig.browserChannel();
        if (configured != null) {
            return configured;
        }
        switch (browserName) {
            case "chrome":
                return "chrome";
            case "edge":
            case "msedge":
                return "msedge";
            default:
                // chromium, firefox and webkit run the build Playwright ships with
                return null;
        }
    }

    private static boolean isChromium(final String browserName) {
        return "chrome".equals(browserName) || "chromium".equals(browserName)
                || "edge".equals(browserName) || "msedge".equals(browserName);
    }

    private static List<String> supportedBrowsers() {
        return Arrays.asList("chrome", "chromium", "edge", "firefox", "webkit");
    }
}
