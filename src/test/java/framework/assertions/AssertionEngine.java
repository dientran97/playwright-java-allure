package framework.assertions;

import framework.logging.Log;
import framework.logging.LogLevel;
import framework.report.ScreenshotManager;
import io.qameta.allure.Allure;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shared engine behind {@link CustomAssertions} (which throws immediately) and
 * {@link SoftAssertions} (which collects the failures until {@code assertAll()}).
 *
 * <p>Whatever the flavour, a verification writes its outcome inside its own step in the report, so
 * the html shows the same story as the console. A failure adds an "Expected / Actual" attachment
 * and the automatic "Screenshot at the fail step" picture when a browser is still open.</p>
 */
final class AssertionEngine {

    private AssertionEngine() {
    }

    /**
     * Runs one verification.
     *
     * @param description what is being verified, as shown in the report
     * @param expected    the expected value, only used to build the evidence
     * @param actual      supplier of the actual value; an exception thrown here is a failure
     * @param predicate   comparison between the actual value and the expected one
     * @param onFailure   what to do with the resulting error: throw it or collect it
     * @param <T>         type of the compared values
     * @return {@code true} when the verification passed
     */
    static <T> boolean verify(final String description,
                              final Object expected,
                              final Supplier<T> actual,
                              final java.util.function.Predicate<T> predicate,
                              final Consumer<AssertionError> onFailure) {
        Object actualValue = null;
        boolean passed;
        String problem = null;
        try {
            actualValue = actual.get();
            @SuppressWarnings("unchecked") final T typed = (T) actualValue;
            passed = predicate.test(typed);
        } catch (RuntimeException e) {
            passed = false;
            problem = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        if (passed) {
            Log.step(LogLevel.PASS, "PASSED - " + description);
            return true;
        }

        final String message = buildMessage(description, expected, actualValue, problem);
        final AssertionError error = new AssertionError(message);
        // the full expected/actual goes to the console and to the attachment, the report step
        // itself gets a single readable line
        Log.fail("VERIFY FAILED - {}", message);
        Log.step(LogLevel.FAIL, "FAILED - " + description);
        Allure.addAttachment("Verification failed - " + description, "text/plain", message, ".txt");
        ScreenshotManager.captureFailure(error);
        onFailure.accept(error);
        return false;
    }

    /**
     * Runs a verification that has no expected value, i.e. a plain boolean condition.
     *
     * @param description what is being verified
     * @param condition   supplier of the condition; an exception thrown here is a failure
     * @param onFailure   what to do with the resulting error
     * @return {@code true} when the verification passed
     */
    static boolean verifyCondition(final String description,
                                   final Supplier<Boolean> condition,
                                   final Consumer<AssertionError> onFailure) {
        return verify(description, Boolean.TRUE, condition, Boolean.TRUE::equals, onFailure);
    }

    private static String buildMessage(final String description, final Object expected,
                                       final Object actual, final String problem) {
        final StringBuilder message = new StringBuilder(description);
        if (problem != null) {
            message.append(System.lineSeparator()).append("  error    : ").append(problem);
        } else {
            message.append(System.lineSeparator()).append("  expected : ").append(expected)
                    .append(System.lineSeparator()).append("  actual   : ").append(actual);
        }
        return message.toString();
    }
}
