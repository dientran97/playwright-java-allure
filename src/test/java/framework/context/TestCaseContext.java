package framework.context;

import framework.config.FrameworkConfig;
import framework.config.FrameworkPaths;
import framework.logging.Log;
import framework.utils.FileUtils;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything the framework needs to know about the test case that is currently running:
 * its suite name, its URS, its test case id, where its screenshots and downloads go and how many
 * screenshots it has produced so far.
 *
 * <p>Nothing is declared twice. Two names carry it all:</p>
 * <ul>
 *     <li>{@code @Test(testName = "<URS>_<testcase ID>_<short description>")} on the test method
 *         gives the URS, the test case id and the description. It is an annotation of the java
 *         class, so it is there whether the run is driven by a TestNG xml or started from the IDE,
 *         which is what makes "Run" and "Debug" work straight from the editor;</li>
 *     <li>{@code <suite name="<projectID>_<URS>_<short description>">} of the TestNG xml gives the
 *         test suite name. An IDE run has no xml, so the suite name falls back to the
 *         {@code suite.name} setting and then to {@code <projectID>_<URS>_<description>}.</li>
 * </ul>
 */
public final class TestCaseContext {

    private static final ThreadLocal<TestCaseContext> CURRENT = new ThreadLocal<>();

    /**
     * Splits {@code <URS>_<testcase ID>_<short description>}. The URS itself may be made of
     * several references joined by underscores ({@code 3.2.1.2_3.2.1.3_3.2.1.12}), so the test case
     * id - the first token that is letters followed by digits, i.e. {@code TC001} - is what marks
     * the boundary between the URS and the description.
     */
    private static final Pattern TEST_NAME_PATTERN =
            Pattern.compile("^(?<urs>.+?)_(?<id>[A-Za-z]{1,10}\\d+)(?:_(?<description>.*))?$");

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

        final String xmlSuiteName = context == null || context.getSuite() == null
                ? null : context.getSuite().getName();
        final String testName = resolveTestName(method, context);

        final Matcher parsed = TEST_NAME_PATTERN.matcher(testName);
        final boolean matches = parsed.matches();
        final String urs = matches ? parsed.group("urs") : "";
        final String testCaseId = matches ? parsed.group("id") : fallbackTestCaseId(method);
        final String description = matches ? nullToEmpty(parsed.group("description")) : testName;
        if (!matches) {
            Log.warn("'{}' does not follow <URS>_<testcase ID>_<short description>, "
                    + "the test case id falls back to '{}'", testName, testCaseId);
        }
        final String suiteName = resolveSuiteName(xmlSuiteName, urs, description);

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
        return FileUtils.sanitize(join(urs, testCaseId));
    }

    /**
     * Finds the {@code <URS>_<testcase ID>_<short description>} name of the test case.
     *
     * @param method  the test method about to run
     * @param context TestNG context of the running {@code <test>} tag, {@code null} when unknown
     * @return {@code @Test(testName = ...)} of the method, then of the class, then the
     *         {@code <test name>} of the xml, and finally the class name
     */
    private static String resolveTestName(final Method method, final ITestContext context) {
        final Test onMethod = method.getAnnotation(Test.class);
        if (onMethod != null && !onMethod.testName().trim().isEmpty()) {
            return onMethod.testName().trim();
        }
        final Test onClass = method.getDeclaringClass().getAnnotation(Test.class);
        if (onClass != null && !onClass.testName().trim().isEmpty()) {
            return onClass.testName().trim();
        }
        final String xmlTestName = context == null ? null : context.getName();
        if (isUsable(xmlTestName)) {
            return xmlTestName.trim();
        }
        return method.getDeclaringClass().getSimpleName();
    }

    /**
     * Builds the test suite name.
     *
     * @param xmlSuiteName the {@code <suite name>} of the TestNG xml, {@code null} on an IDE run
     * @param urs          the URS parsed from the test name
     * @param description  the description parsed from the test name
     * @return the xml suite name, then the {@code suite.name} setting, and finally
     *         {@code <projectID>_<URS>_<description>}
     */
    private static String resolveSuiteName(final String xmlSuiteName, final String urs,
                                           final String description) {
        if (isUsable(xmlSuiteName)) {
            return xmlSuiteName.trim();
        }
        final String configured = FrameworkConfig.get("suite.name", null);
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }
        return join(FrameworkConfig.projectId(), urs, description);
    }

    private static String fallbackTestCaseId(final Method method) {
        final String className = method.getDeclaringClass().getSimpleName();
        final int separator = className.indexOf('_');
        return separator > 0 ? className.substring(0, separator) : className;
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
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
