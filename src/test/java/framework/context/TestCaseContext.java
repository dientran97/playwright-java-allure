package framework.context;

import framework.annotations.TestCaseInfo;
import framework.config.FrameworkConfig;
import framework.config.FrameworkPaths;
import framework.logging.Log;
import framework.utils.FileUtils;
import org.testng.ITestContext;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Everything the framework needs to know about the test case that is currently running:
 * its suite name, its URS, its test case id, where its screenshots and downloads go and how many
 * screenshots it has produced so far.
 *
 * <p>The values come from the TestNG xml when the run is driven by a suite file
 * ({@code <suite name="SP0308_3.2.1.1_Login">} / {@code <test name="3.2.1.1_TC001_...">}) and from
 * the {@link TestCaseInfo} annotation when a single class or a single method is started from the
 * IDE. That double source is what makes "Run" and "Debug" work straight from the editor.</p>
 */
public final class TestCaseContext {

    private static final ThreadLocal<TestCaseContext> CURRENT = new ThreadLocal<>();

    private static final List<String> TESTNG_PLACEHOLDER_NAMES = Arrays.asList(
            "default suite", "default test", "default test name",
            "command line suite", "command line test",
            "surefire suite", "surefire test",
            "failsafe suite", "failsafe test");

    private final String suiteName;
    private final String testName;
    private final String urs;
    private final String testCaseId;
    private final String description;
    private final String testClassName;
    private final String testMethodName;
    private final Path screenshotDirectory;
    private final Path downloadDirectory;
    private final AtomicInteger screenshotIndex = new AtomicInteger(0);
    private final Instant startedAt = Instant.now();

    private TestCaseContext(final String suiteName, final String testName, final String urs,
                            final String testCaseId, final String description,
                            final String testClassName, final String testMethodName) {
        this.suiteName = suiteName;
        this.testName = testName;
        this.urs = urs;
        this.testCaseId = testCaseId;
        this.description = description;
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.screenshotDirectory = FrameworkPaths.screenshotsRoot()
                .resolve(FileUtils.sanitize(suiteName))
                .resolve(FileUtils.sanitize(testCaseId));
        this.downloadDirectory = FrameworkPaths.downloadsRoot()
                .resolve(FileUtils.sanitize(suiteName))
                .resolve(FileUtils.sanitize(testCaseId));
    }

    /**
     * Builds the context of a test case and installs it on the current thread. Called by
     * {@code BaseTest} in its {@code @BeforeMethod}; test code never calls it.
     *
     * @param context TestNG context of the running {@code <test>} tag, {@code null} when unknown
     * @param method  the test method that is about to run
     * @return the freshly created context
     */
    public static TestCaseContext start(final ITestContext context, final Method method) {
        FrameworkConfig.captureXmlParameters(context);

        final TestCaseInfo info = findAnnotation(method);
        final String xmlSuiteName = context == null || context.getSuite() == null
                ? null : context.getSuite().getName();
        final String xmlTestName = context == null ? null : context.getName();

        final String urs = resolveUrs(info, xmlTestName);
        final String testCaseId = resolveTestCaseId(info, xmlTestName, method);
        final String description = resolveDescription(info, xmlTestName, method);
        final String testName = isUsable(xmlTestName)
                ? xmlTestName.trim()
                : join(urs, testCaseId, description);
        final String suiteName = resolveSuiteName(info, xmlSuiteName, urs, description);

        final TestCaseContext testCase = new TestCaseContext(suiteName, testName, urs, testCaseId,
                description, method.getDeclaringClass().getName(), method.getName());
        CURRENT.set(testCase);

        FrameworkPaths.ensureDirectory(testCase.screenshotDirectory);
        if (FrameworkConfig.cleanScreenshotsBeforeTestCase()) {
            FileUtils.cleanDirectory(testCase.screenshotDirectory);
        }
        // the download folder is always emptied so that a test case only ever sees its own files
        FileUtils.cleanDirectory(testCase.downloadDirectory);

        Log.info("===== {} | {} =====", suiteName, testName);
        Log.info("Screenshots : {}", testCase.screenshotDirectory);
        Log.info("Downloads   : {}", testCase.downloadDirectory);
        return testCase;
    }

    /**
     * @return the context of the test case running on this thread
     * @throws IllegalStateException when no test case has been started, which means the test class
     *                               does not extend {@code BaseTest}
     */
    public static TestCaseContext current() {
        final TestCaseContext testCase = CURRENT.get();
        if (testCase == null) {
            throw new IllegalStateException(
                    "No test case context on this thread. Does the test class extend tests.BaseTest?");
        }
        return testCase;
    }

