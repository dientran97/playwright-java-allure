package framework.logging;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging entry point of the framework.
 *
 * <p>Every call writes a line on the console <em>and</em> raises the level of the Allure step that
 * is currently open, so a step in which a warning was logged is rendered as
 * {@code 10:15:32.984  WARN  Click "Login Button"} instead of {@code ... INFO ...}.</p>
 */
public final class Log {

    private static final Logger LOGGER = LoggerFactory.getLogger("Test");

    private Log() {
    }

    /**
     * Logs a debug message.
     *
     * @param message message, may contain {@code {}} placeholders
     * @param args    values for the placeholders
     */
    public static void debug(final String message, final Object... args) {
        LOGGER.debug(message, args);
        StepTracker.raise(LogLevel.DEBUG);
    }

    /**
     * Logs an informational message.
     *
     * @param message message, may contain {@code {}} placeholders
     * @param args    values for the placeholders
     */
    public static void info(final String message, final Object... args) {
        LOGGER.info(message, args);
        StepTracker.raise(LogLevel.INFO);
    }

    /**
     * Logs a successful verification.
     *
     * @param message message, may contain {@code {}} placeholders
     * @param args    values for the placeholders
     */
    public static void pass(final String message, final Object... args) {
        LOGGER.info(message, args);
        StepTracker.raise(LogLevel.PASS);
    }

    /**
     * Logs a warning and marks the enclosing step as {@code WARN}.
     *
     * @param message message, may contain {@code {}} placeholders
     * @param args    values for the placeholders
     */
    public static void warn(final String message, final Object... args) {
        LOGGER.warn(message, args);
        StepTracker.raise(LogLevel.WARN);
    }

    /**
     * Logs an error and marks the enclosing step as {@code ERROR}.
     *
     * @param message message, may contain {@code {}} placeholders
     * @param args    values for the placeholders
     */
    public static void error(final String message, final Object... args) {
        LOGGER.error(message, args);
        StepTracker.raise(LogLevel.ERROR);
    }

    /**
     * Logs a failed verification and marks the enclosing step as {@code FAIL}.
     *
     * @param message message, may contain {@code {}} placeholders
     * @param args    values for the placeholders
     */
    public static void fail(final String message, final Object... args) {
        LOGGER.error(message, args);
        StepTracker.raise(LogLevel.FAIL);
    }

    /**
     * Writes a message on the console and adds it to the Allure report as a step of its own,
     * formatted exactly like every other step.
     *
     * @param level   severity shown in the step name
     * @param message message, already formatted
     */
    public static void step(final LogLevel level, final String message) {
        switch (level) {
            case DEBUG:
                LOGGER.debug(message);
                break;
            case WARN:
                LOGGER.warn(message);
                break;
            case ERROR:
            case FAIL:
                LOGGER.error(message);
                break;
            default:
                LOGGER.info(message);
                break;
        }
        final Status status = level.getRank() >= LogLevel.ERROR.getRank() ? Status.FAILED : Status.PASSED;
        StepTracker.setPendingLevel(level);
        Allure.step(message, status);
    }
}
