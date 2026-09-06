package framework.config;

import org.testng.ITestContext;
import org.testng.xml.XmlTest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read-only view over every setting the framework needs.
 *
 * <p>A value is resolved with the following precedence, the first non blank one wins:</p>
 * <ol>
 *     <li>JVM system property, i.e. {@code -Dbrowser=firefox} on the command line;</li>
 *     <li>TestNG xml parameter, i.e. {@code <parameter name="browser" value="firefox"/>};</li>
 *     <li>the environment file {@code src/test/resources/environments/<env>.properties};</li>
 *     <li>the hard coded default of the getter.</li>
 * </ol>
 *
 * <p>That order is what allows the very same suite to be launched by Maven (values come from the
 * command line) and from the IDE (values come from the xml or from the environment file).</p>
 */
public final class FrameworkConfig {

    /** Default environment used when {@code -Denv} / {@code -P<profile>} is not supplied. */
    public static final String DEFAULT_ENVIRONMENT = "TEST";
    /** Default user file used when {@code -Duser} is not supplied. */
    public static final String DEFAULT_USER = "User1";

    private static final Map<String, String> XML_PARAMETERS = new ConcurrentHashMap<>();

    private FrameworkConfig() {
    }

    /**
     * Captures the parameters declared in the TestNG xml so they can take part in the lookup.
     * Invoked by the framework listeners; tests never need to call it.
     *
     * @param context TestNG context of the currently running {@code <test>} tag, may be {@code null}
     */
    public static void captureXmlParameters(final ITestContext context) {
        if (context == null) {
            return;
        }
        final Map<String, String> parameters = new LinkedHashMap<>();
        if (context.getSuite() != null && context.getSuite().getXmlSuite() != null) {
            parameters.putAll(context.getSuite().getXmlSuite().getParameters());
        }
        final XmlTest xmlTest = context.getCurrentXmlTest();
        if (xmlTest != null) {
            parameters.putAll(xmlTest.getAllParameters());
        }
        parameters.forEach((key, value) -> {
            if (value != null) {
                XML_PARAMETERS.put(key, value);
            }
        });
    }