    /**
     * @return the context of the running test case, or an empty optional when there is none;
     *         used by the listeners, which also run outside of a test method
     */
    public static Optional<TestCaseContext> currentOrEmpty() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * Removes the context from the current thread, called once the test case is over.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * @return the test suite name, i.e. {@code SP0308_3.2.1.1_Login functionality}
     */
    public String getSuiteName() {
        return suiteName;
    }

    /**
     * @return the test case name, i.e. {@code 3.2.1.1_TC001_Login with valid credentials}
     */
    public String getTestName() {
        return testName;
    }

    /**
     * @return the URS reference of the test case, i.e. {@code 3.2.1.1}
     */
    public String getUrs() {
        return urs;
    }

    /**
     * @return the test case identifier, i.e. {@code TC001}
     */
    public String getTestCaseId() {
        return testCaseId;
    }

    /**
     * @return the short description of the test case
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the fully qualified name of the test class
     */
    public String getTestClassName() {
        return testClassName;
    }

    /**
     * @return the name of the test method
     */
    public String getTestMethodName() {
        return testMethodName;
    }

    /**
     * @return {@code screenshots/<suite name>/<testcase id>}
     */
    public Path getScreenshotDirectory() {
        return screenshotDirectory;
    }

    /**
     * @return {@code downloads/<suite name>/<testcase id>}
     */
    public Path getDownloadDirectory() {
        return downloadDirectory;
    }

    /**
     * @return the instant at which the test case started
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Reserves the next screenshot number of this test case. The counter restarts at 1 for every
     * test case.
     *
     * @return the next index, formatted by the caller as {@code SS001}, {@code SS002}, ...
     */
    public int nextScreenshotIndex() {
        return screenshotIndex.incrementAndGet();
    }

    /**
     * @return the prefix shared by every screenshot of this test case, {@code <URS>_<testcase ID>}
     */
    public String screenshotPrefix() {
        return FileUtils.sanitize(urs) + "_" + FileUtils.sanitize(testCaseId);
    }

    private static TestCaseInfo findAnnotation(final Method method) {
        final TestCaseInfo onMethod = method.getAnnotation(TestCaseInfo.class);
        return onMethod != null ? onMethod : method.getDeclaringClass().getAnnotation(TestCaseInfo.class);
    }

    private static String resolveUrs(final TestCaseInfo info, final String xmlTestName) {
        if (info != null && !info.urs().trim().isEmpty()) {
            return info.urs().trim();
        }
        final String[] parts = split(xmlTestName);
        return parts.length > 0 ? parts[0] : "URS";
    }

    private static String resolveTestCaseId(final TestCaseInfo info, final String xmlTestName,
                                            final Method method) {
        if (info != null && !info.id().trim().isEmpty()) {
            return info.id().trim();
        }
        final String[] parts = split(xmlTestName);
        if (parts.length > 1) {
            return parts[1];
        }
        // last resort: the class name follows the TC001_short_description convention
        final String className = method.getDeclaringClass().getSimpleName();
        final int separator = className.indexOf('_');
        return separator > 0 ? className.substring(0, separator) : className;
    }

    private static String resolveDescription(final TestCaseInfo info, final String xmlTestName,
                                             final Method method) {
        if (info != null && !info.description().trim().isEmpty()) {
            return info.description().trim();
        }
        final String[] parts = split(xmlTestName);
        if (parts.length > 2) {
            return parts[2];
        }
        final String className = method.getDeclaringClass().getSimpleName();
        final int separator = className.indexOf('_');
        return separator > 0 ? className.substring(separator + 1).replace('_', ' ') : className;
    }

    private static String resolveSuiteName(final TestCaseInfo info, final String xmlSuiteName,
                                           final String urs, final String description) {
        if (isUsable(xmlSuiteName)) {
            return xmlSuiteName.trim();
        }
        if (info != null && !info.suite().trim().isEmpty()) {
            return info.suite().trim();
        }
        return join(FrameworkConfig.projectId(), urs, description);
    }

    private static String[] split(final String xmlTestName) {
        if (!isUsable(xmlTestName)) {
            return new String[0];
        }
        return xmlTestName.trim().split("_", 3);
    }

    private static boolean isUsable(final String name) {
        return name != null
                && !name.trim().isEmpty()
                && !TESTNG_PLACEHOLDER_NAMES.contains(name.trim().toLowerCase());
    }

    private static String join(final String... parts) {
        final StringBuilder joined = new StringBuilder();
        for (final String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                if (joined.length() > 0) {
                    joined.append('_');
                }
                joined.append(part.trim());
            }
        }
        return joined.toString();
    }

    @Override
    public String toString() {
        return suiteName + " / " + testName;
    }
}