    /**
     * @return an unmodifiable snapshot of the TestNG xml parameters captured so far
     */
    public static Map<String, String> xmlParameters() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(XML_PARAMETERS));
    }

    /**
     * @return the active environment name, i.e. {@code TEST}, {@code UAT}, ...
     */
    public static String environment() {
        final String fromCommandLine = System.getProperty("env");
        return isBlank(fromCommandLine) ? DEFAULT_ENVIRONMENT : fromCommandLine.trim();
    }

    /**
     * @return the name of the user file to load, i.e. {@code User1} for {@code users/User1.properties}
     */
    public static String userName() {
        final String fromCommandLine = System.getProperty("user");
        if (!isBlank(fromCommandLine)) {
            return fromCommandLine.trim();
        }
        final String fromXml = XML_PARAMETERS.get("user");
        if (!isBlank(fromXml)) {
            return fromXml.trim();
        }
        return get("default.user", DEFAULT_USER);
    }

    /**
     * @return the properties of the active environment file
     */
    public static Properties environmentProperties() {
        return ConfigLoader.load("environments/" + environment() + ".properties");
    }

    /**
     * Resolves a single setting following the documented precedence.
     *
     * @param key          property key, also used as the TestNG xml parameter name
     * @param defaultValue value returned when nothing else is configured
     * @return the resolved value, never {@code null} unless {@code defaultValue} is {@code null}
     */
    public static String get(final String key, final String defaultValue) {
        final String fromCommandLine = System.getProperty(key);
        if (!isBlank(fromCommandLine)) {
            return fromCommandLine.trim();
        }
        final String fromXml = XML_PARAMETERS.get(key);
        if (!isBlank(fromXml)) {
            return fromXml.trim();
        }
        final String fromEnvironmentFile = environmentProperties().getProperty(key);
        if (!isBlank(fromEnvironmentFile)) {
            return fromEnvironmentFile.trim();
        }
        return defaultValue;
    }

    /**
     * Resolves a setting that is known under more than one name, i.e. {@code headless} in the
     * environment file and {@code isHeadless} in the TestNG xml.
     *
     * @param defaultValue value returned when none of the aliases is configured
     * @param keys         the accepted names, tried from left to right
     * @return the first value found, or {@code defaultValue}
     */
    public static String getAny(final String defaultValue, final String... keys) {
        for (final String key : keys) {
            final String value = get(key, null);
            if (!isBlank(value)) {
                return value;
            }
        }
        return defaultValue;
    }

    /**
     * @param key          property key
     * @param defaultValue value returned when nothing is configured or the value is not a boolean
     * @return the resolved boolean value
     */
    public static boolean getBoolean(final String key, final boolean defaultValue) {
        final String value = get(key, null);
        return isBlank(value) ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    /**
     * @param key          property key
     * @param defaultValue value returned when nothing is configured or the value is not a number
     * @return the resolved integer value
     */
    public static int getInt(final String key, final int defaultValue) {
        final String value = get(key, null);
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @return the project identifier used when a suite name has to be built from scratch, i.e. {@code SP0308}
     */
    public static String projectId() {
        return get("project.id", "SP0000");
    }

    /**
     * @return the application under test entry point declared by the environment file
     */
    public static String baseUrl() {
        final String url = getAny(null, "url", "baseUrl", "base.url");
        if (isBlank(url)) {
            throw new IllegalStateException("No 'url' declared in environments/" + environment() + ".properties");
        }
        return url;
    }

    /**
     * @return the browser to start: {@code chrome}, {@code chromium}, {@code edge},
     *         {@code firefox} or {@code webkit}
     */
    public static String browser() {
        return getAny("chrome", "browser", "browserType", "browser.type").toLowerCase();
    }

    /**
     * @return {@code true} when the browser must be started without a visible window
     */
    public static boolean headless() {
        return Boolean.parseBoolean(getAny("false", "headless", "isHeadless", "browser.headless"));
    }

    /**
     * @return the browser channel to use ({@code chrome}, {@code msedge}, ...), or {@code null}
     *         to use the Playwright bundled build
     */
    public static String browserChannel() {
        final String channel = get("browser.channel", null);
        return isBlank(channel) ? null : channel;
    }

    /**
     * @return an explicit browser binary to launch, or {@code null} to let Playwright decide
     */
    public static String browserExecutablePath() {
        final String path = get("browser.executable.path", null);
        return isBlank(path) ? null : path;
    }

    /**
     * @return the default Playwright timeout in milliseconds (environment key {@code timeout},
     *         expressed in seconds)
     */
    public static int defaultTimeoutMs() {
        return getInt("timeout", 30) * 1000;
    }

    /**
     * @return the viewport width used for every new browser context
     */
    public static int viewportWidth() {
        return getInt("viewport.width", 1920);
    }

    /**
     * @return the viewport height used for every new browser context
     */
    public static int viewportHeight() {
        return getInt("viewport.height", 1080);
    }

    /**
     * @return {@code true} when self signed certificates must be accepted
     */
    public static boolean ignoreHttpsErrors() {
        return getBoolean("ignore.https.errors", true);
    }

    /**
     * @return {@code true} when a Playwright trace must be recorded for every test case
     */
    public static boolean tracingEnabled() {
        return getBoolean("tracing.enabled", false);
    }

    /**
     * @return {@code true} when the screenshot folder of a test case is emptied before it starts
     */
    public static boolean cleanScreenshotsBeforeTestCase() {
        return getBoolean("screenshots.clean.before.testcase", true);
    }

    /**
     * @return {@code true} when a screenshot must be attached automatically to a failing step
     */
    public static boolean screenshotOnFailure() {
        return getBoolean("screenshot.on.failure", true);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}
